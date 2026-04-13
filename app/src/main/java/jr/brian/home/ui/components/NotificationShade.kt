package jr.brian.home.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.data.NotificationItem
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.util.getSimpleBatteryInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DISMISS_THRESHOLD = -120f

@Composable
fun NotificationShade(
    volume: Float,
    duration: Long,
    visible: Boolean,
    currentPosition: Long,
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    modifier: Modifier = Modifier,
    notifications: List<NotificationItem> = emptyList(),
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {}
) {
    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.6f else 0f,
        animationSpec = tween(300),
        label = "scrim_alpha"
    )

    Box(modifier = modifier.fillMaxSize()) {
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(250)
            ) + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            ShadeCard(
                nowPlaying = nowPlaying,
                currentPosition = currentPosition,
                duration = duration,
                volume = volume,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onVolumeChange = onVolumeChange,
                onSeek = onSeek,
                onDismiss = onDismiss,
                onSettingsClick = onSettingsClick,
                notifications = notifications,
                onDismissNotification = onDismissNotification,
                onClearAllNotifications = onClearAllNotifications
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShadeCard(
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    onSettingsClick: () -> Unit = {},
    notifications: List<NotificationItem> = emptyList(),
    onDismissNotification: (String) -> Unit = {},
    onClearAllNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var batteryPercentage by remember { mutableIntStateOf(0) }
    var isCharging by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { 2 })
    var page0HeightPx by remember { mutableIntStateOf(0) }
    var page1HeightPx by remember { mutableIntStateOf(0) }
    val pagerHeightDp = with(density) { maxOf(page0HeightPx, page1HeightPx).toDp() }

    LaunchedEffect(Unit) {
        val (pct, charging) = context.getSimpleBatteryInfo()
        batteryPercentage = pct
        isCharging = charging
        currentTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp)
            .graphicsLayer {
                translationY = dragOffsetY.coerceAtLeast(0f)
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffsetY < DISMISS_THRESHOLD) onDismiss()
                        dragOffsetY = 0f
                    },
                    onDragCancel = { dragOffsetY = 0f },
                    onVerticalDrag = { _, delta ->
                        dragOffsetY = (dragOffsetY + delta).coerceAtMost(60f)
                    }
                )
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShadeHeader(
                batteryPercentage = batteryPercentage,
                isCharging = isCharging,
                currentTime = currentTime,
                onSettingsClick = onSettingsClick,
                onDismiss = onDismiss
            )

            ShadePagerIndicator(currentPage = pagerState.currentPage)

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (pagerHeightDp > 0.dp) Modifier.height(pagerHeightDp) else Modifier)
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            when (page) {
                                0 -> if (size.height > page0HeightPx) page0HeightPx = size.height
                                1 -> if (size.height > page1HeightPx) page1HeightPx = size.height
                            }
                        }
                ) {
                    when (page) {
                        0 -> ActionsAndNotificationsPage(
                            notifications = notifications,
                            onDismissNotification = onDismissNotification,
                            onClearAllNotifications = onClearAllNotifications
                        )

                        1 -> NowPlayingPage(
                            nowPlaying = nowPlaying,
                            currentPosition = currentPosition,
                            duration = duration,
                            volume = volume,
                            onPlayPause = onPlayPause,
                            onPrevious = onPrevious,
                            onNext = onNext,
                            onVolumeChange = onVolumeChange,
                            onSeek = onSeek
                        )
                    }
                }
            }
        }

        ShadePill(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ShadeHeader(
    batteryPercentage: Int,
    isCharging: Boolean,
    currentTime: String,
    onSettingsClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        HeaderIconButton(
            icon = Icons.Default.Settings,
            contentDescription = stringResource(R.string.common_settings),
            onClick = onSettingsClick
        )
        Spacer(Modifier.weight(1f))

        if (isCharging) {
            Icon(
                imageVector = Icons.Default.BatteryChargingFull,
                contentDescription = null,
                tint = ThemePrimaryColor,
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = "$batteryPercentage%",
            color = ThemePrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = currentTime,
            color = ThemePrimaryColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

@Composable
private fun ShadePagerIndicator(currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(2) { index ->
            Box(
                modifier = Modifier
                    .size(
                        width = if (currentPage == index) 16.dp else 5.dp,
                        height = 5.dp
                    )
                    .clip(CircleShape)
                    .background(
                        if (currentPage == index)
                            ThemePrimaryColor
                        else
                            Color.White.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

@Composable
private fun ShadePill(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(bottom = 8.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.2f))
    )
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ActionsAndNotificationsPage(
    notifications: List<NotificationItem>,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShadeActionTilesRow()

        if (notifications.isNotEmpty()) {
            ShadeNotificationsSection(
                notifications = notifications,
                onDismissNotification = onDismissNotification,
                onClearAllNotifications = onClearAllNotifications
            )
        }
    }
}

@Composable
private fun ShadeActionTilesRow() {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionTile(
            icon = Icons.Default.Wifi,
            label = stringResource(R.string.shade_wifi),
            modifier = Modifier.weight(1f),
            onClick = {
                context.startActivity(
                    Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
        ActionTile(
            icon = Icons.Default.Bluetooth,
            label = stringResource(R.string.shade_bluetooth),
            modifier = Modifier.weight(1f),
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
        ActionTile(
            icon = Icons.Default.Settings,
            label = stringResource(R.string.common_settings),
            modifier = Modifier.weight(1f),
            onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        )
    }
}

@Composable
private fun ShadeNotificationsSection(
    notifications: List<NotificationItem>,
    onDismissNotification: (String) -> Unit,
    onClearAllNotifications: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.08f))
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.shade_notifications),
            color = ThemePrimaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClearAllNotifications
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.common_clear_all),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        notifications.take(5).forEach { item ->
            NotificationRow(
                item = item,
                onDismiss = { onDismissNotification(item.key) }
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ThemePrimaryColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NowPlayingPage(
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit
) {
    if (nowPlaying != null) {
        ActiveNowPlayingContent(
            nowPlaying = nowPlaying,
            currentPosition = currentPosition,
            duration = duration,
            volume = volume,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onVolumeChange = onVolumeChange,
            onSeek = onSeek
        )
    } else {
        NothingPlayingContent(
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

@Composable
private fun NothingPlayingContent(
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.shade_nothing_playing),
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPrevious
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.shade_previous_cd),
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPause
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.shade_play_cd),
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(20.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNext
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.shade_next_cd),
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveNowPlayingContent(
    nowPlaying: NowPlayingManager.NowPlayingInfo,
    currentPosition: Long,
    duration: Long,
    volume: Float,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSeek: (Long) -> Unit
) {
    var isSeeking by remember { mutableStateOf(false) }
    var seekValue by remember { mutableFloatStateOf(0f) }

    val progressFraction = if (!isSeeking) {
        if (duration > 0L) currentPosition.toFloat() / duration.toFloat() else 0f
    } else {
        seekValue
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShadeTrackInfo(nowPlaying = nowPlaying)

        ShadeSeekBar(
            progressFraction = progressFraction,
            isSeeking = isSeeking,
            seekValue = seekValue,
            duration = duration,
            currentPosition = currentPosition,
            onValueChange = { value ->
                isSeeking = true
                seekValue = value
            },
            onValueChangeFinished = {
                if (duration > 0L) onSeek((seekValue * duration).toLong())
                isSeeking = false
            }
        )

        ShadePlaybackControls(
            nowPlaying = nowPlaying,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext
        )

        ShadeVolumeRow(
            volume = volume,
            onVolumeChange = onVolumeChange
        )
    }
}

@Composable
private fun ShadeTrackInfo(nowPlaying: NowPlayingManager.NowPlayingInfo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = nowPlaying.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )
        if (nowPlaying.artist != null) {
            Text(
                text = nowPlaying.artist,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ShadeSeekBar(
    progressFraction: Float,
    isSeeking: Boolean,
    seekValue: Float,
    duration: Long,
    currentPosition: Long,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = progressFraction,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = ThemePrimaryColor.copy(alpha = 0.2f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val displayPosition =
                if (isSeeking && duration > 0L) (seekValue * duration).toLong()
                else currentPosition
            Text(
                text = formatShadeMs(displayPosition),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
            Text(
                text = formatShadeMs(duration),
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun ShadePlaybackControls(
    nowPlaying: NowPlayingManager.NowPlayingInfo,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ThemePrimaryColor.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPrevious
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.shade_previous_cd),
                tint = ThemePrimaryColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(Modifier.width(20.dp))

        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ThemePrimaryColor)
                .then(if (!nowPlaying.isBuffering) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onPlayPause
                ) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (nowPlaying.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = OledBackgroundColor,
                    strokeWidth = 2.5.dp
                )
            } else {
                Icon(
                    imageVector = if (nowPlaying.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (nowPlaying.isPlaying) R.string.shade_pause_cd else R.string.shade_play_cd
                    ),
                    tint = OledBackgroundColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(Modifier.width(20.dp))

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(ThemePrimaryColor.copy(alpha = 0.12f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onNext
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.shade_next_cd),
                tint = ThemePrimaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ShadeVolumeRow(
    volume: Float,
    onVolumeChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = stringResource(R.string.shade_volume_cd),
            tint = ThemePrimaryColor.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ThemePrimaryColor,
                activeTrackColor = ThemePrimaryColor,
                inactiveTrackColor = ThemePrimaryColor.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onDismiss: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.appLabel,
                color = ThemePrimaryColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            if (!item.title.isNullOrBlank()) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            if (!item.text.isNullOrBlank()) {
                Text(
                    text = item.text,
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.shade_dismiss_cd),
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(11.dp)
            )
        }
    }
}

private fun formatShadeMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, min, sec)
    else "%d:%02d".format(min, sec)
}
