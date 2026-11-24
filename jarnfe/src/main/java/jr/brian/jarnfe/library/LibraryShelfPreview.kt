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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt

private const val PREVIEW_SHELF_ITEMS = SHELF_NEIGHBORS * 2 + 1

/**
 * A still shelf at the real sizes and dimming, for the Settings preview. It never takes focus or
 * input. [items] may be empty, in which case [art] draws placeholders for null.
 */
@Composable
internal fun <T : Any> LibraryShelfPreview(
    items: List<T>,
    kind: LibraryShelfKind,
    description: (T?) -> String,
    art: @Composable BoxScope.(T?) -> Unit,
) {
    val shown: List<T?> = items.take(PREVIEW_SHELF_ITEMS).ifEmpty { List(PREVIEW_SHELF_ITEMS) { null } }
    val center = (shown.size - 1) / 2
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val cardWidth = maxWidth * kind.widthFraction
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val cardPx = with(LocalDensity.current) { cardWidth.toPx() }
        Box(Modifier.fillMaxWidth().height(cardWidth / kind.aspectRatio)) {
            shown.forEachIndexed { index, item ->
                val distance = index - center
                val x = shelfSlotX(distance, widthPx, kind, cardPx).roundToInt()
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(x, 0) }
                        .zIndex((SHELF_COMPOSED_REACH - abs(distance)).toFloat())
                        .graphicsLayer {
                            scaleX = shelfSlotScale(distance, kind)
                            scaleY = scaleX
                            alpha = shelfSlotAlpha(distance)
                        }
                        .width(cardWidth),
                ) {
                    LibraryShelfCard(
                        aspectRatio = kind.aspectRatio,
                        description = description(item),
                        onClick = {},
                        modifier = Modifier.focusProperties { canFocus = false },
                    ) { art(item) }
                }
            }
        }
    }
}
