package jr.brian.home.util

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import jr.brian.home.model.app.RecentAppInfo
import java.util.concurrent.TimeUnit

/**
 * Utility object for recent apps functionality.
 */
object RecentAppsUtil {
    
    /**
     * Checks if usage stats permission is granted.
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Gets the list of recently used apps (within the last 10 minutes).
     */
    fun getRecentApps(context: Context): List<RecentAppInfo> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000) // Last 24 hours
        
        // Only show apps used in the last 10 minutes as "recent"
        val recentThreshold = endTime - (10 * 60 * 1000)

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val pm = context.packageManager
        val launcherPackage = context.packageName

        return usageStats
            .filter { it.totalTimeInForeground > 0 }
            .filter { it.lastTimeUsed > recentThreshold } // Only recently used apps
            .sortedByDescending { it.lastTimeUsed }
            .distinctBy { it.packageName }
            .filter { it.packageName != launcherPackage }
            .mapNotNull { stats ->
                try {
                    val appInfo = pm.getApplicationInfo(
                        stats.packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                    val intent = pm.getLaunchIntentForPackage(stats.packageName)
                    if (intent != null) {
                        RecentAppInfo(
                            packageName = stats.packageName,
                            label = pm.getApplicationLabel(appInfo).toString(),
                            icon = pm.getApplicationIcon(appInfo),
                            usageTimeMs = stats.totalTimeInForeground
                        )
                    } else null
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .take(20)
    }

    /**
     * Formats a duration in milliseconds to a human-readable string (e.g., "2h 30m").
     */
    fun formatUsageDuration(durationMs: Long): String {
        if (durationMs <= 0) return "<1m"

        val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours <= 0 -> "${minutes}m"
            minutes <= 0L -> "${hours}h"
            else -> "${hours}h ${minutes}m"
        }
    }
}
