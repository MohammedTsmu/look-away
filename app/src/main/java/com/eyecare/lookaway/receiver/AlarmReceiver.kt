package com.eyecare.lookaway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eyecare.lookaway.service.ReminderScheduler
import com.eyecare.lookaway.service.RunState

/**
 * The watchdog alarm. If the user still wants reminders but the OS killed our
 * service, this revives it. Restarting is safe — the engine ignores a start
 * request when it is already running.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WATCHDOG && RunState.isEnabled(context)) {
            ReminderScheduler.startReminders(context.applicationContext)
        }
    }

    companion object {
        const val ACTION_WATCHDOG = "com.eyecare.lookaway.WATCHDOG"
    }
}
