package jr.brian.jarnfe.utilities

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow

@Immutable
data class JarnUsage(val usedBytes: Long, val totalBytes: Long) {
    val fraction: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

@Immutable
sealed interface JarnCpuReading {
    data object Unavailable : JarnCpuReading
    data object Measuring : JarnCpuReading
    data class Percent(val value: Float) : JarnCpuReading
}

@Immutable
data class JarnBattery(val percent: Int?, val charging: Boolean, val remainingMs: Long?)

@Immutable
data class JarnMonitorSample(
    val ram: JarnUsage,
    val storage: JarnUsage,
    val cpu: JarnCpuReading,
    val battery: JarnBattery,
)

@Immutable
data class JarnCrashEntry(
    val fileName: String,
    val timestamp: Long,
    val preview: String,
    val content: String,
)

@Immutable
data class JarnRecentApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val usageMs: Long,
)

@Immutable
data class JarnFaqSnippet(val label: String, val text: String)

@Immutable
data class JarnFaqEntry(
    val question: String,
    val answer: String,
    val snippets: List<JarnFaqSnippet> = emptyList(),
)

@Immutable
data class JarnFaqSection(val title: String, val entries: List<JarnFaqEntry>)

@Immutable
data class JarnVolume(val level: Int, val max: Int)

@Immutable
data class JarnFloatySettings(
    val unlocked: Boolean = false,
    val active: Boolean = false,
    val sectionKonfetti: Boolean = false,
    val poweredOffEffect: Boolean = false,
    val drawerEffect: Boolean = false,
    val drawerAppCount: Int = 0,
    val bubblePop: Boolean = false,
    val tabCount: Int = 0,
    val enabledTabs: Set<Int> = emptySet(),
) {
    /** An empty set means every tab, as in Classic. */
    fun isTabEnabled(index: Int): Boolean = enabledTabs.isEmpty() || index in enabledTabs
}

/** Host services behind the utility screens. Nothing here reads or polls until a screen asks. */
interface JarnUtilities {
    val volume: Flow<JarnVolume>
    val floaty: Flow<JarnFloatySettings>

    /** Blocking, so call it off the main thread. */
    fun readMonitor(): JarnMonitorSample

    suspend fun crashLogs(): List<JarnCrashEntry>

    suspend fun deleteCrashLog(fileName: String)

    suspend fun clearCrashLogs()

    fun hasUsageAccess(): Boolean

    suspend fun recentApps(): List<JarnRecentApp>

    fun faqSections(): List<JarnFaqSection>

    fun setVolume(level: Int)

    fun setFloatyActive(enabled: Boolean)

    fun setFloatySectionKonfetti(enabled: Boolean)

    fun setFloatyPoweredOffEffect(enabled: Boolean)

    fun setFloatyDrawerEffect(enabled: Boolean)

    fun setFloatyDrawerAppCount(count: Int)

    fun setFloatyBubblePop(enabled: Boolean)

    fun setFloatyTab(index: Int, enabled: Boolean)

    fun resetFloaty()
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnUtilitiesProvider {
    val jarnUtilities: JarnUtilities
}

internal fun Context.jarnUtilitiesOrNull(): JarnUtilities? =
    (applicationContext as? JarnUtilitiesProvider)?.jarnUtilities
