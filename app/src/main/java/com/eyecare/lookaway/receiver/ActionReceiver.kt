package com.eyecare.lookaway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eyecare.lookaway.service.ReminderScheduler

/**
 * Forwards a control action (pause/resume/skip/…) to the running service.
 * Kept for components that prefer a broadcast over a direct service PendingIntent
 * (e.g. widgets or future quick-settings tiles).
 */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        ReminderScheduler.sendAction(context.applicationContext, action)
    }
}
