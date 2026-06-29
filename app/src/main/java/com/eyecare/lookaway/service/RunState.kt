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

    // ---- Mindful-usage nudge bookkeeping (resets each day) ----
    private const val KEY_USAGE_DAY = "usage_day"
    private const val KEY_USAGE_NEXT = "usage_next"

    /** Next screen-time threshold (minutes) at which to nudge today. */
    fun usageNudgeThreshold(context: Context, default: Int): Int {
        val p = prefs(context)
        return if (p.getInt(KEY_USAGE_DAY, -1) != dayKey()) default
        else p.getInt(KEY_USAGE_NEXT, default)
    }

    fun setUsageNudgeThreshold(context: Context, minutes: Int) {
        prefs(context).edit()
            .putInt(KEY_USAGE_DAY, dayKey())
            .putInt(KEY_USAGE_NEXT, minutes)
            .apply()
    }

    private fun dayKey(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.YEAR) * 1000 + c.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
