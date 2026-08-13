package jr.brian.home.esde.util

import jr.brian.home.esde.model.GameInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusBackgroundsTest {

    private fun game(
        fanart: String? = null,
        screenshot: String? = null,
        titlescreen: String? = null,
        miximage: String? = null
    ) = GameInfo(
        path = "/games/foo.zip",
        name = "Foo",
        systemName = "sys",
        fanartPath = fanart,
        screenshotPath = screenshot,
        titlescreenPath = titlescreen,
        miximagePath = miximage
    )

    @Test
    fun `fanart wins when present`() {
        val g = game(fanart = "/f.jpg", screenshot = "/s.jpg", titlescreen = "/t.jpg", miximage = "/m.jpg")
        assertEquals("/f.jpg", g.heroBackgroundPath())
    }

    @Test
    fun `falls back to screenshot when fanart absent`() {
        val g = game(fanart = null, screenshot = "/s.jpg", titlescreen = "/t.jpg", miximage = "/m.jpg")
        assertEquals("/s.jpg", g.heroBackgroundPath())
    }

    @Test
    fun `falls back to titlescreen when fanart and screenshot absent`() {
        val g = game(titlescreen = "/t.jpg", miximage = "/m.jpg")
        assertEquals("/t.jpg", g.heroBackgroundPath())
    }

    @Test
    fun `falls back to miximage when everything else absent`() {
        val g = game(miximage = "/m.jpg")
        assertEquals("/m.jpg", g.heroBackgroundPath())
    }

    @Test
    fun `returns null when nothing is set`() {
        assertNull(game().heroBackgroundPath())
    }

    @Test
    fun `treats blank strings as absent`() {
        val g = game(fanart = "   ", screenshot = "", titlescreen = "/t.jpg")
        assertEquals("/t.jpg", g.heroBackgroundPath())
    }
}
