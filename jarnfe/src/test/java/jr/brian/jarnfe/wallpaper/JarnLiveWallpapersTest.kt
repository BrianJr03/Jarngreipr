package jr.brian.jarnfe.wallpaper

import jr.brian.frontend.look.JARN_LIVE_WALLPAPER_OFF
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnLiveWallpapersTest {
    private val live = JarnLiveWallpaper("starstave", R.string.jarnfe_wallpaper_live) {}
    private val catalog = listOf(live)

    private fun resolve(saved: String?, preview: String? = null, image: String? = IMAGE, show: Boolean = true) =
        resolveJarnBackdrop(show, saved, preview, image, catalog)

    @Test
    fun `live wins over the picked image`() {
        assertEquals(JarnBackdrop.Live(live), resolve("starstave"))
    }

    @Test
    fun `with no live wallpaper the image wins over the theme`() {
        assertEquals(JarnBackdrop.Image(IMAGE), resolve(null))
        assertEquals(JarnBackdrop.Theme, resolve(null, image = null))
    }

    @Test
    fun `an unknown id shows the image or theme`() {
        assertEquals(JarnBackdrop.Image(IMAGE), resolve("future"))
        assertEquals(JarnBackdrop.Theme, resolve("future", image = null))
    }

    @Test
    fun `previewing off shows the image under a saved live wallpaper`() {
        assertEquals(JarnBackdrop.Image(IMAGE), resolve("starstave", preview = JARN_LIVE_WALLPAPER_OFF))
    }

    @Test
    fun `previewing a live wallpaper does not need it saved`() {
        assertEquals(JarnBackdrop.Live(live), resolve(null, preview = "starstave"))
    }

    @Test
    fun `a hidden wallpaper shows the theme`() {
        assertEquals(JarnBackdrop.Theme, resolve("starstave", show = false))
    }

    @Test
    fun `shipped ids are unique and never the off value`() {
        val ids = JarnLiveWallpapers.All.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertFalse(JARN_LIVE_WALLPAPER_OFF in ids)
        assertTrue(ids.none(String::isBlank))
    }

    @Test
    fun `the permanent wallpaper ids stay registered`() {
        val ids = JarnLiveWallpapers.All.map { it.id }
        assertTrue(ids.containsAll(listOf("starstave", "ironweave", "hearthrise", "tidemark")))
    }

    private companion object {
        const val IMAGE = "file:///wallpaper.png"
    }
}
