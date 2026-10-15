package jr.brian.frontend.library.scan

import android.app.Application
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.runBlocking
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

/**
 * Covers [FastWalkScanner]'s file-system walk. The scan rules it
 * applies (hidden skip, extension filter, directories-as-files,
 * SKIPPED_NAMES, depth bound) are shared with the SAF scanner via
 * [ScanRules], so these are also the behavioural contract that the
 * SAF scanner's own tests cross-check against.
 *
 * Mirrors the structure of :app's existing RomScannerTest so the two
 * scanners can be verified against the same scenarios during the
 * Phase 3 UI swap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FastWalkScannerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(
            id = "psx",
            displayName = "PlayStation",
            folderAliases = listOf("ps1"),
            extensions = setOf("iso", "bin", "cue", "chd", "pbp", "img", "m3u"),
        ),
        SystemDef(
            id = "nes",
            displayName = "Nintendo",
            folderAliases = emptyList(),
            extensions = setOf("nes", "fds", "zip"),
        ),
    )

    @Test
    fun `scan returns one entry per real file in a system directory`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "psx/Game.chd")
        writeRom(root, "psx/Other.iso")

        val scanner = FastWalkScanner(catalog)
        val results = scanner.scan(root.absolutePath)

        val psx = results.filter { it.systemId == "psx" }
        assertEquals(2, psx.size)
        assertTrue(psx.any { it.relativePath == "Game.chd" })
        assertTrue(psx.any { it.relativePath == "Other.iso" })
        assertTrue(psx.all { !it.isDirectoryAsFile })
    }

    @Test
    fun `scan preserves nested subdirectories in the relative path`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "psx/Genre/RPG/Xenogears.iso")

        val psx = FastWalkScanner(catalog).scan(root.absolutePath)
        assertEquals(1, psx.size)
        assertEquals("Genre/RPG/Xenogears.iso", psx.single().relativePath)
    }

    @Test
    fun `scan treats a directory whose name ends in a ROM extension as one entry`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        // Xenogears.m3u/ with inner chunks — ES-DE's multi-disc convention.
        writeRom(root, "psx/Xenogears.m3u/Disc1.bin")
        writeRom(root, "psx/Xenogears.m3u/Disc2.bin")

        val psx = FastWalkScanner(catalog).scan(root.absolutePath)
        assertEquals(1, psx.size)
        val entry = psx.single()
        assertEquals("Xenogears.m3u", entry.relativePath)
        assertTrue(entry.isDirectoryAsFile)
        // Must not have descended into the m3u dir.
        assertFalse(psx.any { it.relativePath.startsWith("Xenogears.m3u/") })
    }

    @Test
    fun `scan skips hidden files, media directories, and known sidecar extensions`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "psx/Game.chd")
        writeRom(root, "psx/.DS_Store")
        writeRom(root, "psx/Game.chd.sav")         // save state
        writeRom(root, "psx/Notes.txt")             // text sidecar
        writeRom(root, "psx/media/covers/Game.png") // scraped media

        val psx = FastWalkScanner(catalog).scan(root.absolutePath)
        assertEquals(1, psx.size)
        assertEquals("Game.chd", psx.single().relativePath)
    }

    @Test
    fun `scan matches system directories via folder aliases`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        // "ps1" is an alias of "psx" in the stub catalog.
        writeRom(root, "ps1/Metal Gear.chd")

        val results = FastWalkScanner(catalog).scan(root.absolutePath)
        assertEquals(1, results.size)
        assertEquals("psx", results.single().systemId)
    }

    @Test
    fun `scan ignores folders that do not resolve to a known system`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "unknownsystem/Game.iso")

        val results = FastWalkScanner(catalog).scan(root.absolutePath)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `scan honours the systems filter and skips directories outside it`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "psx/Metal Gear.chd")
        writeRom(root, "nes/Mario.nes")

        val results = FastWalkScanner(catalog).scan(
            sourceRoot = root.absolutePath,
            systems = setOf("psx"),
        )
        assertEquals(setOf("psx"), results.map { it.systemId }.toSet())
    }

    @Test
    fun `scan populates size and mtime from the real file`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        val rom = writeRom(root, "psx/Metal Gear.chd")
        rom.writeText("stub-content-with-known-size")
        rom.setLastModified(1_700_000_000_000L)

        val results = FastWalkScanner(catalog).scan(root.absolutePath)
        val entry = results.single()
        assertEquals(rom.length(), entry.fileSizeBytes)
        assertEquals(1_700_000_000_000L, entry.lastModifiedEpochMillis)
    }

    @Test
    fun `scan returns empty for a missing source root`() = runBlocking {
        val missing = File(tempFolder.root, "does-not-exist")
        val results = FastWalkScanner(catalog).scan(missing.absolutePath)
        assertTrue(results.isEmpty())
    }

    private fun writeRom(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}

