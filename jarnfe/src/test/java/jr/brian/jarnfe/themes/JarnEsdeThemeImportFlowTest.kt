package jr.brian.jarnfe.themes

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import jr.brian.core.model.theme.JarnThemeFormat
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.esde.EsdeElementKey
import jr.brian.frontend.look.esde.EsdeFallbackNote
import jr.brian.frontend.look.esde.EsdeImportOutcome
import jr.brian.frontend.look.esde.EsdeReportItem
import jr.brian.frontend.look.esde.EsdeThemeSelection
import jr.brian.frontend.look.esde.EsdeTranslationNote
import jr.brian.frontend.look.esde.EsdeTranslationReport
import jr.brian.frontend.look.esde.EsdeView
import jr.brian.frontend.look.themes.EsdeThemeFolder
import jr.brian.frontend.look.themes.EsdeThemeScan
import jr.brian.frontend.look.themes.ThemeImportProblem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import jr.brian.jarnfe.settings.EsdeFallbackLine
import jr.brian.jarnfe.settings.fallbackLines
import jr.brian.jarnfe.settings.messageRes
import jr.brian.jarnfe.settings.summary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnEsdeThemeImportFlowTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val current = EsdeThemeFolder(File("/es/themes/linear"), "Linear", isCurrent = true)
    private val other = EsdeThemeFolder(File("/es/themes/zeta"), "Zeta", isCurrent = false)
    private val settings = EsdeSettings(themeSet = "linear", themeVariant = "textlist")

    private fun imported(report: EsdeTranslationReport = EsdeTranslationReport(emptyList(), emptyList(), emptyList())) =
        EsdeImportOutcome.Imported("linear", File("/t"), "Linear", report, emptyList(), 0, 3, 0)

    private fun flow(scan: EsdeThemeScan, outcome: EsdeImportOutcome = imported(), seen: MutableList<EsdeThemeSelection>? = null) =
        JarnEsdeThemeImportFlow(
            scan = { scan },
            runImport = { _, selection -> seen?.add(selection); outcome },
            io = UnconfinedTestDispatcher(),
        )

    @Test
    fun `scan results become the matching steps`() = runTest {
        val noAccess = flow(EsdeThemeScan.NoAccess).apply { start() }
        assertEquals(EsdeImportStep.NoAccess, noAccess.step)
        val none = flow(EsdeThemeScan.NotFound(null)).apply { start() }
        assertEquals(EsdeImportStep.NotFound, none.step)
        val found = flow(EsdeThemeScan.Found(listOf(current, other), settings)).apply { start() }
        assertEquals(EsdeImportStep.Choosing(listOf(current, other), settings), found.step)
    }

    @Test
    fun `importing uses ES-DE's picks only for the theme ES-DE uses now`() = runTest {
        val seen = mutableListOf<EsdeThemeSelection>()
        val flow = flow(EsdeThemeScan.Found(listOf(current, other), settings), seen = seen)
        flow.import(current, settings)
        flow.import(other, settings)
        assertEquals(listOf(EsdeThemeSelection(variant = "textlist"), EsdeThemeSelection()), seen)
        assertTrue(flow.step is EsdeImportStep.Done)
    }

    @Test
    fun `legacy and failed imports say why`() = runTest {
        val legacy = flow(EsdeThemeScan.NotFound(null), EsdeImportOutcome.Legacy).apply { import(other, null) }
        assertEquals(EsdeImportStep.Legacy, legacy.step)
        val failed = flow(EsdeThemeScan.NotFound(null), EsdeImportOutcome.Failed(ThemeImportProblem.TooLarge))
            .apply { import(other, null) }
        assertEquals(EsdeImportStep.Failed(ThemeImportProblem.TooLarge), failed.step)
    }

    @Test
    fun `the report counts parts, not notes about the whole theme`() {
        fun item(name: String, note: EsdeTranslationNote? = null) = EsdeReportItem(EsdeView.System, EsdeElementKey("image", name), note)
        val report = EsdeTranslationReport(
            translated = listOf(item("a"), item("b"), item("c")),
            adjusted = listOf(item("a", EsdeTranslationNote.WheelAsShelf)),
            notUsed = listOf(
                item("d", EsdeTranslationNote.UnsupportedElement),
                item("e", EsdeTranslationNote.UnsupportedElement),
                EsdeReportItem(null, null, EsdeTranslationNote.TransitionsAndTriggers),
            ),
        )
        val summary = report.summary()
        assertEquals(3, summary.cameOver)
        assertEquals(5, summary.total)
        assertEquals(2, summary.needFix)
        assertEquals(listOf(EsdeTranslationNote.WheelAsShelf to 1), summary.changed)
        assertTrue(EsdeTranslationNote.UnsupportedElement to 2 in summary.leftOut)
    }

    @Test
    fun `every translation note has its own words`() {
        val res = EsdeTranslationNote.entries.map { it.messageRes }
        assertEquals(res.size, res.toSet().size)
    }

    @Test
    fun `fallback notes fold into short lines`() {
        val outcome = imported().copy(
            fallbackNotes = listOf(
                EsdeFallbackNote.Colors(JarnPaletteMatch.AccentOnSavedLook, darkVersion = "Dark"),
                EsdeFallbackNote.UnknownSystem("foo"),
                EsdeFallbackNote.UnknownSystem("bar"),
                EsdeFallbackNote.SvgNotCopied("nes"),
                EsdeFallbackNote.LogoNotCopied("snes"),
            ),
            systemsWithArt = 2,
            unusableFiles = 1,
        )
        assertEquals(
            listOf(
                EsdeFallbackLine.Text(R.string.jarnfe_esde_fallback_accent_only),
                EsdeFallbackLine.Text(R.string.jarnfe_esde_fallback_dark_version, "Dark"),
                EsdeFallbackLine.Count(R.plurals.jarnfe_esde_fallback_systems_with_art, 2, 3),
                EsdeFallbackLine.Count(R.plurals.jarnfe_esde_fallback_unknown_systems, 2),
                EsdeFallbackLine.Count(R.plurals.jarnfe_esde_fallback_art_not_copied, 2),
                EsdeFallbackLine.Count(R.plurals.jarnfe_esde_import_unusable_files, 1),
            ),
            outcome.fallbackLines(),
        )
    }

    @Test
    fun `installed themes are read back as own-color layouts, skipping broken folders`() {
        val root = temp.newFolder("themes")
        File(root, "good").apply { mkdirs() }.resolve("theme.json").writeText(JarnThemeFormat.encode(LibraryBuiltInThemes.Grid.spec))
        File(root, "broken").apply { mkdirs() }.resolve("theme.json").writeText("{ nope")
        File(root, ".incoming-x").mkdirs()

        val entries = readImportedThemes(root)

        assertEquals(listOf("good"), entries.map { it.id })
        assertTrue(entries.single().ownColors)
        assertEquals(JarnThemeKind.Imported, entries.single().kind)
        assertEquals(File(root, "good"), entries.single().themeDir)
    }

    @Test
    fun `using an imported layout turns off both Command Panel matches`() {
        val store = JarnLookStore(ApplicationProvider.getApplicationContext())
        store.setMatchCommandPanel(true)
        store.setMatchCommandPanelFonts(true)
        val entry = readImportedThemes(temp.newFolder("t").also {
            File(it, "mine").apply { mkdirs() }.resolve("theme.json").writeText(JarnThemeFormat.encode(LibraryBuiltInThemes.Grid.spec))
        }).single()

        store.applyThemeChoice(JarnThemeChoice(entry))

        assertEquals("mine", store.activeThemeId)
        assertFalse(store.matchCommandPanel)
        assertFalse(store.matchCommandPanelFonts)
    }
}
