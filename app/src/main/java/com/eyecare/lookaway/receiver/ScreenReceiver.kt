package com.eyecare.lookaway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eyecare.lookaway.service.ReminderEngine

/**
 * Tracks screen on/off. Must be registered at runtime — ACTION_SCREEN_ON/OFF are
 * not deliverable to manifest-declared receivers. Feeds the engine so reminders
 * only count screen-on time.
 */
class ScreenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> ReminderEngine.setScreenOn(true)
            Intent.ACTION_SCREEN_OFF -> ReminderEngine.setScreenOn(false)
        }
    }
}
