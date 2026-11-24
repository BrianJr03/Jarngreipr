package jr.brian.frontend.library.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class EsdeRomsFolderTest {
    private fun settings(romDirectory: String) = """
        <?xml version="1.0"?>
        <bool name="ScreenSaverControls" value="true" />
        <string name="ROMDirectory" value="$romDirectory" />
        <string name="MediaDirectory" value="" />
    """.trimIndent()

    @Test
    fun readsTheRomDirectoryEntry() {
        assertEquals("/storage/emulated/0/ROMs", parseEsdeRomDirectory(settings("/storage/emulated/0/ROMs")))
    }

    @Test
    fun blankOrMissingEntryIsNull() {
        assertNull(parseEsdeRomDirectory(settings("")))
        assertNull(parseEsdeRomDirectory(settings("   ")))
        assertNull(parseEsdeRomDirectory("""<string name="MediaDirectory" value="/x" />"""))
        assertNull(parseEsdeRomDirectory("not xml at all"))
    }

    @Test
    fun unescapesXmlEntities() {
        assertEquals(
            "/storage/emulated/0/Games & ROMs",
            parseEsdeRomDirectory(settings("/storage/emulated/0/Games &amp; ROMs")),
        )
    }

    @Test
    fun expandsHomeAndEsPath() {
        val home = "/storage/emulated/0"
        val esde = "/storage/emulated/0/ES-DE"
        assertEquals("/storage/emulated/0/ROMs", resolveEsdeRomDirectory("~/ROMs", home, esde))
        assertEquals("/storage/emulated/0", resolveEsdeRomDirectory("~", home, esde))
        assertEquals("/storage/emulated/0/ES-DE/ROMs", resolveEsdeRomDirectory("%ESPATH%/ROMs", home, esde))
        assertEquals("/storage/1234-ABCD/ROMs", resolveEsdeRomDirectory("/storage/1234-ABCD/ROMs/", home, esde))
        assertEquals("/storage/emulated/0/ROMs", resolveEsdeRomDirectory("~\\ROMs", home, esde))
    }

    @Test
    fun rejectsPathsItCannotResolve() {
        val home = "/storage/emulated/0"
        assertNull(resolveEsdeRomDirectory("ROMs", home, "/e"))
        assertNull(resolveEsdeRomDirectory("%ROMPATH%/x", home, "/e"))
    }

    @Test
    fun documentIdsForEachVolume() {
        assertEquals("primary:ROMs", storageDocumentId("/storage/emulated/0/ROMs"))
        assertEquals("primary:", storageDocumentId("/storage/emulated/0"))
        assertEquals("1234-ABCD:Games/ROMs", storageDocumentId("/storage/1234-ABCD/Games/ROMs/"))
        assertNull(storageDocumentId("/storage/self/primary/ROMs"))
        assertNull(storageDocumentId("/sdcard/ROMs"))
    }

    private fun finder(
        files: Map<String, String>,
        dirs: Set<String>,
        mediaRoot: String? = null,
        volumes: List<String> = listOf("/storage/emulated/0"),
    ) = EsdeRomsFolderFinder(
        mediaRoot = { mediaRoot?.let(::File) },
        volumes = { volumes.map(::File) },
        readText = { files[it.path] },
        isDirectory = { it.path in dirs },
    )

    @Test
    fun usesTheSettingsFolder() {
        val found = finder(
            files = mapOf("/storage/emulated/0/ES-DE/settings/es_settings.xml" to settings("~/Games")),
            dirs = setOf("/storage/emulated/0/Games", "/storage/emulated/0/ROMs"),
        ).find()
        assertEquals(EsdeRomsFolder("/storage/emulated/0/Games", fromSettings = true), found)
    }

    @Test
    fun findsEsdeBesideACustomMediaFolderAndOnSdCards() {
        val besideMedia = finder(
            files = mapOf("/storage/1234-ABCD/Emu/ES-DE/settings/es_settings.xml" to settings("%ESPATH%/ROMs")),
            dirs = setOf("/storage/1234-ABCD/Emu/ES-DE/ROMs"),
            mediaRoot = "/storage/1234-ABCD/Emu/ES-DE/downloaded_media",
        ).find()
        assertEquals("/storage/1234-ABCD/Emu/ES-DE/ROMs", besideMedia?.path)

        val onCard = finder(
            files = mapOf("/storage/1234-ABCD/ES-DE/es_settings.xml" to settings("/storage/1234-ABCD/ROMs")),
            dirs = setOf("/storage/1234-ABCD/ROMs"),
            volumes = listOf("/storage/emulated/0", "/storage/1234-ABCD"),
        ).find()
        assertEquals("/storage/1234-ABCD/ROMs", onCard?.path)
    }

    @Test
    fun fallsBackToInternalRomsWhenSettingsCantBeUsed() {
        val unreadable = finder(files = emptyMap(), dirs = setOf("/storage/emulated/0/ROMs")).find()
        assertEquals(EsdeRomsFolder("/storage/emulated/0/ROMs", fromSettings = false), unreadable)

        val gone = finder(
            files = mapOf("/storage/emulated/0/ES-DE/settings/es_settings.xml" to settings("/storage/9999-0000/ROMs")),
            dirs = setOf("/storage/emulated/0/ROMs"),
        ).find()
        assertEquals(false, gone?.fromSettings)
    }

    @Test
    fun hasEsdeOnlyWhenItsFolderIsThere() {
        assertEquals(true, finder(files = emptyMap(), dirs = setOf("/storage/emulated/0/ES-DE")).hasEsde())
        assertEquals(false, finder(files = emptyMap(), dirs = setOf("/storage/emulated/0/ROMs")).hasEsde())
    }

    @Test
    fun nothingFoundMeansThePlainPicker() {
        assertNull(finder(files = emptyMap(), dirs = emptySet()).find())
    }
}
