package com.eyecare.lookaway.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.eyecare.lookaway.receiver.ScreenReceiver
import android.provider.Settings as AndroidSettings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eyecare.lookaway.R
import com.eyecare.lookaway.data.SettingsRepository
import com.eyecare.lookaway.media.MediaPauser
import com.eyecare.lookaway.overlay.BreakOverlay
import com.eyecare.lookaway.overlay.LimitOverlay
import com.eyecare.lookaway.usage.UsageTracker
import com.eyecare.lookaway.ui.BreakActivity
import com.eyecare.lookaway.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Foreground service that hosts the [ReminderEngine]. It keeps the engine alive
 * in the background, mirrors engine state into the ongoing status notification,
 * and launches the break experience when a break begins.
 */
class ReminderService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var repo: SettingsRepository
    private var started = false
    private var settingsLoaded = false
    private var monitorJob: kotlinx.coroutines.Job? = null
    private val screenReceiver = ScreenReceiver()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.eyecare.lookaway.util.LocaleManager.wrap(newBase))
    }

    override fun onCreate() {
        super.onCreate()
        repo = SettingsRepository(applicationContext)
        wireEngine()
        observeSettings()
        observeState()
        registerScreenReceiver()
        ensureMonitorLoop()
    }

    private fun ensureMonitorLoop() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (true) {
                runCatching { runMonitorTick() }
                kotlinx.coroutines.delay(if (LimitOverlay.isShowing()) 4_000L else 20_000L)
            }
        }
    }

    private fun registerScreenReceiver() {
        // Seed the current state, then listen for changes.
        val pm = getSystemService(PowerManager::class.java)
        ReminderEngine.setScreenOn(pm?.isInteractive ?: true)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Promote to foreground immediately to satisfy the 5s startForeground rule.
        startForegroundStatus()
        ensureSettingsLoaded()
        ensureMonitorLoop()

        when (intent?.action) {
            ACTION_STOP -> stopEngine()
            ACTION_PAUSE -> ReminderEngine.setPaused(true)
            ACTION_RESUME -> { ReminderEngine.setPaused(false); onManualResume() }
            ACTION_TOGGLE -> { ReminderEngine.togglePause(); if (!ReminderEngine.state.value.paused) onManualResume() }
            ACTION_SKIP -> ReminderEngine.endBreak()
            ACTION_BREAK_NOW -> ReminderEngine.breakNow()
            ACTION_START_MONITOR -> { /* keep service alive for usage monitoring only */ }
            ACTION_START, null -> if (RunState.isEnabled(this) || intent?.action == ACTION_START) ensureRunning()
        }

        // Stop the whole service only if nothing wants it anymore.
        if (!RunState.isEnabled(this) && !monitoringWanted()) {
            fullStop()
            return START_NOT_STICKY
        }
        ExternalControls.refreshAll(this)
        refreshStatusNotification()
        return START_STICKY
    }

    /** Load settings synchronously once so lifecycle decisions are accurate. */
    private fun ensureSettingsLoaded() {
        if (!settingsLoaded) {
            ReminderEngine.settings = runBlocking { repo.flow.first() }
            settingsLoaded = true
        }
    }

    private fun monitoringWanted(): Boolean {
        val s = ReminderEngine.settings
        return (s.mindfulUsageEnabled || s.appLimits.isNotEmpty()) && UsageTracker.hasAccess(this)
    }

    /** A manual resume cancels any pending timed-pause auto-resume. */
    private fun onManualResume() {
        ReminderScheduler.cancelAutoResume(this)
        RunState.clearResumeAt(this)
    }

    private fun ensureRunning() {
        if (!RunState.isEnabled(this)) RunState.setEnabled(this, true)
        if (!started) {
            val initial = runBlocking { repo.flow.first() }
            ReminderEngine.settings = initial
            ReminderEngine.start(initial)
            started = true
        }
    }

    /** Stop the 20-20-20 engine, but leave the service running if it's still
     *  wanted for usage monitoring. */
    private fun stopEngine() {
        if (!started && !ReminderEngine.state.value.isRunning) return
        val settings = ReminderEngine.settings
        ReminderEngine.stop()
        BreakOverlay.hide(this)
        NotificationManagerCompat.from(this).cancel(Notifications.ID_BREAK)
        MediaPauser.forget(this)
        if (settings.remindWhenOff) {
            ReminderScheduler.scheduleOffReminder(this, settings.remindWhenOffHours)
        }
        started = false
        ExternalControls.refreshAll(this)
    }

    private fun fullStop() {
        stopEngine()
        LimitOverlay.hide(this)
        stopForegroundCompat()
        stopSelf()
    }

    private fun evaluateLifecycle() {
        if (!RunState.isEnabled(this) && !monitoringWanted()) fullStop()
    }

    // ---- Engine wiring -------------------------------------------------------

    private fun wireEngine() {
        ReminderEngine.onShowBreak = { onBreakStarted() }
        ReminderEngine.onBreakEnd = { onBreakEnded() }
        ReminderEngine.onTick = {
            refreshStatusNotification()
            ExternalControls.refreshWidget(this)
            if (BreakOverlay.isShowing()) {
                val st = ReminderEngine.state.value
                BreakOverlay.update(st.secondsRemaining, 1f - st.progress.coerceIn(0f, 1f))
            }
        }
    }

    private fun observeSettings() {
        scope.launch {
            repo.flow.collect {
                ReminderEngine.applySettings(it)
                settingsLoaded = true
                // Disabling monitoring while reminders are off should release us.
                evaluateLifecycle()
            }
        }
    }

    private fun observeState() {
        scope.launch {
            ReminderEngine.state.collect { state ->
                if (state.phase == Phase.IDLE && started) {
                    stopEngine()
                    evaluateLifecycle()
                } else {
                    refreshStatusNotification()
                }
            }
        }
    }

    private fun onBreakStarted() {
        val s = ReminderEngine.settings
        if (s.pauseMediaOnBreak) MediaPauser.pauseActive(this)

        if (!s.fullScreenBreak) {
            postBreakNotification(fullScreen = false)
            Feedback.playBreakStart(this, s.sound, s.vibrate, s.soundUri)
            return
        }

        // Try our overlay window first — it shows even on OEMs that block
        // background activity starts (MIUI), as long as overlay access is granted.
        val overlayShown = AndroidSettings.canDrawOverlays(this) &&
            BreakOverlay.show(this, showSkip = !s.strictMode)
        if (overlayShown) {
            BreakOverlay.update(s.breakSeconds, 1f)
            // A QUIET companion notification (no heads-up) so it doesn't pop over
            // and steal focus from the overlay — that caused the overlay to flicker
            // and drop behind other apps on some devices.
            postBreakNotification(fullScreen = false, quiet = true)
        } else {
            // No overlay → rely on the full-screen-intent notification + Activity.
            postBreakNotification(fullScreen = true, quiet = false)
            launchBreakActivityIfPossible()
        }

        Feedback.playBreakStart(this, s.sound, s.vibrate, s.soundUri)
    }

    private fun onBreakEnded() {
        val s = ReminderEngine.settings
        BreakOverlay.hide(this)
        NotificationManagerCompat.from(this).cancel(Notifications.ID_BREAK)
        Feedback.playBreakEnd(this, s.sound, s.vibrate, s.soundUri)
        // Resume whatever we paused (no-op if nothing was paused).
        MediaPauser.resume(this)
    }

    private fun launchBreakActivityIfPossible() {
        // A direct activity start works when we hold the overlay permission; the
        // full-screen-intent notification is the fallback for everyone else.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || AndroidSettings.canDrawOverlays(this)) {
            runCatching {
                startActivity(
                    Intent(this, BreakActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    // ---- Notifications -------------------------------------------------------

    private fun startForegroundStatus() {
        val notification = buildStatusNotification()
        // The specialUse foreground service type is only known on Android 14+.
        // On older versions the plain overload is correct (the manifest type is
        // ignored there anyway).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                Notifications.ID_STATUS,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(Notifications.ID_STATUS, notification)
        }
    }

    /** Runs every ~20s (faster while a limit overlay is up) for usage reminders. */
    private fun runMonitorTick() {
        val s = ReminderEngine.settings
        if (!s.mindfulUsageEnabled && s.appLimits.isEmpty()) return
        if (!UsageTracker.hasAccess(this)) return
        // Focus session or screen off → stay quiet and drop any limit overlay.
        val interactive = getSystemService(PowerManager::class.java)?.isInteractive ?: true
        if (RunState.isFocusActive(this) || !interactive) {
            LimitOverlay.hide(this)
            return
        }

        // ---- Total daily screen-time nudge ----
        if (s.mindfulUsageEnabled) {
            val minutes = UsageTracker.todayForegroundMinutes(this)
            val threshold = RunState.usageNudgeThreshold(this, s.mindfulUsageThresholdMin)
            if (minutes >= threshold) {
                postUsageNudge(minutes)
                RunState.setUsageNudgeThreshold(this, minutes + s.mindfulUsageRepeatMin)
            }
        }

        // ---- Per-app limits ----
        if (s.appLimits.isEmpty()) {
            LimitOverlay.hide(this)
            return
        }
        val fg = UsageTracker.currentForegroundApp(this)
        // Drop the overlay as soon as the user leaves the over-limit app.
        if (LimitOverlay.isShowing() && LimitOverlay.showingPackage() != fg) {
            LimitOverlay.hide(this)
        }
        if (LimitOverlay.isShowing()) return

        val perApp = UsageTracker.perAppMinutesToday(this)
        val now = System.currentTimeMillis()
        val overlayOk = AndroidSettings.canDrawOverlays(this)
        for ((pkg, limit) in s.appLimits) {
            if ((perApp[pkg] ?: 0) < limit) continue
            if (now < RunState.limitMutedUntil(this, pkg)) continue
            val used = perApp[pkg] ?: 0
            if (overlayOk && fg == pkg) {
                showLimitOverlay(pkg, used, limit)
                break // one at a time
            } else if (!overlayOk) {
                // No overlay permission → fall back to a notification with a cooldown.
                postAppLimitNudge(pkg, used)
                RunState.setLimitMute(this, pkg, now + s.mindfulUsageRepeatMin.toLong() * 60_000)
            }
        }
    }

    private fun showLimitOverlay(pkg: String, usedMinutes: Int, limitMinutes: Int) {
        val label = UsageTracker.appLabel(this, pkg)
        val usedText = getString(
            R.string.limit_used_text, formatDuration(usedMinutes), formatDuration(limitMinutes),
        )
        LimitOverlay.show(
            context = this,
            packageName = pkg,
            appLabel = label,
            usedText = usedText,
            onSnooze = {
                RunState.setLimitMute(this, pkg, System.currentTimeMillis() + 5 * 60_000L)
                LimitOverlay.hide(this)
            },
            onDismiss = {
                RunState.setLimitMute(this, pkg, System.currentTimeMillis() + 15 * 60_000L)
                LimitOverlay.hide(this)
            },
        )
    }

    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    private fun postAppLimitNudge(pkg: String, usedMinutes: Int) {
        if (!canPostNotifications()) return
        val openApp = PendingIntent.getActivity(
            this, 31,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val label = UsageTracker.appLabel(this, pkg)
        val text = getString(R.string.app_limit_nudge_text, label, formatDuration(usedMinutes))
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(getString(R.string.app_limit_nudge_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        // Tag by package so multiple apps can each show their own nudge.
        NotificationManagerCompat.from(this).notify(pkg, Notifications.ID_USAGE, notification)
    }

    @SuppressLint("MissingPermission") // guarded by canPostNotifications()
    private fun postUsageNudge(minutes: Int) {
        if (!canPostNotifications()) return
        val openApp = PendingIntent.getActivity(
            this, 30,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = getString(R.string.usage_nudge_text, formatDuration(minutes))
        val notification = NotificationCompat.Builder(this, Notifications.CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(getString(R.string.usage_nudge_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(Notifications.ID_USAGE, notification)
    }

    private fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) getString(R.string.dur_h_m, h, m) else getString(R.string.dur_m, m)
    }

    private fun formatTimeOfDay(epochMillis: Long): String =
        android.text.format.DateFormat.getTimeFormat(this)
            .format(java.util.Date(epochMillis))

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    // Guarded by canPostNotifications(); lint can't see through the helper.
    @SuppressLint("MissingPermission")
    private fun refreshStatusNotification() {
        if ((!started && !monitoringWanted()) || !canPostNotifications()) return
        NotificationManagerCompat.from(this)
            .notify(Notifications.ID_STATUS, buildStatusNotification())
    }

    private fun buildStatusNotification(): Notification {
        val state = ReminderEngine.state.value
        val resumeAt = RunState.resumeAt(this)
        val monitorOnly = !state.isRunning && monitoringWanted()
        val contentText = when {
            monitorOnly -> getString(R.string.notif_monitoring)
            !state.isRunning -> getString(R.string.home_state_idle)
            state.paused && resumeAt > System.currentTimeMillis() ->
                getString(R.string.notif_paused_until, formatTimeOfDay(resumeAt))
            state.paused -> getString(R.string.notif_paused)
            state.inQuietHours -> getString(R.string.settings_quiet)
            state.phase == Phase.BREAK ->
                getString(R.string.notif_break_text, state.secondsRemaining)
            else -> getString(R.string.notif_next_break, formatClock(state.secondsRemaining))
        }

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, Notifications.CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(getString(R.string.notif_running_title))
            .setContentText(contentText)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            // Keep the ongoing status off the lock screen; it's only useful
            // while the device is unlocked and in use.
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Engine controls only make sense while the 20-20-20 reminder is running.
        if (state.isRunning) {
            if (state.paused) {
                builder.addAction(0, getString(R.string.action_resume), servicePI(ACTION_RESUME, 1))
            } else {
                builder.addAction(0, getString(R.string.action_pause), servicePI(ACTION_PAUSE, 2))
            }
            builder.addAction(0, getString(R.string.action_stop), servicePI(ACTION_STOP, 3))
        }

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun postBreakNotification(fullScreen: Boolean, quiet: Boolean = false) {
        val s = ReminderEngine.settings
        val openBreak = PendingIntent.getActivity(
            this, 10,
            Intent(this, BreakActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Quiet companion goes on the silent status channel so it never pops a
        // heads-up; the real alert uses the high-importance break channel.
        val channel = if (quiet) Notifications.CHANNEL_STATUS else Notifications.CHANNEL_BREAK
        val builder = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(getString(R.string.notif_break_title))
            .setContentText(getString(R.string.notif_break_text, s.breakSeconds))
            .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openBreak)
            .setOngoing(fullScreen || quiet)
            .addAction(0, getString(R.string.action_done), servicePI(ACTION_SKIP, 4))

        if (quiet) {
            builder.setSilent(true)
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            if (fullScreen) builder.setFullScreenIntent(openBreak, true)
        }

        if (canPostNotifications()) {
            NotificationManagerCompat.from(this)
                .notify(Notifications.ID_BREAK, builder.build())
        }
    }

    private fun servicePI(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ReminderService::class.java).setAction(action)
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { unregisterReceiver(screenReceiver) }
        LimitOverlay.hide(this)
        ReminderEngine.onShowBreak = null
        ReminderEngine.onBreakEnd = null
        ReminderEngine.onTick = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.eyecare.lookaway.START"
        const val ACTION_STOP = "com.eyecare.lookaway.STOP"
        const val ACTION_PAUSE = "com.eyecare.lookaway.PAUSE"
        const val ACTION_RESUME = "com.eyecare.lookaway.RESUME"
        const val ACTION_TOGGLE = "com.eyecare.lookaway.TOGGLE"
        const val ACTION_SKIP = "com.eyecare.lookaway.SKIP"
        const val ACTION_BREAK_NOW = "com.eyecare.lookaway.BREAK_NOW"
        const val ACTION_START_MONITOR = "com.eyecare.lookaway.START_MONITOR"

        fun formatClock(totalSeconds: Int): String =
            com.eyecare.lookaway.util.Format.clock(totalSeconds)
    }
}
