package jr.brian.home.data

import jr.brian.frontend.library.art.CustomGameArt
import jr.brian.frontend.library.db.GameInfoRow
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverride
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class UserOverrideArtTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `reads the art Classic saved, under Classic's key`() {
        val root = temp.root.path
        val basename = File("/roms/gba/Metroid Fusion.gba").nameWithoutExtension
        UserMetadataOverrideStore.write(
            root,
            "gba",
            basename,
            UserMetadataOverride(
                artworkPath = "/art/cover.png",
                marqueeImagePath = "/art/marquee.png",
                fanartPath = "/art/fanart.png",
                screenshotPath = "/art/shot.png",
            ),
        )

        assertEquals(
            mapOf(
                basename to CustomGameArt(
                    cover = "/art/cover.png",
                    marquee = "/art/marquee.png",
                    fanart = "/art/fanart.png",
                    screenshot = "/art/shot.png",
                ),
            ),
            readUserOverrideArt(root, "gba"),
        )
    }

    @Test
    fun `an override with only text gives no art`() {
        val root = temp.root.path
        UserMetadataOverrideStore.write(root, "gba", "Text", UserMetadataOverride(displayName = "Name"))

        assertTrue(readUserOverrideArt(root, "gba").isEmpty())
    }

    @Test
    fun `reading never changes the override folder`() {
        val root = temp.root.path
        UserMetadataOverrideStore.write(root, "gba", "A", UserMetadataOverride(artworkPath = "/a.png"))
        val before = temp.root.walkTopDown().map { it.path to it.lastModified() }.toList()

        readUserOverrideArt(root, "gba")

        assertEquals(before, temp.root.walkTopDown().map { it.path to it.lastModified() }.toList())
    }

    @Test
    fun `Classic's winning art for a game is unchanged`() {
        val row = mockk<GameInfoRow>(relaxed = true)
        every { row.absolutePath } returns "/roms/gba/A.gba"
        every { row.relativePath } returns "A.gba"
        every { row.displayName } returns "A"
        every { row.systemId } returns "gba"
        every { row.coverPath } returns "/media/cover.png"
        every { row.marqueePath } returns "/media/marquee.png"
        every { row.lastPlayed } returns null

        assertEquals("/media/cover.png", row.toGameInfo().artworkPath)
        val edited = row.toGameInfo(UserMetadataOverride(artworkPath = "/art/mine.png"))
        assertEquals("/art/mine.png", edited.artworkPath)
        assertEquals("/media/marquee.png", edited.marqueeImagePath)
    }
}
