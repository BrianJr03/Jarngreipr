package jr.brian.home.jarn

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import androidx.compose.runtime.snapshotFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.FloatyModeManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.util.CrashLogger
import jr.brian.home.util.RecentAppsUtil
import jr.brian.jarnfe.utilities.JarnBattery
import jr.brian.jarnfe.utilities.JarnCpuTicks
import jr.brian.jarnfe.utilities.JarnCrashEntry
import jr.brian.jarnfe.utilities.JarnFaqSection
import jr.brian.jarnfe.utilities.JarnFloatySettings
import jr.brian.jarnfe.utilities.JarnMonitorSample
import jr.brian.jarnfe.utilities.JarnRecentApp
import jr.brian.jarnfe.utilities.JarnUsage
import jr.brian.jarnfe.utilities.JarnUtilities
import jr.brian.jarnfe.utilities.JarnVolume
import jr.brian.jarnfe.utilities.jarnChargeRemainingMs
import jr.brian.jarnfe.utilities.jarnCpuReading
import jr.brian.jarnfe.utilities.jarnDischargeRemainingMs
import jr.brian.jarnfe.utilities.jarnParseCpuTicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Reuses Classic's stores, so every setting here is already covered by backup and restore. */
@Singleton
class AppJarnUtilities @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val floatyManager: FloatyModeManager,
    private val pageTypes: PageTypeManager,
) : JarnUtilities {
    private val audio = context.getSystemService(AudioManager::class.java)
    private var lastCpuTicks: JarnCpuTicks? = null

    override val volume: Flow<JarnVolume> = callbackFlow {
        fun publish() {
            trySend(
                JarnVolume(
                    level = audio.getStreamVolume(AudioManager.STREAM_MUSIC),
                    max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                ),
            )
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) = publish()
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        publish()
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }.conflate().distinctUntilChanged()

    override val floaty: Flow<JarnFloatySettings> = combine(
        snapshotFlow { floatyManager.toJarn() },
        pageTypes.pageTypes,
    ) { settings, pages -> settings.copy(tabCount = pages.size) }

    @Synchronized
    override fun readMonitor(): JarnMonitorSample {
        val ticks = readCpuTicks()
        val cpu = jarnCpuReading(lastCpuTicks, ticks)
        lastCpuTicks = ticks
        return JarnMonitorSample(readRam(), readStorage(), cpu, readBattery())
    }

    override suspend fun crashLogs(): List<JarnCrashEntry> = CrashLogger.getCrashLogs().map {
        JarnCrashEntry(it.fileName, it.timestamp, it.preview, it.content)
    }

    override suspend fun deleteCrashLog(fileName: String) {
        CrashLogger.deleteCrashLog(fileName)
    }

    override suspend fun clearCrashLogs() {
        CrashLogger.clearAllCrashLogs()
    }

    override fun hasUsageAccess(): Boolean = RecentAppsUtil.hasUsageStatsPermission(context)

    override suspend fun recentApps(): List<JarnRecentApp> = withContext(Dispatchers.IO) {
        RecentAppsUtil.getRecentApps(context).map {
            JarnRecentApp(it.packageName, it.label, it.icon, it.usageTimeMs)
        }
    }

    override fun faqSections(): List<JarnFaqSection> = jarnFaqSections(context)

    override fun setVolume(level: Int) {
        runCatching { audio.setStreamVolume(AudioManager.STREAM_MUSIC, level, 0) }
    }

    override fun setFloatyActive(enabled: Boolean) = floatyManager.setFloatyMode(enabled)

    override fun setFloatySectionKonfetti(enabled: Boolean) =
        floatyManager.updateSectionTapKonfettiEffectEnabled(enabled)

    override fun setFloatyPoweredOffEffect(enabled: Boolean) =
        floatyManager.updatePoweredOffFloatyEffectEnabled(enabled)

    override fun setFloatyDrawerEffect(enabled: Boolean) =
        floatyManager.updateAppsModalFloatyEffectEnabled(enabled)

    override fun setFloatyDrawerAppCount(count: Int) = floatyManager.updateAppDrawerFloatyAppCount(count)

    override fun setFloatyBubblePop(enabled: Boolean) = floatyManager.updateAppDrawerBubblePopEnabled(enabled)

    override fun setFloatyTab(index: Int, enabled: Boolean) =
        floatyManager.setTabEnabled(index, enabled, pageTypes.pageTypes.value.size)

    override fun resetFloaty() = floatyManager.reset()

    private fun readCpuTicks(): JarnCpuTicks? = runCatching {
        File("/proc/stat").bufferedReader().use { jarnParseCpuTicks(it.readLine()) }
    }.getOrNull()

    private fun readRam(): JarnUsage {
        val info = ActivityManager.MemoryInfo()
        context.getSystemService(ActivityManager::class.java).getMemoryInfo(info)
        return JarnUsage(info.totalMem - info.availMem, info.totalMem)
    }

    private fun readStorage(): JarnUsage {
        val stat = StatFs(Environment.getDataDirectory().path)
        return JarnUsage(stat.totalBytes - stat.availableBytes, stat.totalBytes)
    }

    private fun readBattery(): JarnBattery {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val manager = context.getSystemService(BatteryManager::class.java)
        val remaining = when {
            manager == null -> null
            charging -> jarnChargeRemainingMs(manager.computeChargeTimeRemaining())
            else -> jarnDischargeRemainingMs(
                manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER),
                manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            )
        }
        return JarnBattery(
            percent = if (level >= 0 && scale > 0) level * 100 / scale else null,
            charging = charging,
            remainingMs = remaining,
        )
    }
}

private fun FloatyModeManager.toJarn() = JarnFloatySettings(
    unlocked = isUnlocked,
    active = isFloatyModeActive,
    sectionKonfetti = isSectionTapKonfettiEnabled,
    poweredOffEffect = isPoweredOffFloatyEffectEnabled,
    drawerEffect = isAppsModalFloatyEffectEnabled,
    drawerAppCount = appDrawerFloatyAppCount,
    bubblePop = isAppDrawerBubblePopEnabled,
    enabledTabs = enabledTabs,
)
