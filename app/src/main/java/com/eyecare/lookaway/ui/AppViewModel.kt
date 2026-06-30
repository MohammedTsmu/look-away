package com.eyecare.lookaway.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.lookaway.data.Settings
import com.eyecare.lookaway.data.SettingsRepository
import com.eyecare.lookaway.data.ThemeMode
import com.eyecare.lookaway.service.ReminderEngine
import com.eyecare.lookaway.service.ReminderScheduler
import com.eyecare.lookaway.service.ReminderService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Bridges the UI to settings (DataStore) and the running reminder engine. */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<Settings> = repo.flow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = Settings(),
    )

    val engineState = ReminderEngine.state

    // ---- Reminder control ----
    fun start() = ReminderScheduler.startReminders(context())
    fun stop() = ReminderScheduler.stopReminders(context())
    fun togglePause() = ReminderScheduler.sendAction(context(), ReminderService.ACTION_TOGGLE)

    /** Pause now and auto-resume after [minutes]. */
    fun pauseForMinutes(minutes: Int) =
        ReminderScheduler.pauseFor(context(), minutes.toLong() * 60 * 1000)

    /** Pause now and auto-resume at the next [hour]:00 (default 8am). */
    fun pauseUntilMorning(hour: Int = 8) {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        ReminderScheduler.pauseFor(context(), cal.timeInMillis - System.currentTimeMillis())
    }
    fun previewBreak() {
        // Make sure something is running, then jump to a break.
        if (!ReminderEngine.state.value.isRunning) start()
        ReminderScheduler.sendAction(context(), ReminderService.ACTION_BREAK_NOW)
    }

    // ---- Settings setters ----
    fun setWorkMinutes(v: Int) = edit { setWorkMinutes(v) }
    fun setBreakSeconds(v: Int) = edit { setBreakSeconds(v) }
    fun setSound(v: Boolean) = edit { setSound(v) }
    fun setSoundUri(v: String) = edit { setSoundUri(v) }
    fun setVibrate(v: Boolean) = edit { setVibrate(v) }
    fun setFullScreen(v: Boolean) = edit { setFullScreen(v) }
    fun setStrict(v: Boolean) = edit { setStrict(v) }
    fun setPauseWhenScreenOff(v: Boolean) = edit { setPauseWhenScreenOff(v) }
    fun setPauseMedia(v: Boolean) = edit { setPauseMedia(v) }
    fun setRemindWhenOff(v: Boolean) = edit { setRemindWhenOff(v) }
    fun setRemindWhenOffHours(v: Int) = edit { setRemindWhenOffHours(v) }
    fun setMindfulEnabled(v: Boolean) = edit { setMindfulEnabled(v) }
    fun setMindfulThreshold(v: Int) = edit { setMindfulThreshold(v) }
    fun setMindfulRepeat(v: Int) = edit { setMindfulRepeat(v) }
    fun setAppLimit(pkg: String, minutes: Int) = edit { setAppLimit(pkg, minutes) }
    fun removeAppLimit(pkg: String) = edit { removeAppLimit(pkg) }

    // ---- Usage helpers ----
    suspend fun loadInstalledApps(): List<com.eyecare.lookaway.usage.InstalledApp> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.eyecare.lookaway.usage.UsageTracker.installedLaunchableApps(context())
        }

    fun appLabel(pkg: String): String =
        com.eyecare.lookaway.usage.UsageTracker.appLabel(context(), pkg)

    fun screenMinutesToday(): Int =
        com.eyecare.lookaway.usage.UsageTracker.todayForegroundMinutes(context())

    fun appMinutesToday(pkg: String): Int =
        com.eyecare.lookaway.usage.UsageTracker.appMinutesToday(context(), pkg)

    fun last7DaysMinutes(): IntArray =
        com.eyecare.lookaway.usage.UsageTracker.last7DaysMinutes(context())

    // ---- Focus session ----
    fun startFocus(minutes: Int) =
        com.eyecare.lookaway.service.RunState.setFocusUntil(
            context(), System.currentTimeMillis() + minutes.toLong() * 60 * 1000,
        )

    fun stopFocus() = com.eyecare.lookaway.service.RunState.clearFocus(context())

    fun focusUntil(): Long = com.eyecare.lookaway.service.RunState.focusUntil(context())
    fun setStartOnBoot(v: Boolean) = edit { setStartOnBoot(v) }
    fun setStartOnOpen(v: Boolean) = edit { setStartOnOpen(v) }
    fun setQuietEnabled(v: Boolean) = edit { setQuietEnabled(v) }
    fun setQuietStart(v: Int) = edit { setQuietStart(v) }
    fun setQuietEnd(v: Int) = edit { setQuietEnd(v) }
    fun setTheme(v: ThemeMode) = edit { setTheme(v) }
    fun setAccent(v: Int) = edit { setAccent(v) }

    private fun edit(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch { repo.block() }
    }

    private fun context() = getApplication<Application>().applicationContext
}
