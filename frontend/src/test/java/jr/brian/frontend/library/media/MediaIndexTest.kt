package jr.brian.frontend.library.media

import android.app.Application
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
 * Covers [MediaIndex] + [EsdeLayout] + [JarngreiprLayout]. The scanner
 * can emit arbitrary ROM basenames; the index must find media for
 * them in a single `listFiles` per media folder regardless of which
 * layout owns the folder, with the first configured root winning on
 * collisions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MediaIndexTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `EsdeLayout resolves under downloaded_media per system per kind`() = runBlocking {
        val root = tempFolder.newFolder("ES-DE")
        writeMedia(root, "downloaded_media/nes/covers/Mario.png")
        writeMedia(root, "downloaded_media/nes/marquees/Mario.png")

        val index = MediaIndex.build(
            mediaRoots = listOf(root),
            layouts = listOf(EsdeLayout()),
            systems = setOf("nes"),
        )
        val resolved = index.resolve(systemFolder = "nes", basename = "Mario")
        assertTrue(resolved.cover?.endsWith("downloaded_media/nes/covers/Mario.png") == true)
        assertTrue(resolved.marquee?.endsWith("downloaded_media/nes/marquees/Mario.png") == true)
        assertNull(resolved.fanart)
        assertNull(resolved.video)
    }

    @Test
    fun `JarngreiprLayout resolves under the app-owned media dir`() = runBlocking {
        val root = tempFolder.newFolder("Jarngreipr Media")
        writeMedia(root, "nes/covers/Mario.png")
        writeMedia(root, "nes/videos/Mario.mp4")

        val index = MediaIndex.build(
            mediaRoots = listOf(root),
            layouts = listOf(JarngreiprLayout()),
            systems = setOf("nes"),
        )
        val resolved = index.resolve(systemFolder = "nes", basename = "Mario")
        assertTrue(resolved.cover?.endsWith("nes/covers/Mario.png") == true)
        assertTrue(resolved.video?.endsWith("nes/videos/Mario.mp4") == true)
    }

    @Test
    fun `basename lookup is case-insensitive`() = runBlocking {
        val root = tempFolder.newFolder("ES-DE")
        // Scraper wrote lowercase but gamelist carried the mixed-case name.
        writeMedia(root, "downloaded_media/nes/covers/super mario bros.png")

        val index = MediaIndex.build(
            mediaRoots = listOf(root),
            layouts = listOf(EsdeLayout()),
            systems = setOf("nes"),
        )
        val resolved = index.resolve(systemFolder = "nes", basename = "Super Mario Bros")
        assertTrue(resolved.cover != null)
    }

    @Test
    fun `first configured root wins on collisions`() = runBlocking {
        val primary = tempFolder.newFolder("primary")
        val secondary = tempFolder.newFolder("secondary")
        writeMedia(primary, "downloaded_media/nes/covers/Mario.png")
        writeMedia(secondary, "downloaded_media/nes/covers/Mario.png")

        val index = MediaIndex.build(
            mediaRoots = listOf(primary, secondary),
            layouts = listOf(EsdeLayout()),
            systems = setOf("nes"),
        )
        val resolved = index.resolve(systemFolder = "nes", basename = "Mario")
        assertTrue(
            "primary root must win over secondary — got ${resolved.cover}",
            resolved.cover?.contains("primary") == true,
        )
    }

    @Test
    fun `an empty index resolves everything to null`() {
        val resolved = MediaIndex.empty().resolve("nes", "Mario")
        assertEquals(MediaIndex.Resolved.EMPTY, resolved)
    }

    @Test
    fun `empty basename returns EMPTY without touching the index`() = runBlocking {
        val root = tempFolder.newFolder("ES-DE")
        writeMedia(root, "downloaded_media/nes/covers/Mario.png")

        val index = MediaIndex.build(
            mediaRoots = listOf(root),
            layouts = listOf(EsdeLayout()),
            systems = setOf("nes"),
        )
        assertEquals(MediaIndex.Resolved.EMPTY, index.resolve("nes", ""))
    }

    @Test
    fun `missing media root is handled, not thrown`() = runBlocking {
        val missing = File(tempFolder.root, "does-not-exist")
        val index = MediaIndex.build(
            mediaRoots = listOf(missing),
            layouts = listOf(EsdeLayout()),
            systems = setOf("nes"),
        )
        assertEquals(MediaIndex.Resolved.EMPTY, index.resolve("nes", "Mario"))
    }

    private fun writeMedia(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}
