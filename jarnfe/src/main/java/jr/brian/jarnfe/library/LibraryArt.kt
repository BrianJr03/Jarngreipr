package jr.brian.jarnfe.library

import android.content.Context
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.BitmapFactoryDecoder
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import jr.brian.core.ui.library.JarnArtPlaceholder
import jr.brian.jarnfe.themekit.withThemeArt

private const val SYSTEM_LOGO_FRACTION = 0.62f
private const val SYSTEM_LOGO_AREA_HEIGHT = 0.7f
private const val SVG_SUFFIX = ".svg"

/** Missing or unreadable art becomes a styled title card, never a broken image. */
@Composable
internal fun BoxScope.LibraryArt(
    path: String?,
    title: String,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    onAspectRatio: ((Float?) -> Unit)? = null,
) {
    var failed by remember(path) { mutableStateOf(false) }
    if (path.isNullOrBlank() || failed) {
        JarnArtPlaceholder(title, Modifier.matchParentSize())
    } else {
        AsyncImage(
            model = rememberStillImage(path),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            alignment = alignment,
            contentScale = contentScale,
            onSuccess = { loaded -> onAspectRatio?.invoke(loaded.painter.intrinsicSize.aspectRatioOrNull()) },
            onError = {
                failed = true
                onAspectRatio?.invoke(null)
            },
        )
    }
}

private fun Size.aspectRatioOrNull(): Float? =
    if (isSpecified && width > 0f && height > 0f) width / height else null

/**
 * A game's cover, shown whole whatever its shape. [onAspectRatio] gets the cover's own shape once
 * it loads, or null for the title card.
 */
@Composable
internal fun BoxScope.LibraryCoverArt(
    game: JarnLibraryGame,
    alignment: Alignment = Alignment.Center,
    onAspectRatio: ((Float?) -> Unit)? = null,
) {
    LibraryArt(game.artPath, game.title, ContentScale.Fit, alignment, onAspectRatio)
}

/** The art chosen for the Deck when it is readable, otherwise the game's whole tile art. */
@Composable
internal fun BoxScope.LibraryDeckArt(game: JarnLibraryGame) {
    var deckArtFailed by remember(game.deckArtPath) { mutableStateOf(false) }
    val deckArt = game.deckArtPath?.takeUnless { it.isBlank() || deckArtFailed }
    if (deckArt == null) {
        LibraryCoverArt(game)
    } else {
        AsyncImage(
            model = rememberStillImage(deckArt),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit,
            onError = { deckArtFailed = true },
        )
    }
}

/**
 * A system's background with its logo on top. Either one can be missing. The logo sits high to
 * clear a caption, or in the [logoAlignment] given when there is none.
 */
@Composable
internal fun BoxScope.LibrarySystemArt(shown: JarnLibrarySystem, logoAlignment: Alignment = Alignment.TopCenter) {
    val system = shown.withThemeArt()
    if (system.artPath != null || system.logoPath == null) LibraryArt(system.artPath, system.title)
    system.logoPath?.let { logo ->
        AsyncImage(
            model = rememberStillImage(logo),
            contentDescription = null,
            modifier = Modifier
                .align(logoAlignment)
                .fillMaxHeight(SYSTEM_LOGO_AREA_HEIGHT)
                .fillMaxWidth(SYSTEM_LOGO_FRACTION),
            contentScale = ContentScale.Fit,
        )
    }
}

/** Decodes only the first frame, so a grid of animated backgrounds stays smooth. */
@Composable
private fun rememberStillImage(path: String): ImageRequest {
    val context = LocalContext.current
    return remember(context, path) { stillImageBuilder(context, path).build() }
}

internal fun stillImageBuilder(context: Context, path: String): ImageRequest.Builder {
    val decoder = if (path.endsWith(SVG_SUFFIX, ignoreCase = true)) {
        SvgDecoder.Factory()
    } else {
        BitmapFactoryDecoder.Factory()
    }
    return ImageRequest.Builder(context).data(path).decoderFactory(decoder)
}
