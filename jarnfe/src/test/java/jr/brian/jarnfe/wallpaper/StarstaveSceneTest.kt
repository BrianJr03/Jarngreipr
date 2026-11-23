package jr.brian.jarnfe.wallpaper

import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarstaveSceneTest {
    @Test
    fun `the seeded field matches the approved design`() {
        val stars = StarstaveScene.Stars
        assertEquals(600, stars.size)
        assertEquals(-896.25702f, stars[0], 0.001f)
        assertEquals(2123.9221f, stars[1], 0.001f)
        assertEquals(1.7452713f, stars[2], 0.0001f)
        assertEquals(5.1279874f, stars[3], 0.0001f)
        assertEquals(584.21466f, stars[596], 0.001f)
        assertEquals(1097.6415f, stars[597], 0.001f)
        assertEquals(4.8289914f, stars[598], 0.0001f)
        assertEquals(4.1754603f, stars[599], 0.0001f)
    }

    @Test
    fun `the sign has eight nodes and the designed length`() {
        assertEquals(8, StarstaveScene.NodeX.size)
        assertEquals(8, StarstaveScene.NodeY.size)
        assertEquals(1813.2495f, StarstaveScene.TotalReach, 0.001f)
    }

    @Test
    fun `the still frame is the finished sign`() {
        val seconds = starstaveSeconds(123_456L, isStill = true)
        val cycle = starstaveCycle(seconds)
        assertEquals(STARSTAVE_STILL_SECONDS, seconds, 0.0)
        assertEquals(1.0, starstaveDrawProgress(cycle, isStill = true), 0.0)
        assertEquals(1.0, starstaveFade(cycle, isStill = true), 0.0)
    }

    @Test
    fun `the first live frame already shows the finished sign`() {
        val cycle = starstaveCycle(starstaveSeconds(0L, isStill = false))
        assertEquals(1.0, starstaveDrawProgress(cycle, isStill = false), 0.0)
        assertEquals(1.0, starstaveFade(cycle, isStill = false), 0.0)
    }

    @Test
    fun `the line draws then fades within the sixty second cycle`() {
        assertEquals(0.5, starstaveDrawProgress(0.31, isStill = false), 1e-9)
        assertEquals(1.0, starstaveDrawProgress(0.7, isStill = false), 0.0)
        assertEquals(1.0, starstaveFade(0.86, isStill = false), 0.0)
        assertEquals(0.5, starstaveFade(0.93, isStill = false), 1e-9)
        assertEquals(0.25, starstaveCycle(75.0), 1e-9)
    }

    @Test
    fun `the head walks the line from first node to last`() {
        val scene = StarstaveScene
        assertEquals(scene.NodeX[0], starstaveHeadX(0f), 0.001f)
        assertEquals(scene.NodeY[0], starstaveHeadY(0f), 0.001f)
        assertEquals(scene.NodeX[7], starstaveHeadX(scene.TotalReach), 0.001f)
        assertEquals(scene.NodeY[7], starstaveHeadY(scene.TotalReach), 0.001f)
        val halfFirst = scene.NodeReach[1] / 2f
        assertEquals((scene.NodeX[0] + scene.NodeX[1]) / 2f, starstaveHeadX(halfFirst), 0.01f)
        assertEquals((scene.NodeY[0] + scene.NodeY[1]) / 2f, starstaveHeadY(halfFirst), 0.01f)
    }

    @Test
    fun `stars breathe across exactly four levels`() {
        val seen = (0 until 2000).map { starstaveStarLevel(it * 0.05, 0f) }.toSet()
        assertEquals(setOf(0, 1, 2, 3), seen)
    }

    @Test
    fun `both windows map into one continuous scene`() {
        assertEquals(1f, starstaveScale(1080f), 0f)
        assertEquals(960f, starstaveHalfWidth(1920f, 1080f), 0f)
        assertEquals(620f, starstaveHalfWidth(1240f, 1080f), 0f)
        assertEquals(0f, starstaveOriginY(showsDial = false), 0f)
        assertEquals(1080f, starstaveOriginY(showsDial = true), 0f)
    }

    @Test
    fun `normal layout shows sky on top and dial on bottom`() {
        assertFalse(starstaveShowsDial(TOP_DISPLAY, TOP_DISPLAY, hasBothScreens = true))
        assertTrue(starstaveShowsDial(BOTTOM_DISPLAY, TOP_DISPLAY, hasBothScreens = true))
    }

    @Test
    fun `a swap keeps each scene half on its physical screen`() {
        val swapped = JarnSessionState(
            swapRequested = true,
            deckDisplayAvailable = true,
            deckHostReady = true,
        )
        assertEquals(JarnScreen.Deck, swapped.roleOn(JarnScreen.Stage))
        assertEquals(JarnScreen.Stage, swapped.roleOn(JarnScreen.Deck))
        assertFalse(starstaveShowsDial(TOP_DISPLAY, TOP_DISPLAY, swapped.deckHostReady))
        assertTrue(starstaveShowsDial(BOTTOM_DISPLAY, TOP_DISPLAY, swapped.deckHostReady))
    }

    @Test
    fun `a single screen always shows the sky`() {
        assertFalse(starstaveShowsDial(TOP_DISPLAY, TOP_DISPLAY, hasBothScreens = false))
        assertFalse(starstaveShowsDial(BOTTOM_DISPLAY, TOP_DISPLAY, hasBothScreens = false))
    }

    @Test
    fun `an unknown display shows the sky`() {
        assertFalse(starstaveShowsDial(null, TOP_DISPLAY, hasBothScreens = true))
    }

    private companion object {
        const val TOP_DISPLAY = 0
        const val BOTTOM_DISPLAY = 4
    }
}
