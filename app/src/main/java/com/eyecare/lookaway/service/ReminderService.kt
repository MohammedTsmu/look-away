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

        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything()
                return START_NOT_STICKY
            }
            ACTION_PAUSE -> ReminderEngine.setPaused(true)
            ACTION_RESUME -> { ReminderEngine.setPaused(false); onManualResume() }
            ACTION_TOGGLE -> { ReminderEngine.togglePause(); if (!ReminderEngine.state.value.paused) onManualResume() }
            ACTION_SKIP -> ReminderEngine.endBreak()
            ACTION_BREAK_NOW -> ReminderEngine.breakNow()
            ACTION_START, null -> ensureRunning()
        }
        ExternalControls.refreshAll(this)
        return START_STICKY
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

    private fun stopEverything() {
        val settings = ReminderEngine.settings
        ReminderEngine.stop()
        NotificationManagerCompat.from(this).cancel(Notifications.ID_BREAK)
        MediaPauser.forget(this)
        // So a turned-off app doesn't stay forgotten, schedule a gentle nudge.
        if (settings.remindWhenOff) {
            ReminderScheduler.scheduleOffReminder(this, settings.remindWhenOffHours)
        }
        started = false
        ExternalControls.refreshAll(this)
        stopForegroundCompat()
        stopSelf()
    }

    // ---- Engine wiring -------------------------------------------------------

    private fun wireEngine() {
        ReminderEngine.onShowBreak = { onBreakStarted() }
        ReminderEngine.onBreakEnd = { onBreakEnded() }
        ReminderEngine.onTick = {
            refreshStatusNotification()
            ExternalControls.refreshWidget(this)
        }
    }

    private fun observeSettings() {
        scope.launch {
            repo.flow.collect { ReminderEngine.settings = it }
        }
    }

    private fun observeState() {
        scope.launch {
            ReminderEngine.state.collect { state ->
                if (state.phase == Phase.IDLE && started) {
                    // Engine stopped from elsewhere.
                    stopEverything()
                } else {
                    refreshStatusNotification()
                }
            }
        }
    }

    private fun onBreakStarted() {
        val s = ReminderEngine.settings
        if (s.pauseMediaOnBreak) MediaPauser.pauseActive(this)
        if (s.fullScreenBreak) {
            postBreakNotification(fullScreen = true)
            launchBreakActivityIfPossible()
            // BreakActivity owns sound/vibration so it can sync with its UI.
        } else {
            postBreakNotification(fullScreen = false)
            Feedback.playBreakStart(this, s.sound, s.vibrate, s.soundUri)
        }
    }

    private fun onBreakEnded() {
        NotificationManagerCompat.from(this).cancel(Notifications.ID_BREAK)
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
        if (!started || !canPostNotifications()) return
        NotificationManagerCompat.from(this)
            .notify(Notifications.ID_STATUS, buildStatusNotification())
    }

    private fun buildStatusNotification(): Notification {
        val state = ReminderEngine.state.value
        val resumeAt = RunState.resumeAt(this)
        val contentText = when {
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

        // Pause / Resume toggle.
        if (state.paused) {
            builder.addAction(0, getString(R.string.action_resume), servicePI(ACTION_RESUME, 1))
        } else {
            builder.addAction(0, getString(R.string.action_pause), servicePI(ACTION_PAUSE, 2))
        }
        builder.addAction(0, getString(R.string.action_stop), servicePI(ACTION_STOP, 3))

        return builder.build()
    }

    @SuppressLint("MissingPermission")
    private fun postBreakNotification(fullScreen: Boolean) {
        val s = ReminderEngine.settings
        val openBreak = PendingIntent.getActivity(
            this, 10,
            Intent(this, BreakActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, Notifications.CHANNEL_BREAK)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(getString(R.string.notif_break_title))
            .setContentText(getString(R.string.notif_break_text, s.breakSeconds))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openBreak)
            .setOngoing(fullScreen)

        if (fullScreen) {
            builder.setFullScreenIntent(openBreak, true)
        } else {
            builder.addAction(0, getString(R.string.action_done), servicePI(ACTION_SKIP, 4))
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

        fun formatClock(totalSeconds: Int): String =
            com.eyecare.lookaway.util.Format.clock(totalSeconds)
    }
}
