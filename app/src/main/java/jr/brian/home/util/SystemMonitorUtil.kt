package jr.brian.home.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs

fun getRAMUsage(context: Context): Triple<Float, Float, Float> {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)

    val totalRAM = memInfo.totalMem / (1024f * 1024f * 1024f)
    val availableRAM = memInfo.availMem / (1024f * 1024f * 1024f)
    val usedRAM = totalRAM - availableRAM
    val usagePercent = (usedRAM / totalRAM) * 100f

    return Triple(usagePercent, usedRAM, totalRAM)
}

fun getBatteryInfo(context: Context): Pair<Float, String> {
    val batteryIntent = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )

    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

    val batteryPercent = if (level >= 0 && scale > 0) {
        (level.toFloat() / scale.toFloat()) * 100f
    } else {
        0f
    }

    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

    val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    val timeRemaining = when {
        isCharging -> {
            batteryManager?.computeChargeTimeRemaining()?.let { millis ->
                val hours = millis / (1000 * 60 * 60)
                val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
                "${hours}h ${minutes}m until full"
            } ?: "Charging"
        }

        else -> {
            // Try to get battery current discharge rate
            val currentNow =
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
            val chargeCounter =
                batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0

            // Calculate time remaining if we have valid data
            if (currentNow < 0 && chargeCounter > 0) {
                // currentNow is negative when discharging (in microamps)
                // chargeCounter is in microamp-hours
                val hoursRemaining =
                    (chargeCounter.toFloat() / kotlin.math.abs(currentNow.toFloat()))
                val hours = hoursRemaining.toInt()
                val minutes = ((hoursRemaining - hours) * 60).toInt()

                if (hours > 0 || minutes > 0) {
                    "${hours}h ${minutes}m remaining"
                } else {
                    "Calculating..."
                }
            } else {
                "On Battery"
            }
        }
    }

    return Pair(batteryPercent, timeRemaining)
}

fun getStorageInfo(): Triple<Float, Float, Float> {
    val stat = StatFs(Environment.getDataDirectory().path)
    val totalBytes = stat.totalBytes
    val availableBytes = stat.availableBytes
    val usedBytes = totalBytes - availableBytes

    val totalGB = totalBytes / (1024f * 1024f * 1024f)
    val usedGB = usedBytes / (1024f * 1024f * 1024f)
    val usagePercent = (usedGB / totalGB) * 100f

    return Triple(usagePercent, usedGB, totalGB)
}
