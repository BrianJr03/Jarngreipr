package jr.brian.home.ui.components.wallpaper

import android.os.Build
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.theme.oledBackgroundColor

@Composable
fun WallpaperDisplay(
    wallpaperUri: String?,
    wallpaperType: WallpaperType,
    modifier: Modifier = Modifier
) {
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
            wallpaperUri?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri.toUri()),
                    contentDescription = "Wallpaper",
                    modifier = modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        WallpaperType.GIF -> {
            wallpaperUri?.let { uri ->
                val context = LocalContext.current
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
                        imageLoader = imageLoader
                    ),
                    contentDescription = "Animated Wallpaper",
                    modifier = modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        WallpaperType.VIDEO -> {
            wallpaperUri?.let { uri ->
                VideoWallpaper(
                    uri = uri,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun VideoWallpaper(
    uri: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri.toUri()))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
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
