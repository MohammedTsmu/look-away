package com.eyecare.lookaway.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Single source of truth for [Settings], backed by Jetpack DataStore.
 * Exposes a reactive [flow] plus granular updaters used by the settings UI.
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val workMinutes = intPreferencesKey("work_minutes")
        val breakSeconds = intPreferencesKey("break_seconds")
        val sound = booleanPreferencesKey("sound")
        val soundUri = androidx.datastore.preferences.core.stringPreferencesKey("sound_uri")
        val vibrate = booleanPreferencesKey("vibrate")
        val fullScreen = booleanPreferencesKey("full_screen")
        val strict = booleanPreferencesKey("strict_mode")
        val pauseScreenOff = booleanPreferencesKey("pause_screen_off")
        val pauseMedia = booleanPreferencesKey("pause_media")
        val startOnBoot = booleanPreferencesKey("start_on_boot")
        val startOnOpen = booleanPreferencesKey("start_on_open")
        val quietEnabled = booleanPreferencesKey("quiet_enabled")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val remindOff = booleanPreferencesKey("remind_off")
        val remindOffHours = intPreferencesKey("remind_off_hours")
        val mindfulEnabled = booleanPreferencesKey("mindful_enabled")
        val mindfulThreshold = intPreferencesKey("mindful_threshold")
        val mindfulRepeat = intPreferencesKey("mindful_repeat")
        val theme = intPreferencesKey("theme_mode")
        val accent = intPreferencesKey("accent_index")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            workMinutes = this[Keys.workMinutes] ?: d.workMinutes,
            breakSeconds = this[Keys.breakSeconds] ?: d.breakSeconds,
            sound = this[Keys.sound] ?: d.sound,
            soundUri = this[Keys.soundUri] ?: d.soundUri,
            vibrate = this[Keys.vibrate] ?: d.vibrate,
            fullScreenBreak = this[Keys.fullScreen] ?: d.fullScreenBreak,
            strictMode = this[Keys.strict] ?: d.strictMode,
            pauseWhenScreenOff = this[Keys.pauseScreenOff] ?: d.pauseWhenScreenOff,
            pauseMediaOnBreak = this[Keys.pauseMedia] ?: d.pauseMediaOnBreak,
            startOnBoot = this[Keys.startOnBoot] ?: d.startOnBoot,
            startOnAppOpen = this[Keys.startOnOpen] ?: d.startOnAppOpen,
            quietHoursEnabled = this[Keys.quietEnabled] ?: d.quietHoursEnabled,
            quietStartMinutes = this[Keys.quietStart] ?: d.quietStartMinutes,
            quietEndMinutes = this[Keys.quietEnd] ?: d.quietEndMinutes,
            remindWhenOff = this[Keys.remindOff] ?: d.remindWhenOff,
            remindWhenOffHours = this[Keys.remindOffHours] ?: d.remindWhenOffHours,
            mindfulUsageEnabled = this[Keys.mindfulEnabled] ?: d.mindfulUsageEnabled,
            mindfulUsageThresholdMin = this[Keys.mindfulThreshold] ?: d.mindfulUsageThresholdMin,
            mindfulUsageRepeatMin = this[Keys.mindfulRepeat] ?: d.mindfulUsageRepeatMin,
            themeMode = ThemeMode.entries.getOrElse(this[Keys.theme] ?: 0) { ThemeMode.SYSTEM },
            accentIndex = this[Keys.accent] ?: d.accentIndex,
        )
    }

    suspend fun setWorkMinutes(v: Int) = edit { it[Keys.workMinutes] = v.coerceIn(1, 180) }
    suspend fun setBreakSeconds(v: Int) = edit { it[Keys.breakSeconds] = v.coerceIn(5, 600) }
    suspend fun setSound(v: Boolean) = edit { it[Keys.sound] = v }
    suspend fun setSoundUri(v: String) = edit { it[Keys.soundUri] = v }
    suspend fun setVibrate(v: Boolean) = edit { it[Keys.vibrate] = v }
    suspend fun setFullScreen(v: Boolean) = edit { it[Keys.fullScreen] = v }
    suspend fun setStrict(v: Boolean) = edit { it[Keys.strict] = v }
    suspend fun setPauseWhenScreenOff(v: Boolean) = edit { it[Keys.pauseScreenOff] = v }
    suspend fun setPauseMedia(v: Boolean) = edit { it[Keys.pauseMedia] = v }
    suspend fun setStartOnBoot(v: Boolean) = edit { it[Keys.startOnBoot] = v }
    suspend fun setStartOnOpen(v: Boolean) = edit { it[Keys.startOnOpen] = v }
    suspend fun setQuietEnabled(v: Boolean) = edit { it[Keys.quietEnabled] = v }
    suspend fun setQuietStart(v: Int) = edit { it[Keys.quietStart] = v.coerceIn(0, 1439) }
    suspend fun setQuietEnd(v: Int) = edit { it[Keys.quietEnd] = v.coerceIn(0, 1439) }
    suspend fun setRemindWhenOff(v: Boolean) = edit { it[Keys.remindOff] = v }
    suspend fun setRemindWhenOffHours(v: Int) = edit { it[Keys.remindOffHours] = v.coerceIn(1, 168) }
    suspend fun setMindfulEnabled(v: Boolean) = edit { it[Keys.mindfulEnabled] = v }
    suspend fun setMindfulThreshold(v: Int) = edit { it[Keys.mindfulThreshold] = v.coerceIn(15, 600) }
    suspend fun setMindfulRepeat(v: Int) = edit { it[Keys.mindfulRepeat] = v.coerceIn(10, 240) }
    suspend fun setTheme(v: ThemeMode) = edit { it[Keys.theme] = v.ordinal }
    suspend fun setAccent(v: Int) = edit { it[Keys.accent] = v }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
