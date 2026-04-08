package jr.brian.home.ui.screens

import android.content.Context
import android.media.AudioManager
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.model.WakeMethod
import jr.brian.home.model.floaty.FloatingAppInit
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.components.apps.FloatingAppsEngine
import jr.brian.home.ui.components.DualVolumeControls
import jr.brian.home.ui.components.VolumeSlider
import jr.brian.home.ui.extensions.pressWithHaptic
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.util.rememberAutoFocus
import jr.brian.home.util.getSimpleBatteryInfo
import jr.brian.home.util.rememberFpsMonitor
import jr.brian.home.viewmodels.RssViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

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
    val floatyModeManager = LocalFloatyModeManager.current
    val wakeMethod by powerSettingsManager.wakeMethod.collectAsStateWithLifecycle()
    val poweredOffBrightness by powerSettingsManager.poweredOffBrightness.collectAsStateWithLifecycle()
    val uiColor = remember(poweredOffBrightness) {
        Color.White.copy(alpha = poweredOffBrightness / 100f)
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    val rssViewModel: RssViewModel = hiltViewModel()
    val nowPlaying by rssViewModel.nowPlaying.collectAsStateWithLifecycle()

    var showInfo by remember { mutableStateOf(false) }
    var batteryPercentage by remember { mutableStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }
    var localMusicVolume by remember(musicVolume) { mutableFloatStateOf(musicVolume.toFloat()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val isFloatyMode =
        floatyModeManager.isFloatyModeActive && floatyModeManager.isPoweredOffFloatyEffectEnabled
    val floatingEngine = remember { FloatingAppsEngine() }

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
    PoweredOffFloatyEngineEffects(
        floatingEngine = floatingEngine,
        showInfo = showInfo,
        isFloatyMode = isFloatyMode,
        containerSize = containerSize
    )

    CompositionLocalProvider(
        LocalViewConfiguration provides customViewConfiguration
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
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
                modifier = if (isFloatyMode) {
                    floatingEngine.positions["battery"]?.let {
                        Modifier.offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                    } ?: Modifier
                } else {
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 8.dp)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isCharging) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Charging",
                            tint = uiColor,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = "$batteryPercentage%",
                        color = uiColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isFloatyMode) {
                    floatingEngine.positions["fps"]?.let {
                        Modifier.offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                    } ?: Modifier
                } else {
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(end = 16.dp, top = 8.dp)
                }
            ) {
                Text(
                    text = stringResource(R.string.monitor_fps_label, fps),
                    color = uiColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isFloatyMode) {
                    floatingEngine.positions["time"]?.let {
                        Modifier.offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                    } ?: Modifier
                } else {
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 16.dp, top = 8.dp)
                }
            ) {
                Text(
                    text = currentTime,
                    color = uiColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isFloatyMode) {
                    floatingEngine.positions["center"]?.let {
                        Modifier
                            .offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                            .fillMaxWidth(0.8f)
                    } ?: Modifier.fillMaxWidth(0.8f)
                } else {
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.8f)
                }
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
                    DualVolumeControls(isVisible = showInfo, tintColor = uiColor)
                }
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isFloatyMode) {
                    floatingEngine.positions["volDown"]?.let {
                        Modifier.offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                    } ?: Modifier
                } else {
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp)
                }
            ) {
                VolumeButton(
                    icon = Icons.AutoMirrored.Filled.VolumeDown,
                    contentDescription = stringResource(R.string.volume_down_description),
                    tintColor = uiColor,
                    onClick = {
                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val newVolume = (currentVolume - 1).coerceAtLeast(0)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            newVolume,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                )
            }

            // Volume Up Button - Bottom Right
            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = if (isFloatyMode) {
                    floatingEngine.positions["volUp"]?.let {
                        Modifier.offset { IntOffset(it.x.roundToInt(), it.y.roundToInt()) }
                    } ?: Modifier
                } else {
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                }
            ) {
                VolumeButton(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.volume_up_description),
                    tintColor = uiColor,
                    onClick = {
                        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val newVolume = (currentVolume + 1).coerceAtMost(maxVolume)
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            newVolume,
                            AudioManager.FLAG_SHOW_UI
                        )
                    }
                )
            }

            AnimatedVisibility(
                visible = showInfo,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, bottom = 16.dp)
            ) {
                nowPlaying?.let { info ->
                    NowPlayingWidget(
                        uiColor = uiColor,
                        info = info,
                        onPlayPause = { rssViewModel.togglePlayPause() },
                        onPrevious = { rssViewModel.skipToPrevious() },
                        onNext = { rssViewModel.skipToNext() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingWidget(
    uiColor: Color,
    info: NowPlayingManager.NowPlayingInfo,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth(0.75f)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = info.title,
            color = uiColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )
        if (info.artist != null) {
            Text(
                text = info.artist,
                color = uiColor,
                fontSize = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(uiColor.copy(alpha = 0.1f), CircleShape)
                    .clickable(onClick = onPrevious),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    tint = uiColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(uiColor.copy(alpha = 0.15f), CircleShape)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (info.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (info.isPlaying) "Pause" else "Play",
                    tint = uiColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(uiColor.copy(alpha = 0.1f), CircleShape)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    tint = uiColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PoweredOffFloatyEngineEffects(
    floatingEngine: FloatingAppsEngine,
    showInfo: Boolean,
    isFloatyMode: Boolean,
    containerSize: IntSize
) {
    LaunchedEffect(showInfo, isFloatyMode, containerSize) {
        if (!showInfo || !isFloatyMode || containerSize == IntSize.Zero) return@LaunchedEffect
        val width = containerSize.width.toFloat()
        val height = containerSize.height.toFloat()
        val centerWidth = width * 0.8f
        floatingEngine.initialize(
            apps = listOf(
                FloatingAppInit(id = "battery", x = 16f, y = 8f, width = 180f, height = 56f),
                FloatingAppInit(
                    id = "fps",
                    x = (width - 220f) / 2f,
                    y = 8f,
                    width = 220f,
                    height = 56f
                ),
                FloatingAppInit(
                    id = "time",
                    x = (width - 180f - 16f).coerceAtLeast(0f),
                    y = 8f,
                    width = 180f,
                    height = 56f
                ),
                FloatingAppInit(
                    id = "center",
                    x = ((width - centerWidth) / 2f).coerceAtLeast(0f),
                    y = ((height - 120f) / 2f).coerceAtLeast(0f),
                    width = centerWidth.coerceAtLeast(220f),
                    height = 120f
                ),
                FloatingAppInit(
                    id = "volDown",
                    x = 16f,
                    y = (height - 72f).coerceAtLeast(0f),
                    width = 56f,
                    height = 56f
                ),
                FloatingAppInit(
                    id = "volUp",
                    x = (width - 72f).coerceAtLeast(0f),
                    y = (height - 72f).coerceAtLeast(0f),
                    width = 56f,
                    height = 56f
                )
            ),
            width = width,
            height = height
        )
    }
    LaunchedEffect(showInfo, isFloatyMode) {
        if (!showInfo || !isFloatyMode) return@LaunchedEffect
        var lastNanos = 0L
        while (isActive) {
            withInfiniteAnimationFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = (frameNanos - lastNanos) / 1_000_000_000f
                    floatingEngine.tick(dt.coerceAtMost(0.05f))
                }
                lastNanos = frameNanos
            }
        }
    }
}

@Composable
private fun VolumeButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.DarkGray
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed)

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(pressScale)
            .background(
                color = tintColor.copy(alpha = tintColor.alpha * if (isPressed) 0.6f else 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .border(
                width = 1.dp,
                color = tintColor.copy(alpha = tintColor.alpha * 0.7f),
                shape = RoundedCornerShape(6.dp)
            )
            .pressWithHaptic(
                onClick, contentDescription,
                haptic = haptic,
                onPressChange = { isPressed = it },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tintColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun MusicVolumeSlider(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = Color.DarkGray
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
                tint = tintColor
            )
        },
        tintColor = tintColor
    )
}
