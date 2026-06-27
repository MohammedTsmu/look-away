package com.eyecare.lookaway.service

import android.content.Context

/**
 * A tiny synchronous flag for "the user wants reminders running". Backed by
 * SharedPreferences so receivers and the service can read it without
 * suspending. The reactive [Settings] live in DataStore; this is just the
 * on/off switch the OS needs to consult on cold restarts and boot.
 */
object RunState {
    private const val PREFS = "run_state"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_RESUME_AT = "resume_at"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Epoch millis when a timed pause should auto-resume, or 0 if none. */
    fun resumeAt(context: Context): Long =
        prefs(context).getLong(KEY_RESUME_AT, 0L)

    fun setResumeAt(context: Context, epochMillis: Long) {
        prefs(context).edit().putLong(KEY_RESUME_AT, epochMillis).apply()
    }

    fun clearResumeAt(context: Context) {
        prefs(context).edit().remove(KEY_RESUME_AT).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
