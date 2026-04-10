package jr.brian.home.ui.screens.rss

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import jr.brian.home.R
import jr.brian.home.model.rss.RssItem
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.subtleCardGradient
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.util.animatedColor
import jr.brian.home.ui.util.formatPubDate
import jr.brian.home.ui.util.stripHtml

@Composable
internal fun RssItemCard(
    item: RssItem,
    isCurrentlyPlaying: Boolean,
    modifier: Modifier = Modifier,
    useDMYDateFormat: Boolean,
    use24HourClock: Boolean,
    onClick: () -> Unit,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    val hasImage = item.imageUrl.isNotEmpty()
    val hasVideo = item.videoUrl.isNotEmpty()
    val hasAudio = item.audioUrl.isNotEmpty()
    val plainDescription = remember(item.description) {
        stripHtml(item.description).trim()
    }
    val animColor = animatedColor()
    val audioIconColor = if (isCurrentlyPlaying) animColor else ThemeAccentColor
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(brush = subtleCardGradient(false), shape = RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                brush = borderBrush(isFocused = false),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column {
            if (hasImage || hasVideo) {
                val mediaUrl = if (hasImage) item.imageUrl else item.videoUrl
                MediaThumbnail(
                    url = mediaUrl,
                    isVideo = !hasImage,
                    isAudio = hasAudio && !hasVideo,
                    isCurrentlyPlaying = isCurrentlyPlaying,
                    onVideoClick = onVideoClick,
                    onAudioClick = onAudioClick
                )
            }

            Column(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = if (hasImage || hasVideo) 10.dp else 14.dp,
                    bottom = 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (hasVideo && hasImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onVideoClick)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = stringResource(R.string.rss_tab_play_video_cd),
                            tint = ThemeAccentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.rss_tab_video_available),
                            color = ThemeAccentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                if (hasAudio && !hasVideo && !hasImage) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable(onClick = onAudioClick)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = stringResource(R.string.rss_tab_play_audio_cd),
                            tint = audioIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.rss_tab_listen),
                            color = audioIconColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                if (plainDescription.isNotEmpty()) {
                    Text(
                        text = plainDescription,
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                if (item.pubDate.isNotEmpty()) {
                    Text(
                        text = formatPubDate(item.pubDate, useDMYDateFormat, use24HourClock),
                        color = ThemeSecondaryColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
internal fun MediaThumbnail(
    url: String,
    isVideo: Boolean,
    isAudio: Boolean,
    isCurrentlyPlaying: Boolean = false,
    onVideoClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    val context = LocalContext.current
    var imageState by remember { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }

    val imageAlpha by animateFloatAsState(
        targetValue = if (imageState is AsyncImagePainter.State.Success) 1f else 0f,
        animationSpec = tween(300),
        label = "image_fade"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(Color.White.copy(alpha = 0.06f))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(url)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onState = { imageState = it },
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (imageAlpha < 1f) Modifier else Modifier
                )
        )

        if (isVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onVideoClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = stringResource(R.string.rss_tab_play_video_cd),
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        if (isAudio) {
            val thumbnailAudioColor = animatedColor(
                firstSeen = isCurrentlyPlaying,
                fallbackColor = Color.White.copy(alpha = 0.9f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onAudioClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = stringResource(R.string.rss_tab_play_audio_cd),
                    tint = thumbnailAudioColor,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = imageState is AsyncImagePainter.State.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = ThemePrimaryColor.copy(alpha = 0.4f),
                    strokeWidth = 2.dp
                )
            }
        }

        if (imageState is AsyncImagePainter.State.Error) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }
    }
}
