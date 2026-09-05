package jr.brian.home.esde.util

import kotlinx.coroutines.runBlocking
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
import java.io.File

/**
 * Robolectric-backed so the extension-parsing test can use android.util.Xml.
 * The pure-filesystem tests would run without Robolectric, but colocating them
 * keeps the class small and the setup consistent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RomScannerTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `scan returns one entry per real file in a system directory`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "ps2/Game.chd")
        writeRom(root, "ps2/Other.iso")

        val results = RomScanner.scan(listOf(root.absolutePath))

        val ps2 = results["ps2"]!!
        assertEquals(2, ps2.size)
        assertTrue(ps2.any { it.relativePath == "Game.chd" })
        assertTrue(ps2.any { it.relativePath == "Other.iso" })
        assertTrue(ps2.all { !it.isDirectoryAsFile })
    }

    @Test
    fun `scan preserves nested subdirectories in the relative path`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "ps2/Genre/RPG/Xenogears.iso")

        val ps2 = RomScanner.scan(listOf(root.absolutePath))["ps2"]!!
        assertEquals(1, ps2.size)
        assertEquals("Genre/RPG/Xenogears.iso", ps2.single().relativePath)
    }

    @Test
    fun `scan treats a directory whose name ends in a ROM extension as one entry`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        // Xenogears.m3u/ with inner chunks — a multi-disc entry in ES-DE.
        writeRom(root, "psx/Xenogears.m3u/Disc1.bin")
        writeRom(root, "psx/Xenogears.m3u/Disc2.bin")
        // A stray .bin file at the same level to prove we didn't recurse into m3u/.
        // (bin is a psx extension; this file MUST NOT appear because the directory
        // itself is the entry.)

        val psx = RomScanner.scan(listOf(root.absolutePath))["psx"]!!
        assertEquals(1, psx.size)
        val entry = psx.single()
        assertEquals("Xenogears.m3u", entry.relativePath)
        assertTrue(entry.isDirectoryAsFile)
        // We must not have recursed and picked up inner files.
        assertFalse(psx.any { it.relativePath.startsWith("Xenogears.m3u/") })
    }

    @Test
    fun `scan skips hidden files, media directories, and known sidecar extensions`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        writeRom(root, "psx/Game.chd")
        writeRom(root, "psx/.DS_Store")
        writeRom(root, "psx/Game.chd.sav")   // save file
        writeRom(root, "psx/Notes.txt")       // text sidecar
        writeRom(root, "psx/media/covers/Game.png") // scraped media inside system dir

        val psx = RomScanner.scan(listOf(root.absolutePath))["psx"]!!
        assertEquals(1, psx.size)
        assertEquals("Game.chd", psx.single().relativePath)
    }

    @Test
    fun `scan honors ordering across multiple roots and dedups first-wins`() = runBlocking {
        val primary = tempFolder.newFolder("Primary")
        val secondary = tempFolder.newFolder("Secondary")
        writeRom(primary, "psx/Same.chd")
        writeRom(secondary, "psx/Same.chd")
        writeRom(secondary, "psx/OnlyInSecondary.chd")

        val psx = RomScanner.scan(
            listOf(primary.absolutePath, secondary.absolutePath)
        )["psx"]!!
        val paths = psx.map { it.absolutePath }.toSet()
        assertTrue(paths.contains(File(primary, "psx/Same.chd").absolutePath))
        assertFalse(paths.contains(File(secondary, "psx/Same.chd").absolutePath))
        assertTrue(paths.contains(File(secondary, "psx/OnlyInSecondary.chd").absolutePath))
    }

    @Test
    fun `scan omits systems that yield no ROMs`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        File(root, "empty").mkdirs()
        File(root, "wrongext").mkdirs()
        File(root, "wrongext/notes.txt").writeText("x")

        val results = RomScanner.scan(listOf(root.absolutePath))
        assertNull(results["empty"])
        assertNull(results["wrongext"])
    }

    @Test
    fun `scan uses es_systems xml extension element when provided`() = runBlocking {
        val root = tempFolder.newFolder("Roms")
        val fakeExtDir = File(root, "fakesys").also { it.mkdirs() }
        // .foo isn't in any emulator registry — it can only match via es_systems.xml.
        File(fakeExtDir, "Game.foo").writeText("stub")

        val esFile = tempFolder.newFile("es_systems.xml").apply {
            writeText(
                """
                <systemList>
                  <system>
                    <name>fakesys</name>
                    <extension>.foo</extension>
                  </system>
                </systemList>
                """.trimIndent()
            )
        }

        val fakesys = RomScanner.scan(listOf(root.absolutePath), esSystemsFile = esFile)["fakesys"]
        assertEquals(1, fakesys!!.size)
        assertEquals("Game.foo", fakesys.single().relativePath)
    }

    private fun writeRom(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}
