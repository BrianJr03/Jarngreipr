package jr.brian.frontend.look.esde

import android.app.Application
import jr.brian.frontend.look.esde.EsdeParseIssue.Kind
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

/**
 * Whole synthetic themes through the parser: a golden per fixture, plus the few facts each
 * fixture exists to prove, spelled out so a golden re-record can't quietly lose them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeSyntheticThemesTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val parser = EsdeThemeParser()

    private fun parse(theme: EsdeSyntheticTheme, selection: EsdeThemeSelection = theme.selection): EsdeThemeModel =
        parser.parse(theme.install(temp.newFolder()), theme.systems, selection)

    private fun EsdeThemeModel.props(system: String, view: EsdeView, type: String, name: String) =
        systems.getValue(system).views[view]?.get(type, name)

    private fun EsdeThemeModel.kinds() = issues.map { it.kind }.toSet()

    @Test
    fun `every fixture matches its parser golden`() {
        val failures = EsdeSyntheticTheme.entries.mapNotNull { theme ->
            runCatching {
                EsdeFixtureGoldens.assertGolden(theme, PARSER_GOLDEN, parse(theme).toGoldenText(theme))
            }.exceptionOrNull()
        }
        failures.firstOrNull()?.let { first ->
            throw AssertionError(failures.joinToString("\n") { it.message.orEmpty() }, first)
        }
    }

    @Test
    fun `horizon carousel resolves per-system art through system theme`() {
        val model = parse(EsdeSyntheticTheme.HorizonCarousel)

        val nes = model.props("nes", EsdeView.System, "carousel", "systemCarousel").orEmpty()
        assertEquals("horizontal", nes["type"])
        assertEquals("./logos/nes.svg", nes["staticImage"])
        assertEquals("5AB4FF33", nes["color"])
        assertEquals("./art/n64/backdrop.webp", model.props("n64", EsdeView.System, "image", "backdrop")?.get("path"))
        assertEquals("5", nes["maxItemCount"])
        assertTrue(model.issues.isEmpty())
    }

    @Test
    fun `horizon carousel narrows for a 4 by 3 screen`() {
        val model = parse(EsdeSyntheticTheme.HorizonCarousel, EsdeThemeSelection(aspectRatio = "4:3"))

        assertEquals("3", model.props("nes", EsdeView.System, "carousel", "systemCarousel")?.get("maxItemCount"))
    }

    @Test
    fun `wheel shows a video only in the video variant`() {
        val withVideos = parse(EsdeSyntheticTheme.WheelVideo)
        val noVideos = parse(EsdeSyntheticTheme.WheelVideo, EsdeThemeSelection(variant = "noVideos"))

        assertEquals("withVideos", withVideos.selection.variant)
        assertEquals("1.2", withVideos.props("nes", EsdeView.Gamelist, "video", "preview")?.get("delay"))
        assertNull(withVideos.props("nes", EsdeView.Gamelist, "image", "preview"))
        assertNull(noVideos.props("nes", EsdeView.Gamelist, "video", "preview"))
        assertEquals("screenshot", noVideos.props("nes", EsdeView.Gamelist, "image", "preview")?.get("imageType"))
        assertEquals(
            "verticalWheel",
            withVideos.props("snes", EsdeView.System, "carousel", "systemCarousel")?.get("type"),
        )
    }

    @Test
    fun `cover grid takes its label size from the chosen font size`() {
        val large = parse(EsdeSyntheticTheme.CoverGrid)
        val medium = parse(EsdeSyntheticTheme.CoverGrid, EsdeThemeSelection())

        assertEquals("0.045", large.props("nes", EsdeView.Gamelist, "grid", "gamelistGrid")?.get("fontSize"))
        assertEquals("0.03", medium.props("nes", EsdeView.Gamelist, "grid", "gamelistGrid")?.get("fontSize"))
        assertEquals("./bg/plain.webp", large.props("nes", EsdeView.System, "image", "background")?.get("path"))
    }

    @Test
    fun `list detail reads its pane from includes with shared colors`() {
        val model = parse(EsdeSyntheticTheme.ListDetail)

        assertEquals("description", model.props("nes", EsdeView.Gamelist, "text", "summary")?.get("metadata"))
        assertEquals("8A99A6", model.props("nes", EsdeView.Gamelist, "datetime", "released")?.get("color"))
        assertEquals("E8EEF2", model.props("nes", EsdeView.System, "textlist", "systemList")?.get("primaryColor"))
        assertTrue(model.props("nes", EsdeView.Gamelist, "rating", "stars") != null)
        assertTrue(model.issues.isEmpty())
    }

    @Test
    fun `per-system folders win and other systems fall back to the root theme`() {
        val model = parse(EsdeSyntheticTheme.PerSystem)

        // Paths resolve against the file that holds them, as in ES-DE.
        assertEquals("./nes/logo.svg", model.props("nes", EsdeView.Gamelist, "image", "systemLogo")?.get("path"))
        assertNull(model.props("snes", EsdeView.Gamelist, "image", "systemLogo"))
        assertNull(model.props("n64", EsdeView.Gamelist, "image", "systemLogo"))
        val snesList = model.props("snes", EsdeView.Gamelist, "textlist", "gamelistTextlist").orEmpty()
        assertEquals("0.5 0.2", snesList["pos"])
        // snes defines panel before its include redefines it; the golden records today's winner.
        assertTrue(snesList.containsKey("backgroundColor"))
        assertEquals(
            "./_inc/backgrounds/n64.jpg",
            model.props("n64", EsdeView.Gamelist, "image", "background")?.get("path"),
        )
        assertTrue(model.issues.isEmpty())
    }

    @Test
    fun `color schemes pick the light scheme and keep the hidden one out of the default`() {
        val paper = parse(EsdeSyntheticTheme.ColorSchemes)
        val fallback = parse(EsdeSyntheticTheme.ColorSchemes, EsdeThemeSelection())

        assertEquals("F6F3EC", paper.props("nes", EsdeView.Gamelist, "image", "background")?.get("color"))
        assertEquals("B24A2E", paper.props("nes", EsdeView.Gamelist, "textlist", "gamelistTextlist")?.get("selectorColor"))
        assertEquals("dusk", fallback.selection.colorScheme)
        assertFalse(paper.capabilities.colorSchemes.single { it.name == "debug" }.selectable)
    }

    @Test
    fun `legacy format is reported and nothing is read`() {
        val model = parse(EsdeSyntheticTheme.LegacyFormat)

        assertTrue(model.isLegacy)
        assertTrue(model.systems.isEmpty())
    }

    @Test
    fun `hostile theme is contained and every problem is reported`() {
        val model = parse(EsdeSyntheticTheme.Hostile)

        assertEquals(
            setOf(
                Kind.IncludeCycle,
                Kind.PathOutsideTheme,
                Kind.DocTypeRefused,
                Kind.FileTooLarge,
                Kind.MissingInclude,
                Kind.UnknownVariable,
                Kind.Malformed,
            ),
            model.kinds(),
        )
        val system = model.systems.getValue("nes").views[EsdeView.System]
        assertNull(system?.get("text", "leak"))
        assertNull(system?.get("text", "entity"))
        assertTrue(system?.get("text", "fromA") != null)
        assertTrue(system?.get("text", "fromB") != null)
        val gamelist = model.systems.getValue("nes").views.getValue(EsdeView.Gamelist)
        assertEquals(emptyMap<String, String>(), gamelist["image", "escape"])
        assertEquals(emptyMap<String, String>(), gamelist["image", "home"])
        assertEquals("0.1 0.1", gamelist["textlist", "gamelistTextlist"]?.get("pos"))
    }
}
