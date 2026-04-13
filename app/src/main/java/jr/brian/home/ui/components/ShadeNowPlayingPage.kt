package jr.brian.home.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.data.NowPlayingManager
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
internal fun NowPlayingPage(
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
    PlayerArtworkCard(
        nowPlaying = null,
        onPlayPause = onPlayPause,
        onPrevious = onPrevious,
        onNext = onNext
    )
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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PlayerArtworkCard(
            nowPlaying = nowPlaying,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext
        )

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

        AnimatedVisibility(visible = !nowPlaying.isSystemMedia) {
            ShadeVolumeRow(
                volume = volume,
                onVolumeChange = onVolumeChange
            )
        }
    }
}

@Composable
private fun PlayerArtworkCard(
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
    ) {
        ArtworkBackground(imageUrl = nowPlaying?.imageUrl)
        ArtworkGradientScrim()
        ArtworkOverlayContent(
            nowPlaying = nowPlaying,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

@Composable
private fun ArtworkBackground(imageUrl: String?) {
    if (!imageUrl.isNullOrEmpty()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        ArtworkPlaceholder()
    }
}

@Composable
private fun ArtworkPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun ArtworkGradientScrim() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.35f to Color.Black.copy(alpha = 0.15f),
                    1f to Color.Black.copy(alpha = 0.88f)
                )
            )
    )
}

@Composable
private fun BoxScope.ArtworkOverlayContent(
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (nowPlaying != null) {
            TrackInfoRow(nowPlaying = nowPlaying)
        } else {
            NothingPlayingLabel()
        }
        PlayerControlsRow(
            nowPlaying = nowPlaying,
            onPlayPause = onPlayPause,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

@Composable
private fun NothingPlayingLabel() {
    Text(
        text = stringResource(R.string.shade_nothing_playing),
        color = Color.White.copy(alpha = 0.35f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun TrackInfoRow(nowPlaying: NowPlayingManager.NowPlayingInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nowPlaying.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            if (nowPlaying.artist != null) {
                Text(
                    text = nowPlaying.artist,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        if (nowPlaying.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = ThemePrimaryColor,
                strokeWidth = 1.5.dp
            )
        }
    }
}

@Composable
private fun PlayerControlsRow(
    nowPlaying: NowPlayingManager.NowPlayingInfo?,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val isActive = nowPlaying != null
    val buttonBg = if (isActive) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f)
    val iconTint = if (isActive) Color.White else Color.White.copy(alpha = 0.3f)
    val playBg = if (isActive) ThemePrimaryColor else Color.White.copy(alpha = 0.09f)
    val playIconTint = if (isActive) OledBackgroundColor else Color.White.copy(alpha = 0.3f)
    val isPlayClickable = nowPlaying?.isBuffering != true

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        MediaControlButton(size = 34.dp, backgroundColor = buttonBg, onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = stringResource(R.string.shade_previous_cd),
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(playBg)
                .then(
                    if (isPlayClickable) Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPlayPause
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (nowPlaying?.isBuffering == true) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = OledBackgroundColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = if (nowPlaying?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (nowPlaying?.isPlaying == true) R.string.shade_pause_cd else R.string.shade_play_cd
                    ),
                    tint = playIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        MediaControlButton(size = 34.dp, backgroundColor = buttonBg, onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = stringResource(R.string.shade_next_cd),
                tint = iconTint,
                modifier = Modifier.size(18.dp)
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
        Text(
            text = stringResource(R.string.volume_percentage, (volume * 100).toInt()),
            color = ThemePrimaryColor.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MediaControlButton(
    size: Dp,
    backgroundColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}

private fun formatShadeMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0L)
    val hours = totalSec / 3600
    val min = (totalSec % 3600) / 60
    val sec = totalSec % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, min, sec)
    else "%d:%02d".format(min, sec)
}
