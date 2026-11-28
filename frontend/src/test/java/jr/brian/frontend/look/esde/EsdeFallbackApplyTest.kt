package jr.brian.frontend.look.esde

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemeSystemArt
import jr.brian.core.model.theme.ThemeTextStyle
import jr.brian.core.model.theme.ThemeTokens
import jr.brian.core.model.theme.ThemeTypeScale
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnPaletteMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The fallback's colors, art and fonts land in the spec the import installs. */
class EsdeFallbackApplyTest {
    private val colors = JarnLooks.Grove.colors.toThemeColors()

    private fun result(fonts: EsdeFontFiles = EsdeFontFiles(), art: Map<String, EsdeSystemArtFiles> = emptyMap()) = EsdeFallbackResult(
        colors = colors,
        tier = JarnPaletteMatch.FullPalette,
        darkVersion = null,
        systemArt = art,
        fonts = fonts,
        assets = emptyList(),
        notes = emptyList(),
    )

    private val translated = JarnThemeSpec(
        meta = ThemeMeta(name = "Imported"),
        tokens = ThemeTokens(colors = JarnLooks.Forge.colors.toThemeColors(), type = ThemeTypeScale(title = ThemeTextStyle(size = 40f))),
    )

    @Test
    fun `colors and per-system art come over`() {
        val art = mapOf("snes" to EsdeSystemArtFiles(logo = "esde/logos/snes.svg"))
        val spec = result(art = art).applyTo(translated)
        assertEquals(colors, spec.tokens.colors)
        assertEquals(mapOf("snes" to ThemeSystemArt(logo = "esde/logos/snes.svg")), spec.systemArt)
    }

    @Test
    fun `titles take the display font and text the body font, keeping styled sizes`() {
        val type = result(EsdeFontFiles(display = "esde/fonts/display.ttf", body = "esde/fonts/body.otf")).applyTo(translated).tokens.type
        assertEquals(ThemeTextStyle(size = 40f, font = "esde/fonts/display.ttf"), type.title)
        assertEquals("esde/fonts/display.ttf", type.display?.font)
        assertEquals(56f, type.display?.size)
        assertEquals(700, type.display?.weight)
        assertEquals("esde/fonts/body.otf", type.body?.font)
        assertEquals("esde/fonts/body.otf", type.captionStrong?.font)
    }

    @Test
    fun `a lone body font fills both, and no fonts leave the type alone`() {
        val type = result(EsdeFontFiles(body = "b.ttf")).applyTo(translated).tokens.type
        assertEquals("b.ttf", type.heading?.font)
        assertEquals("b.ttf", type.label?.font)
        val untouched = result().applyTo(translated).tokens.type
        assertEquals(translated.tokens.type, untouched)
        assertNull(untouched.body)
    }
}
