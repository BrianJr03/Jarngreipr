package jr.brian.jarnfe.wallpaper

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.hardware.display.DisplayManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import jr.brian.core.ui.components.JarnScrim
import jr.brian.core.ui.components.JarnScrimEdge

internal data class JarnWallpaperDecodeSize(val width: Int, val height: Int)

/**
 * One size for every window, so the second window reuses the first's cached bitmap instead of
 * decoding its own. Null when no display reports a usable size.
 */
internal fun jarnWallpaperDecodeSize(
    displays: List<Pair<Int, Int>>,
    isPortrait: Boolean,
): JarnWallpaperDecodeSize? {
    val usable = displays.filter { (width, height) -> width > 0 && height > 0 }
    if (usable.isEmpty()) return null
    val long = usable.maxOf { (width, height) -> maxOf(width, height) }
    val short = usable.maxOf { (width, height) -> minOf(width, height) }
    return if (isPortrait) JarnWallpaperDecodeSize(short, long) else JarnWallpaperDecodeSize(long, short)
}

private fun jarnWallpaperRequest(context: Context, uri: String): ImageRequest {
    val displays = context.getSystemService(DisplayManager::class.java).displays
        .map { it.mode.physicalWidth to it.mode.physicalHeight }
    val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    val size = jarnWallpaperDecodeSize(displays, isPortrait)
    return ImageRequest.Builder(context)
        .data(uri)
        .apply { size?.let { size(it.width, it.height) } }
        .build()
}

/** A picked image or GIF under the scrim. A GIF holds its frame whenever the gate says stop. */
@Composable
internal fun JarnWallpaperImage(uri: String) {
    val context = LocalContext.current
    val request = remember(context, uri) { jarnWallpaperRequest(context, uri) }
    val canAnimate by rememberJarnWallpaperCanAnimate()
    var failed by remember(uri) { mutableStateOf(false) }
    var animation by remember(uri) { mutableStateOf<Animatable?>(null) }
    LaunchedEffect(animation, canAnimate) {
        animation?.run { if (canAnimate) start() else stop() }
    }
    if (failed) return
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        onSuccess = { animation = it.result.drawable as? Animatable },
        onError = { failed = true },
    )
    JarnScrim(JarnScrimEdge.Full, Modifier.fillMaxSize())
}
