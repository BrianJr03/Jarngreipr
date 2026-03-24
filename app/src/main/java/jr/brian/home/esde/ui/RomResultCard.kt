package jr.brian.home.esde.ui

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.util.ESDEMediaConstants
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.animations.animatedFlip
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.colors.animatedGradientBorder
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.themePrimaryColor
import jr.brian.home.ui.theme.themeSecondaryColor
import java.io.File
import android.view.KeyEvent as AndroidKeyEvent

@Composable
private fun glowPulseAlpha(isFocused: Boolean): Float {
    return if (isFocused) {
        val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowPulse"
        )
        pulse
    } else {
        1f
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RomResultCard(
    game: GameInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit = {},
    autoFocus: Boolean = false,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onToggleKeyboard: () -> Unit = {},
    mediaType: RomSearchCardMediaType = RomSearchCardMediaType.PhysicalMedia,
    focusAnimationEnabled: Boolean = false,
    isFocusAnimationDisabled: Boolean = false,
    flipEnabled: Boolean = false,
    flipDisabledForGame: Boolean = false
) {
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isFocused by remember { mutableStateOf(false) }
    val scale = animatedFocusedScale(isFocused)

    val isDiscPlatform = remember(game.systemName) {
        game.systemName.lowercase() in ESDEMediaConstants.DISC_PLATFORMS
    }

    val shouldFlip = flipEnabled && !flipDisabledForGame && !(isDiscPlatform  && mediaType == RomSearchCardMediaType.PhysicalMedia)
    val flipRotation = animatedFlip(isFocused = isFocused && shouldFlip, durationMillis = 1200)

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
            }
        }
    }

    val shouldSpin = focusAnimationEnabled && !isFocusAnimationDisabled &&
            mediaType == RomSearchCardMediaType.PhysicalMedia && isDiscPlatform
    val discRotation = animatedRotation(isFocused = isFocused && shouldSpin, durationMillis = 1200)

    val imageData = remember(game, mediaType) {
        val path = when (mediaType) {
            RomSearchCardMediaType.PhysicalMedia -> game.physicalMediaPath ?: game.artworkPath
            RomSearchCardMediaType.Covers -> game.artworkPath ?: game.physicalMediaPath
            RomSearchCardMediaType.Screenshots -> game.screenshotPath ?: game.physicalMediaPath
            ?: game.artworkPath

            RomSearchCardMediaType.Fanart -> game.fanartPath ?: game.physicalMediaPath
            ?: game.artworkPath

            RomSearchCardMediaType.TitleScreens -> game.titlescreenPath ?: game.physicalMediaPath
            ?: game.artworkPath

            RomSearchCardMediaType.Marquee -> game.marqueeImagePath ?: game.physicalMediaPath
            ?: game.artworkPath

            RomSearchCardMediaType.MixImages -> game.miximagePath ?: game.physicalMediaPath
            ?: game.artworkPath
        }
        path?.let { File(it) }
    }

    val hasImage = imageData != null
    val shape = RoundedCornerShape(8.dp)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "glowAlpha"
    )

    val glowRadius by animateDpAsState(
        targetValue = if (isFocused) 2.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = EaseInOutCubic),
        label = "glowRadius"
    )

    val glowPulse = glowPulseAlpha(isFocused)
    val primary = themePrimaryColor()

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .then(if (hasImage) Modifier.scale(scale) else Modifier)
            .drawBehind {
                if (glowAlpha > 0f && hasImage) {
                    val pulse = if (isFocused) glowPulse else 1f
                    val radius = (size.maxDimension / 1.7f) + glowRadius.toPx()
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 1.3f * glowAlpha * pulse),
                                Color.Transparent
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }
            }
            .then(
                if (!hasImage && isFocused) Modifier.animatedGradientBorder(
                    colors = listOf(primary, themeSecondaryColor()),
                    shape = shape
                )
                else if (!hasImage) Modifier.border(width = 2.dp, Color.DarkGray, shape = shape)
                else Modifier
            )
            .clip(shape)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    onFocused()
                }
            }
            .onKeyEvent { keyEvent ->
                when (keyEvent.type) {
                    KeyEventType.KeyUp if keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BUTTON_SELECT -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleKeyboard()
                        true
                    }

                    KeyEventType.KeyUp if keyEvent.nativeKeyEvent.keyCode == AndroidKeyEvent.KEYCODE_BUTTON_START -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                        true
                    }

                    else -> false
                }
            }
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (hasImage) 1f else 4f / 3f)
            ) {
                if (hasImage) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageData)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = game.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(0.85f)
                            .then(if (shouldSpin) Modifier.rotate(discRotation) else Modifier)
                            .then(if (shouldFlip) Modifier.graphicsLayer {
                                rotationY = flipRotation
                            } else Modifier),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = game.name,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 13.sp
                    )
                }

                if (game.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ThemeAccentColor,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(14.dp)
                    )
                }

                if (!hasImage) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = game.systemName.uppercase(),
                            color = ThemeAccentColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
