package com.eyecare.lookaway.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eyecare.lookaway.R
import com.eyecare.lookaway.service.Notifications
import com.eyecare.lookaway.service.ReminderScheduler
import com.eyecare.lookaway.service.ReminderService
import com.eyecare.lookaway.service.RunState
import com.eyecare.lookaway.ui.MainActivity

/**
 * Handles the scheduled alarms:
 * - WATCHDOG: revive the service if the OS killed it (safe when already running).
 * - AUTO_RESUME: end a timed pause and resume reminders.
 * - OFF_REMINDER: nudge the user that eye-care has been left off.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_WATCHDOG ->
                if (RunState.isEnabled(app)) ReminderScheduler.startReminders(app)

            ACTION_AUTO_RESUME -> {
                RunState.clearResumeAt(app)
                if (RunState.isEnabled(app)) {
                    ReminderScheduler.sendAction(app, ReminderService.ACTION_RESUME)
                }
            }

            ACTION_OFF_REMINDER ->
                if (!RunState.isEnabled(app)) postOffNudge(app)
        }
    }

    @android.annotation.SuppressLint("MissingPermission") // guarded by canPost()
    private fun postOffNudge(context: Context) {
        if (!canPost(context)) return

        val open = PendingIntent.getActivity(
            context, 20,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val turnOn = PendingIntent.getBroadcast(
            context, 21,
            Intent(context, ActionReceiver::class.java).setAction(ReminderService.ACTION_START),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_NUDGE)
            .setSmallIcon(R.drawable.ic_eye)
            .setContentTitle(context.getString(R.string.nudge_off_title))
            .setContentText(context.getString(R.string.nudge_off_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.nudge_off_text)))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_turn_on), turnOn)
            .build()

        NotificationManagerCompat.from(context).notify(Notifications.ID_NUDGE, notification)
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val ACTION_WATCHDOG = "com.eyecare.lookaway.WATCHDOG"
        const val ACTION_AUTO_RESUME = "com.eyecare.lookaway.AUTO_RESUME"
        const val ACTION_OFF_REMINDER = "com.eyecare.lookaway.OFF_REMINDER"
    }
}
