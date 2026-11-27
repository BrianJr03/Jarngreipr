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

/** Only a systems view, so games keep the stored built-in. */
private val SystemsOnly = TemplateCarousel.copy(
    id = "systems-only",
    theme = LibraryBuiltInThemes.Carousel.copy(
        spec = LibraryBuiltInThemes.Carousel.spec.copy(
            views = ThemeViews(systems = LibraryBuiltInThemes.Carousel.spec.views.systems),
        ),
    ),
)

private fun display(systems: JarnBrowseView, games: JarnBrowseView) =
    JarnDisplayPrefs(systemView = systems, gameView = games)

class JarnActiveThemeTest {
    private val installed = listOf(TemplateCarousel, OwnColorsTheme, SystemsOnly)

    @Test
    fun `no saved theme lands on the stored grid or carousel`() {
        val choice = resolveJarnThemeChoice(null, display(JarnBrowseView.Carousel, JarnBrowseView.Carousel), installed)
        assertEquals(JarnThemeChoice.BuiltIn(JarnBrowseView.Carousel, JarnBrowseView.Carousel), choice)
        val theme = choice.activeTheme(JarnDisplayPrefs())
        assertSame(JarnThemeRegistry.Carousel, theme.systems)
        assertSame(JarnThemeRegistry.Carousel, theme.games)
    }

    @Test
    fun `different stored views keep their mix`() {
        val prefs = display(JarnBrowseView.Grid, JarnBrowseView.Carousel)
        val choice = resolveJarnThemeChoice(null, prefs, installed) as JarnThemeChoice.BuiltIn
        assertTrue(choice.mixed)
        val theme = choice.activeTheme(prefs)
        assertSame(JarnThemeRegistry.Grid, theme.forView(ThemeView.Systems))
        assertSame(JarnThemeRegistry.Carousel, theme.forView(ThemeView.Games))
    }

    @Test
    fun `a theme not on this device falls back to the built-ins`() {
        val prefs = display(JarnBrowseView.Grid, JarnBrowseView.Carousel)
        assertEquals(
            JarnThemeChoice.BuiltIn(JarnBrowseView.Grid, JarnBrowseView.Carousel),
            resolveJarnThemeChoice("not-installed", prefs, installed),
        )
    }

    @Test
    fun `an installed theme draws both views`() {
        val choice = resolveJarnThemeChoice("template-shelf", JarnDisplayPrefs(), installed)
        assertEquals(JarnThemeChoice.Installed(TemplateCarousel), choice)
        val theme = choice.activeTheme(JarnDisplayPrefs())
        assertSame(TemplateCarousel, theme.systems)
        assertSame(TemplateCarousel, theme.games)
    }

    @Test
    fun `a view the theme leaves out keeps the stored built-in`() {
        val prefs = display(JarnBrowseView.Grid, JarnBrowseView.Carousel)
        val theme = JarnThemeChoice.Installed(SystemsOnly).activeTheme(prefs)
        assertSame(SystemsOnly, theme.systems)
        assertSame(JarnThemeRegistry.Carousel, theme.games)
    }

    @Test
    fun `built-ins and templates follow the look`() {
        assertNull(JarnThemeChoice.BuiltIn(JarnBrowseView.Grid, JarnBrowseView.Carousel).activeTheme(JarnDisplayPrefs()).ownColors)
        assertNull(JarnThemeChoice.Installed(TemplateCarousel).activeTheme(JarnDisplayPrefs()).ownColors)
    }

    @Test
    fun `a theme with its own colors hands them over`() {
        val theme = JarnThemeChoice.Installed(OwnColorsTheme).activeTheme(JarnDisplayPrefs())
        assertEquals(OwnColorsTheme.theme.spec.tokens.colors, theme.ownColors)
    }

    @Test
    fun `the shipped templates are pickable and follow the look`() {
        val templates = JarnThemeRegistry.installed
        assertEquals(listOf("template.big-shelf", "template.cover-grid", "template.list-art"), templates.map { it.id })
        assertTrue(templates.all { it.kind == JarnThemeKind.Template && !it.ownColors && it.nameRes != null })
        val saved = resolveJarnThemeChoice("template.list-art", JarnDisplayPrefs())
        assertNull(saved.activeTheme(JarnDisplayPrefs()).ownColors)
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
