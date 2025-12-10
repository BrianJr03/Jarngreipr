package jr.brian.home.ui.components.wallpaper

import android.content.Context
import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.theme.oledBackgroundColor

@Composable
fun WallpaperDisplay(
    wallpaperUri: String?,
    wallpaperType: WallpaperType,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    var showError by remember(wallpaperUri, wallpaperType) { mutableStateOf(false) }

    LaunchedEffect(showError) {
        if (showError && wallpaperType != WallpaperType.NONE) {
            wallpaperManager.clearWallpaper()
        }
    }

    when (wallpaperType) {
        WallpaperType.NONE -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(oledBackgroundColor())
            )
        }

        WallpaperType.TRANSPARENT -> {
            Box(modifier = modifier.fillMaxSize())
        }

        WallpaperType.IMAGE -> {
            if (showError) {
                FallbackWallpaper(modifier)
            } else {
                wallpaperUri?.let { uri ->
                    LaunchedEffect(uri) {
                        showError = !isUriAccessible(context, uri)
                    }

                    if (!showError) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = uri.toUri(),
                                onError = { showError = true }
                            ),
                            contentDescription = "Wallpaper",
                            modifier = modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } ?: FallbackWallpaper(modifier)
            }
        }

        WallpaperType.GIF -> {
            if (showError) {
                FallbackWallpaper(modifier)
            } else {
                wallpaperUri?.let { uri ->
                    LaunchedEffect(uri) {
                        showError = !isUriAccessible(context, uri)
                    }

                    if (!showError) {
                        val imageLoader = remember {
                            ImageLoader.Builder(context)
                                .components {
                                    if (Build.VERSION.SDK_INT >= 28) {
                                        add(ImageDecoderDecoder.Factory())
                                    } else {
                                        add(GifDecoder.Factory())
                                    }
                                }
                                .build()
                        }

                        Image(
                            painter = rememberAsyncImagePainter(
                                model = uri.toUri(),
                                imageLoader = imageLoader,
                                onError = { showError = true }
                            ),
                            contentDescription = "Animated Wallpaper",
                            modifier = modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } ?: FallbackWallpaper(modifier)
            }
        }

        WallpaperType.VIDEO -> {
            if (showError) {
                FallbackWallpaper(modifier)
            } else {
                wallpaperUri?.let { uri ->
                    LaunchedEffect(uri) {
                        showError = !isUriAccessible(context, uri)
                    }

                    if (!showError) {
                        VideoWallpaper(
                            uri = uri,
                            modifier = modifier,
                            onError = { showError = true }
                        )
                    }
                } ?: FallbackWallpaper(modifier)
            }
        }
    }
}

@Composable
private fun FallbackWallpaper(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(oledBackgroundColor())
    )
}

private fun isUriAccessible(
    context: Context,
    uriString: String
): Boolean {
    return try {
        val uri = uriString.toUri()
        context.contentResolver.openInputStream(uri)?.use { true } ?: false
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun VideoWallpaper(
    uri: String,
    modifier: Modifier = Modifier,
    onError: () -> Unit = {}
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            try {
                setMediaItem(MediaItem.fromUri(uri.toUri()))
                repeatMode = Player.REPEAT_MODE_ALL
                volume = 0f

                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        onError()
                    }
                })

                prepare()
                playWhenReady = true
            } catch (_: Exception) {
                onError()
            }
        }
    }

    DisposableEffect(uri) {
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
