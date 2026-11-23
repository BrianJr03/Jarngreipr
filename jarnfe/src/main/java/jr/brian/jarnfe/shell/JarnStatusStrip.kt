package jr.brian.jarnfe.shell

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.R
import java.util.Date

private data class JarnDeviceStatus(val time: String, val batteryPercent: Int?)

private fun readDeviceStatus(context: Context): JarnDeviceStatus {
    val battery = context.getSystemService(BatteryManager::class.java)
        ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        ?.takeIf { it in 0..100 }
    return JarnDeviceStatus(DateFormat.getTimeFormat(context).format(Date()), battery)
}

@Composable
private fun rememberDeviceStatus(): JarnDeviceStatus {
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

@Composable
internal fun JarnStatusReadout() {
    val status = rememberDeviceStatus()
    val colors = JarnTheme.colors
    Row(
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
