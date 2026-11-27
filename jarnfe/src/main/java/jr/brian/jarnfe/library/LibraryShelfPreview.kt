package jr.brian.jarnfe.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PREVIEW_SHELF_ITEMS = SHELF_NEIGHBORS * 2 + 1

/**
 * A still shelf at the real sizes and dimming, for the Settings preview. It never takes focus or
 * input. [items] may be empty, in which case [art] draws placeholders for null. The centered card
 * shrinks to stay within [maxCardHeight] when one is given, and a theme passes its own [geometry].
 */
@Composable
internal fun <T : Any> LibraryShelfPreview(
    items: List<T>,
    kind: LibraryShelfKind,
    description: (T?) -> String,
    maxCardHeight: Dp = Dp.Unspecified,
    geometry: LibraryShelfGeometry = kind.geometry,
    art: @Composable BoxScope.(T?) -> Unit,
) {
    val shown: List<T?> = items.take(PREVIEW_SHELF_ITEMS).ifEmpty { List(PREVIEW_SHELF_ITEMS) { null } }
    val center = (shown.size - 1) / 2
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fullWidth = maxWidth * geometry.widthFraction
        val cardWidth = if (maxCardHeight.isSpecified) minOf(fullWidth, maxCardHeight * geometry.aspectRatio) else fullWidth
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val cardPx = with(LocalDensity.current) { cardWidth.toPx() }
        Box(Modifier.fillMaxWidth().height(cardWidth / geometry.aspectRatio)) {
            shown.forEachIndexed { index, item ->
                val distance = index - center
                val x = shelfSlotX(distance, widthPx, geometry, cardPx).roundToInt()
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(x, 0) }
                        .zIndex((SHELF_COMPOSED_REACH - abs(distance)).toFloat())
                        .graphicsLayer {
                            scaleX = shelfSlotScale(distance, geometry)
                            scaleY = scaleX
                            alpha = shelfSlotVisibility(shelfSlotAlpha(distance))
                        }
                        .width(cardWidth),
                ) {
                    LibraryShelfCard(
                        LibraryShelfSlotCard(
                            aspectRatio = geometry.aspectRatio,
                            modifier = Modifier.focusProperties { canFocus = false },
                            onClick = {},
                            description = description(item),
                            artAlpha = { shelfArtAlpha(shelfSlotAlpha(distance)) },
                        ),
                    ) { art(item) }
                }
            }
        }
    }
}
