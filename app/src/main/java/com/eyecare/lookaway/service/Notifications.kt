package com.eyecare.lookaway.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService

/** Notification channel ids + one-time channel setup. */
object Notifications {

    const val CHANNEL_STATUS = "status"
    const val CHANNEL_BREAK = "break"

    const val ID_STATUS = 1001
    const val ID_BREAK = 1002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return

        val status = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(com.eyecare.lookaway.R.string.channel_status_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(com.eyecare.lookaway.R.string.channel_status_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        // Kept silent on purpose: sound + vibration are played programmatically
        // so the in-app toggles take effect at runtime (channel settings are
        // frozen at creation time on Android O+). Still HIGH so the break can
        // pop a heads-up and fire its full-screen intent.
        val breakCh = NotificationChannel(
            CHANNEL_BREAK,
            context.getString(com.eyecare.lookaway.R.string.channel_break_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(com.eyecare.lookaway.R.string.channel_break_desc)
            setShowBadge(true)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannel(status)
        nm.createNotificationChannel(breakCh)
    }
}
