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
import androidx.compose.ui.platform.LocalView
import jr.brian.core.model.PRIMARY_DISPLAY_ID
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.look.jarnLookStoreOrNull

/** A physical screen. A swap moves roles between screens, never their wallpapers. */
enum class JarnWallpaperScreen { Top, Bottom }

/**
 * The frontend wallpaper. [currentUri] is the top screen's, and both screens' while they share
 * one. The bottom screen's own choice lives in the look store. Backed by snapshot state.
 */
interface JarnWallpaperSource {
    /** The image or GIF to show, or null for the theme background. */
    val currentUri: String?

    /** Saves a picked image or GIF for [screen]. False when the file can't be used. */
    suspend fun applyPicked(uri: Uri, screen: JarnWallpaperScreen = JarnWallpaperScreen.Top): Boolean

    fun useThemeBackground(screen: JarnWallpaperScreen = JarnWallpaperScreen.Top)

    /**
     * Turning sharing off gives the bottom screen its own copy of the top's wallpaper. Turning it
     * back on shows the top's on both and removes the bottom's file.
     */
    suspend fun setSameOnBothScreens(same: Boolean)

    /** [uri] when its file is still there, else null, since a restore can point at nothing. */
    fun usableOrNull(uri: String?): String?
}

/** What [screen] shows: the shared wallpaper, or the bottom screen's own one. */
internal fun jarnScreenWallpaperUri(
    screen: JarnWallpaperScreen,
    sameOnBoth: Boolean,
    topUri: String?,
    bottomUri: String?,
): String? = if (screen == JarnWallpaperScreen.Bottom && !sameOnBoth) bottomUri else topUri

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
        imageUri = rememberJarnWallpaperScreen()?.let { rememberJarnScreenWallpaperUri(source, store, it) },
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

/**
 * This window's physical screen, or null until the window is on a display. Read from the display
 * itself rather than the session, so the bottom screen never shows the top's wallpaper while the
 * Deck is still starting.
 */
@Composable
internal fun rememberJarnWallpaperScreen(): JarnWallpaperScreen? =
    jarnWallpaperScreenOf(LocalView.current.display?.displayId, PRIMARY_DISPLAY_ID)

internal fun jarnWallpaperScreenOf(displayId: Int?, primaryDisplayId: Int): JarnWallpaperScreen? = when (displayId) {
    null -> null
    primaryDisplayId -> JarnWallpaperScreen.Top
    else -> JarnWallpaperScreen.Bottom
}

@Composable
internal fun rememberJarnScreenWallpaperUri(
    source: JarnWallpaperSource?,
    store: JarnLookStore?,
    screen: JarnWallpaperScreen,
): String? {
    val sameOnBoth = store?.sameWallpaperOnBothState?.collectAsState()?.value ?: true
    val bottomUri = store?.bottomWallpaperUriState?.collectAsState()?.value
    return jarnScreenWallpaperUri(screen, sameOnBoth, source?.currentUri, source?.usableOrNull(bottomUri))
}

/** Its own layer, so animating it never redraws the UI above. Live art carries no scrim. */
@Composable
private fun LiveWallpaperLayer(wallpaper: JarnLiveWallpaper) {
    wallpaper.content(Modifier.fillMaxSize().graphicsLayer { clip = true })
}
