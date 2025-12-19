package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.util.CpuMonitor
import jr.brian.home.util.getBatteryInfo
import jr.brian.home.util.getRAMUsage
import jr.brian.home.util.getStorageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MonitorScreen(
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cpuMonitor = remember { CpuMonitor() }

    var batteryTime by remember { mutableStateOf("") }
    var ramUsedGB by remember { mutableFloatStateOf(0f) }
    var ramTotalGB by remember { mutableFloatStateOf(0f) }
    var storageUsedGB by remember { mutableFloatStateOf(0f) }
    var storageTotalGB by remember { mutableFloatStateOf(0f) }
    var batteryPercent by remember { mutableFloatStateOf(0f) }
    var storagePercent by remember { mutableFloatStateOf(0f) }
    var ramUsagePercent by remember { mutableFloatStateOf(0f) }
    var cpuUsagePercent by remember { mutableFloatStateOf(0f) }

    BackHandler(onBack = onDismiss)

    DisposableEffect(Unit) {
        val job = scope.launch(Dispatchers.IO) {
            while (true) {
                val ramInfo = getRAMUsage(context)
                ramUsagePercent = ramInfo.first
                ramUsedGB = ramInfo.second
                ramTotalGB = ramInfo.third

                cpuUsagePercent = cpuMonitor.getCurrentUsage()

                val batteryInfo = getBatteryInfo(context)
                batteryPercent = batteryInfo.first
                batteryTime = batteryInfo.second

                val storageInfo = getStorageInfo()
                storagePercent = storageInfo.first
                storageUsedGB = storageInfo.second
                storageTotalGB = storageInfo.third

                delay(1000)
            }
        }

        onDispose {
            job.cancel()
        }
    }

    Scaffold(
        containerColor = OledBackgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .systemBarsPadding()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.monitor_screen_title),
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.monitor_screen_description),
                                color = ThemeSecondaryColor,
                                fontSize = 14.sp
                            )
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.monitor_close),
                                tint = Color.White
                            )
                        }
                    }
                }

                item {
                    MonitorCard(
                        label = stringResource(R.string.monitor_ram_label),
                        value = "${ramUsagePercent.roundToInt()}%",
                        subtitle = "%.2f GB / %.2f GB".format(ramUsedGB, ramTotalGB),
                        progress = ramUsagePercent / 100f
                    )
                }

                item {
                    MonitorCard(
                        label = stringResource(R.string.monitor_cpu_label),
                        value = "${cpuUsagePercent.roundToInt()}%",
                        progress = cpuUsagePercent / 100f
                    )
                }

                item {
                    MonitorCard(
                        label = stringResource(R.string.monitor_battery_label),
                        value = "${batteryPercent.roundToInt()}%",
                        progress = batteryPercent / 100f
                    )
                }

                item {
                    MonitorCard(
                        label = stringResource(R.string.monitor_battery_time_label),
                        value = batteryTime,
                        progress = null
                    )
                }

                item {
                    MonitorCard(
                        label = stringResource(R.string.monitor_storage_label),
                        value = "${storagePercent.roundToInt()}%",
                        subtitle = "%.2f GB / %.2f GB".format(storageUsedGB, storageTotalGB),
                        progress = storagePercent / 100f
                    )
                }
            }
        }
    }
}

@Composable
private fun MonitorCard(
    label: String,
    value: String,
    subtitle: String? = null,
    progress: Float? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        OledCardLightColor,
                        OledCardColor
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = ThemePrimaryColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = value,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }

            if (progress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = when {
                        progress >= 0.9f -> Color.Red
                        progress >= 0.7f -> Color(0xFFFFA500)
                        else -> ThemeSecondaryColor
                    },
                    trackColor = Color.DarkGray.copy(alpha = 0.3f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
