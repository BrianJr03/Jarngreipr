package jr.brian.home.esde.util

import jr.brian.home.esde.model.GameInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Tests focus on ES-DE's "Directories interpreted as files" convention (e.g. `Xenogears.m3u/`).
 * ES-DE's `Utils::FileSystem::getStem` skips the extension strip when the path is a directory,
 * so the scraped-media basename keeps the extension (`Xenogears.m3u.png`, not `Xenogears.png`).
 * Our helpers must probe both layouts so directory-as-file entries display media correctly.
 */
class RomPathUtilsTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    // region mediaBasenameCandidates

    @Test
    fun `file with extension yields both stem and full name`() {
        // We can't tell upfront whether `Xenogears.iso` names a file or a directory
        // (ES-DE's "directories interpreted as files" supports any extension), so we
        // emit both basenames. The fullName probe is a harmless miss for regular files
        // and the only hit for directory-as-file entries.
        val candidates = mediaBasenameCandidates("Xenogears.iso")
        assertEquals(
            listOf("Xenogears" to null, "Xenogears.iso" to null),
            candidates
        )
    }

    @Test
    fun `file in subfolder keeps parent for both basenames`() {
        val candidates = mediaBasenameCandidates("RPG/Xenogears.iso")
        assertEquals(
            listOf("Xenogears" to "RPG", "Xenogears.iso" to "RPG"),
            candidates
        )
    }

    @Test
    fun `directory-as-file path yields stem plus full directory name`() {
        // ES-DE records `./Xenogears.m3u` for the m3u directory itself; media file is
        // `Xenogears.m3u.png`. The full-name candidate is what makes that lookup hit.
        val candidates = mediaBasenameCandidates("Xenogears.m3u")
        assertEquals(
            listOf("Xenogears" to null, "Xenogears.m3u" to null),
            candidates
        )
    }

    @Test
    fun `inner-file variant yields stem, fullname, and parent-as-basename`() {
        // Some setups store the inner-file path (`./Xenogears.m3u/Xenogears.m3u`).
        // ES-DE media for that case lives under the grandparent using the directory name,
        // so the third candidate strips the duplicated dir from the parent path.
        val candidates = mediaBasenameCandidates("Xenogears.m3u/Xenogears.m3u")
        assertEquals(
            listOf(
                "Xenogears" to "Xenogears.m3u",
                "Xenogears.m3u" to "Xenogears.m3u",
                "Xenogears.m3u" to null
            ),
            candidates
        )
    }

    @Test
    fun `inner-file variant under a subfolder strips only the duplicated segment`() {
        val candidates = mediaBasenameCandidates("RPG/Xenogears.m3u/Xenogears.m3u")
        assertEquals(
            listOf(
                "Xenogears" to "RPG/Xenogears.m3u",
                "Xenogears.m3u" to "RPG/Xenogears.m3u",
                "Xenogears.m3u" to "RPG"
            ),
            candidates
        )
    }

    @Test
    fun `extensionless filename yields only stem`() {
        val candidates = mediaBasenameCandidates("Tetris")
        assertEquals(listOf("Tetris" to null), candidates)
    }

    @Test
    fun `file with multiple dots strips only the last extension`() {
        val candidates = mediaBasenameCandidates("Final.Fantasy.VII.m3u")
        assertEquals(
            listOf(
                "Final.Fantasy.VII" to null,
                "Final.Fantasy.VII.m3u" to null
            ),
            candidates
        )
    }

    // endregion

    // region findFirstMedia — the Xenogears bug regression coverage

    @Test
    fun `regular file resolves to stem-named media`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "Xenogears.iso",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "psx/screenshots/Xenogears.png"), hit)
    }

    @Test
    fun `m3u directory resolves to media that keeps the extension`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.m3u.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "Xenogears.m3u",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "psx/screenshots/Xenogears.m3u.png"), hit)
    }

    @Test
    fun `inner-file m3u variant still finds the directory-named media`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.m3u.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "Xenogears.m3u/Xenogears.m3u",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "psx/screenshots/Xenogears.m3u.png"), hit)
    }

    @Test
    fun `m3u directory under a real subfolder preserves the subfolder`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/RPG/Xenogears.m3u.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "RPG/Xenogears.m3u",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "psx/screenshots/RPG/Xenogears.m3u.png"), hit)
    }

    @Test
    fun `regular file beats directory-as-file when both names exist`() {
        // Same lookup should prefer the stem match for normal files. The Xenogears.iso.png
        // file would only exist by accident; make sure we don't pick it over Xenogears.png.
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.png")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.iso.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "Xenogears.iso",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "psx/screenshots/Xenogears.png"), hit)
    }

    @Test
    fun `folders are tried in declared order`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.png")
        writeMedia(mediaRoot, "psx/covers/Xenogears.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("covers", "screenshots"),
            gameFilename = "Xenogears.iso",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "psx/covers/Xenogears.png"), hit)
    }

    @Test
    fun `system aliases are tried in order`() {
        // snes-msu1 aliases to snes for media lookup — caller passes both, exact name first.
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "snes/screenshots/Contra.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("snes-msu1", "snes"),
            folders = listOf("screenshots"),
            gameFilename = "Contra.sfc",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(mediaRoot, "snes/screenshots/Contra.png"), hit)
    }

    @Test
    fun `extensions are tried in declared order`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.jpg")
        writeMedia(mediaRoot, "psx/screenshots/Xenogears.webp")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "Xenogears.iso",
            extensions = listOf("png", "jpg", "webp")
        )

        assertEquals(mediaFile(mediaRoot, "psx/screenshots/Xenogears.jpg"), hit)
    }

    @Test
    fun `suffix variants probe sidecar names like scummvm`() {
        val mediaRoot = tempFolder.newFolder("media")
        writeMedia(mediaRoot, "scummvm/marquees/dig.scummvm.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("scummvm"),
            folders = listOf("marquees"),
            gameFilename = "dig.scummvm",
            extensions = listOf("png"),
            suffixVariants = listOf("", ".scummvm")
        )

        assertEquals(mediaFile(mediaRoot, "scummvm/marquees/dig.scummvm.png"), hit)
    }

    @Test
    fun `secondary media root is consulted when primary lacks the file`() {
        // Phase 2.6: resolution walks an ordered list of media roots. A game whose art
        // exists only under RetroHrai!'s root must resolve when ES-DE has none.
        val primary = tempFolder.newFolder("esde-media")
        val secondary = tempFolder.newFolder("retrohrai-media")
        writeMedia(secondary, "psx/covers/Xenogears.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(primary.absolutePath, secondary.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("covers"),
            gameFilename = "Xenogears.iso",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(secondary, "psx/covers/Xenogears.png"), hit)
    }

    @Test
    fun `first media root wins when both roots have the file`() {
        // Same Phase 2.6 contract — ES-DE root[0] takes priority over RetroHrai! root[1]
        // so the user's own scrape is preferred when both sources have art.
        val primary = tempFolder.newFolder("esde-media")
        val secondary = tempFolder.newFolder("retrohrai-media")
        writeMedia(primary, "psx/covers/Xenogears.png")
        writeMedia(secondary, "psx/covers/Xenogears.png")

        val hit = findFirstMedia(
            mediaPaths = listOf(primary.absolutePath, secondary.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("covers"),
            gameFilename = "Xenogears.iso",
            extensions = listOf("png")
        )

        assertEquals(mediaFile(primary, "psx/covers/Xenogears.png"), hit)
    }

    @Test
    fun `returns null when nothing matches`() {
        val mediaRoot = tempFolder.newFolder("media")

        val hit = findFirstMedia(
            mediaPaths = listOf(mediaRoot.absolutePath),
            systemNames = listOf("psx"),
            folders = listOf("screenshots"),
            gameFilename = "Xenogears.m3u",
            extensions = listOf("png", "jpg")
        )

        assertNull(hit)
    }

    // endregion

    // region resolveRomPath

    @Test
    fun `resolveRomPath preserves a subdirectory`() {
        val root = tempFolder.newFolder("Roms")
        val rom = File(root, "ps2/Disc1/Game.iso")
        rom.parentFile?.mkdirs()
        rom.writeText("stub")

        val hit = resolveRomPath(
            game("./Disc1/Game.iso", "ps2"),
            listOf(root.absolutePath),
        )

        assertEquals(rom.absolutePath, hit)
    }

    @Test
    fun `resolveRomPath preserves an ES-DE directory-as-file m3u path`() {
        // ./Xenogears.m3u/Xenogears.m3u — the inner file inside the m3u directory.
        // The PSP/PS2 short-circuit used to collapse this to `Xenogears.m3u` on
        // disk, breaking multi-disc games.
        val root = tempFolder.newFolder("Roms")
        val rom = File(root, "ps2/Xenogears.m3u/Xenogears.m3u")
        rom.parentFile?.mkdirs()
        rom.writeText("disc1.chd\ndisc2.chd\n")

        val hit = resolveRomPath(
            game("./Xenogears.m3u/Xenogears.m3u", "ps2"),
            listOf(root.absolutePath),
        )

        assertEquals(rom.absolutePath, hit)
    }

    @Test
    fun `resolveRomPath finds a ROM under the second entry of romsPaths`() {
        val primary = tempFolder.newFolder("Primary")
        val secondary = tempFolder.newFolder("Secondary")
        val rom = File(secondary, "ps2/Game.chd")
        rom.parentFile?.mkdirs()
        rom.writeText("stub")

        val hit = resolveRomPath(
            game("./Game.chd", "ps2"),
            listOf(primary.absolutePath, secondary.absolutePath),
        )

        assertEquals(rom.absolutePath, hit)
    }

    @Test
    fun `resolveRomPath treats PSP and PS2 identically`() {
        // Guards against re-coupling. Same rom-shape under both systems should
        // resolve the same way — neither may keep an arbitrary short-circuit
        // the other doesn't share.
        val root = tempFolder.newFolder("Roms")
        val ps2Rom = File(root, "ps2/Disc1/Game.iso")
        ps2Rom.parentFile?.mkdirs()
        ps2Rom.writeText("stub")
        val pspRom = File(root, "psp/Disc1/Game.iso")
        pspRom.parentFile?.mkdirs()
        pspRom.writeText("stub")

        val ps2Hit = resolveRomPath(
            game("./Disc1/Game.iso", "ps2"),
            listOf(root.absolutePath),
        )
        val pspHit = resolveRomPath(
            game("./Disc1/Game.iso", "psp"),
            listOf(root.absolutePath),
        )

        assertEquals(ps2Rom.absolutePath, ps2Hit)
        assertEquals(pspRom.absolutePath, pspHit)
        assertTrue(
            "PSP and PS2 must not diverge in resolveRomPath",
            ps2Hit!!.endsWith("ps2/Disc1/Game.iso") &&
                pspHit!!.endsWith("psp/Disc1/Game.iso"),
        )
    }

    @Test
    fun `resolveRomPath falls back to primary-root canonical path when nothing exists`() {
        // Preserves the "permission timing" fallback: File.exists() can lie
        // for paths our process cannot stat, and returning null would break
        // launch for a ROM the emulator can still open via SAF.
        val root = tempFolder.newFolder("Roms")

        val hit = resolveRomPath(
            game("./Ghost.chd", "ps2"),
            listOf(root.absolutePath),
        )

        assertNotNull(hit)
        assertEquals(
            File(root, "ps2/Ghost.chd").absolutePath,
            hit,
        )
    }

    // endregion

    private fun game(path: String, systemName: String) = GameInfo(
        path = path,
        name = File(path).nameWithoutExtension,
        systemName = systemName,
    )

    private fun writeMedia(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }

    private fun mediaFile(root: File, relative: String): String =
        File(root, relative).absolutePath
}
