package jr.brian.jarnfe.library

import androidx.compose.ui.unit.dp
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeTextRole
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.ui.theme.JarnSpacing
import jr.brian.core.ui.theme.JarnType
import jr.brian.jarnfe.themekit.ThemeInsets
import jr.brian.jarnfe.themekit.ThemeLayoutIssue
import jr.brian.jarnfe.themekit.ThemeScreen
import jr.brian.jarnfe.themekit.placeThemeRect
import jr.brian.jarnfe.themekit.resolveThemeView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The #558 layout templates: stable ids, a clean trip through the format, and a clean fit on the Thor. */
class LibraryTemplateThemesTest {
    /** The Thor's top and bottom screens, in px. */
    private val thorScreens = listOf(1920 to 1080, 1240 to 1080)

    /** The Library stage body on the Thor, under the header (183px) and above the hint bar (988px). */
    private val stageHeight = 988 - 183

    private val thorDensity = 369f / 160f

    /** 40dp at 369 dpi, for a theme drawn over the whole screen. */
    private val hintBarPx = 92

    @Test
    fun `templates keep their stable ids in picker order`() {
        assertEquals(
            listOf("template.big-shelf", "template.cover-grid", "template.list-art"),
            LibraryTemplateThemes.All.map { it.id },
        )
        LibraryTemplateThemes.All.forEach { assertEquals(it, LibraryTemplateThemes.byId(it.id)) }
    }

    @Test
    fun `every template survives the theme format`() {
        LibraryTemplateThemes.All.forEach { theme ->
            val decoded = JarnThemeFormat.decode(JarnThemeFormat.encode(theme.spec))
            assertEquals(theme.id, JarnThemeDecodeResult.Decoded(theme.spec), decoded)
        }
    }

    @Test
    fun `every template lays out with no issues on the Thor stage`() {
        templateViews().forEach { (name, view) ->
            thorScreens.forEach { (w, _) ->
                val placed = resolveThemeView(view, ThemeScreen(w, stageHeight))
                assertEquals("$name at ${w}px", emptyList<ThemeLayoutIssue>(), placed.issues)
            }
        }
    }

    @Test
    fun `full screen only the full bleed decor runs under the hint bar`() {
        templateViews().forEach { (name, view) ->
            thorScreens.forEach { (w, h) ->
                val placed = resolveThemeView(view, ThemeScreen(w, h, ThemeInsets(bottom = hintBarPx)))
                val fullBleed = view.layers
                    .filter { requireNotNull(placeThemeRect(it.rect, w, h)).bottom > h - hintBarPx }
                    .map { ThemeLayoutIssue.InSafeZone(it.id) }
                assertEquals("$name at ${w}x$h", fullBleed, placed.issues)
            }
        }
    }

    @Test
    fun `every text layer fits its lines on the Thor stage`() {
        val type = JarnType()
        val platePadding = JarnSpacing().xs.value * 2
        templateViews().forEach { (name, view) ->
            view.layers.forEach { layer ->
                val (role, lines) = when (layer) {
                    is ThemeLayer.Text -> layer.style to layer.maxLines
                    is ThemeLayer.Date -> layer.style to 1
                    else -> return@forEach
                }
                val needed = (type.lineHeightOf(role) * lines + platePadding) * thorDensity
                val bounds = requireNotNull(placeThemeRect(layer.rect, 1920, stageHeight))
                assertTrue("$name ${layer.id}: ${bounds.height}px < $needed", bounds.height >= needed)
            }
        }
    }

    @Test
    fun `template primaries take their geometry from the spec alone`() {
        val shelf = BigShelfGames.primary as ThemePrimary.Shelf
        val shelfGeometry = libraryShelfGeometry(shelf, LibraryShelfKind.Games)
        assertEquals(1f / 1.3f, shelfGeometry.neighborScale, 1e-6f)
        assertEquals(false, shelfGeometry.showsCaption)

        val grid = CoverGridGames.primary as ThemePrimary.Grid
        val gridGeometry = libraryGridGeometry(grid, GameCellMinSize, 500.dp)
        assertEquals(LibraryGridGeometry(GameCellMinSize, 500.dp * 0.012f, 500.dp * 0.012f, columns = 4, itemAspect = 0.7f, showTitle = false), gridGeometry)

        val list = libraryListGeometry(ListArtGames.primary as ThemePrimary.List)
        assertEquals(LibraryListGeometry(rows = 5, secondary = (ListArtGames.primary as ThemePrimary.List).secondary), list)
    }

    @Test
    fun `list rows split the box and never drop under two lines`() {
        val geometry = LibraryListGeometry(rows = 5, secondary = (ListArtGames.primary as ThemePrimary.List).secondary)
        assertEquals(48.dp, libraryListRowHeight(296.dp, 20.dp, 4.dp, geometry))
        assertEquals(LibraryListTwoLineMin, libraryListRowHeight(100.dp, 20.dp, 4.dp, geometry))
        assertEquals(LibraryListOneLineMin, libraryListRowHeight(100.dp, 20.dp, 4.dp, geometry.copy(secondary = null)))
    }

    @Test
    fun `list templates show every row they ask for on the Thor stage`() {
        listOf(ListArtGames, ListArtSystems).forEach { view ->
            val primary = view.primary as ThemePrimary.List
            val geometry = libraryListGeometry(primary)
            val boxHeight = (primary.box.height * stageHeight / thorDensity).dp
            val spacing = JarnSpacing().xs
            val row = libraryListRowHeight(boxHeight, JarnSpacing().focusEdge, spacing, geometry)
            val shown = row * geometry.rows + spacing * (geometry.rows - 1) + JarnSpacing().focusEdge * 2
            assertTrue("${primary.rows} rows need $shown of $boxHeight", shown <= boxHeight + 0.01.dp)
        }
    }

    private fun templateViews(): List<Pair<String, ViewSpec>> = LibraryTemplateThemes.All.flatMap { theme ->
        ThemeView.entries.map { view -> "${theme.id} $view" to requireNotNull(theme.spec.views[view]) }
    }

    private fun JarnType.lineHeightOf(role: ThemeTextRole): Float = when (role) {
        ThemeTextRole.Display -> display
        ThemeTextRole.Title -> title
        ThemeTextRole.Heading -> heading
        ThemeTextRole.Body -> body
        ThemeTextRole.Label -> label
        ThemeTextRole.LabelStrong -> labelStrong
        ThemeTextRole.Caption -> caption
        ThemeTextRole.CaptionStrong -> captionStrong
    }.lineHeight.value
}
