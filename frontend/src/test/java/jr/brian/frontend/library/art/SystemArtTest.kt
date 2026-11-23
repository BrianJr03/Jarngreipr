package jr.brian.frontend.library.art

import jr.brian.core.model.SystemArtSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SystemArtTest {
    @get:Rule
    val temp = TemporaryFolder()

    private fun image(system: String, folder: String, name: String): File =
        File(temp.root, "$system/$folder/$name").apply {
            parentFile?.mkdirs()
            writeText("art")
        }

    private fun dir(system: String) = File(temp.root, system)

    @Test
    fun `prefers the image named after its system`() {
        image("nds", PLATFORM_LOGOS_FOLDER, "a_other.png")
        val expected = image("nds", PLATFORM_LOGOS_FOLDER, "nds.png")

        assertEquals(expected.path, platformImage(listOf(dir("nds")), PLATFORM_LOGOS_FOLDER))
    }

    @Test
    fun `falls through to the next system folder`() {
        val expected = image("3ds", PLATFORM_BACKGROUNDS_FOLDER, "3ds.gif")

        assertEquals(
            expected.path,
            platformImage(listOf(dir("n3ds"), dir("3ds")), PLATFORM_BACKGROUNDS_FOLDER),
        )
    }

    @Test
    fun `ignores files that are not images`() {
        image("gba", PLATFORM_LOGOS_FOLDER, "gba.txt")

        assertNull(platformImage(listOf(dir("gba")), PLATFORM_LOGOS_FOLDER))
    }

    @Test
    fun `bundled logo uses the first folder that has one`() {
        assertEquals(
            "file:///android_asset/system_logos/3ds.svg",
            bundledLogo(listOf("n3ds", "3ds"), setOf("3ds.svg")),
        )
        assertNull(bundledLogo(listOf("n3ds"), setOf("gba.svg")))
    }

    @Test
    fun `retrohrai keeps its art and borrows only a missing logo`() {
        val background = SystemArt(backgroundPath = "bg.gif")

        assertEquals(
            SystemArt("bg.gif", "bundled.svg"),
            pickSystemArt(SystemArtSource.RetroHrai, background, "bundled.svg"),
        )
        assertEquals(
            SystemArt("bg.gif", "logo.png"),
            pickSystemArt(SystemArtSource.RetroHrai, SystemArt("bg.gif", "logo.png"), "bundled.svg"),
        )
    }

    @Test
    fun `bundled shows only its logo and falls back when it has none`() {
        val retroHrai = SystemArt("bg.gif", "logo.png")

        assertEquals(
            SystemArt(logoPath = "bundled.svg"),
            pickSystemArt(SystemArtSource.Bundled, retroHrai, "bundled.svg"),
        )
        assertEquals(retroHrai, pickSystemArt(SystemArtSource.Bundled, retroHrai, null))
    }
}
