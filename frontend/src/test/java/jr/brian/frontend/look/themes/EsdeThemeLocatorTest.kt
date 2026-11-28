package jr.brian.frontend.look.themes

import jr.brian.frontend.library.setup.EsdeSettings
import jr.brian.frontend.library.setup.parseEsdeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EsdeThemeLocatorTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun locator(vararg esde: File, access: Boolean = true) = EsdeThemeLocator(
        esdeFolders = { esde.toList() },
        hasAllFilesAccess = { access },
        readText = { runCatching { it.readText() }.getOrNull() },
    )

    private fun esdeDir(name: String = "ES-DE", theme: String? = "linear-es-de"): File =
        temp.newFolder(name).also { dir ->
            if (theme != null) {
                File(dir, "settings").mkdirs()
                File(dir, "settings/es_settings.xml").writeText(
                    """
                    <string name="ROMDirectory" value="~/ROMs" />
                    <string name="Theme" value="$theme" />
                    <string name="ThemeVariant" value="textlist" />
                    <string name="ThemeColorScheme" value="dark" />
                    <string name="ThemeAspectRatio" value="16:9" />
                    """.trimIndent(),
                )
            }
        }

    private fun theme(esde: File, folder: String, capabilities: String? = "<themeCapabilities/>"): File =
        File(esde, "themes/$folder").also { dir ->
            dir.mkdirs()
            capabilities?.let { File(dir, "capabilities.xml").writeText(it) }
        }

    @Test
    fun `reads every theme setting`() {
        val xml = """
            <string name="Theme" value="art-book-next" />
            <string name="ThemeVariant" value="Grid &amp; list" />
            <string name="ThemeColorScheme" value="light" />
            <string name="ThemeAspectRatio" value="4:3" />
            <string name="ROMDirectory" value="/storage/emulated/0/ROMs" />
        """.trimIndent()
        assertEquals(
            EsdeSettings("/storage/emulated/0/ROMs", "art-book-next", "Grid & list", "light", "4:3"),
            parseEsdeSettings(xml),
        )
    }

    @Test
    fun `older ThemeSet key is read when Theme is missing`() {
        assertEquals("slate", parseEsdeSettings("""<string name="ThemeSet" value="slate" />""").themeSet)
        assertNull(parseEsdeSettings("""<string name="Theme" value=" " />""").themeSet)
    }

    @Test
    fun `no all-files access says so without reading`() {
        val esde = esdeDir()
        theme(esde, "linear-es-de")
        assertEquals(EsdeThemeScan.NoAccess, locator(esde, access = false).scan())
    }

    @Test
    fun `not found points at the themes folder to copy into`() {
        val esde = esdeDir()
        theme(esde, "no-capabilities", capabilities = null)
        assertEquals(EsdeThemeScan.NotFound(File(esde, "themes")), locator(esde).scan())
        assertEquals(EsdeThemeScan.NotFound(null), locator().scan())
    }

    @Test
    fun `lists themes with the current one first and names from capabilities`() {
        val esde = esdeDir()
        theme(esde, "zeta")
        theme(esde, "alpha", "<themeCapabilities><themeName> Alpha &amp; Co </themeName></themeCapabilities>")
        theme(esde, "linear-es-de", "<themeCapabilities><themeName>Linear</themeName></themeCapabilities>")
        theme(esde, ".hidden")
        File(esde, "themes/loose.xml").writeText("<x/>")

        val found = locator(esde).scan() as EsdeThemeScan.Found

        assertEquals(listOf("Linear", "Alpha & Co", "zeta"), found.themes.map { it.name })
        assertEquals(listOf(true, false, false), found.themes.map { it.isCurrent })
        assertEquals("textlist", found.settings?.themeVariant)
    }

    @Test
    fun `themes across ES-DE folders are merged, the first folder winning a shared name`() {
        val first = esdeDir("first", theme = null)
        val second = esdeDir("second", theme = null)
        theme(first, "shared", "<themeCapabilities><themeName>First</themeName></themeCapabilities>")
        theme(second, "shared", "<themeCapabilities><themeName>Second</themeName></themeCapabilities>")
        theme(second, "other")

        val found = locator(first, second).scan() as EsdeThemeScan.Found

        assertEquals(listOf("First", "other"), found.themes.map { it.name })
        assertTrue(found.themes.none { it.isCurrent })
        assertNull(found.settings)
    }

    @Test
    fun `scanning never writes`() {
        val esde = esdeDir()
        theme(esde, "linear-es-de")
        val before = esde.walk().map { it.path to it.lastModified() }.toList()

        locator(esde).scan()

        assertEquals(before, esde.walk().map { it.path to it.lastModified() }.toList())
    }
}
