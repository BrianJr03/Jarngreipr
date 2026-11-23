package jr.brian.jarnfe.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val MONITOR_INTERVAL_MS = 1_000L
private const val METER_DANGER = 0.9f
private val TwoColumnWidth = 560.dp
private val MonitorScrollStep = 96.dp

@Immutable
private data class MonitorMeterItem(
    val label: String,
    val value: String,
    val supporting: String? = null,
    val fraction: Float? = null,
)

/** Polls once a second, and only while [rememberUtilityLive] says this screen is in use. */
@Composable
internal fun JarnMonitorPanel(utilities: JarnUtilities, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val live = rememberUtilityLive()
    var sample by remember { mutableStateOf<JarnMonitorSample?>(null) }
    LaunchedEffect(utilities, live) {
        if (!live) return@LaunchedEffect
        while (isActive) {
            sample = withContext(Dispatchers.IO) { utilities.readMonitor() }
            delay(MONITOR_INTERVAL_MS)
        }
    }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { MonitorScrollStep.toPx() }
    UtilityLayer(
        onBack = onClose,
        bindings = {
            on(JarnAction.MoveUp, R.string.jarnfe_utilities_scroll) { scope.launch { scroll.scrollBy(-stepPx) } }
            on(JarnAction.MoveDown) { scope.launch { scroll.scrollBy(stepPx) } }
        },
    ) {
        Column(
            modifier = modifier.fillMaxSize().verticalScroll(scroll).padding(JarnTheme.space.lg),
            verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        ) {
            UtilityTitle(stringResource(R.string.jarnfe_utilities_monitor))
            if (!live) UtilityNote(stringResource(R.string.jarnfe_utilities_monitor_paused))
            sample?.let { MonitorMeters(monitorItems(it)) }
        }
    }
}

@Composable
private fun monitorItems(sample: JarnMonitorSample): List<MonitorMeterItem> = listOf(
    usageItem(stringResource(R.string.jarnfe_utilities_ram), sample.ram),
    cpuItem(sample.cpu),
    MonitorMeterItem(
        label = stringResource(R.string.jarnfe_utilities_battery),
        value = sample.battery.percent?.let { stringResource(R.string.jarnfe_utilities_percent, it) }
            ?: stringResource(R.string.jarnfe_utilities_not_available),
        supporting = batteryStatus(sample.battery),
        fraction = sample.battery.percent?.let { it / 100f },
    ),
    usageItem(stringResource(R.string.jarnfe_utilities_storage), sample.storage),
)

@Composable
private fun usageItem(label: String, usage: JarnUsage) = MonitorMeterItem(
    label = label,
    value = stringResource(R.string.jarnfe_utilities_percent, (usage.fraction * 100).roundToInt()),
    supporting = stringResource(
        R.string.jarnfe_utilities_gb_used,
        jarnGigabytes(usage.usedBytes),
        jarnGigabytes(usage.totalBytes),
    ),
    fraction = usage.fraction,
)

@Composable
private fun cpuItem(cpu: JarnCpuReading): MonitorMeterItem {
    val label = stringResource(R.string.jarnfe_utilities_cpu)
    return when (cpu) {
        is JarnCpuReading.Percent -> MonitorMeterItem(
            label = label,
            value = stringResource(R.string.jarnfe_utilities_percent, cpu.value.roundToInt()),
            fraction = cpu.value / 100f,
        )
        JarnCpuReading.Measuring -> MonitorMeterItem(label, stringResource(R.string.jarnfe_utilities_measuring))
        JarnCpuReading.Unavailable -> MonitorMeterItem(
            label = label,
            value = stringResource(R.string.jarnfe_utilities_not_available),
            supporting = stringResource(R.string.jarnfe_utilities_cpu_blocked),
        )
    }
}

@Composable
private fun batteryStatus(battery: JarnBattery): String {
    val remaining = battery.remainingMs?.let { ms ->
        val duration = jarnDuration(ms)
        stringResource(R.string.jarnfe_utilities_duration, duration.hours, duration.minutes)
    }
    return when {
        battery.charging && remaining != null -> stringResource(R.string.jarnfe_utilities_until_full, remaining)
        battery.charging -> stringResource(R.string.jarnfe_utilities_charging)
        remaining != null -> stringResource(R.string.jarnfe_utilities_remaining, remaining)
        else -> stringResource(R.string.jarnfe_utilities_on_battery)
    }
}

@Composable
private fun MonitorMeters(items: List<MonitorMeterItem>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= TwoColumnWidth) 2 else 1
        Column(verticalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
            items.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm)) {
                    row.forEach { MonitorMeter(it, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MonitorMeter(item: MonitorMeterItem, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(JarnTheme.colors.surface, JarnTheme.radius.mdShape)
            .padding(JarnTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            JarnText(item.label, JarnTheme.type.label, JarnTheme.colors.textMid, modifier = Modifier.weight(1f))
            JarnText(item.value, JarnTheme.type.heading, JarnTheme.colors.textHigh, maxLines = 1)
        }
        item.supporting?.let { UtilityNote(it) }
        item.fraction?.let { MonitorBar(it) }
    }
}

@Composable
private fun MonitorBar(fraction: Float) {
    val color = if (fraction >= METER_DANGER) JarnTheme.colors.danger else JarnTheme.colors.accent
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(JarnTheme.colors.raised, CircleShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, CircleShape),
        )
    }
}
