package jr.brian.home.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.WakeMethod
import jr.brian.home.ui.components.DualVolumeControls
import jr.brian.home.ui.components.VolumeSlider
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.util.rememberAutoFocus
import jr.brian.home.util.getSimpleBatteryInfo
import jr.brian.home.util.rememberFpsMonitor
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PoweredOffScreen(
    modifier: Modifier = Modifier,
    onPowerOn: () -> Unit = {},
    musicVolume: Int = 100,
    onMusicVolumeChange: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val focusRequester = rememberAutoFocus()
    val interactionSource = remember { MutableInteractionSource() }
    val powerSettingsManager = LocalPowerSettingsManager.current
    val wakeMethod by powerSettingsManager.wakeMethod.collectAsStateWithLifecycle()

    var showInfo by remember { mutableStateOf(false) }
    var batteryPercentage by remember { mutableStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }
    var localMusicVolume by remember(musicVolume) { mutableFloatStateOf(musicVolume.toFloat()) }

    val fps = rememberFpsMonitor().value

    val defaultConfig = LocalViewConfiguration.current
    val customViewConfiguration = remember(defaultConfig) {
        object : ViewConfiguration {
            override val longPressTimeoutMillis: Long = 1000L
            override val doubleTapTimeoutMillis: Long = defaultConfig.doubleTapTimeoutMillis
            override val doubleTapMinTimeMillis: Long = defaultConfig.doubleTapMinTimeMillis
            override val touchSlop: Float = defaultConfig.touchSlop
        }
    }

    LaunchedEffect(showInfo) {
        if (showInfo) {
            val simpleBatteryInfo = context.getSimpleBatteryInfo()
            batteryPercentage = simpleBatteryInfo.first
            isCharging = simpleBatteryInfo.second

            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            currentTime = timeFormat.format(Date())

            delay(3000)
            showInfo = false
        }
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides customViewConfiguration
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        onPowerOn()
                        true
                    } else {
                        false
                    }
                }
                .then(
                    when (wakeMethod) {
                        WakeMethod.SINGLE_TAP -> {
                            Modifier.clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                showInfo = true
                            }
                        }
                        WakeMethod.DOUBLE_TAP -> {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showInfo = true },
                                    onDoubleTap = { onPowerOn() }
                                )
                            }
                        }
                        WakeMethod.LONG_PRESS -> {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = { showInfo = true },
                                    onLongPress = { onPowerOn() }
                                )
                            }
                        }
                    }
                )
        ) {
            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Charging",
                            tint = Color.DarkGray,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = "$batteryPercentage%",
                        color = Color.DarkGray,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(end = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.monitor_fps_label, fps),
                    color = Color.DarkGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 8.dp)
            ) {
                Text(
                    text = currentTime,
                    color = Color.DarkGray,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.8f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
//                    if (isEsdeMode) {
//                        MusicVolumeSlider(
//                            volume = localMusicVolume,
//                            onVolumeChange = { newVolume ->
//                                localMusicVolume = newVolume
//                                onMusicVolumeChange(newVolume.toInt())
//                            }
//                        )
//                        Spacer(modifier = Modifier.height(8.dp))
//                    }
                    DualVolumeControls(isVisible = showInfo)
                }
            }
        }
    }
}

@Composable
private fun MusicVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    VolumeSlider(
        label = stringResource(R.string.esde_settings_music_volume),
        volume = volume,
        maxVolume = 100f,
        onVolumeChange = onVolumeChange,
        modifier = modifier,
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.DarkGray
            )
        }
    )
}
