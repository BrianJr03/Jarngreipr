package jr.brian.jarnfe.apps

import androidx.compose.ui.geometry.Rect
import jr.brian.core.ui.focus.JarnFocusReach
import jr.brian.core.ui.focus.jarnFocusBorderReach
import jr.brian.core.ui.theme.JarnSpacing
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Proves the focus border rule for app tiles on the Deck: a focused tile's border sits outside
 * the tile and never touches a neighbor, the section title above or the panel edge. All sizes are
 * in dp. The Thor's bottom screen is 1240 by 1080 at 369 dpi.
 */
class JarnAppsFocusBorderGeometryTest {
    private val spacing = JarnSpacing()
    private val margin = spacing.deckMargin.value
    private val focusEdge = spacing.focusEdge.value
    private val thorWidth = 1240f / THOR_DENSITY
    private val widths = listOf(thorWidth, 360f, 900f)
    private val tileHeights = listOf(MIN_TILE_HEIGHT, 92f, 120f)

    private fun Rect.envelope(reach: JarnFocusReach) =
        Rect(left - reach.sides, top - reach.top, right + reach.sides, bottom + reach.bottom)

    @Test
    fun `a focused app border clears every neighbor, the title and the panel edge`() {
        widths.forEach { width ->
            tileHeights.forEach { height -> assertGrid(width, height) }
        }
    }

    @Test
    fun `the Thor's bottom screen fits four columns with room for the border`() {
        val available = thorWidth - 2 * margin
        val columns = jarnAppColumns(available)
        assertTrue("columns on the Thor: $columns", columns >= 4)
        assertTrue(jarnAppCellWidth(available, columns) >= APP_TILE_MIN_WIDTH)
    }

    private fun assertGrid(width: Float, tileHeight: Float) {
        val available = width - 2 * margin
        val columns = jarnAppColumns(available)
        val cellWidth = jarnAppCellWidth(available, columns)
        val titleBottom = focusEdge + TITLE_HEIGHT
        val firstRowTop = titleBottom + AppTileRowGap
        val cells = (0 until 3).flatMap { r ->
            (0 until minOf(3, columns)).map { c ->
                val left = margin + c * (cellWidth + AppTileColumnGap)
                val top = firstRowTop + r * (tileHeight + AppTileRowGap)
                Rect(left, top, left + cellWidth, top + tileHeight)
            }
        }
        cells.forEachIndexed { index, focused ->
            val reach = jarnFocusBorderReach(focused.width, focused.height)
            assertTrue("reach is within the worst case", reach.sides <= AppTileFocusReach.sides && reach.top <= AppTileFocusReach.top)
            assertTrue("border gap must be positive", reach.innerGap > 0f)
            val border = focused.envelope(reach)
            cells.filterIndexed { other, _ -> other != index }.forEach { neighbor ->
                assertTrue("border hits a neighbor on ${width}dp at height $tileHeight", !border.overlaps(neighbor))
            }
            assertTrue("border hits the section title on ${width}dp", border.top > titleBottom)
            assertTrue("border leaves the panel on ${width}dp", border.left >= 0f && border.right <= width)
        }
        val topTile = Rect(margin, focusEdge, margin + cellWidth, focusEdge + tileHeight)
        assertTrue("an untitled first row's border leaves the list", topTile.envelope(AppTileFocusReach).top >= 0f)
    }

    private companion object {
        const val THOR_DENSITY = 369f / 160f
        const val TITLE_HEIGHT = 18f
        const val MIN_TILE_HEIGHT = 60f
    }
}
