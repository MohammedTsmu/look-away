package com.eyecare.lookaway.data

/** Which theme the user has chosen. Ordinal is persisted, so order is stable. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * All user-tunable preferences. Defaults encode the classic 20-20-20 rule:
 * every 20 minutes, look ~20 feet away for 20 seconds.
 */
data class Settings(
    val workMinutes: Int = 20,
    val breakSeconds: Int = 20,
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    val fullScreenBreak: Boolean = true,
    val dimScreen: Boolean = true,
    val strictMode: Boolean = false,
    val pauseWhenScreenOff: Boolean = true,
    val pauseMediaOnBreak: Boolean = true,
    val startOnBoot: Boolean = true,
    val startOnAppOpen: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStartMinutes: Int = 22 * 60,
    val quietEndMinutes: Int = 7 * 60,
    val remindWhenOff: Boolean = true,
    val remindWhenOffHours: Int = 24,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentIndex: Int = 0,
) {
    val workSeconds: Int get() = workMinutes * 60
}
