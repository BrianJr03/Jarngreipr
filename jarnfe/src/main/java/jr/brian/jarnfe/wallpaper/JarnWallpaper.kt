package jr.brian.jarnfe.wallpaper

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import jr.brian.core.ui.components.JarnScrim
import jr.brian.core.ui.components.JarnScrimEdge
import jr.brian.core.ui.theme.JarnTheme

/** Reads the wallpaper the user already chose. Backed by snapshot state, so changes recompose. */
interface JarnWallpaperSource {
    /** The image or GIF to show, or null for the theme background. */
    val currentUri: String?
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnWallpaperProvider {
    val jarnWallpaperSource: JarnWallpaperSource
}

fun Context.jarnWallpaperSourceOrNull(): JarnWallpaperSource? =
    (applicationContext as? JarnWallpaperProvider)?.jarnWallpaperSource

/** The theme background, with the saved wallpaper dimmed on top when [showWallpaper] is true. */
@Composable
internal fun JarnWallpaperBackdrop(showWallpaper: Boolean) {
    val context = LocalContext.current
    val source = remember(context) { context.jarnWallpaperSourceOrNull() }
    val uri = source?.currentUri.takeIf { showWallpaper }
    Box(
        Modifier
            .fillMaxSize()
            .background(JarnTheme.colors.bg),
    ) {
        uri?.let { WallpaperImage(it) }
    }
}

@Composable
private fun WallpaperImage(uri: String) {
    var failed by remember(uri) { mutableStateOf(false) }
    if (failed) return
    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
        onError = { failed = true },
    )
    JarnScrim(JarnScrimEdge.Full, Modifier.fillMaxSize())
}
