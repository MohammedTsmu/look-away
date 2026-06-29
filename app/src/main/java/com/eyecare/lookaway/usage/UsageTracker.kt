package com.eyecare.lookaway.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.provider.Settings
import androidx.core.content.getSystemService
import java.util.Calendar

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
