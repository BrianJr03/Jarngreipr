package jr.brian.jarnfe.themes

import androidx.compose.ui.graphics.toArgb
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ThemeViews
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

internal val TemplateCarousel = JarnThemeEntry(
    id = "template-shelf",
    kind = JarnThemeKind.Template,
    theme = LibraryBuiltInThemes.Carousel,
)

internal val OwnColorsTheme = JarnThemeEntry(
    id = "imported-red",
    kind = JarnThemeKind.Imported,
    theme = LibraryBuiltInThemes.Grid,
    ownColors = true,
)

/** Only a systems view, so games fall back unless overridden. */
private val SystemsOnly = TemplateCarousel.copy(
    id = "systems-only",
    theme = LibraryBuiltInThemes.Carousel.copy(
        spec = LibraryBuiltInThemes.Carousel.spec.copy(
            views = ThemeViews(systems = LibraryBuiltInThemes.Carousel.spec.views.systems),
        ),
    ),
)

internal val TestThemes = JarnThemeRegistry.builtIns + listOf(TemplateCarousel, OwnColorsTheme, SystemsOnly)

private fun display(systems: JarnBrowseView, games: JarnBrowseView) =
    JarnDisplayPrefs(systemView = systems, gameView = games)

class JarnActiveThemeTest {
    @Test
    fun `a player from before layouts lands on their stored built-in`() {
        val choice = resolveJarnThemeChoice(null, display(JarnBrowseView.Carousel, JarnBrowseView.Carousel), TestThemes)
        assertEquals(JarnThemeChoice(JarnThemeRegistry.Carousel), choice)
        val theme = choice.activeTheme()
        assertSame(JarnThemeRegistry.Carousel, theme.systems)
        assertSame(JarnThemeRegistry.Carousel, theme.games)
    }

    @Test
    fun `an old mix becomes the systems layout with a games override`() {
        val choice = resolveJarnThemeChoice(null, display(JarnBrowseView.Grid, JarnBrowseView.Carousel), TestThemes)
        assertEquals(JarnThemeChoice(JarnThemeRegistry.Grid, games = JarnBrowseView.Carousel), choice)
        val theme = choice.activeTheme()
        assertSame(JarnThemeRegistry.Grid, theme.forView(ThemeView.Systems))
        assertSame(JarnThemeRegistry.Carousel, theme.forView(ThemeView.Games))
    }

    @Test
    fun `a theme not on this device falls back to the built-ins`() {
        val prefs = display(JarnBrowseView.SameAsLayout, JarnBrowseView.SameAsLayout)
        assertEquals(JarnThemeChoice(JarnThemeRegistry.Grid), resolveJarnThemeChoice("not-installed", prefs, TestThemes))
    }

    @Test
    fun `a saved layout draws both views unless one is overridden`() {
        val same = display(JarnBrowseView.SameAsLayout, JarnBrowseView.SameAsLayout)
        val plain = resolveJarnThemeChoice("template-shelf", same, TestThemes).activeTheme()
        assertSame(TemplateCarousel, plain.systems)
        assertSame(TemplateCarousel, plain.games)

        val overridden = display(JarnBrowseView.SameAsLayout, JarnBrowseView.Grid)
        val mixed = resolveJarnThemeChoice("template-shelf", overridden, TestThemes).activeTheme()
        assertSame(TemplateCarousel, mixed.systems)
        assertSame(JarnThemeRegistry.Grid, mixed.games)
    }

    @Test
    fun `with a saved layout a matching override is still an override`() {
        val prefs = display(JarnBrowseView.Grid, JarnBrowseView.SameAsLayout)
        assertEquals(
            JarnThemeChoice(JarnThemeRegistry.Grid, systems = JarnBrowseView.Grid),
            resolveJarnThemeChoice(JarnThemeRegistry.Grid.id, prefs, TestThemes),
        )
    }

    @Test
    fun `a view the layout leaves out draws the grid`() {
        val theme = JarnThemeChoice(SystemsOnly).activeTheme()
        assertSame(SystemsOnly, theme.systems)
        assertSame(JarnThemeRegistry.Grid, theme.games)
    }

    @Test
    fun `built-ins and templates follow the look`() {
        assertNull(JarnThemeChoice(JarnThemeRegistry.Grid, games = JarnBrowseView.Carousel).activeTheme().ownColors)
        assertNull(JarnThemeChoice(TemplateCarousel).activeTheme().ownColors)
    }

    @Test
    fun `a theme with its own colors hands them over`() {
        assertEquals(OwnColorsTheme.theme.spec.tokens.colors, JarnThemeChoice(OwnColorsTheme).activeTheme().ownColors)
    }

    @Test
    fun `the shipped layouts are pickable and follow the look`() {
        val ids = JarnThemeRegistry.installed.map { it.id }
        assertEquals(
            listOf("builtin-grid", "builtin-carousel", "template.big-shelf", "template.cover-grid", "template.list-art"),
            ids,
        )
        assertTrue(JarnThemeRegistry.templates.all { it.kind == JarnThemeKind.Template && !it.ownColors && it.nameRes != null && it.blurbRes != null })
        assertNull(resolveJarnThemeChoice("template.list-art", JarnDisplayPrefs()).activeTheme().ownColors)
    }

    @Test
    fun `every shipped layout has its own sketch`() {
        val kinds = JarnThemeRegistry.installed.map { it.sketchKind }
        assertEquals(kinds.distinct(), kinds)
        assertEquals(JarnThemeSketchKind.Carousel, TemplateCarousel.sketchKind)
    }

    @Test
    fun `override rows step same, grid, carousel`() {
        assertEquals(JarnBrowseView.Grid, null.nextOverride())
        assertEquals(JarnBrowseView.Carousel, JarnBrowseView.Grid.nextOverride())
        assertNull(JarnBrowseView.Carousel.nextOverride())
    }

    @Test
    fun `browse view follows the primary`() {
        assertEquals(JarnBrowseView.Carousel, JarnThemeRegistry.Carousel.browseView(ThemeView.Games))
        assertEquals(JarnBrowseView.Grid, JarnThemeRegistry.Grid.browseView(ThemeView.Systems))
        assertEquals(JarnBrowseView.Grid, SystemsOnly.browseView(ThemeView.Games))
    }

    @Test
    fun `readable theme colors pass repair and unreadable ones do not`() {
        val dark = themeColors(bg = 0xFF101418.toInt(), text = 0xFFF2F5F8.toInt())
        assertEquals(dark.bg.argb, dark.toReadableJarnColors()?.bg?.toArgb())
        assertNull(themeColors(bg = 0xFF101418.toInt(), text = 0xFF101418.toInt()).toReadableJarnColors())
    }
}

internal fun themeColors(bg: Int, text: Int, accent: Int = 0xFF6FB6FF.toInt()) = ThemeColors(
    bg = ThemeColor(bg),
    surface = ThemeColor(0xFF1A2026.toInt()),
    raised = ThemeColor(0xFF232B33.toInt()),
    textHigh = ThemeColor(text),
    textMid = ThemeColor(text),
    textLow = ThemeColor(text),
    accent = ThemeColor(accent),
    danger = ThemeColor(0xFFFF8A80.toInt()),
    success = ThemeColor(0xFF7BE0A3.toInt()),
)
