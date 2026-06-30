package com.eyecare.lookaway.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.content.getSystemService
import java.util.Calendar

/** An installed, launchable app the user can set a limit for. */
data class InstalledApp(val packageName: String, val label: String)

/**
 * Reads how long the device has been used today via [UsageStatsManager]. Used for
 * the gentle "you've been on your phone a while — step away" reminder. Requires
 * the user to grant Usage Access (a special permission), so every call degrades
 * to 0/false when it isn't granted.
 */
object UsageTracker {

    fun hasAccess(context: Context): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Total foreground (≈ active screen) minutes across all apps since midnight. */
    fun todayForegroundMinutes(context: Context): Int {
        if (!hasAccess(context)) return 0
        val usm = context.getSystemService<UsageStatsManager>() ?: return 0
        val start = startOfToday()
        val now = System.currentTimeMillis()
        val stats = runCatching { usm.queryAndAggregateUsageStats(start, now) }.getOrNull()
            ?: return 0
        val totalMs = stats.values.sumOf { it.totalTimeInForeground }
        return (totalMs / 60_000L).toInt()
    }

    /** Per-package foreground minutes since midnight (only apps with usage today). */
    fun perAppMinutesToday(context: Context): Map<String, Int> {
        if (!hasAccess(context)) return emptyMap()
        val usm = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val stats = runCatching { usm.queryAndAggregateUsageStats(startOfToday(), System.currentTimeMillis()) }
            .getOrNull() ?: return emptyMap()
        return stats.mapValues { (it.value.totalTimeInForeground / 60_000L).toInt() }
            .filterValues { it > 0 }
    }

    fun appMinutesToday(context: Context, packageName: String): Int =
        perAppMinutesToday(context)[packageName] ?: 0

    /** The package most recently brought to the foreground (last ~minute), or null. */
    fun currentForegroundApp(context: Context): String? {
        if (!hasAccess(context)) return null
        val usm = context.getSystemService<UsageStatsManager>() ?: return null
        val now = System.currentTimeMillis()
        val events = runCatching { usm.queryEvents(now - 60_000, now) }.getOrNull() ?: return null
        var pkg: String? = null
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                pkg = event.packageName
            }
        }
        return pkg
    }

    /** Best-effort human label for a package. */
    fun appLabel(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    /** All launchable apps (excluding us), sorted by label. Call off the main thread. */
    fun installedLaunchableApps(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching { pm.queryIntentActivities(intent, 0) }.getOrNull().orEmpty()
        return resolved
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Foreground minutes for each of the last 7 days, oldest first (index 6 = today).
     */
    fun last7DaysMinutes(context: Context): IntArray {
        val out = IntArray(7)
        if (!hasAccess(context)) return out
        val usm = context.getSystemService<UsageStatsManager>() ?: return out
        val now = System.currentTimeMillis()
        for (i in 0 until 7) {
            val dayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_YEAR, -(6 - i))
            }.timeInMillis
            val dayEnd = (dayStart + 24L * 60 * 60 * 1000).coerceAtMost(now)
            if (dayEnd <= dayStart) continue
            val stats = runCatching { usm.queryAndAggregateUsageStats(dayStart, dayEnd) }.getOrNull()
            out[i] = stats?.values?.sumOf { it.totalTimeInForeground }?.let { (it / 60_000L).toInt() } ?: 0
        }
        return out
    }

    fun usageAccessIntent(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    private fun startOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
