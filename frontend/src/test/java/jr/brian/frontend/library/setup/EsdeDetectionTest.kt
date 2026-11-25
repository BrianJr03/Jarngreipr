package jr.brian.frontend.library.setup

import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EsdeDetectionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(id = "nes", displayName = "Nintendo", extensions = setOf("nes")),
        SystemDef(id = "gba", displayName = "Game Boy Advance", extensions = setOf("gba")),
    )

    @Test
    fun `counts games and favorites in known systems only`() = runBlocking {
        val volume = tempFolder.newFolder("storage")
        gamelist(volume, "nes", "<game><path>./a.nes</path><favorite>true</favorite></game><game><path>./b.nes</path></game>")
        gamelist(volume, "gba", "<game>\n<path>./c.gba</path>\n<favorite> TRUE </favorite>\n</game>")
        gamelist(volume, "mystery", "<game><path>./x.bin</path><favorite>true</favorite></game>")

        val found = detector(volume, access = true).detect()

        assertEquals(EsdeDetection(found = true, hasFileAccess = true, systems = 2, games = 3, favorites = 2), found)
        assertTrue(found.worthOffering)
    }

    @Test
    fun `without file access nothing is counted but ES-DE is still found`() = runBlocking {
        val volume = tempFolder.newFolder("storage")
        gamelist(volume, "nes", "<game><path>./a.nes</path></game>")

        val found = detector(volume, access = false).detect()

        assertEquals(EsdeDetection(found = true, hasFileAccess = false), found)
        assertTrue(found.worthOffering)
    }

    @Test
    fun `no ES-DE folder means nothing to offer`() = runBlocking {
        val volume = tempFolder.newFolder("storage")

        val found = detector(volume, access = true).detect()

        assertFalse(found.found)
        assertFalse(found.worthOffering)
    }

    @Test
    fun `ES-DE with empty gamelists is not worth offering`() = runBlocking {
        val volume = tempFolder.newFolder("storage")
        gamelist(volume, "nes", "")

        assertFalse(detector(volume, access = true).detect().worthOffering)
    }

    private fun detector(volume: File, access: Boolean) = EsdeDetector(
        esde = EsdeRomsFolderFinder(
            mediaRoot = { null },
            volumes = { listOf(volume) },
            readText = { null },
            isDirectory = File::isDirectory,
        ),
        catalog = catalog,
        hasFileAccess = { access },
    )

    private fun gamelist(volume: File, system: String, games: String) {
        val dir = File(volume, "ES-DE/gamelists/$system").apply { mkdirs() }
        File(dir, "gamelist.xml").writeText("<?xml version=\"1.0\"?>\n<gameList>\n$games\n</gameList>\n")
    }
}
