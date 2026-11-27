package jr.brian.jarnfe.library

import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeView
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.jarnfe.themekit.ThemePxRect
import jr.brian.jarnfe.themekit.ThemeScreen
import jr.brian.jarnfe.themekit.resolveThemeView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Grid and Carousel as built-in themes resolve to exactly today's browse geometry. */
class LibraryBuiltInThemesTest {
    /** Thor top and bottom, Odin 2, a 720p handheld, and the parity test's stage, in px. */
    private val screens = listOf(1920 to 1080, 1240 to 1080, 1080 to 1240, 1280 to 720, 2560 to 1600, 1600 to 2560)

    @Test
    fun `saved browse values map to their built-in`() {
        assertEquals(LibraryBuiltInThemes.Grid, LibraryBuiltInThemes.forView(JarnBrowseView.Grid))
        assertEquals(LibraryBuiltInThemes.Carousel, LibraryBuiltInThemes.forView(JarnBrowseView.Carousel))
    }

    @Test
    fun `built-in primaries fill the browse area on every screen`() {
        builtInViews().forEach { (_, spec) ->
            screens.forEach { (w, h) ->
                val placed = resolveThemeView(spec, ThemeScreen(w, h))
                assertEquals(ThemePxRect(0, 0, w, h), placed.primary)
                assertTrue(placed.issues.isEmpty())
                assertTrue(placed.below.isEmpty() && placed.above.isEmpty())
            }
        }
    }

    @Test
    fun `carousel resolves to today's shelf geometry for systems and games`() {
        LibraryShelfKind.entries.forEach { kind ->
            val theme = LibraryBuiltInThemes.Carousel
            val primary = theme.spec.views[kind.themeView]?.primary as ThemePrimary.Shelf
            assertEquals(kind.geometry, libraryShelfGeometry(primary, kind, theme.pinsFor(kind.themeView).shelf))
        }
    }

    @Test
    fun `grid resolves to today's adaptive grid for systems and games`() {
        ThemeView.entries.forEach { view ->
            val theme = LibraryBuiltInThemes.Grid
            val primary = theme.spec.views[view]?.primary as ThemePrimary.Grid
            listOf(112.dp, 148.dp, 148.dp * 1.25f).forEach { cell ->
                assertEquals(
                    LibraryGridGeometry(cell, rowSpacing = LibraryGridRowSpacing),
                    libraryGridGeometry(primary, cell, 960.dp, theme.pinsFor(view).grid),
                )
            }
        }
    }

    @Test
    fun `carousel spec states the neighbor counts it shows`() {
        LibraryShelfKind.entries.forEach { kind ->
            val primary = LibraryBuiltInThemes.Carousel.spec.views[kind.themeView]?.primary as ThemePrimary.Shelf
            assertEquals(kind.geometry.neighborsBefore, primary.itemsBefore)
            assertEquals(kind.geometry.neighborsAfter, primary.itemsAfter)
            assertEquals(kind.geometry.neighborScale, 1f / primary.focusedScale, 1e-6f)
        }
    }

    @Test
    fun `an unpinned shelf derives scale and neighbors from its spec`() {
        val primary = ThemePrimary.Shelf(box = LibraryFullBox, itemsBefore = 1, itemsAfter = 3, focusedScale = 2f)
        val geometry = libraryShelfGeometry(primary, LibraryShelfKind.Games)
        assertEquals(LibraryShelfKind.Games.geometry.copy(neighborScale = 0.5f, neighborsBefore = 1, neighborsAfter = 3), geometry)
    }

    @Test
    fun `an unpinned grid fixes its columns and spaces by box width`() {
        val primary = ThemePrimary.Grid(box = ThemeRect(0f, 0f, 0.5f, 1f), columns = 4, spacing = 0.02f)
        val geometry = libraryGridGeometry(primary, 112.dp, 600.dp)
        assertEquals(LibraryGridGeometry(112.dp, rowSpacing = 12.dp, columnSpacing = 12.dp, columns = 4), geometry)
    }

    @Test
    fun `built-in specs survive the theme format`() {
        listOf(LibraryBuiltInThemes.Grid, LibraryBuiltInThemes.Carousel).forEach { theme ->
            assertEquals(JarnThemeDecodeResult.Decoded(theme.spec), JarnThemeFormat.decode(JarnThemeFormat.encode(theme.spec)))
        }
    }

    private fun builtInViews() = listOf(LibraryBuiltInThemes.Grid, LibraryBuiltInThemes.Carousel).flatMap { theme ->
        ThemeView.entries.map { view -> view to requireNotNull(theme.spec.views[view]) }
    }
}
