package jr.brian.frontend.look.esde

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

private const val FALLBACK_GOLDEN = "fallback.txt"
private val FontFile = File("../core-ui/src/main/res/font/jarn_barlow_condensed_bold.ttf")

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeThemeFallbackTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val parser = EsdeThemeParser()
    private val catalog = StubSystemCatalog(SystemDef("nes", "NES"), SystemDef("snes", "SNES", folderAliases = listOf("sfc")), SystemDef("n64", "N64"))
    private val savedLook = JarnLooks.Forge

    /** Copies without decoding, so tests don't need real image codecs. */
    private val copyScaler = EsdeImageScaler { source, target, _, _, _ -> source.copyTo(target, overwrite = true).isFile }

    private fun fallback(
        theme: EsdeSyntheticTheme,
        selection: EsdeThemeSelection = theme.selection,
        withReparse: Boolean = true,
        prepare: (File) -> Unit = {},
    ): Pair<EsdeFallbackResult, File> {
        val dir = theme.install(temp.newFolder())
        prepare(dir)
        val out = temp.newFolder()
        val model = parser.parse(dir, theme.systems, selection)
        val reparse = { scheme: String -> parser.parse(dir, theme.systems, selection.copy(colorScheme = scheme)) }
        val result = EsdeThemeFallback.build(
            model = model,
            themeDir = dir,
            savedLook = savedLook,
            catalog = catalog,
            outDir = out,
            reparse = reparse.takeIf { withReparse },
            copier = EsdeAssetCopier(dir, out, scaler = copyScaler),
        )
        return result to out
    }

    @Test
    fun `fixtures match their fallback goldens`() {
        val themes = listOf(
            EsdeSyntheticTheme.ColorSchemes,
            EsdeSyntheticTheme.PerSystem,
            EsdeSyntheticTheme.HorizonCarousel,
            EsdeSyntheticTheme.ListDetail,
            EsdeSyntheticTheme.LegacyFormat,
        )
        themes.forEach { theme ->
            EsdeFixtureGoldens.assertGolden(theme, FALLBACK_GOLDEN, fallback(theme).first.toGoldenText())
        }
    }

    @Test
    fun `a light scheme switches to the theme's dark scheme`() {
        val (result, _) = fallback(EsdeSyntheticTheme.ColorSchemes)
        assertEquals(JarnPaletteMatch.FullPalette, result.tier)
        assertEquals("Dusk", result.darkVersion)
        assertEquals(Color(0xFF14181F).argbInt(), result.colors.bg.argb)
    }

    @Test
    fun `a dark scheme keeps its own full palette`() {
        val (result, _) = fallback(EsdeSyntheticTheme.ColorSchemes, EsdeThemeSelection(colorScheme = "dusk"))
        assertEquals(JarnPaletteMatch.FullPalette, result.tier)
        assertNull(result.darkVersion)
    }

    @Test
    fun `a light theme with no dark scheme keeps only its accent`() {
        val (result, _) = fallback(EsdeSyntheticTheme.ColorSchemes, withReparse = false)
        assertEquals(JarnPaletteMatch.AccentOnSavedLook, result.tier)
        assertTrue(EsdeFallbackNote.LightThemeAccentOnly in result.notes)
        assertEquals(savedLook.colors.bg.argbInt(), result.colors.bg.argb)
    }

    @Test
    fun `danger and success keep the saved look's hues`() {
        val (result, _) = fallback(EsdeSyntheticTheme.ColorSchemes, EsdeThemeSelection(colorScheme = "dusk"))
        assertEquals(hue(savedLook.colors.danger.argbInt()), hue(result.colors.danger.argb), 2f)
        assertEquals(hue(savedLook.colors.success.argbInt()), hue(result.colors.success.argb), 2f)
    }

    @Test
    fun `only the player's systems' art is copied, shrunk, under the theme folder`() {
        val (result, out) = fallback(EsdeSyntheticTheme.PerSystem) { dir ->
            File(dir, "_inc/backgrounds").mkdirs()
            listOf("nes", "snes", "megadrive").forEach { File(dir, "_inc/backgrounds/$it.jpg").writeBytes(pngHeader(64, 32)) }
            File(dir, "nes/logo.svg").writeText("<svg/>")
        }
        assertEquals(setOf("nes", "snes"), result.systemArt.keys)
        assertEquals("esde/backgrounds/nes.jpg", result.systemArt["nes"]?.background)
        assertTrue(File(out, "esde/backgrounds/snes.jpg").isFile)
        assertFalse(File(out, "esde/backgrounds/megadrive.jpg").exists())
        assertEquals("esde/logos/nes.svg", result.systemArt["nes"]?.logo)
        assertEquals("<svg/>", File(out, "esde/logos/nes.svg").readText())
        assertTrue(EsdeFallbackNote.BackgroundNotCopied("n64") in result.notes)
    }

    @Test
    fun `a variant system borrows its parent's art`() {
        val dir = temp.newFolder("theme")
        File(dir, "logos").mkdirs()
        File(dir, "logos/snes.png").writeBytes(pngHeader(40, 20))
        File(dir, "theme.xml").writeText(
            "<theme><view name=\"system\"><carousel name=\"systemCarousel\"><staticImage>./logos/\${system.theme}.png</staticImage></carousel></view></theme>",
        )
        File(dir, "capabilities.xml").writeText("<themeCapabilities><themeName>Variant</themeName></themeCapabilities>")
        val model = parser.parse(dir, listOf(EsdeSystemRef("snesh")))
        val out = temp.newFolder()
        val result = EsdeThemeFallback.build(model, dir, savedLook, catalog, out, copier = EsdeAssetCopier(dir, out, scaler = copyScaler))
        assertEquals(mapOf("snes" to EsdeSystemArtFiles(logo = "esde/logos/snes.png")), result.systemArt)
    }

    @Test
    fun `unknown systems and paths outside the theme are left out`() {
        val dir = temp.newFolder("theme")
        File(temp.root, "secret.png").writeBytes(pngHeader(8, 8))
        File(dir, "theme.xml").writeText(
            "<theme><view name=\"system\"><image name=\"logo\"><path>../secret.png</path></image></view></theme>",
        )
        File(dir, "capabilities.xml").writeText("<themeCapabilities/>")
        val model = parser.parse(dir, listOf(EsdeSystemRef("nes"), EsdeSystemRef("zxspectrum")))
        val out = temp.newFolder()
        val result = EsdeThemeFallback.build(model, dir, savedLook, catalog, out, copier = EsdeAssetCopier(dir, out, scaler = copyScaler))
        assertTrue(result.systemArt.isEmpty())
        assertTrue(EsdeFallbackNote.UnknownSystem("zxspectrum") in result.notes)
    }

    @Test
    fun `a hostile SVG logo is refused and Jarn's own logo stays`() {
        val dir = temp.newFolder("theme")
        File(dir, "logos").mkdirs()
        File(dir, "logos/nes.svg").writeText("<svg><script>fetch('https://x')</script></svg>")
        File(dir, "theme.xml").writeText(
            "<theme><view name=\"system\"><image name=\"logo\"><path>./logos/\${system.theme}.svg</path></image></view></theme>",
        )
        File(dir, "capabilities.xml").writeText("<themeCapabilities/>")
        val model = parser.parse(dir, listOf(EsdeSystemRef("nes")))
        val out = temp.newFolder()
        val result = EsdeThemeFallback.build(model, dir, savedLook, catalog, out, copier = EsdeAssetCopier(dir, out, scaler = copyScaler))
        assertTrue(result.systemArt.isEmpty())
        assertTrue(EsdeFallbackNote.SvgRefused("nes") in result.notes)
        assertFalse(File(out, "esde/logos/nes.svg").exists())
    }

    @Test
    fun `copies stop at the cap and say so`() {
        val theme = EsdeSyntheticTheme.PerSystem.install(temp.newFolder())
        File(theme, "_inc/backgrounds").mkdirs()
        listOf("nes", "snes", "n64").forEach { File(theme, "_inc/backgrounds/$it.jpg").writeBytes(pngHeader(64, 32)) }
        val model = parser.parse(theme, EsdeSyntheticTheme.PerSystem.systems)
        val out = temp.newFolder()
        val capped = EsdeAssetCopier(theme, out, capBytes = pngHeader(64, 32).size * 2L, scaler = copyScaler)
        val result = EsdeThemeFallback.build(model, theme, savedLook, catalog, out, copier = capped)
        assertEquals(2, result.assets.size)
        assertTrue(EsdeFallbackNote.AssetsCapped in result.notes)
    }

    @Test
    fun `fonts are used only when they pass the glyph check`() {
        val dir = temp.newFolder("theme")
        File(dir, "fonts").mkdirs()
        FontFile.copyTo(File(dir, "fonts/good.ttf"))
        File(dir, "fonts/bad.ttf").writeText("not a font")
        File(dir, "theme.xml").writeText(
            "<theme><view name=\"gamelist\"><textlist name=\"gamelistTextlist\"><fontPath>./fonts/bad.ttf</fontPath></textlist>" +
                "<text name=\"title\"><fontPath>./fonts/good.ttf</fontPath></text></view></theme>",
        )
        File(dir, "capabilities.xml").writeText("<themeCapabilities/>")
        val model = parser.parse(dir, listOf(EsdeSystemRef("nes")))
        val out = temp.newFolder()
        val result = EsdeThemeFallback.build(model, dir, savedLook, catalog, out, copier = EsdeAssetCopier(dir, out, scaler = copyScaler))
        assertEquals(EsdeFontFiles(display = "esde/fonts/display.ttf"), result.fonts)
        assertTrue(EsdeFallbackNote.FontRefused("./fonts/bad.ttf") in result.notes)
    }

    @Test
    fun `colors read RRGGBB and RRGGBBAA and drop mostly transparent ones`() {
        assertEquals(Color(0xFF6FC3FF), parseEsdeColor("6FC3FF"))
        assertEquals(Color(0xFF5AB4FF), parseEsdeColor("5AB4FFCC"))
        assertNull(parseEsdeColor("5AB4FF33"))
        assertNull(parseEsdeColor("\${bg}"))
        assertNull(parseEsdeColor("12345"))
    }
}

private fun Color.argbInt(): Int = toArgb()

private fun hue(argb: Int): Float = FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }[0]

/** A PNG signature and IHDR, enough for the size check; the test scaler never decodes it. */
private fun pngHeader(width: Int, height: Int): ByteArray {
    val buf = java.nio.ByteBuffer.allocate(33)
    buf.put(byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10))
    buf.putInt(13).put("IHDR".toByteArray()).putInt(width).putInt(height)
    return buf.array()
}

private fun EsdeFallbackResult.toGoldenText(): String = buildString {
    appendLine("tier=$tier darkVersion=$darkVersion")
    appendLine("colors=$colors")
    systemArt.toSortedMap().forEach { (id, files) -> appendLine("art $id: logo=${files.logo} background=${files.background}") }
    appendLine("fonts=$fonts")
    appendLine("assets=${assets.joinToString()}")
    notes.forEach { appendLine("note $it") }
}
