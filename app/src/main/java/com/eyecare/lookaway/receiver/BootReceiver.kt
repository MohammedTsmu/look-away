package com.eyecare.lookaway.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.eyecare.lookaway.data.SettingsRepository
import com.eyecare.lookaway.service.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts reminders after a reboot or an app update, but only if the user
 * opted in via "Start after reboot".
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pending = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        val settings = SettingsRepository(appContext).flow.first()
                        val monitor = (settings.mindfulUsageEnabled || settings.appLimits.isNotEmpty()) &&
                            com.eyecare.lookaway.usage.UsageTracker.hasAccess(appContext)
                        when {
                            settings.startOnBoot -> ReminderScheduler.startReminders(appContext)
                            monitor -> ReminderScheduler.startMonitor(appContext)
                        }
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
