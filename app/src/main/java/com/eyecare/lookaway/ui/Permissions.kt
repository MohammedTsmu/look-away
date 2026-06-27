package com.eyecare.lookaway.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/** Snapshot of the permissions that make reminders reliable. */
data class PermissionStatus(
    val notifications: Boolean,
    val overlay: Boolean,
    val exactAlarm: Boolean,
    val batteryUnrestricted: Boolean,
) {
    val allEssentialGranted: Boolean get() = notifications && exactAlarm
}

object Permissions {

    fun snapshot(context: Context) = PermissionStatus(
        notifications = hasNotifications(context),
        overlay = hasOverlay(context),
        exactAlarm = hasExactAlarm(context),
        batteryUnrestricted = isBatteryUnrestricted(context),
    )

    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    fun hasOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasExactAlarm(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() ?: false
        } else true

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService<PowerManager>() ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun overlayIntent(context: Context) = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )

    fun exactAlarmIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
        } else appSettingsIntent(context)

    @android.annotation.SuppressLint("BatteryLife")
    fun batteryIntent(context: Context) = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"),
    )

    fun appSettingsIntent(context: Context) = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )
}
