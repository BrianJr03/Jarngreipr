package jr.brian.home.esde.util

import jr.brian.home.esde.data.SystemStamp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Behavioural tests for the filesystem-first pipeline: RomScanner emits entries,
 * GamelistMetadataSource decorates them, and RomIndexBuilder emits GameInfo.
 * The invariants tested here are the ones the redesign is supposed to enforce —
 * gamelist entries with no backing file MUST be dropped, files with no gamelist
 * entry MUST appear titled from their stem, and fuzzy matching MUST NOT creep
 * in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RomIndexBuilderTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `gamelist entry with no scanned file is dropped`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeRom(roms, "psx/Real.iso")

        val esdeRoot = tempFolder.newFolder("ES-DE")
        writeGamelist(esdeRoot, "psx", listOf(
            gameXml(path = "./Real.iso", name = "Real Game"),
            gameXml(path = "./MissingFromDisk.iso", name = "Ghost Game"),
        ))

        val source = GamelistMetadataSource(esdeRoot.absolutePath, mediaPaths = emptyList())
        val games = RomIndexBuilder.buildIndex(
            romsPaths = listOf(roms.absolutePath),
            esSystemsFile = null,
            metadataSource = source,
        )

        val paths = games.map { it.path }
        assertEquals(1, games.size)
        assertEquals("./Real.iso", paths.single())
        assertEquals("Real Game", games.single().name)
    }

    @Test
    fun `file on disk with no gamelist entry appears with filename stem title`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeRom(roms, "psx/Undocumented.iso")

        val esdeRoot = tempFolder.newFolder("ES-DE")
        writeGamelist(esdeRoot, "psx", listOf(
            gameXml(path = "./Something.iso", name = "Nothing to match"),
        ))

        val source = GamelistMetadataSource(esdeRoot.absolutePath, mediaPaths = emptyList())
        val games = RomIndexBuilder.buildIndex(
            romsPaths = listOf(roms.absolutePath),
            esSystemsFile = null,
            metadataSource = source,
        )
        assertEquals(1, games.size)
        // Title comes from the filename stem.
        assertEquals("Undocumented", games.single().name)
        // Absolute path is set to the scanned file.
        assertTrue(games.single().romAbsolutePath!!.endsWith("psx/Undocumented.iso"))
    }

    @Test
    fun `match falls back basename then normalized basename in that order`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        // On disk: relative path is a filename only, but gamelist writes ./Sub/Foo.iso
        writeRom(roms, "psx/Foo.iso")
        // Another game whose disk name uses an en-dash where the gamelist uses hyphen.
        writeRom(roms, "psx/Sonic – Adventure.iso")

        val esdeRoot = tempFolder.newFolder("ES-DE")
        writeGamelist(esdeRoot, "psx", listOf(
            gameXml(path = "./Sub/Foo.iso", name = "Basename-match"),
            gameXml(path = "./Sonic - Adventure.iso", name = "Normalized-match"),
        ))

        val source = GamelistMetadataSource(esdeRoot.absolutePath, mediaPaths = emptyList())
        val games = RomIndexBuilder.buildIndex(
            romsPaths = listOf(roms.absolutePath),
            esSystemsFile = null,
            metadataSource = source,
        )
        val nameByBase = games.associateBy { File(it.path).name }
        assertEquals("Basename-match", nameByBase["Foo.iso"]?.name)
        assertEquals("Normalized-match", nameByBase["Sonic – Adventure.iso"]?.name)
    }

    @Test
    fun `region-tagged gamelist entry does NOT match a plain-name file on disk`() = runBlocking {
        // Guards against fuzzy matching creeping in. Foo (USA).iso and Foo.iso
        // are different discs — silently booting the wrong one is worse than
        // showing an undecorated entry.
        val roms = tempFolder.newFolder("Roms")
        writeRom(roms, "psx/Foo.iso")

        val esdeRoot = tempFolder.newFolder("ES-DE")
        writeGamelist(esdeRoot, "psx", listOf(
            gameXml(path = "./Foo (USA).iso", name = "Should NOT bind"),
        ))

        val source = GamelistMetadataSource(esdeRoot.absolutePath, mediaPaths = emptyList())
        val games = RomIndexBuilder.buildIndex(
            romsPaths = listOf(roms.absolutePath),
            esSystemsFile = null,
            metadataSource = source,
        )
        assertEquals(1, games.size)
        // Filename-derived title, NOT the gamelist name — no match happened.
        assertEquals("Foo", games.single().name)
        assertNull(games.single().description)
    }

    @Test
    fun `NoOpMetadataSource produces filename-titled entries and reads no gamelist`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeRom(roms, "psx/Game.iso")
        writeRom(roms, "psx/Other.chd")
        // Deliberately place a broken gamelist here — if the pipeline reads it
        // in No-Op mode, this test would explode. Silence proves no read.
        val esdeRoot = tempFolder.newFolder("ES-DE")
        File(esdeRoot, "gamelists/psx").mkdirs()
        File(esdeRoot, "gamelists/psx/gamelist.xml").writeText("<not valid xml at all")

        val games = RomIndexBuilder.buildIndex(
            romsPaths = listOf(roms.absolutePath),
            esSystemsFile = null,
            metadataSource = NoOpMetadataSource,
        )
        assertEquals(2, games.size)
        val titles = games.map { it.name }.toSet()
        assertEquals(setOf("Game", "Other"), titles)
        // No metadata at all — descriptions are null, ratings are 0f.
        assertTrue(games.all { it.description == null && it.rating == 0f })
    }

    @Test
    fun `stamp reflects entry count so a new file invalidates a cached stamp`() {
        val roms = tempFolder.newFolder("Roms")
        val systemDir = File(roms, "psx").apply { mkdirs() }
        File(systemDir, "One.iso").writeText("x")

        val before = RomIndexBuilder.stamp(
            systemName = "psx",
            romsPaths = listOf(roms.absolutePath),
            esdeRootPath = null,
            decorationEnabled = false,
        )
        assertNotNull(before)
        File(systemDir, "Two.iso").writeText("y")
        val after = RomIndexBuilder.stamp(
            systemName = "psx",
            romsPaths = listOf(roms.absolutePath),
            esdeRootPath = null,
            decorationEnabled = false,
        )
        assertNotNull(after)
        assertFalse(
            "Adding a file must change the stamp so the cache misses",
            before!!.matches(after!!),
        )
    }

    @Test
    fun `stamp with decoration off uses gamelistLastModified = -1`() {
        val roms = tempFolder.newFolder("Roms")
        File(roms, "psx").mkdirs()

        val stamp: SystemStamp = RomIndexBuilder.stamp(
            systemName = "psx",
            romsPaths = listOf(roms.absolutePath),
            esdeRootPath = tempFolder.newFolder("ES-DE").absolutePath,
            decorationEnabled = false,
        )!!
        assertEquals(-1L, stamp.gamelistLastModified)
        assertFalse(stamp.decorationEnabled)
    }

    private fun writeRom(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }

    private fun gameXml(path: String, name: String, desc: String? = null): String {
        val descLine = desc?.let { "<desc>$it</desc>" } ?: ""
        return """
            <game>
              <path>$path</path>
              <name>$name</name>
              $descLine
            </game>
        """.trimIndent()
    }

    private fun writeGamelist(esdeRoot: File, systemName: String, entries: List<String>) {
        val dir = File(esdeRoot, "gamelists/$systemName").apply { mkdirs() }
        val xml = buildString {
            append("<?xml version=\"1.0\"?>\n<gameList>\n")
            entries.forEach { append(it).append('\n') }
            append("</gameList>\n")
        }
        File(dir, "gamelist.xml").writeText(xml)
    }
}
