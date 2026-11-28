package jr.brian.frontend.look.themes

import jr.brian.core.model.theme.JarnThemeSpec
import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeMeta
import jr.brian.core.model.theme.ThemePicture
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeSource
import jr.brian.core.model.theme.ThemeSystemArt
import jr.brian.core.model.theme.ThemeTextStyle
import jr.brian.core.model.theme.ThemeTypeScale
import jr.brian.core.model.theme.ThemeViewPatch
import jr.brian.core.model.theme.ThemeViews
import jr.brian.core.model.theme.ViewSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class JarnThemeExporterTest {
    @get:Rule
    val temp = TemporaryFolder()

    private lateinit var themeDir: File
    private lateinit var installed: File
    private val exporter = JarnThemeExporter()

    @Before
    fun setup() {
        themeDir = temp.newFolder("source")
        installed = temp.newFolder("installed")
    }

    private fun file(path: String, bytes: ByteArray) = File(themeDir, path).apply {
        parentFile?.mkdirs()
        writeBytes(bytes)
    }

    private fun rect() = ThemeRect(0f, 0f, 1f, 1f)

    private fun specNaming(vararg pictures: String, font: String? = null, logo: String? = null, narrow: String? = null) =
        testSpec().copy(
            tokens = testSpec().tokens.copy(type = ThemeTypeScale(title = font?.let { ThemeTextStyle(size = 30f, font = it) })),
            views = ThemeViews(
                games = ViewSpec(
                    primary = ThemePrimary.Grid(box = rect()),
                    layers = pictures.mapIndexed { index, path ->
                        ThemeLayer.Picture(id = "p$index", rect = rect(), source = ThemePicture.File(path))
                    },
                    narrow = narrow?.let {
                        ThemeViewPatch(layers = listOf(ThemeLayer.Picture(id = "n", rect = rect(), source = ThemePicture.File(it))))
                    },
                ),
            ),
            systemArt = logo?.let { mapOf("nes" to ThemeSystemArt(logo = it)) }.orEmpty(),
        )

    private fun export(spec: JarnThemeSpec, preview: File? = null): ThemeExportResult.Written {
        val result = exporter.write(spec, themeDir, File(temp.root, "out/${themeFileName(spec)}"), preview)
        assertTrue("Expected Written, got $result", result is ThemeExportResult.Written)
        return result as ThemeExportResult.Written
    }

    private fun entries(file: File) = ZipFile(file).use { zip -> zip.entries().toList().map { it.name } }

    @Test
    fun `an exported theme installs again with the same spec and files`() {
        file("art/bg.png", pngHeader(64, 64))
        file("fonts/title.ttf", ttf())
        file("logos/nes.png", pngHeader(8, 8))
        val spec = specNaming("art/bg.png", font = "fonts/title.ttf", logo = "logos/nes.png")

        val written = export(spec)
        val result = JarnThemeInstaller(installed).install(written.file)

        assertTrue("Expected Installed, got $result", result is ThemeImportResult.Installed)
        result as ThemeImportResult.Installed
        assertEquals(spec, result.spec)
        assertEquals(0, result.dropped)
        assertEquals(setOf("theme.json", "art", "fonts", "logos"), result.folder.list()!!.toSet())
        assertEquals("art-book.jarntheme", written.file.name)
    }

    @Test
    fun `only named files are written, never unreferenced or outside ones`() {
        file("art/bg.png", pngHeader(64, 64))
        file("art/unused.png", pngHeader(64, 64))
        file("notes.txt", "secret".toByteArray())
        File(temp.root, "outside.png").writeBytes(pngHeader(1, 1))

        val written = export(specNaming("art/bg.png", "../outside.png", "art/missing.png", "notes.txt"))

        assertEquals(listOf("theme.json", "art/bg.png"), entries(written.file))
        assertEquals(1, written.assets)
        assertEquals(3, written.skipped)
    }

    @Test
    fun `narrow patch pictures travel too`() {
        file("art/narrow.png", pngHeader(32, 32))

        assertEquals(listOf("theme.json", "art/narrow.png"), entries(export(specNaming(narrow = "art/narrow.png")).file))
    }

    @Test
    fun `a preview rides along at the root when given`() {
        val preview = temp.newFile("shot.png").apply { writeBytes(pngHeader(320, 180)) }

        assertEquals(listOf("theme.json", THEME_PREVIEW_FILE), entries(export(testSpec(), preview).file))
    }

    @Test
    fun `a theme with no folder exports just its theme json`() {
        val result = exporter.write(testSpec(), null, File(temp.root, "out/x.jarntheme"))

        assertEquals(listOf("theme.json"), entries((result as ThemeExportResult.Written).file))
    }

    @Test
    fun `files past the caps fail without leaving a partial file`() {
        file("art/bg.png", pngHeader(64, 64) + ByteArray(2000))
        val out = File(temp.root, "out/big.jarntheme")

        val result = JarnThemeExporter(ThemeArchiveLimits(maxTotalBytes = 1000)).write(specNaming("art/bg.png"), themeDir, out)

        assertEquals(ThemeExportResult.Failed(ThemeImportProblem.TooLarge), result)
        assertFalse(out.exists())
        assertTrue(File(temp.root, "out").listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `licenses decide whether an import can be shared`() {
        fun verdict(license: String?, source: ThemeSource = ThemeSource.EsDe) =
            themeShareVerdict(ThemeMeta(name = "T", license = license, source = source))

        assertEquals(ThemeShareVerdict.Allowed, verdict(null, ThemeSource.Jarn))
        listOf("CC BY 4.0", "CC-BY-SA-4.0", "CC0", "MIT", "Apache-2.0", "GPL-3.0", "Public Domain", "Creative Commons Attribution 4.0")
            .forEach { assertEquals(it, ThemeShareVerdict.AllowedWithCredit, verdict(it)) }
        listOf(null, "", "CC BY-NC-SA 4.0", "CC-BY-ND", "All rights reserved", "Proprietary", "Custom, ask me", "Creative Commons NonCommercial")
            .forEach { assertEquals("$it", ThemeShareVerdict.LicenseForbids, verdict(it)) }
    }
}
