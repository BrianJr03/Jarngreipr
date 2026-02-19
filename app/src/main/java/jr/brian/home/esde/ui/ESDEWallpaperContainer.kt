package jr.brian.home.esde.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.preferences.BackgroundScaleMode
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.preferences.LogoAlignment
import jr.brian.home.esde.ui.components.ScrollingDescriptionBox
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MIXIMAGES
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.wallpaper.WallpaperState
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import java.io.File

private const val DEFAULT_BACKGROUND_PATH = "file:///android_asset/fallback/black.png"

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun ESDEWallpaperContainer(
    state: WallpaperState,
    modifier: Modifier = Modifier,
    onOpenMarqueeShortcut: (() -> Unit)? = null,
    onWallpaperClick: (() -> Unit)? = null,
    onWallpaperDoubleClick: (() -> Unit)? = null,
    hideMarquee: Boolean = false,
    pagerScrollProgress: Float = 0f,
    currentPageIndex: Int = 0,
    dockTopY: Float? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val wallpaperManager = LocalWallpaperManager.current
    val wallpaperType = wallpaperManager.getWallpaperType()

    val homeTabManager = LocalHomeTabManager.current
    val preferencesManager = LocalESDEPreferencesManager.current
    val homeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
    val prefsState by preferencesManager.state.collectAsStateWithLifecycle()

    val isOnHomePage = currentPageIndex == homeTabIndex
    val excludeEffectsFromHome = prefsState.excludeEffectsFromHome
    val effectiveBlurLevel = if (excludeEffectsFromHome && isOnHomePage) 0f else state.blurLevel
    val effectiveDimmingLevel =
        if (excludeEffectsFromHome && isOnHomePage) 0f else state.dimmingLevel

    val logoOnlyMode = prefsState.logoOnlyMode
    val backgroundColor = when {
        wallpaperType == WallpaperType.TRANSPARENT -> Color.Transparent
        logoOnlyMode -> Color.Transparent
        else -> state.backgroundColor
    }
    val showEsdeContent = wallpaperType == WallpaperType.ESDE
    val showEsdeBackground = showEsdeContent && !logoOnlyMode

    val isDescriptionOverlayEnabled = prefsState.isDescriptionOverlayOnPage(currentPageIndex)
    val showDescription = showEsdeContent && isDescriptionOverlayEnabled

    val marqueeEnabledForContext = if (state.isShowingGameBackground) {
        prefsState.showMarqueeForGame
    } else {
        prefsState.showMarqueeForSystem
    }

    val showLogo = showEsdeContent &&
            state.logoPath != null &&
            marqueeEnabledForContext

    val logoAlignment = when (prefsState.logoAlignment) {
        LogoAlignment.TopLeft -> Alignment.TopStart
        LogoAlignment.Top -> Alignment.TopCenter
        LogoAlignment.TopRight -> Alignment.TopEnd
        LogoAlignment.Center -> Alignment.Center
        LogoAlignment.BottomLeft -> Alignment.BottomStart
        LogoAlignment.Bottom -> Alignment.BottomCenter
        LogoAlignment.BottomRight -> Alignment.BottomEnd
        LogoAlignment.FreePosition -> Alignment.Center
    }

    val isFreePosition = prefsState.logoAlignment == LogoAlignment.FreePosition
    val isMarqueeDraggable = isFreePosition && !prefsState.marqueePositionLocked
    val useAnimatedLogo = !prefsState.marqueePositionLocked

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (onWallpaperDoubleClick != null || onWallpaperClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onWallpaperClick?.invoke() },
                        onDoubleClick = { onWallpaperDoubleClick?.invoke() }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (showEsdeBackground) {
            val backgroundScaleMode = if (state.isShowingGameBackground) {
                prefsState.gameBackgroundScaleMode
            } else {
                prefsState.systemBackgroundScaleMode
            }

            val hasMiximage = state.currentImagePath?.contains(FOLDER_MIXIMAGES) == true
            val useSmallSize = prefsState.isAndroidGamesSelected()
                && state.isShowingGameBackground
                && !hasMiximage
            val androidGamesScale = prefsState.androidGamesBackgroundScale

            if (state.systemBackgroundVideoPath != null) {
                val videoModifier = if (useSmallSize) {
                    Modifier
                        .fillMaxSize(androidGamesScale)
                        .align(Alignment.Center)
                } else {
                    Modifier.fillMaxSize()
                }
                VideoBackground(
                    videoPath = state.systemBackgroundVideoPath,
                    modifier = videoModifier,
                    blurLevel = effectiveBlurLevel,
                    backgroundScaleMode = backgroundScaleMode
                )
            } else {
                AnimatedWallpaperImage(
                imagePath = state.currentImagePath ?: DEFAULT_BACKGROUND_PATH,
                useSmallSize = useSmallSize,
                smallSizeScale = androidGamesScale,
                blurLevel = effectiveBlurLevel,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale,
                backgroundScaleMode = backgroundScaleMode
            )
            }

            if (!state.isScreensaverActive && !state.isGameRunning && !logoOnlyMode) {
                DimmingOverlay(alpha = effectiveDimmingLevel)
            }
        }

        if (showLogo && !useAnimatedLogo) {
            AnimatedLogo(
                logoPath = state.logoPath,
                hideMarquee = hideMarquee,
                logoAlignment = logoAlignment,
                state = state,
                pagerScrollProgress = pagerScrollProgress,
                openMarqueeShortcut = null,
                dockTopY = dockTopY,
                isFreePosition = isFreePosition,
                isDraggable = isMarqueeDraggable,

                isPositionLocked = prefsState.marqueePositionLocked,
                freeOffsetX = prefsState.logoOffsetX,
                freeOffsetY = prefsState.logoOffsetY,
                onOffsetChange = { x, y -> preferencesManager.setLogoOffset(x, y) },
                minWidthPercent = prefsState.marqueeMinWidthPercent
            )
        }

        content()

        if (showLogo && useAnimatedLogo) {
            AnimatedLogo(
                logoPath = state.logoPath,
                hideMarquee = hideMarquee,
                logoAlignment = logoAlignment,
                state = state,
                pagerScrollProgress = pagerScrollProgress,
                openMarqueeShortcut = onOpenMarqueeShortcut,
                dockTopY = dockTopY,
                isFreePosition = isFreePosition,
                isDraggable = isMarqueeDraggable,
                isPositionLocked = prefsState.marqueePositionLocked,
                freeOffsetX = prefsState.logoOffsetX,
                freeOffsetY = prefsState.logoOffsetY,
                onOffsetChange = { x, y -> preferencesManager.setLogoOffset(x, y) },
                minWidthPercent = prefsState.marqueeMinWidthPercent
            )
        }

        val descriptionText = state.gameDescription
        if (showDescription && !hideMarquee && descriptionText != null) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(
                    animationSpec = tween(durationMillis = 400)
                ) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    transformOrigin = TransformOrigin.Center
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 300)
                ) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(durationMillis = 300),
                    transformOrigin = TransformOrigin.Center
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ScrollingDescriptionBox(
                    description = descriptionText,
                    modifier = Modifier
                        .size(
                            width = state.marqueeWidth.dp,
                            height = state.marqueeHeight.dp
                        ),
                    scrollSpeed = 30f,
                    pauseDurationMs = 3000
                )
            }
        }

        if (showEsdeBackground && (state.isGameRunning || state.isScreensaverActive)) {
            DimmingOverlay(alpha = effectiveDimmingLevel)
        }
    }
}

private data class WallpaperAnimationState(
    val imagePath: String,
    val useSmallSize: Boolean,
    val smallSizeScale: Float,
    val backgroundScaleMode: BackgroundScaleMode
)

@Composable
private fun AnimatedWallpaperImage(
    imagePath: String,
    useSmallSize: Boolean = false,
    smallSizeScale: Float = 0.5f,
    blurLevel: Float = 0f,
    animationStyle: AnimationStyle = AnimationStyle.Fade,
    animationDuration: Int = 300,
    animationScale: Float = 0.9f,
    backgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = WallpaperAnimationState(imagePath, useSmallSize, smallSizeScale, backgroundScaleMode),
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 400)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 400))
            },
            label = "WallpaperTransition"
        ) { state ->
            val imageModifier = if (state.useSmallSize) {
                Modifier.fillMaxSize(state.smallSizeScale)
            } else {
                Modifier.fillMaxSize()
            }
            WallpaperImage(
                imagePath = state.imagePath,
                modifier = imageModifier,
                blurLevel = blurLevel,
                animationStyle = animationStyle,
                animationDuration = animationDuration,
                animationScale = animationScale,
                backgroundScaleMode = state.backgroundScaleMode
            )
        }
    }
}

@Composable
private fun WallpaperImage(
    imagePath: String,
    modifier: Modifier = Modifier,
    blurLevel: Float = 0f,
    animationStyle: AnimationStyle = AnimationStyle.Fade,
    animationDuration: Int = 300,
    animationScale: Float = 0.9f,
    backgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop
) {
    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current

    val animationState = rememberImageAnimationState(
        imagePath = imagePath,
        animate = true,
        animationStyle = animationStyle,
        animationDuration = animationDuration,
        animationScale = animationScale
    )

    val blurModifier = if (blurLevel > 0f) {
        Modifier.blur(blurLevel.dp)
    } else {
        Modifier
    }

    val imageData = remember(imagePath) {
        when {
            imagePath.startsWith("file:///android_asset/") -> imagePath
            imagePath.startsWith("content://") -> imagePath.toUri()
            else -> File(imagePath)
        }
    }

    val contentScale = when (backgroundScaleMode) {
        BackgroundScaleMode.Crop -> ContentScale.Crop
        BackgroundScaleMode.Fit -> ContentScale.Fit
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageData)
            .crossfade(animationStyle == AnimationStyle.Fade)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "Background wallpaper",
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier)
            .run { with(animationState) { animatedGraphicsLayer() } },
        contentScale = contentScale
    )
}

@Composable
private fun DimmingOverlay(alpha: Float) {
    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = alpha))
        )
    }
}

@Composable
private fun BoxScope.AnimatedLogo(
    logoPath: String?,
    hideMarquee: Boolean,
    logoAlignment: Alignment,
    state: WallpaperState,
    pagerScrollProgress: Float,
    openMarqueeShortcut: (() -> Unit)?,
    dockTopY: Float?,
    isFreePosition: Boolean = false,
    isDraggable: Boolean = false,
    isPositionLocked: Boolean = false,
    freeOffsetX: Float = 0f,
    freeOffsetY: Float = 0f,
    onOffsetChange: ((Float, Float) -> Unit)? = null,
    minWidthPercent: Float = 0.5f
) {
    val isUsingDefaultBackground = state.currentImagePath == null
    val density = LocalDensity.current

    val bubbleScale = 1f - (pagerScrollProgress * 0.3f)
    val bubbleAlpha = 1f - (pagerScrollProgress * 0.4f)

    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetX by remember { mutableFloatStateOf(freeOffsetX) }
    var dragOffsetY by remember { mutableFloatStateOf(freeOffsetY) }

    LaunchedEffect(freeOffsetX, freeOffsetY, isDragging) {
        if (!isDragging) {
            dragOffsetX = freeOffsetX
            dragOffsetY = freeOffsetY
        }
    }

    BoxWithConstraints(modifier = Modifier.align(Alignment.Center)) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }

        val verticalOffsetDp = if (dockTopY != null && !isFreePosition && !isPositionLocked) {
            val centerY = dockTopY / 2f
            val marqueeTargetY = when (logoAlignment) {
                Alignment.TopCenter -> dockTopY * 0.25f
                Alignment.Center -> centerY
                Alignment.BottomCenter -> dockTopY * 0.75f
                else -> centerY
            }
            with(density) { (marqueeTargetY - screenHeightPx / 2f).toDp() }
        } else {
            0.dp
        }

        val finalAlignment = if ((dockTopY != null && !isPositionLocked) || isFreePosition) {
            Alignment.Center
        } else {
            logoAlignment
        }

        val freeOffsetXDp = if (isFreePosition) with(density) { dragOffsetX.toDp() } else 0.dp
        val freeOffsetYDp =
            if (isFreePosition) with(density) { dragOffsetY.toDp() } else verticalOffsetDp

        AnimatedVisibility(
            visible = !hideMarquee,
            enter = fadeIn(
                animationSpec = tween(durationMillis = 400)
            ) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                transformOrigin = TransformOrigin.Center
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = 300)
            ) + scaleOut(
                targetScale = 0.6f,
                animationSpec = tween(durationMillis = 300),
                transformOrigin = TransformOrigin.Center
            ),
            modifier = Modifier
                .align(finalAlignment)
                .offset(
                    x = freeOffsetXDp,
                    y = freeOffsetYDp
                )
                .then(
                    if (isDraggable) {
                        Modifier.pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = {
                                    isDragging = true
                                },
                                onDragEnd = {
                                    isDragging = false
                                    onOffsetChange?.invoke(dragOffsetX, dragOffsetY)
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(
                                        -screenWidthPx / 2f + 50f,
                                        screenWidthPx / 2f - 50f
                                    )
                                    dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(
                                        -screenHeightPx / 2f + 50f,
                                        screenHeightPx / 2f - 50f
                                    )
                                }
                            )
                        }
                    } else Modifier
                )
        ) {
            val minWidth = (state.marqueeWidth * minWidthPercent).dp
            MarqueeImage(
                marqueePath = logoPath,
                modifier = Modifier
                    .sizeIn(
                        minWidth = minWidth,
                        maxWidth = state.marqueeWidth.dp, 
                        maxHeight = state.marqueeHeight.dp
                    )
                    .scale(bubbleScale)
                    .graphicsLayer { alpha = bubbleAlpha },
                animate = isUsingDefaultBackground,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale,
                onClick = if (isFreePosition) openMarqueeShortcut else null,
                onLongClick = if (!isFreePosition) openMarqueeShortcut else null
            )
        }
    }
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarqueeImage(
    marqueePath: String?,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    animationStyle: AnimationStyle = AnimationStyle.Fade,
    animationDuration: Int = 300,
    animationScale: Float = 0.9f,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    if (marqueePath == null) return

    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current

    val imageData = remember(marqueePath) {
        when {
            marqueePath.startsWith("file:///android_asset/") -> marqueePath
            marqueePath.startsWith("content://") -> marqueePath.toUri()
            else -> File(marqueePath)
        }
    }

    val animationState = rememberImageAnimationState(
        imagePath = marqueePath,
        animate = animate,
        animationStyle = animationStyle,
        animationDuration = animationDuration,
        animationScale = animationScale
    )

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageData)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "System/game logo",
        modifier = modifier
            .run { with(animationState) { animatedGraphicsLayer() } }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onDoubleClick = { onClick?.invoke() },
                onLongClick = { onLongClick?.invoke() }
            ),
        contentScale = ContentScale.Fit
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoBackground(
    videoPath: String,
    modifier: Modifier = Modifier,
    blurLevel: Float = 0f,
    backgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop
) {
    val context = LocalContext.current

    val videoUri = remember(videoPath) {
        when {
            videoPath.startsWith("content://") -> videoPath.toUri()
            else -> Uri.fromFile(File(videoPath))
        }
    }

    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            try {
                setMediaItem(MediaItem.fromUri(videoUri))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f
                prepare()
                playWhenReady = true
            } catch (e: Exception) {
                android.util.Log.e("VideoBackground", "Failed to load video: $videoPath", e)
            }
        }
    }

    DisposableEffect(videoPath) {
        onDispose {
            exoPlayer.release()
        }
    }

    val blurModifier = if (blurLevel > 0f) {
        Modifier.blur(blurLevel.dp)
    } else {
        Modifier
    }

    val resizeMode = when (backgroundScaleMode) {
        BackgroundScaleMode.Crop -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        BackgroundScaleMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.resizeMode = resizeMode
        },
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier)
    )
}
