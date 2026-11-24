package jr.brian.jarnfe.library

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.imageLoader
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import jr.brian.core.ui.library.JarnArtPlaceholder
import jr.brian.core.ui.theme.JarnTheme

internal data class HeldImage(val path: String, val bitmap: ImageBitmap)

internal fun interface HeldArtDecoder {
    suspend fun decode(path: String, size: IntSize): ImageBitmap?
}

/** Keeps the current image until the next is decoded, then fades one out as the other fades in. */
@Stable
internal class HeldArtState {
    var shown by mutableStateOf<HeldImage?>(null)
        private set
    var outgoing by mutableStateOf<HeldImage?>(null)
        private set
    var isSettled by mutableStateOf(false)
        private set
    var settledPaths by mutableStateOf<List<String>?>(null)
        private set
    val reveal = Animatable(1f)

    /** The first path that decodes wins. If none does, the image fades to the fallback. */
    suspend fun moveTo(
        paths: List<String>,
        fade: AnimationSpec<Float>,
        decode: suspend (String) -> ImageBitmap?,
    ) {
        val current = shown
        if (current != null && paths.firstOrNull() == current.path) {
            settledPaths = paths
            return finishFade(fade)
        }
        val next = paths.firstNotNullOfOrNull { path -> decode(path)?.let { HeldImage(path, it) } }
        isSettled = true
        settledPaths = paths
        if (next?.path == current?.path) return finishFade(fade)
        outgoing = current ?: outgoing
        shown = next
        reveal.snapTo(0f)
        finishFade(fade)
    }

    private suspend fun finishFade(fade: AnimationSpec<Float>) {
        reveal.animateTo(1f, fade)
        outgoing = null
    }
}

/**
 * Art that never flashes: nothing changes until the next image is ready. [paths] are tried in
 * order and decoded at this box's size. [fallback] shows only once no path could be decoded.
 */
@Composable
internal fun BoxScope.LibraryHeldArt(
    paths: List<String>,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    canLoad: Boolean = rememberJarnArtCanLoad(),
    decoder: HeldArtDecoder = rememberHeldArtDecoder(),
    fallback: @Composable BoxScope.() -> Unit = {},
) {
    val state = remember { HeldArtState() }
    var size by remember { mutableStateOf<IntSize?>(null) }
    val fade = JarnTheme.motion.panel.spec<Float>()
    NowRevealWhen(state.settledPaths == paths)
    LaunchedEffect(paths, size != null, canLoad) {
        val target = size
        if (canLoad && target != null) state.moveTo(paths, fade) { decoder.decode(it, target) }
    }
    Box(
        Modifier
            .matchParentSize()
            .onSizeChanged { if (it.width > 0 && it.height > 0) size = it },
    ) {
        state.outgoing?.let {
            HeldImageLayer(it, contentScale, alignment, Modifier.graphicsLayer { alpha = 1f - state.reveal.value })
        }
        val shown = state.shown
        if (shown != null) {
            HeldImageLayer(shown, contentScale, alignment, Modifier.graphicsLayer { alpha = state.reveal.value })
        } else if (state.isSettled) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = state.reveal.value }) { fallback() }
        }
    }
}

/** The Deck art when readable, otherwise the tile art, otherwise a title card. */
@Composable
internal fun BoxScope.LibraryHeldDeckArt(game: JarnLibraryGame) {
    val paths = remember(game.deckArtPath, game.artPath) {
        listOfNotNull(game.deckArtPath, game.artPath).filter { it.isNotBlank() }.distinct()
    }
    LibraryHeldArt(paths, contentScale = ContentScale.Fit) {
        JarnArtPlaceholder(game.title, Modifier.matchParentSize())
    }
}

@Composable
private fun BoxScope.HeldImageLayer(
    image: HeldImage,
    contentScale: ContentScale,
    alignment: Alignment,
    modifier: Modifier = Modifier,
) {
    val painter = remember(image.bitmap) { BitmapPainter(image.bitmap) }
    Image(
        painter = painter,
        contentDescription = null,
        modifier = Modifier.matchParentSize().then(modifier),
        alignment = alignment,
        contentScale = contentScale,
    )
}

@Composable
private fun rememberHeldArtDecoder(): HeldArtDecoder {
    val context = LocalContext.current
    return remember(context) {
        HeldArtDecoder { path, size ->
            val request = stillImageBuilder(context, path)
                .size(size.width, size.height)
                .precision(Precision.INEXACT)
                .scale(Scale.FILL)
                .build()
            val result = context.imageLoader.execute(request) as? SuccessResult
            (result?.drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
        }
    }
}
