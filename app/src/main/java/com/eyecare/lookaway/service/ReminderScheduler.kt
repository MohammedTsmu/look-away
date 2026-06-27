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
    private const val AUTO_RESUME_REQUEST = 43
    private const val OFF_REMINDER_REQUEST = 44
    private val WATCHDOG_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES

    fun startReminders(context: Context) {
        RunState.setEnabled(context, true)
        RunState.clearResumeAt(context)
        cancelAutoResume(context)
        cancelOffReminder(context)
        val intent = Intent(context, ReminderService::class.java)
            .setAction(ReminderService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
        scheduleWatchdog(context)
    }

    fun stopReminders(context: Context) {
        RunState.setEnabled(context, false)
        RunState.clearResumeAt(context)
        cancelWatchdog(context)
        cancelAutoResume(context)
        val intent = Intent(context, ReminderService::class.java)
            .setAction(ReminderService.ACTION_STOP)
        context.startService(intent)
    }

    fun sendAction(context: Context, action: String) {
        val intent = Intent(context, ReminderService::class.java).setAction(action)
        ContextCompat.startForegroundService(context, intent)
    }

    /** Pause now and schedule an automatic resume [millis] from now. */
    fun pauseFor(context: Context, millis: Long) {
        sendAction(context, ReminderService.ACTION_PAUSE)
        val resumeAt = System.currentTimeMillis() + millis
        RunState.setResumeAt(context, resumeAt)
        scheduleExact(context, resumeAt, autoResumeIntent(context))
    }

    fun cancelAutoResume(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(autoResumeIntent(context))
    }

    /** Schedule a nudge [hours] from now reminding the user reminders are off. */
    fun scheduleOffReminder(context: Context, hours: Int) {
        val at = System.currentTimeMillis() + hours.toLong() * 60 * 60 * 1000
        scheduleExact(context, at, offReminderIntent(context))
    }

    fun cancelOffReminder(context: Context) {
        context.getSystemService<AlarmManager>()?.cancel(offReminderIntent(context))
    }

    private fun scheduleExact(context: Context, atEpochMillis: Long, pi: PendingIntent) {
        val am = context.getSystemService<AlarmManager>() ?: return
        val canExact = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else true
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMillis, pi)
        }
    }

    private fun autoResumeIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_AUTO_RESUME)
        return PendingIntent.getBroadcast(
            context, AUTO_RESUME_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun offReminderIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmReceiver.ACTION_OFF_REMINDER)
        return PendingIntent.getBroadcast(
            context, OFF_REMINDER_REQUEST, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
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
