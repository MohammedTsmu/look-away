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

    // Per-app limit nudges already shown today (own day key, independent of total).
    private const val KEY_APP_DAY = "app_nudge_day"
    private const val KEY_APP_NUDGED = "app_nudged"

    fun appNudgedToday(context: Context): Set<String> {
        val p = prefs(context)
        return if (p.getInt(KEY_APP_DAY, -1) != dayKey()) emptySet()
        else p.getStringSet(KEY_APP_NUDGED, emptySet()) ?: emptySet()
    }

    fun markAppNudged(context: Context, pkg: String) {
        val p = prefs(context)
        val today = dayKey()
        val current = if (p.getInt(KEY_APP_DAY, -1) == today) {
            p.getStringSet(KEY_APP_NUDGED, emptySet()) ?: emptySet()
        } else emptySet()
        p.edit().putInt(KEY_APP_DAY, today).putStringSet(KEY_APP_NUDGED, current + pkg).apply()
    }

    // ---- Focus session: mutes usage/app nudges until this epoch ----
    private const val KEY_FOCUS_UNTIL = "focus_until"

    fun focusUntil(context: Context): Long = prefs(context).getLong(KEY_FOCUS_UNTIL, 0L)

    fun isFocusActive(context: Context): Boolean =
        focusUntil(context) > System.currentTimeMillis()

    fun setFocusUntil(context: Context, epochMillis: Long) {
        prefs(context).edit().putLong(KEY_FOCUS_UNTIL, epochMillis).apply()
    }

    fun clearFocus(context: Context) {
        prefs(context).edit().remove(KEY_FOCUS_UNTIL).apply()
    }

    // Per-app limit reminder cooldown: don't remind again for this app until epoch.
    private const val KEY_LIMIT_MUTE = "limit_mute"

    fun limitMutedUntil(context: Context, pkg: String): Long {
        val set = prefs(context).getStringSet(KEY_LIMIT_MUTE, emptySet()) ?: emptySet()
        val entry = set.firstOrNull { it.substringBeforeLast('=') == pkg } ?: return 0L
        return entry.substringAfterLast('=').toLongOrNull() ?: 0L
    }

    fun setLimitMute(context: Context, pkg: String, untilEpoch: Long) {
        val p = prefs(context)
        val set = p.getStringSet(KEY_LIMIT_MUTE, emptySet()) ?: emptySet()
        val without = set.filterNot { it.substringBeforeLast('=') == pkg }.toMutableSet()
        without.add("$pkg=$untilEpoch")
        p.edit().putStringSet(KEY_LIMIT_MUTE, without).apply()
    }

    // How many times an app's limit reminder was dismissed today (for escalation).
    private const val KEY_DISMISS_DAY = "limit_dismiss_day"
    private const val KEY_DISMISS_COUNTS = "limit_dismiss_counts"

    fun limitDismissCount(context: Context, pkg: String): Int {
        val p = prefs(context)
        if (p.getInt(KEY_DISMISS_DAY, -1) != dayKey()) return 0
        val set = p.getStringSet(KEY_DISMISS_COUNTS, emptySet()) ?: emptySet()
        return set.firstOrNull { it.substringBeforeLast('=') == pkg }
            ?.substringAfterLast('=')?.toIntOrNull() ?: 0
    }

    fun incLimitDismissCount(context: Context, pkg: String) {
        val p = prefs(context)
        val today = dayKey()
        val set = if (p.getInt(KEY_DISMISS_DAY, -1) == today) {
            p.getStringSet(KEY_DISMISS_COUNTS, emptySet()) ?: emptySet()
        } else emptySet()
        val current = set.firstOrNull { it.substringBeforeLast('=') == pkg }
            ?.substringAfterLast('=')?.toIntOrNull() ?: 0
        val without = set.filterNot { it.substringBeforeLast('=') == pkg }.toMutableSet()
        without.add("$pkg=${current + 1}")
        p.edit().putInt(KEY_DISMISS_DAY, today).putStringSet(KEY_DISMISS_COUNTS, without).apply()
    }

    private fun dayKey(): Int {
        val c = java.util.Calendar.getInstance()
        return c.get(java.util.Calendar.YEAR) * 1000 + c.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
