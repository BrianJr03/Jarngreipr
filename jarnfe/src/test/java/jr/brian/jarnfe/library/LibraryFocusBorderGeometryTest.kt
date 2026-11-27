package jr.brian.jarnfe.library

import androidx.compose.ui.geometry.Rect
import jr.brian.core.ui.focus.JarnFocusReach
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnFocusBorderReach
import jr.brian.core.ui.library.JARN_GAME_TILE_ASPECT
import jr.brian.core.ui.library.JARN_SYSTEM_TILE_ASPECT
import jr.brian.core.ui.library.jarnFitSize
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val SystemsShelf = LibraryShelfKind.Systems.geometry

/**
 * Proves the focus border rule at real Stage sizes: each focused item's border sits outside its
 * own art and never touches a neighbor, the tabs or the screen edge. All sizes are in dp.
 *
 * The Thor's top screen is 1920 by 1080 at 369 dpi. The Stage body runs from under the header
 * (183px) to the hint bar (988px).
 */
class LibraryFocusBorderGeometryTest {
    private class Stage(val width: Float, val height: Float)

    private val thor = Stage(width = 1920f / THOR_DENSITY, height = (988f - 183f) / THOR_DENSITY)
    private val narrow = Stage(width = 600f, height = thor.height)
    private val stages = listOf(thor, narrow)
    private val tileScales = listOf(0.75f, 1f, 1.5f)

    private fun Rect.envelope(reach: JarnFocusReach) =
        Rect(left - reach.sides, top - reach.top, right + reach.sides, bottom + reach.bottom)

    private fun Rect.clearOf(other: Rect) = !overlaps(other)

    private fun assertBorderOutsideArt(art: Rect, reach: JarnFocusReach) {
        assertTrue("border gap must be positive", reach.innerGap > 0f)
        val inner = art.inflate(reach.innerGap)
        assertTrue(inner.left < art.left && inner.top < art.top && inner.right > art.right && inner.bottom > art.bottom)
    }

    private fun centered(cx: Float, cy: Float, width: Float, height: Float) =
        Rect(cx - width / 2, cy - height / 2, cx + width / 2, cy + height / 2)

    @Test
    fun `carousel games border clears both neighbors, the top and the title`() {
        stages.forEach { stage -> assertShelf(stage, LibraryShelfKind.Games, shelfHeight = stage.height) }
    }

    @Test
    fun `carousel systems under the continue card stay clear too`() {
        stages.forEach { stage ->
            assertShelf(stage, LibraryShelfKind.Systems, shelfHeight = stage.height - FOCUS_EDGE - HERO_HEIGHT)
        }
    }

    @Test
    fun `carousel systems with names off grow into the freed room and stay clear`() {
        stages.forEach { stage ->
            val shelfHeight = stage.height - FOCUS_EDGE - HERO_HEIGHT
            val named = shelfFit(stage.width, shelfHeight, SystemsShelf, false, FOCUS_EDGE, SHELF_ONE_LINE_CAPTION)
            val fit = assertShelf(stage, LibraryShelfKind.Systems, shelfHeight, captioned = false)
            assertTrue("cards grow with names off on ${stage.width}dp", fit.cardHeight > named.cardHeight)
        }
    }

    private fun assertShelf(
        stage: Stage,
        kind: LibraryShelfKind,
        shelfHeight: Float,
        captioned: Boolean = true,
    ): ShelfFit {
        val isNarrow = stage.width < SHELF_NARROW_WIDTH.value
        val geometry = kind.geometry
        val reserve = if (captioned) geometry.captionReserve else SHELF_NO_CAPTION
        val fit = shelfFit(stage.width, shelfHeight, geometry, isNarrow, FOCUS_EDGE, reserve)
        assertTrue("card must have room on ${stage.width}dp", fit.cardHeight > 0f)
        val center = centered(stage.width / 2, fit.centerY, fit.cardWidth, fit.cardHeight)
        val reach = jarnFocusBorderReach(fit.cardWidth, fit.cardHeight)
        val border = center.envelope(reach)
        assertBorderOutsideArt(center, reach)
        val minGap = shelfMinGap(fit.cardWidth, fit.cardHeight)
        listOf(-1, 1).forEach { side ->
            val x = shelfSlotX(side, stage.width, geometry, fit.cardWidth, minGap)
            val neighbor = centered(
                stage.width / 2 + x,
                fit.centerY,
                fit.cardWidth * geometry.neighborScale,
                fit.cardHeight * geometry.neighborScale,
            )
            assertTrue("$kind border hits a neighbor on ${stage.width}dp", border.clearOf(neighbor))
        }
        assertTrue("$kind border leaves the top of the shelf", border.top >= 0f)
        if (captioned) {
            val titleTop = center.bottom + CAPTION_GAP
            assertTrue("$kind border reaches the title", border.bottom < titleTop)
        } else {
            assertTrue("$kind border leaves the bottom of the shelf", border.bottom <= shelfHeight)
        }
        return fit
    }

    @Test
    fun `continue card border clears the tabs, the screen edge and the shelf below`() {
        stages.forEach { stage ->
            val hero = Rect(STAGE_MARGIN, FOCUS_EDGE, stage.width - STAGE_MARGIN, FOCUS_EDGE + HERO_HEIGHT)
            val reach = jarnFocusBorderReach(hero.width, hero.height, JarnFocusStyle.ScaleBorder)
            val border = hero.envelope(reach)
            assertBorderOutsideArt(hero, reach)
            assertTrue("banner border reaches the tabs", border.top >= 0f)
            assertTrue("banner border leaves the screen", border.left >= 0f && border.right <= stage.width)
            val shelfHeight = stage.height - hero.bottom
            val fit = shelfFit(stage.width, shelfHeight, SystemsShelf, false, FOCUS_EDGE, SHELF_ONE_LINE_CAPTION)
            val card = centered(stage.width / 2, hero.bottom + fit.centerY, fit.cardWidth, fit.cardHeight)
            assertTrue("banner border hits the shelf card", border.clearOf(card))
            val gridFirstRow = hero.bottom + LibraryGridRowSpacing.value
            assertTrue("banner border hits the first grid row", border.bottom < gridFirstRow)
        }
    }

    @Test
    fun `game grid border clears every neighbor at every tile size`() {
        stages.forEach { stage ->
            tileScales.forEach { scale ->
                assertGrid(stage, GameCellMinSize.value * scale, JARN_GAME_TILE_ASPECT, topOffset = FOCUS_EDGE)
            }
        }
    }

    @Test
    fun `system grid border clears every neighbor and the continue card above`() {
        stages.forEach { stage ->
            tileScales.forEach { scale ->
                val firstRowTop = FOCUS_EDGE + HERO_HEIGHT + LibraryGridRowSpacing.value
                val cells = assertGrid(stage, SystemCellMinSize.value * scale, JARN_SYSTEM_TILE_ASPECT, firstRowTop)
                val hero = Rect(STAGE_MARGIN, FOCUS_EDGE, stage.width - STAGE_MARGIN, FOCUS_EDGE + HERO_HEIGHT)
                val first = cells.first()
                val reach = jarnFocusBorderReach(first.width, first.height)
                assertTrue("first row border hits the continue card", first.envelope(reach).clearOf(hero))
            }
        }
    }

    @Test
    fun `system grid with names off keeps the same tile, so it stays clear`() {
        stages.forEach { stage ->
            tileScales.forEach { scale ->
                val firstRowTop = FOCUS_EDGE + HERO_HEIGHT + LibraryGridRowSpacing.value
                assertGrid(stage, SystemCellMinSize.value * scale, JARN_SYSTEM_TILE_ASPECT, firstRowTop)
            }
        }
    }

    /** Lays out a 3 by 3 block like LazyVerticalGrid's adaptive columns and checks the middle tile. */
    private fun assertGrid(stage: Stage, cellMin: Float, slotAspect: Float, topOffset: Float): List<Rect> {
        val column = LibraryGridColumnSpacing.value
        val row = LibraryGridRowSpacing.value
        val available = stage.width - 2 * STAGE_MARGIN
        val columns = maxOf(1, ((available + column) / (cellMin + column)).toInt())
        val cellWidth = (available - column * (columns - 1)) / columns
        val cellHeight = cellWidth / slotAspect
        val cells = (0 until 3).flatMap { r ->
            (0 until minOf(3, columns)).map { c ->
                val left = STAGE_MARGIN + c * (cellWidth + column)
                val top = topOffset + r * (cellHeight + row)
                Rect(left, top, left + cellWidth, top + cellHeight)
            }
        }
        val focusedIndex = if (columns >= 3) 4 else 1
        val focused = cells[focusedIndex]
        val (artWidth, artHeight) = jarnFitSize(focused.width, focused.height, slotAspect)
        val art = centered(focused.center.x, focused.center.y, artWidth, artHeight)
        val reach = jarnFocusBorderReach(art.width, art.height)
        val border = art.envelope(reach)
        assertBorderOutsideArt(art, reach)
        cells.filterIndexed { index, _ -> index != focusedIndex }.forEach { neighbor ->
            assertTrue("grid border hits a neighbor at cell $cellMin on ${stage.width}dp", border.clearOf(neighbor))
        }
        val topRow = cells.first().envelope(jarnFocusBorderReach(cellWidth, cellHeight))
        assertTrue("top row border leaves the focus edge", topRow.top >= topOffset - FOCUS_EDGE)
        assertFalse("side border leaves the screen", topRow.left < 0f || topRow.right > stage.width)
        return cells
    }

    private companion object {
        const val THOR_DENSITY = 369f / 160f
        const val FOCUS_EDGE = 20f
        const val STAGE_MARGIN = 24f
        const val HERO_HEIGHT = 160f
        const val CAPTION_GAP = 16f
    }
}
