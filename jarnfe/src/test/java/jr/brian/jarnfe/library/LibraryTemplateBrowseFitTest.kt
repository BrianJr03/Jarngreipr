package jr.brian.jarnfe.library

import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.model.theme.forArea
import jr.brian.core.ui.theme.JarnSpacing
import jr.brian.core.ui.theme.JarnType
import jr.brian.jarnfe.themekit.ThemePxRect
import jr.brian.jarnfe.themekit.ThemeScreen
import jr.brian.jarnfe.themekit.resolveThemeView
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Each template's list gets a usable size in the real Thor browse area, with the Continue playing
 * strip showing over the systems views. Sizes are in dp unless named px.
 */
class LibraryTemplateBrowseFitTest {
    private val density = 369f / 160f
    private val space = JarnSpacing()

    /** The browse body under the tabs (183px) or the game list title (128px), above the hint bar (988px). */
    private val systemsHeight = 988 - 183
    private val gamesHeight = 988 - 128
    private val widths = listOf(1920, 1240)

    private val stripBottomPx = ((space.md + LibraryContinueStripHeight).value * density).toInt()

    /**
     * A captioned grid's last row either shows whole or stops above its caption, so no name is cut
     * in half. Checked on both Thor screens, under the tabs (183px measured, 157px on device), each
     * with the layout its own shape picks.
     */
    @Test
    fun `captioned grid rows never cut a caption on either Thor screen`() {
        val type = JarnType()
        val caption = space.sm.value + type.captionStrong.lineHeight.value + type.caption.lineHeight.value + space.xs.value
        val views = LibraryTemplateThemes.All.flatMap { listOfNotNull(it.spec.views.systems, it.spec.views.games) }
        views.forEach { spec ->
            widths.forEach { width ->
                listOf(systemsHeight, 988 - 157).forEach { height ->
                    val primary = spec.forArea(width, height).primary as? ThemePrimary.Grid ?: return@forEach
                    if (!primary.showTitle) return@forEach
                    val box = resolveThemeView(spec, ThemeScreen(width, height)).primary.toDp()
                    val gap = box.w * primary.spacing
                    val cell = (box.w - space.stageMargin.value * 2 - gap * (primary.columns - 1)) / primary.columns
                    val tile = cell / primary.itemAspect
                    val shown = (box.h - space.focusEdge.value) % (tile + gap)
                    assertTrue("${width}x$height: last row shows ${shown}dp of a ${tile}dp tile", shown <= tile - caption || shown >= tile)
                }
            }
        }
    }

    @Test
    fun `the bottom screen gets the narrow layouts and the top keeps the wide ones`() {
        val bottomShelf = BigShelfGames.forArea(1240, gamesHeight).primary as ThemePrimary.Shelf
        val topShelf = BigShelfGames.forArea(1920, gamesHeight).primary as ThemePrimary.Shelf
        assertTrue(bottomShelf.itemsBefore == 1 && bottomShelf.itemsAfter == 1)
        assertTrue(topShelf.itemsBefore == 2 && topShelf.itemsAfter == 2)
        assertTrue((CoverGridGames.forArea(1240, gamesHeight).primary as ThemePrimary.Grid).columns == 3)
        assertTrue((CoverGridSystems.forArea(1240, systemsHeight).primary as ThemePrimary.Grid).columns == 2)
        assertTrue((CoverGridSystems.forArea(1920, systemsHeight).primary as ThemePrimary.Grid).columns == 3)
    }

    @Test
    fun `the Continue playing strip fits the band systems views keep clear`() {
        assertTrue(stripBottomPx <= (LIBRARY_HEADER_BAND * systemsHeight).toInt())
    }

    @Test
    fun `nothing a systems view shows on top starts under the strip`() {
        LibraryTemplateThemes.All.forEach { theme ->
            val view = requireNotNull(theme.spec.views.systems)
            widths.forEach { w ->
                val placed = resolveThemeView(view, ThemeScreen(w, systemsHeight))
                assertTrue("${theme.id} primary at ${placed.primary.top}px", placed.primary.top >= stripBottomPx)
                (placed.below + placed.above)
                    .filter { it.layer.drawsContent() }
                    .forEach { assertTrue("${theme.id} ${it.layer.id} at ${it.bounds.top}px", it.bounds.top >= stripBottomPx) }
            }
        }
    }

    @Test
    fun `every template list is usable in the Thor browse area`() {
        LibraryTemplateThemes.All.forEach { theme ->
            widths.forEach { w ->
                checkUsable("${theme.id} systems at $w", requireNotNull(theme.spec.views.systems), w, systemsHeight, LibraryShelfKind.Systems)
                checkUsable("${theme.id} games at $w", requireNotNull(theme.spec.views.games), w, gamesHeight, LibraryShelfKind.Games)
            }
        }
    }

    private fun checkUsable(name: String, view: ViewSpec, width: Int, height: Int, kind: LibraryShelfKind) {
        val box = resolveThemeView(view, ThemeScreen(width, height)).primary.toDp()
        when (val primary = view.forArea(width, height).primary) {
            is ThemePrimary.Shelf -> {
                val geometry = libraryShelfGeometry(primary, kind)
                val fit = shelfFit(box.w, box.h, geometry, box.w < SHELF_NARROW_WIDTH.value, space.focusEdge.value, SHELF_NO_CAPTION)
                assertTrue("$name: centered card ${fit.cardHeight}dp tall", fit.cardHeight >= MIN_SHELF_CARD)
            }
            is ThemePrimary.Grid -> {
                val gap = box.w * primary.spacing
                val cell = (box.w - space.stageMargin.value * 2 - gap * (primary.columns - 1)) / primary.columns
                val rows = (box.h - space.focusEdge.value * 2) / (cell / primary.itemAspect + gap)
                assertTrue("$name: $rows rows of ${cell}dp cells", rows >= MIN_GRID_ROWS && cell >= MIN_GRID_CELL)
            }
            is ThemePrimary.List -> {
                val geometry = libraryListGeometry(primary)
                val row = libraryListRowHeight(box.h.dp, space.focusEdge, space.xs, geometry)
                val shown = row * geometry.rows + space.xs * (geometry.rows - 1) + space.focusEdge * 2
                assertTrue("$name: ${geometry.rows} rows need $shown of ${box.h}dp", shown.value <= box.h + 0.01f)
            }
        }
    }

    private fun ThemeLayer.drawsContent(): Boolean =
        this is ThemeLayer.Text || this is ThemeLayer.Date || this is ThemeLayer.Rating || (z > 0)

    private data class DpBox(val w: Float, val h: Float)

    private fun ThemePxRect.toDp() = DpBox(width / density, height / density)

    private companion object {
        const val MIN_SHELF_CARD = 110f
        const val MIN_GRID_ROWS = 1.5f
        const val MIN_GRID_CELL = 64f
    }
}
