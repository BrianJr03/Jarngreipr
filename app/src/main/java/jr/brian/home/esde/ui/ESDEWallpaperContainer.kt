package jr.brian.home.esde.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.preferences.LogoAlignment
import jr.brian.home.esde.preferences.ScreensaverBehavior
import jr.brian.home.esde.wallpaper.WallpaperState
import jr.brian.home.ui.screens.PoweredOffScreen
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import java.io.File

private const val DEFAULT_BACKGROUND_PATH = "file:///android_asset/fallback/default_background.webp"

@Composable
fun ESDEWallpaperContainer(
    state: WallpaperState,
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    val rememberContent = remember { content }
    val wallpaperManager = LocalWallpaperManager.current
    val wallpaperType = wallpaperManager.getWallpaperType()

    val backgroundColor = if (wallpaperType == WallpaperType.TRANSPARENT) {
        Color.Transparent
    } else {
        state.backgroundColor
    }
    val showEsdeContent = wallpaperType == WallpaperType.ESDE
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (showEsdeContent) {
            AnimatedWallpaperImage(
                imagePath = state.currentImagePath ?: DEFAULT_BACKGROUND_PATH,
                modifier = Modifier.fillMaxSize(),
                blurLevel = state.blurLevel,
                animationStyle = state.animationStyle,
                animationDuration = state.animationDuration,
                animationScale = state.animationScale
            )

            if (state.isVideoPlaying && state.videoPath != null) {
                ESDEVideoPlayer(
                    videoPath = state.videoPath,
                    audioEnabled = state.videoAudioEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }

            val isUsingDefaultBackground = state.currentImagePath == null
            
            if (
                state.showSystemLogo
                && state.marqueePath != null
                && !state.isVideoPlaying
            ) {
                val logoAlignment = when (state.logoAlignment) {
                    LogoAlignment.Top -> Alignment.TopCenter
                    LogoAlignment.Center -> Alignment.Center
                    LogoAlignment.Bottom -> Alignment.BottomCenter
                }
                MarqueeImage(
                    marqueePath = state.marqueePath,
                    modifier = Modifier
                        .size(state.marqueeWidth.dp, state.marqueeHeight.dp)
                        .align(logoAlignment),
                    animate = isUsingDefaultBackground,
                    animationStyle = state.animationStyle,
                    animationDuration = state.animationDuration,
                    animationScale = state.animationScale
                )
            }
        }

        val hideForVideo = state.hideContentOnVideo && state.isVideoPlaying && showEsdeContent
        val hideForScreensaver = state.isScreensaverActive && 
            state.screensaverBehavior == ScreensaverBehavior.PowerOff && 
            showEsdeContent
        val shouldShowContent = !hideForVideo && !hideForScreensaver

        if (state.isScreensaverActive &&
            state.screensaverBehavior == ScreensaverBehavior.PowerOff && 
            showEsdeContent
        ) {
            PoweredOffScreen(
                onPowerOn = {}
            )
        }

        if (shouldShowContent) {
            rememberContent?.invoke(this)
        }

        if (showEsdeContent && !state.isVideoPlaying) {
            DimmingOverlay(alpha = state.dimmingLevel)
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

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
            }
            .build()
    }

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

@Composable
private fun MarqueeImage(
    marqueePath: String?,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    animationStyle: AnimationStyle = AnimationStyle.Fade,
    animationDuration: Int = 300,
    animationScale: Float = 0.9f
) {
    if (marqueePath == null) return

    val context = LocalContext.current

    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

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

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageData)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "System/game logo",
        modifier = modifier
            .run { with(animationState) { animatedGraphicsLayer() } },
        contentScale = ContentScale.Fit
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun ESDEVideoPlayer(
    videoPath: String,
    modifier: Modifier = Modifier,
    audioEnabled: Boolean = false,
    onError: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember(videoPath) {
        ExoPlayer.Builder(context).build().apply {
            try {
                val file = File(videoPath)
                if (file.exists()) {
                    setMediaItem(MediaItem.fromUri(file.toUri()))
                    repeatMode = Player.REPEAT_MODE_ALL
                    volume = if (audioEnabled) 1f else 0f
                    prepare()
                    playWhenReady = true
                }
            } catch (_: Exception) {
                onError()
            }
        }
    }

    LaunchedEffect(audioEnabled) {
        exoPlayer.volume = if (audioEnabled) 1f else 0f
    }

    DisposableEffect(videoPath) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        },
        modifier = modifier.fillMaxSize()
    )
}
