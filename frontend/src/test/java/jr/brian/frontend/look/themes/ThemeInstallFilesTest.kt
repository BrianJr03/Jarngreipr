package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Files

class ThemeInstallFilesTest {
    @get:Rule
    val temp = TemporaryFolder()

    private val svg = """<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10"><rect width="10" height="10"/></svg>"""
    private val box = ThemeRect(0f, 0f, 1f, 1f)

    private fun picture(id: String, vararg paths: String) = ThemeLayer.Picture(
        id, box, source = ThemePicture.File(paths.first()), fallbacks = paths.drop(1).map { ThemePicture.File(it) },
    )

    private fun spec(vararg layers: ThemeLayer) = testSpec().copy(
        views = ThemeViews(games = ViewSpec(ThemePrimary.Shelf(box), layers.toList())),
    )

    @Test
    fun `svg files are kept unless they could expand or reach out`() {
        assertTrue(isSafeSvg(svg.toByteArray()))
        assertFalse(isSafeSvg("""<!DOCTYPE svg [<!ENTITY a "aaaa">]><svg>&a;</svg>""".toByteArray()))
        assertFalse(isSafeSvg("""<svg><script>x()</script></svg>""".toByteArray()))
        assertFalse(isSafeSvg("""<svg><image href="https://evil/x.png"/></svg>""".toByteArray()))
        assertFalse(isSafeSvg("not an svg".toByteArray()))
        assertEquals(ThemeAssetKind.Vector, themeAssetKind("art/logo.SVG"))
    }

    @Test
    fun `a bad picture falls to its next good fallback`() {
        val source = temp.newFolder("esde-theme")
        File(source, "art").mkdirs()
        File(source, "art/logo.svg").writeText(svg)
        File(source, "art/frame.png").writeBytes(pngHeader(100, 100))
        val installer = JarnThemeInstaller(temp.newFolder("themes"))

        val result = installer.installFiles(
            spec(picture("logo", "art/missing.png", "art/logo.svg"), picture("gone", "art/nope.png")),
            source,
            listOf("art/logo.svg", "art/frame.png", "art/missing.png"),
        ) as ThemeImportResult.Installed

        val layers = result.spec.views.games!!.layers
        assertEquals(listOf("logo"), layers.map { it.id })
        assertEquals(ThemePicture.File("art/logo.svg"), (layers.single() as ThemeLayer.Picture).source)
        assertTrue(File(result.folder, "art/logo.svg").isFile)
        assertTrue(File(result.folder, "art/frame.png").isFile)
        assertEquals(2, result.dropped)
    }

    @Test
    fun `files outside the source folder are never copied`() {
        val outside = temp.newFile("secret.png").apply { writeBytes(pngHeader(1, 1)) }
        val source = temp.newFolder("esde-theme")
        Files.createSymbolicLink(File(source, "link.png").toPath(), outside.toPath())
        val installer = JarnThemeInstaller(temp.newFolder("themes"))

        val result = installer.installFiles(
            spec(picture("a", "link.png"), picture("b", "../secret.png")),
            source,
            listOf("link.png", "../secret.png", "/abs.png"),
        ) as ThemeImportResult.Installed

        assertEquals(listOf("theme.json"), result.folder.list()!!.toList())
        assertTrue(result.spec.views.games!!.layers.isEmpty())
    }

    @Test
    fun `a replace id re-installs over the same folder and extra files go in before the move`() {
        val source = temp.newFolder("esde-theme")
        val themes = temp.newFolder("themes")
        val installer = JarnThemeInstaller(themes)
        val first = installer.installFiles(spec(), source, emptyList()) as ThemeImportResult.Installed

        val again = installer.installFiles(spec(), source, emptyList(), replaceId = first.id) { dir ->
            File(dir, "note.json").writeText("{}")
        } as ThemeImportResult.Installed

        assertEquals(first.id, again.id)
        assertEquals(listOf(first.id), themes.list()!!.toList())
        assertTrue(File(again.folder, "note.json").isFile)
    }

    @Test
    fun `too many named files are refused`() {
        val installer = JarnThemeInstaller(temp.newFolder("themes"), ThemeArchiveLimits(maxEntries = 2))
        val result = installer.installFiles(spec(), temp.newFolder("src"), listOf("a.png", "b.png", "c.png"))
        assertEquals(ThemeImportResult.Rejected(ThemeImportProblem.TooManyEntries), result)
    }
}
