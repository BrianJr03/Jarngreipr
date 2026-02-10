package jr.brian.home.esde.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.util.LocalESDEImageLoader
import jr.brian.home.esde.preferences.BackgroundScaleMode
import jr.brian.home.esde.preferences.LogoAlignment
import jr.brian.home.esde.wallpaper.WallpaperState
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.ScrollingDescriptionBox
import java.io.File

private const val DEFAULT_BACKGROUND_PATH = "file:///android_asset/fallback/default_background.webp"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ESDEWallpaperContainer(
    state: WallpaperState,
    modifier: Modifier = Modifier,
    onMarqueeLongClick: (() -> Unit)? = null,
    onWallpaperDoubleClick: (() -> Unit)? = null,
    hideMarquee: Boolean = false,
    pagerScrollProgress: Float = 0f,
    overlayMode: Boolean = true,
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

    val backgroundColor = if (wallpaperType == WallpaperType.TRANSPARENT) {
        Color.Transparent
    } else {
        state.backgroundColor
    }
    val showEsdeContent = wallpaperType == WallpaperType.ESDE

    // Show description instead of marquee when Description mode is selected and description is available
    val showDescription = showEsdeContent &&
//            prefsState.gameImageType == GameImageType.Description &&
            state.gameDescription != null

    val marqueeEnabledForContext = if (state.isShowingGameBackground) {
        prefsState.showMarqueeForGame
    } else {
        prefsState.showMarqueeForSystem
    }

    val showMarquee = showEsdeContent &&
            state.marqueePath != null &&
            !showDescription &&
            marqueeEnabledForContext

    val logoAlignment = when (state.logoAlignment) {
        LogoAlignment.Top -> Alignment.TopCenter
        LogoAlignment.Center -> Alignment.Center
        LogoAlignment.Bottom -> Alignment.BottomCenter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (onWallpaperDoubleClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onDoubleClick = onWallpaperDoubleClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (showEsdeContent) {
            val backgroundScaleMode = if (state.isShowingGameBackground) {
                prefsState.gameBackgroundScaleMode
            } else {
                prefsState.systemBackgroundScaleMode
            }

            val imageModifier = if (prefsState.isAndroidGamesSelected()
                && state.isShowingGameBackground
            ) {
                Modifier
                    .fillMaxSize(0.35f)
                    .align(Alignment.Center)
            } else {
                Modifier.fillMaxSize()
            }

            AnimatedWallpaperImage(
                imagePath = state.currentImagePath ?: DEFAULT_BACKGROUND_PATH,
                modifier = imageModifier,
                blurLevel = effectiveBlurLevel,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale,
                backgroundScaleMode = backgroundScaleMode
            )

            if (!state.isScreensaverActive && !state.isGameRunning) {
                DimmingOverlay(alpha = effectiveDimmingLevel)
            }
        }

        if (showMarquee && !overlayMode) {
            AnimatedMarquee(
                marqueePath = state.marqueePath,
                hideMarquee = hideMarquee,
                logoAlignment = logoAlignment,
                state = state,
                pagerScrollProgress = pagerScrollProgress,
                onLongClick = null,
                dockTopY = dockTopY
            )
        }

        content()

        if (showMarquee && overlayMode) {
            AnimatedMarquee(
                marqueePath = state.marqueePath,
                hideMarquee = hideMarquee,
                logoAlignment = logoAlignment,
                state = state,
                pagerScrollProgress = pagerScrollProgress,
                onLongClick = onMarqueeLongClick,
                dockTopY = dockTopY
            )
        }

        if (showDescription /* && state.gameDescription != null */ && !hideMarquee) {
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
                modifier = Modifier.align(logoAlignment)
            ) {
                ScrollingDescriptionBox(
                    description = state.gameDescription,
                    modifier = Modifier
                        .size(state.marqueeWidth.dp, state.marqueeHeight.dp),
                    scrollSpeed = 30f,
                    pauseDurationMs = 3000
                )
            }
        }

        if (showEsdeContent && (state.isGameRunning || state.isScreensaverActive)) {
            DimmingOverlay(alpha = effectiveDimmingLevel)
        }
    }
}

@Composable
private fun AnimatedWallpaperImage(
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
            imagePath.startsWith("content://") -> Uri.parse(imagePath)
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
private fun BoxScope.AnimatedMarquee(
    marqueePath: String?,
    hideMarquee: Boolean,
    logoAlignment: Alignment,
    state: WallpaperState,
    pagerScrollProgress: Float,
    onLongClick: (() -> Unit)?,
    dockTopY: Float?
) {
    val isUsingDefaultBackground = state.currentImagePath == null
    val density = LocalDensity.current

    val bubbleScale = 1f - (pagerScrollProgress * 0.3f)
    val bubbleAlpha = 1f - (pagerScrollProgress * 0.4f)

    BoxWithConstraints(modifier = Modifier.align(Alignment.Center)) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        
        // Calculate vertical offset to center marquee in available space above dock
        val verticalOffsetDp = if (dockTopY != null) {
            val availableHeight = dockTopY
            val centerY = availableHeight / 2f
            val marqueeTargetY = when (logoAlignment) {
                Alignment.TopCenter -> availableHeight * 0.25f
                Alignment.Center -> centerY
                Alignment.BottomCenter -> availableHeight * 0.75f
                else -> centerY
            }
            with(density) { (marqueeTargetY - screenHeightPx / 2f).toDp() }
        } else {
            0.dp
        }
        
        val finalAlignment = if (dockTopY != null) {
            Alignment.Center  // Use center as base, then apply offset
        } else {
            logoAlignment
        }

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
                .offset(y = verticalOffsetDp)
        ) {
            MarqueeImage(
                marqueePath = marqueePath,
                modifier = Modifier
                    .sizeIn(maxWidth = state.marqueeWidth.dp, maxHeight = state.marqueeHeight.dp)
                    .wrapContentSize()
                    .scale(bubbleScale)
                    .graphicsLayer { alpha = bubbleAlpha },
                animate = isUsingDefaultBackground,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale,
                onLongClick = onLongClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MarqueeImage(
    marqueePath: String?,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    animationStyle: AnimationStyle = AnimationStyle.Fade,
    animationDuration: Int = 300,
    animationScale: Float = 0.9f,
    onLongClick: (() -> Unit)? = null
) {
    if (marqueePath == null) return

    val context = LocalContext.current
    val imageLoader = LocalESDEImageLoader.current

    val imageData = remember(marqueePath) {
        when {
            marqueePath.startsWith("file:///android_asset/") -> marqueePath
            marqueePath.startsWith("content://") -> Uri.parse(marqueePath)
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

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed)

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageData)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "System/game logo",
        modifier = modifier
            .run { with(animationState) { animatedGraphicsLayer() } }
            .scale(pressScale)
            .offset(y = offsetY)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onDoubleClick = {},
                onLongClick = { onLongClick?.invoke() }
            ),
        contentScale = ContentScale.Fit
    )
}

