package com.eyecare.lookaway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eyecare.lookaway.service.ReminderScheduler
import com.eyecare.lookaway.service.ReminderService

/**
 * Forwards a control action to the reminder system from a broadcast (e.g. a
 * notification action button or a future widget / quick-settings tile).
 * START/STOP go through the full scheduler so run-state, watchdog, and the
 * off-reminder are all kept consistent.
 */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (val action = intent.action) {
            null -> return
            ReminderService.ACTION_START -> ReminderScheduler.startReminders(app)
            ReminderService.ACTION_STOP -> ReminderScheduler.stopReminders(app)
            else -> ReminderScheduler.sendAction(app, action)
        }
    }
}
