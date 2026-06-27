package com.eyecare.lookaway.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.eyecare.lookaway.receiver.AlarmReceiver

/**
 * Lifecycle helpers for the reminder service plus a low-frequency AlarmManager
 * "watchdog" that revives the service if the OS killed it. Reviving is safe:
 * the engine ignores a start request when it's already running.
 */
object ReminderScheduler {

    private const val WATCHDOG_REQUEST = 42
    private val WATCHDOG_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES

    fun startReminders(context: Context) {
        RunState.setEnabled(context, true)
        val intent = Intent(context, ReminderService::class.java)
            .setAction(ReminderService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
        scheduleWatchdog(context)
    }

    fun stopReminders(context: Context) {
        RunState.setEnabled(context, false)
        cancelWatchdog(context)
        val intent = Intent(context, ReminderService::class.java)
            .setAction(ReminderService.ACTION_STOP)
        context.startService(intent)
    }

    fun sendAction(context: Context, action: String) {
        val intent = Intent(context, ReminderService::class.java).setAction(action)
        ContextCompat.startForegroundService(context, intent)
    }

    fun scheduleWatchdog(context: Context) {
        val am = context.getSystemService<AlarmManager>() ?: return
        val first = SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL
        am.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            first,
            WATCHDOG_INTERVAL,
            watchdogIntent(context),
        )
    }

    fun cancelWatchdog(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(watchdogIntent(context))
    }

    private fun watchdogIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_WATCHDOG)
        return PendingIntent.getBroadcast(
            context,
            WATCHDOG_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
