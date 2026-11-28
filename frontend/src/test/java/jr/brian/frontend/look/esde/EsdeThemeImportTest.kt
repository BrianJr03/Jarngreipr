package jr.brian.frontend.look.esde

import android.app.Application
import jr.brian.core.model.theme.JarnThemeDecodeResult
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.model.theme.ThemeSource
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import jr.brian.frontend.look.themes.EsdeThemeFolder
import jr.brian.frontend.look.themes.readEsdeImportRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeThemeImportTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var themes: File

    private val options = EsdeTranslateOptions(
        fallbackColors = ThemeColors(
            bg = ThemeColor(0xFF000000.toInt()), surface = ThemeColor(0xFF111111.toInt()),
            raised = ThemeColor(0xFF222222.toInt()), textHigh = ThemeColor(0xFFFFFFFF.toInt()),
            textMid = ThemeColor(0xFFDDDDDD.toInt()), textLow = ThemeColor(0xFFBBBBBB.toInt()),
            accent = ThemeColor(0xFFFF8800.toInt()), danger = ThemeColor(0xFFFF4444.toInt()),
            success = ThemeColor(0xFF44FF44.toInt()),
        ),
        fallbackName = "Imported",
        systemsTopClear = 0.2f,
    )

    @Before
    fun setup() {
        themes = temp.newFolder("themes")
    }

    private fun import(theme: EsdeSyntheticTheme, source: File = theme.install(temp.newFolder())) =
        EsdeThemeImport(themes).run(source, theme.systems, theme.selection, options) to source

    @Test
    fun `every fixture imports or says why, and never writes to ES-DE`() {
        EsdeSyntheticTheme.entries.forEach { theme ->
            val source = theme.install(temp.newFolder())
            val before = source.walk().map { it.path to it.lastModified() }.toList()
            val (outcome, _) = import(theme, source)
            when (theme) {
                EsdeSyntheticTheme.LegacyFormat -> assertEquals(EsdeImportOutcome.Legacy, outcome)
                else -> assertTrue("$theme: $outcome", outcome is EsdeImportOutcome.Imported)
            }
            assertEquals(theme.name, before, source.walk().map { it.path to it.lastModified() }.toList())
        }
    }

    @Test
    fun `an import installs an ES-DE theme with its record and files`() {
        val (outcome, source) = import(EsdeSyntheticTheme.HorizonCarousel)
        val imported = outcome as EsdeImportOutcome.Imported

        val saved = JarnThemeFormat.decode(File(imported.folder, "theme.json").readText()) as JarnThemeDecodeResult.Decoded
        assertEquals(ThemeSource.EsDe, saved.spec.meta.source)
        val record = readEsdeImportRecord(imported.folder)!!
        assertEquals(source.path, record.sourceFolder)
        assertTrue(imported.report.translated.isNotEmpty())
    }

    @Test
    fun `importing the same folder again replaces the earlier copy`() {
        val source = EsdeSyntheticTheme.CoverGrid.install(temp.newFolder())
        val first = import(EsdeSyntheticTheme.CoverGrid, source).first as EsdeImportOutcome.Imported
        val second = import(EsdeSyntheticTheme.CoverGrid, source).first as EsdeImportOutcome.Imported

        assertEquals(first.themeId, second.themeId)
        assertEquals(listOf(first.themeId), themes.list()!!.toList())
    }

    @Test
    fun `a missing folder fails without installing`() {
        val outcome = EsdeThemeImport(themes).run(File(temp.root, "gone"), emptyList(), EsdeThemeSelection(), options)
        assertTrue(outcome is EsdeImportOutcome.Failed)
        assertEquals(emptyList<String>(), themes.list()!!.toList())
    }

    @Test
    fun `with no library the theme's own system folders stand in`() {
        val source = EsdeSyntheticTheme.PerSystem.install(temp.newFolder())
        val outcome = EsdeThemeImport(themes).run(source, emptyList(), EsdeThemeSelection(), options)
        assertTrue(outcome is EsdeImportOutcome.Imported)
    }

    @Test
    fun `the fallback hook sees every translation before install`() {
        var seen = 0
        val fallback = EsdeImportFallback { _, t, _, outDir, _ ->
            seen++
            assertTrue(outDir.path.contains(".incoming-"))
            EsdeFallbackOutcome(t.spec.copy(meta = t.spec.meta.copy(name = "Fixed")))
        }
        val source = EsdeSyntheticTheme.ListDetail.install(temp.newFolder())
        val outcome = EsdeThemeImport(themes, fallback = fallback).run(source, EsdeFixtureSystems, EsdeThemeSelection(), options)
        assertEquals(1, seen)
        assertEquals("fixed", (outcome as EsdeImportOutcome.Imported).themeId)
    }

    @Test
    fun `the readable fallback sets the theme's colors and reports what it did`() {
        val catalog = StubSystemCatalog(SystemDef("nes", "NES"), SystemDef("snes", "SNES"), SystemDef("n64", "N64"))
        val source = EsdeSyntheticTheme.ColorSchemes.install(temp.newFolder())
        val outcome = EsdeThemeImport(themes, EsdeImportFallback.readable(JarnLooks.Forge, catalog))
            .run(source, EsdeFixtureSystems, EsdeSyntheticTheme.ColorSchemes.selection, options) as EsdeImportOutcome.Imported

        val saved = JarnThemeFormat.decode(File(outcome.folder, "theme.json").readText()) as JarnThemeDecodeResult.Decoded
        assertTrue(saved.spec.tokens.colors != options.fallbackColors)
        assertTrue(outcome.fallbackNotes.any { it is EsdeFallbackNote.Colors || it is EsdeFallbackNote.LightThemeAccentOnly })
    }

    @Test
    fun `only a theme with no readable colors follows the Look`() {
        assertTrue(followsLookFor(JarnPaletteMatch.SavedLook))
        assertFalse(followsLookFor(JarnPaletteMatch.AccentOnSavedLook))
        assertFalse(followsLookFor(JarnPaletteMatch.FullPalette))
    }

    @Test
    fun `every installed import agrees with its color report`() {
        val catalog = StubSystemCatalog(SystemDef("nes", "NES"), SystemDef("snes", "SNES"), SystemDef("n64", "N64"))
        val tiers = mutableSetOf<JarnPaletteMatch>()
        listOf(JarnLooks.Forge, JarnLooks.Fjord).forEach { look ->
            EsdeSyntheticTheme.entries.filter { it != EsdeSyntheticTheme.LegacyFormat }.forEach { theme ->
                val root = temp.newFolder()
                val source = theme.install(temp.newFolder())
                val outcome = EsdeThemeImport(root, EsdeImportFallback.readable(look, catalog))
                    .run(source, theme.systems, theme.selection, options) as EsdeImportOutcome.Imported
                val tier = outcome.fallbackNotes.filterIsInstance<EsdeFallbackNote.Colors>().single().tier
                tiers += tier
                val saved = JarnThemeFormat.decode(File(outcome.folder, "theme.json").readText()) as JarnThemeDecodeResult.Decoded
                assertEquals("$theme on ${look.id}", tier == JarnPaletteMatch.SavedLook, saved.spec.meta.followsLook)
            }
        }
        assertTrue("fixtures should cover more than one tier: $tiers", tiers.size > 1)
    }

    @Test
    fun `without a fallback the import follows the Look`() {
        val (outcome, _) = import(EsdeSyntheticTheme.HorizonCarousel)
        val saved = JarnThemeFormat.decode(File((outcome as EsdeImportOutcome.Imported).folder, "theme.json").readText())
        assertTrue((saved as JarnThemeDecodeResult.Decoded).spec.meta.followsLook)
    }

    @Test
    fun `ES-DE's own picks apply only to the theme it uses now`() {
        val settings = EsdeSettings(themeVariant = "textlist", themeColorScheme = "dark", themeAspectRatio = "16:9")
        val current = EsdeThemeFolder(File("/x"), "X", isCurrent = true)
        assertEquals(EsdeThemeSelection(variant = "textlist", colorScheme = "dark", aspectRatio = "16:9"), settings.selectionFor(current))
        assertEquals(EsdeThemeSelection(), settings.selectionFor(current.copy(isCurrent = false)))
        assertEquals(EsdeThemeSelection(), null.selectionFor(current))
    }
}
