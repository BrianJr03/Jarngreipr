package jr.brian.home.esde.ui

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.wallpaper.WallpaperState
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ESDEWallpaperContainer(
    state: WallpaperState,
    modifier: Modifier = Modifier,
    content: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(state.backgroundColor)
    ) {
        if (state.isVideoPlaying && state.videoPath != null) {
            ESDEVideoPlayer(
                videoPath = state.videoPath,
                modifier = Modifier.fillMaxSize()
            )
        }

        AnimatedWallpaperImage(
            imagePath = state.currentImagePath,
            blurLevel = state.blurLevel,
            animationStyle = state.animationStyle,
            animationDuration = state.animationDuration,
            animationScale = state.animationScale
        )

        DimmingOverlay(alpha = state.dimmingLevel)

        if (state.marqueePath != null) {
            MarqueeImage(
                marqueePath = state.marqueePath,
                modifier = Modifier
                    .size(300.dp, 150.dp)
                    .align(Alignment.Center)
            )
        }

        content?.invoke(this)
    }
}

@Composable
private fun AnimatedWallpaperImage(
    imagePath: String?,
    blurLevel: Float = 0f,
    animationStyle: AnimationStyle = AnimationStyle.Fade,
    animationDuration: Int = 300,
    animationScale: Float = 0.9f,
    modifier: Modifier = Modifier
) {
    if (imagePath == null) return

    // Track previous image path to detect changes
    var previousPath by remember { mutableStateOf<String?>(null) }
    val isSameImage = (previousPath == imagePath)

    LaunchedEffect(imagePath) {
        previousPath = imagePath
    }

    // Animation state
    val scaleAnimatable = remember(imagePath) {
        Animatable(if (isSameImage || animationStyle == AnimationStyle.None) 1f else animationScale)
    }
    val alphaAnimatable = remember(imagePath) {
        Animatable(if (isSameImage || animationStyle == AnimationStyle.None) 1f else 0f)
    }

    // Animate when image changes
    LaunchedEffect(imagePath) {
        if (!isSameImage && animationStyle != AnimationStyle.None) {
            when (animationStyle) {
                AnimationStyle.Fade -> {
                    alphaAnimatable.animateTo(
                        1f,
                        animationSpec = tween(animationDuration)
                    )
                }

                AnimationStyle.ScaleFade,
                AnimationStyle.Custom -> {
                    launch {
                        scaleAnimatable.animateTo(
                            1f,
                            animationSpec = tween(animationDuration)
                        )
                    }
                    launch {
                        alphaAnimatable.animateTo(
                            1f,
                            animationSpec = tween(animationDuration)
                        )
                    }
                }

                else -> {
                    // No animation
                }
            }
        }
    }

    val blurModifier = if (blurLevel > 0f) {
        Modifier.blur(blurLevel.dp)
    } else {
        Modifier
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(File(imagePath))
            .crossfade(animationStyle == AnimationStyle.Fade)
            .build(),
        contentDescription = "Background wallpaper",
        modifier = modifier
            .fillMaxSize()
            .then(blurModifier)
            .graphicsLayer {
                scaleX = scaleAnimatable.value
                scaleY = scaleAnimatable.value
                alpha = alphaAnimatable.value
            },
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
    modifier: Modifier = Modifier
) {
    if (marqueePath == null) return

    val context = LocalContext.current

    // Create ImageLoader with SVG decoder support
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    // Determine the data source - asset URI or file path
    val imageData = remember(marqueePath) {
        if (marqueePath.startsWith("file:///android_asset/")) {
            // Asset URI - pass as string for Coil to handle
            marqueePath
        } else {
            // File path
            File(marqueePath)
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageData)
            .build(),
        imageLoader = imageLoader,
        contentDescription = "System/game logo",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun ESDEVideoPlayer(
    videoPath: String,
    modifier: Modifier = Modifier,
    audioEnabled: Boolean = false,
    onError: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember {
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
        modifier = modifier.fillMaxSize()
    )
}
