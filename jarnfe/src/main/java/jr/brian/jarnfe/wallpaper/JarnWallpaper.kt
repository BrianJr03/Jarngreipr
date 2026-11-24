package jr.brian.jarnfe.wallpaper

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.look.jarnLookStoreOrNull

/** The shared frontend wallpaper. Backed by snapshot state, so changes recompose. */
interface JarnWallpaperSource {
    /** The image or GIF to show, or null for the theme background. */
    val currentUri: String?

    /** Saves a picked image or GIF as the wallpaper. False when the file can't be used. */
    suspend fun applyPicked(uri: Uri): Boolean

    fun useThemeBackground()
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnWallpaperProvider {
    val jarnWallpaperSource: JarnWallpaperSource
}

fun Context.jarnWallpaperSourceOrNull(): JarnWallpaperSource? =
    (applicationContext as? JarnWallpaperProvider)?.jarnWallpaperSource

/** The theme background, with the live wallpaper or dimmed image on top when [showWallpaper] is true. */
@Composable
internal fun JarnWallpaperBackdrop(showWallpaper: Boolean) {
    val context = LocalContext.current
    val source = remember(context) { context.jarnWallpaperSourceOrNull() }
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val savedLiveId = store?.liveWallpaperIdState?.collectAsState()?.value
    val backdrop = resolveJarnBackdrop(
        showWallpaper = showWallpaper,
        savedLiveId = savedLiveId,
        previewLiveId = JarnLiveWallpaperPreview.id,
        imageUri = source?.currentUri,
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(JarnTheme.colors.bg),
    ) {
        when (backdrop) {
            is JarnBackdrop.Live -> LiveWallpaperLayer(backdrop.wallpaper)
            is JarnBackdrop.Image -> JarnWallpaperImage(backdrop.uri)
            JarnBackdrop.Theme -> Unit
        }
    }
}

/** Its own layer, so animating it never redraws the UI above. Live art carries no scrim. */
@Composable
private fun LiveWallpaperLayer(wallpaper: JarnLiveWallpaper) {
    wallpaper.content(Modifier.fillMaxSize().graphicsLayer { clip = true })
}
