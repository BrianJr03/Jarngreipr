package jr.brian.jarnfe.wallpaper

import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.jarnContrastRatio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnWallpaperReadabilityTest {
    @Test
    fun `bare text stays readable on a white image in every look`() {
        JarnLooks.All.forEach { look ->
            val colors = look.colors
            val background = colors.brightestScrimmedBackground()
            listOf(colors.textHigh, colors.textMid, colors.readableOverWallpaper(colors.accent)).forEach {
                assertTrue(look.id, jarnContrastRatio(it, background) >= JARN_MIN_TEXT_CONTRAST)
            }
        }
    }

    @Test
    fun `accent is kept where it is readable and replaced where it is not`() {
        val forge = JarnLooks.Forge.colors
        assertEquals(forge.textHigh, forge.readableOverWallpaper(forge.accent))
        listOf(JarnLooks.Fjord, JarnLooks.Grove, JarnLooks.Night).forEach {
            assertEquals(it.id, it.colors.accent, it.colors.readableOverWallpaper(it.colors.accent))
        }
    }

    @Test
    fun `both windows request the same decode size`() {
        val displays = listOf(1920 to 1080, 1080 to 1240)
        assertEquals(JarnWallpaperDecodeSize(1920, 1080), jarnWallpaperDecodeSize(displays, isPortrait = false))
        assertEquals(
            jarnWallpaperDecodeSize(displays, isPortrait = false),
            jarnWallpaperDecodeSize(displays.reversed(), isPortrait = false),
        )
    }

    @Test
    fun `decode size follows orientation`() {
        assertEquals(
            JarnWallpaperDecodeSize(1080, 1920),
            jarnWallpaperDecodeSize(listOf(1920 to 1080), isPortrait = true),
        )
    }

    @Test
    fun `no usable display leaves the size to the window`() {
        assertNull(jarnWallpaperDecodeSize(emptyList(), isPortrait = false))
        assertNull(jarnWallpaperDecodeSize(listOf(0 to 0), isPortrait = false))
    }
}
