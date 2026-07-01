package jr.brian.home.esde.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
import jr.brian.home.esde.ui.frontend.FrontendTokens
import jr.brian.home.esde.ui.frontend.emitFocusHapticIfReady
import jr.brian.home.esde.ui.frontend.focusFloatPhase
import jr.brian.home.esde.ui.frontend.rememberFloatAmplitude
import jr.brian.home.esde.util.ESDEMediaConstants
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.ui.animations.animatedFlip
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.animations.animatedRotation
import jr.brian.home.ui.theme.ThemeAccentColor
import java.io.File
import android.view.KeyEvent as AndroidKeyEvent

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RomResultCard(
    game: GameInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocused: () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    onToggleKeyboard: () -> Unit = {},
    mediaType: RomSearchCardMediaType = RomSearchCardMediaType.PhysicalMedia,
    focusAnimationEnabled: Boolean = false,
    isFocusAnimationDisabled: Boolean = false,
    flipEnabled: Boolean = false,
    flipDisabledForGame: Boolean = false,
    focusAnimationDelayMs: Int = 150
) {
    val imageLoader = LocalESDEImageLoader.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    var isFocused by remember { mutableStateOf(false) }
    var isFocusedDelayed by remember { mutableStateOf(false) }
    val scale = animatedFocusedScale(isFocused)

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(focusAnimationDelayMs.toLong())
            isFocusedDelayed = true
        } else {
            isFocusedDelayed = false
        }
    }

    val isDiscPlatform = remember(game.systemName) {
        game.systemName.lowercase() in ESDEMediaConstants.DISC_PLATFORMS
    }

    val shouldFlip = flipEnabled && !flipDisabledForGame && !(isDiscPlatform  && mediaType == RomSearchCardMediaType.PhysicalMedia)
    val flipRotation = animatedFlip(isFocused = isFocusedDelayed && shouldFlip, durationMillis = 1200)

    val shouldSpin = focusAnimationEnabled && !isFocusAnimationDisabled &&
            mediaType == RomSearchCardMediaType.PhysicalMedia && isDiscPlatform
    val discRotation = animatedRotation(isFocused = isFocusedDelayed && shouldSpin, durationMillis = 1200)

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

    val isAndroidEntry = imageData == null && (
        game.systemName.equals("androidapps", ignoreCase = true) ||
        game.systemName.equals("androidgames", ignoreCase = true)
    )
    val appIcon = remember(game.path, isAndroidEntry) {
        if (!isAndroidEntry) null
        else runCatching {
            val pkg = game.path.trimEnd('/').removeSuffix(".app")
            context.packageManager.getApplicationIcon(pkg)
        }.getOrNull()
    }

    val hasImage = imageData != null || appIcon != null
    val shape = RoundedCornerShape(8.dp)
    val focusProgress by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(
            durationMillis = FrontendTokens.Motion.FocusMs,
            easing = FrontendTokens.Motion.Easing
        ),
        label = "romCardFocus"
    )
    val floatPhase = focusFloatPhase(isFocused)
    val floatAmplitude = rememberFloatAmplitude()

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .graphicsLayer {
                translationY = (-FOCUS_LIFT.toPx() + floatAmplitude.toPx() * floatPhase) * focusProgress
            }
            .scale(scale)
            .then(
                if (!hasImage) Modifier.border(
                    width = 2.dp,
                    color = lerp(Color.DarkGray, ThemeAccentColor, focusProgress),
                    shape = shape
                ) else Modifier
            )
            .clip(shape)
            .then(if (!hasImage) Modifier.background(Color.DarkGray) else Modifier)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    view.emitFocusHapticIfReady()
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
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
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
                if (imageData != null) {
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
                } else if (appIcon != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(appIcon)
                            .crossfade(true)
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = game.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(0.75f)
                            .then(if (shouldFlip) Modifier.graphicsLayer {
                                rotationY = flipRotation
                            } else Modifier),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    AnimatedGameTitle(
                        name = game.name,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
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

private val FOCUS_LIFT = FrontendTokens.Spacing.M
