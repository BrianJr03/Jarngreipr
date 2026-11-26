package jr.brian.jarnfe.shell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.isOnTopScreen
import jr.brian.jarnfe.look.rememberJarnDisplay
import jr.brian.jarnfe.wallpaper.JarnWallpaperGate
import java.util.Date

private val BatterySaverIconSize = 16.dp

internal const val JARN_STATUS_READOUT_TAG = "jarn_status_readout"

/** At or below this the battery reads as low. */
internal const val LOW_BATTERY_PERCENT = 15

internal data class JarnDeviceStatus(val time: String, val batteryPercent: Int?, val isCharging: Boolean = false)

internal val JarnDeviceStatus.isBatteryLow: Boolean
    get() = !isCharging && batteryPercent != null && batteryPercent <= LOW_BATTERY_PERCENT

private fun readDeviceStatus(context: Context): JarnDeviceStatus {
    val manager = context.getSystemService(BatteryManager::class.java)
    val battery = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.takeIf { it in 0..100 }
    return JarnDeviceStatus(
        time = DateFormat.getTimeFormat(context).format(Date()),
        batteryPercent = battery,
        isCharging = manager?.isCharging == true,
    )
}

/** The clock ticks once a minute and the battery on its own broadcasts, so nothing polls. */
@Composable
internal fun rememberDeviceStatus(): JarnDeviceStatus {
    val context = LocalContext.current
    var status by remember(context) { mutableStateOf(readDeviceStatus(context)) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                status = readDeviceStatus(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return status
}

/** Only on the top screen, and only while Battery Saver is actually holding animations still. */
internal fun showsBatterySaverIcon(batterySaverOn: Boolean, pausesOnBatterySaver: Boolean, onTopScreen: Boolean): Boolean =
    batterySaverOn && pausesOnBatterySaver && onTopScreen

/**
 * The clock, battery and Battery Saver icon, on the top screen only, whichever role is drawn
 * there. A swapped Stage on the bottom screen shows none of them.
 */
@Composable
internal fun JarnTopScreenStatus(modifier: Modifier = Modifier, onTopScreen: Boolean = isOnTopScreen()) {
    if (!onTopScreen) return
    Box(modifier) { JarnStatusReadout() }
}

@Composable
private fun JarnStatusReadout() {
    val showsSaver = showsBatterySaverIcon(
        batterySaverOn = JarnWallpaperGate.batterySaverOn,
        pausesOnBatterySaver = rememberJarnDisplay().pauseOnBatterySaver,
        onTopScreen = true,
    )
    JarnStatusReadoutContent(rememberDeviceStatus(), showsSaver)
}

@Composable
internal fun JarnStatusReadoutContent(status: JarnDeviceStatus, showsBatterySaver: Boolean) {
    val colors = JarnTheme.colors
    Row(
        modifier = Modifier.testTag(JARN_STATUS_READOUT_TAG),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showsBatterySaver) JarnBatterySaverIcon()
        JarnText(status.time, JarnTheme.type.captionStrong, colors.textMid, maxLines = 1)
        status.batteryPercent?.let { percent ->
            JarnText(
                text = stringResource(R.string.jarnfe_battery_percent, percent),
                style = JarnTheme.type.captionStrong,
                color = colors.textMid,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun JarnBatterySaverIcon() {
    Image(
        painter = painterResource(R.drawable.ic_jarn_battery_saver),
        contentDescription = stringResource(R.string.jarnfe_status_battery_saver),
        colorFilter = ColorFilter.tint(JarnTheme.colors.textMid),
        modifier = Modifier.size(BatterySaverIconSize),
    )
}
