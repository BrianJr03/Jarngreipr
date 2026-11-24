package jr.brian.core.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme

/** The slot every system sits in, in the grid. */
const val JARN_SYSTEM_TILE_ASPECT = 136f / 98f
/** The slot every game sits in, in the grid. */
const val JARN_GAME_TILE_ASPECT = 4f / 5f

internal val JarnTileCaptionScrim = Color(0xFF080706).copy(alpha = 0.82f)
internal val JarnTileCaptionTitle = Color.White
internal val JarnTileCaptionSubtitle = Color(0xFFE5DED8)

/** [focusBorder] adds the thin focus border outside the tile. */
@Composable
fun JarnSystemTile(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusBorder: Boolean = false,
    art: @Composable BoxScope.() -> Unit,
) {
    JarnLibraryTileFrame(
        shape = JarnTheme.tileShape,
        aspectRatio = JARN_SYSTEM_TILE_ASPECT,
        focusStyle = if (focusBorder) JarnFocusStyle.FloatBorder else JarnFocusStyle.Float,
        captioned = true,
        description = null,
        onClick = onClick,
        modifier = modifier,
        art = art,
    ) {
        JarnTileCaption(title, subtitle, Modifier.align(Alignment.BottomStart))
    }
}

/**
 * A game's art in a slot of [aspectRatio]. With [artAspectRatio] the tile shrinks to the art's own
 * shape inside the slot, so focus wraps the art itself. [focusBorder] adds the thin focus border.
 */
@Composable
fun JarnGameTile(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    aspectRatio: Float = JARN_GAME_TILE_ASPECT,
    artAspectRatio: Float? = null,
    focusBorder: Boolean = false,
    art: @Composable BoxScope.() -> Unit,
) {
    JarnLibraryTileFrame(
        shape = JarnTheme.look.tileShape ?: JarnTheme.radius.smShape,
        aspectRatio = aspectRatio,
        artAspectRatio = artAspectRatio,
        focusStyle = if (focusBorder) JarnFocusStyle.FloatBorder else JarnFocusStyle.Float,
        captioned = false,
        showCard = false,
        description = title,
        onClick = onClick,
        modifier = modifier,
        art = art,
    )
}

@Composable
private fun JarnLibraryTileFrame(
    shape: Shape,
    aspectRatio: Float,
    captioned: Boolean,
    artAspectRatio: Float? = null,
    focusStyle: JarnFocusStyle = JarnFocusStyle.Float,
    showCard: Boolean = true,
    description: String?,
    onClick: () -> Unit,
    modifier: Modifier,
    art: @Composable BoxScope.() -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .jarnFitAspect(artAspectRatio)
            .semantics { description?.let { contentDescription = it } }
            .jarnInteractive(shape, source, focusStyle = focusStyle, onClick = onClick)
            .clip(shape)
            .then(if (showCard) Modifier.background(JarnTheme.colors.raised) else Modifier),
    ) {
        JarnArtSlot(captioned, art)
        overlay()
    }
}

@Composable
private fun JarnTileCaption(title: String, subtitle: String?, modifier: Modifier) {
    val space = JarnTheme.space
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(space.sm)
                .background(Brush.verticalGradient(listOf(Color.Transparent, JarnTileCaptionScrim))),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .background(JarnTileCaptionScrim)
                .padding(start = space.sm, end = space.sm, bottom = space.xs),
        ) {
            JarnText(title, JarnTheme.type.captionStrong, JarnTileCaptionTitle, maxLines = 1)
            subtitle?.let {
                JarnText(it, JarnTheme.type.caption, JarnTileCaptionSubtitle, maxLines = 1)
            }
        }
    }
}

/**
 * Lays out what follows at [aspectRatio], fitted and centered in the slot, while the slot keeps
 * its full size. Null leaves the slot as it is.
 */
fun Modifier.jarnFitAspect(aspectRatio: Float?): Modifier {
    if (aspectRatio == null || aspectRatio <= 0f) return this
    return layout { measurable, constraints ->
        val slotWidth = constraints.maxWidth
        val slotHeight = constraints.maxHeight
        val (width, height) = jarnFitSize(slotWidth.toFloat(), slotHeight.toFloat(), aspectRatio)
        val placeable = measurable.measure(Constraints.fixed(width.roundToInt(), height.roundToInt()))
        layout(slotWidth, slotHeight) {
            placeable.place((slotWidth - placeable.width) / 2, (slotHeight - placeable.height) / 2)
        }
    }
}

/** The largest size of [aspectRatio] that fits a [slotWidth] by [slotHeight] slot. */
fun jarnFitSize(slotWidth: Float, slotHeight: Float, aspectRatio: Float): Pair<Float, Float> =
    if (slotWidth / slotHeight > aspectRatio) {
        slotHeight * aspectRatio to slotHeight
    } else {
        slotWidth to slotWidth / aspectRatio
    }
