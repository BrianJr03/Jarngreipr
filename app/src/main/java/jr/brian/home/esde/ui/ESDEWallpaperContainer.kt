package jr.brian.home.esde.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.util.LocalESDEImageLoader
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

@Composable
fun ESDEWallpaperContainer(
    state: WallpaperState,
    modifier: Modifier = Modifier,
    onMarqueeLongClick: (() -> Unit)? = null,
    hideMarquee: Boolean = false,
    pagerScrollProgress: Float = 0f,
    overlayMode: Boolean = true,
    currentPageIndex: Int = 0,
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
    val effectiveDimmingLevel = if (excludeEffectsFromHome && isOnHomePage) 0f else state.dimmingLevel

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
    
    val showMarquee = showEsdeContent &&
            state.marqueePath != null &&
            !showDescription

    val logoAlignment = when (state.logoAlignment) {
        LogoAlignment.Top -> Alignment.TopCenter
        LogoAlignment.Center -> Alignment.Center
        LogoAlignment.Bottom -> Alignment.BottomCenter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (showEsdeContent) {
            AnimatedWallpaperImage(
                imagePath = state.currentImagePath ?: DEFAULT_BACKGROUND_PATH,
                modifier = Modifier.fillMaxSize(),
                blurLevel = effectiveBlurLevel,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale
            )

            if (!state.isScreensaverActive && !state.isGameRunning) {
                DimmingOverlay(alpha = effectiveDimmingLevel)
            }
        }

        if (showMarquee && !overlayMode && !hideMarquee) {
            val isUsingDefaultBackground = state.currentImagePath == null
            MarqueeImage(
                marqueePath = state.marqueePath,
                modifier = Modifier
                    .align(logoAlignment)
                    .size(state.marqueeWidth.dp, state.marqueeHeight.dp),
                animate = isUsingDefaultBackground,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale,
                onLongClick = null
            )
        }

        content()

        if (showMarquee && overlayMode) {
            val isUsingDefaultBackground = state.currentImagePath == null

            val bubbleScale = 1f - (pagerScrollProgress * 0.3f)
            val bubbleAlpha = 1f - (pagerScrollProgress * 0.4f)

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
                modifier = Modifier.align(logoAlignment)
            ) {
                MarqueeImage(
                    marqueePath = state.marqueePath,
                    modifier = Modifier
                        .size(state.marqueeWidth.dp, state.marqueeHeight.dp)
                        .scale(bubbleScale)
                        .graphicsLayer { alpha = bubbleAlpha },
                    animate = isUsingDefaultBackground,
                    animationStyle = state.animationStyle,
                    animationDuration = state.animationDuration,
                    animationScale = state.animationScale,
                    onLongClick = onMarqueeLongClick
                )
            }
        }

        if (showDescription && state.gameDescription != null && !hideMarquee) {
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
    animationScale: Float = 0.9f
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
        if (imagePath.startsWith("file:///android_asset/")) {
            imagePath
        } else {
            File(imagePath)
        }
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
        contentScale = ContentScale.Crop
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
        if (marqueePath.startsWith("file:///android_asset/")) {
            marqueePath
        } else {
            File(marqueePath)
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

