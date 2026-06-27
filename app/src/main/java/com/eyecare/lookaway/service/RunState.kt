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

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
