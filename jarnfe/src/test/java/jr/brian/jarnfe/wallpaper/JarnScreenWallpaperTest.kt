package jr.brian.jarnfe.wallpaper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JarnScreenWallpaperTest {
    @Test
    fun `a shared wallpaper shows on both screens`() {
        assertEquals("top", jarnScreenWallpaperUri(JarnWallpaperScreen.Top, true, "top", "bottom"))
        assertEquals("top", jarnScreenWallpaperUri(JarnWallpaperScreen.Bottom, true, "top", "bottom"))
    }

    @Test
    fun `each screen shows its own when not shared`() {
        assertEquals("top", jarnScreenWallpaperUri(JarnWallpaperScreen.Top, false, "top", "bottom"))
        assertEquals("bottom", jarnScreenWallpaperUri(JarnWallpaperScreen.Bottom, false, "top", "bottom"))
    }

    @Test
    fun `a window knows its screen from its display alone`() {
        assertEquals(JarnWallpaperScreen.Top, jarnWallpaperScreenOf(0, 0))
        assertEquals(JarnWallpaperScreen.Bottom, jarnWallpaperScreenOf(4, 0))
    }

    @Test
    fun `a window not yet on a display draws no picked image`() {
        assertNull(jarnWallpaperScreenOf(null, 0))
    }

    @Test
    fun `a bottom screen with no own wallpaper shows the theme`() {
        assertNull(jarnScreenWallpaperUri(JarnWallpaperScreen.Bottom, false, "top", null))
    }
}
