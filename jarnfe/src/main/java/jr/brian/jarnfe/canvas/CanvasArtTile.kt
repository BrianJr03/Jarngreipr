package jr.brian.jarnfe.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import jr.brian.core.model.GameImageType
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.library.JarnArtPlaceholder
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.wallpaper.readableOverWallpaper

private const val ALPHA_SHIFT = 24
private const val ALPHA_MASK = 0xFFL
private const val RGB_MASK = 0x00FFFFFFL
private const val OPAQUE = 0xFF000000L
private const val DEFAULT_CARD = 0L

/** Black's RGB is all zero, so it gets its own hidden value to stay distinct from the default card. */
private const val HIDDEN_BLACK = 0x00000001L

private const val ART_INSET_FRACTION = 0.12f
private const val ART_INSET_MIN_FRACTION_OF_SM = 0.75f
private const val PLACEHOLDER_MAX_LINES = 3

/** Any fully transparent color. Classic paints these as an invisible background. */
internal val CanvasItem.EsdeArtItem.isCardTransparent: Boolean
    get() = backgroundColorArgb?.let { (it ushr ALPHA_SHIFT) and ALPHA_MASK == 0L } == true

/**
 * Hides the card by clearing the color's alpha, which keeps its RGB for turning it back on.
 * The default card is stored as 0.
 */
internal fun CanvasItem.EsdeArtItem.withCardTransparencyToggled(): CanvasItem.EsdeArtItem {
    val color = backgroundColorArgb
    val next = when {
        color == null -> DEFAULT_CARD
        isCardTransparent -> when (color and RGB_MASK) {
            DEFAULT_CARD -> null
            HIDDEN_BLACK -> OPAQUE
            else -> (color and RGB_MASK) or OPAQUE
        }
        color and RGB_MASK == 0L -> HIDDEN_BLACK
        else -> color and RGB_MASK
    }
    return copy(backgroundColorArgb = next)
}

/** Scales with the tile, staying clear of the focus ring on small tiles and capped on large ones. */
internal fun canvasArtInset(shortestSide: Dp, minInset: Dp, maxInset: Dp): Dp =
    (shortestSide * ART_INSET_FRACTION).coerceIn(minInset, maxInset)

/** Concentric with the card's corner. A square card keeps square art. */
internal fun canvasArtCorner(cardCorner: Dp, inset: Dp, minCorner: Dp): Dp =
    (cardCorner - inset).coerceAtLeast(min(minCorner, cardCorner))

@Composable
internal fun BoxScope.ArtTile(item: CanvasItem.EsdeArtItem, stageArt: CanvasStageArt?) {
    val space = JarnTheme.space
    val cardCorner = item.resolvedBackgroundCornerRadiusDp.dp
    val background = item.backgroundColorArgb?.let { Color(it) } ?: Color.Transparent
    val transparent = item.isCardTransparent
    BoxWithConstraints(
        Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(cardCorner))
            .background(background),
    ) {
        val inset = canvasArtInset(min(maxWidth, maxHeight), space.sm * ART_INSET_MIN_FRACTION_OF_SM, space.md)
        val artShape = RoundedCornerShape(canvasArtCorner(cardCorner, inset, space.xs))
        val artModifier = Modifier.padding(inset).clip(artShape)
        when {
            stageArt != null -> ArtTileImage(item, stageArt, artModifier, transparent)
            transparent -> ArtScrimPlaceholder(stringResource(R.string.jarnfe_canvas_tile_art))
            else -> LabelTile(stringResource(R.string.jarnfe_canvas_tile_art))
        }
    }
}

/** A logo is always shown whole, since cropping would cut off its name. */
@Composable
private fun BoxScope.ArtTileImage(
    item: CanvasItem.EsdeArtItem,
    art: CanvasStageArt,
    artModifier: Modifier,
    transparent: Boolean,
) {
    val shown = rememberHeldCanvasArt(art, item.resolvedImageType) ?: return
    val scale = if (shown.isLogo) ContentScale.Fit else item.resolvedContentScale.toContentScale()
    CanvasImage(
        model = rememberCanvasArtModel(shown.path, shown.previousPath),
        title = shown.title,
        scale = scale,
        imageModifier = artModifier,
        placeholder = { title ->
            if (transparent) {
                ArtScrimPlaceholder(title)
            } else {
                JarnArtPlaceholder(title, Modifier.matchParentSize())
            }
        },
    )
}

/** What an art tile draws. [previousPath] is the image it showed before, kept while [path] loads. */
internal data class CanvasHeldArt(
    val title: String,
    val path: String?,
    val isLogo: Boolean,
    val previousPath: String? = null,
)

/** Null only while nothing has resolved yet. Pending art keeps the last image instead of guessing. */
internal fun canvasHeldArt(art: CanvasStageArt, type: GameImageType, held: CanvasHeldArt?): CanvasHeldArt? {
    if (art.isPending) return held
    val path = art.pathFor(type)
    if (held != null && held.title == art.title && held.path == path) return held
    return CanvasHeldArt(art.title, path, isLogo = path != null && path == art.logoPath, previousPath = held?.path)
}

@Composable
private fun rememberHeldCanvasArt(art: CanvasStageArt, type: GameImageType): CanvasHeldArt? {
    var held by remember { mutableStateOf<CanvasHeldArt?>(null) }
    val next = canvasHeldArt(art, type, held)
    SideEffect { held = next }
    return next
}

/** With no card behind it, the scrim keeps an empty tile findable and its text readable on any wallpaper. */
@Composable
private fun BoxScope.ArtScrimPlaceholder(title: String) {
    val colors = JarnTheme.colors
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(colors.artScrim)
            .padding(JarnTheme.space.xs),
        contentAlignment = Alignment.Center,
    ) {
        JarnText(
            text = title,
            style = JarnTheme.baseType.labelStrong,
            color = colors.readableOverWallpaper(colors.textHigh),
            maxLines = PLACEHOLDER_MAX_LINES,
            textAlign = TextAlign.Center,
        )
    }
}
