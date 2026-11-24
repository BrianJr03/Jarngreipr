package jr.brian.jarnfe.splash

import jr.brian.jarnfe.splash.JarnSplashTimeline.DOCK_START
import jr.brian.jarnfe.splash.JarnSplashTimeline.DROP_START
import jr.brian.jarnfe.splash.JarnSplashTimeline.END
import jr.brian.jarnfe.splash.JarnSplashTimeline.REVEAL_START
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnSplashTimelineTest {
    @Test
    fun `suffix drops before the wordmark docks`() {
        assertEquals(0f, JarnSplashTimeline.drop(DROP_START), 0f)
        assertEquals(1f, JarnSplashTimeline.drop(DOCK_START), 0f)
        assertEquals(0f, JarnSplashTimeline.dock(DOCK_START), 0f)
    }

    @Test
    fun `wordmark has docked before content appears`() {
        assertEquals(1f, JarnSplashTimeline.dock(REVEAL_START), 0f)
        assertEquals(0f, JarnSplashTimeline.fade(REVEAL_START), 0f)
        assertEquals(0f, JarnSplashTimeline.tile(REVEAL_START, 0), 0f)
    }

    @Test
    fun `tiles appear in order`() {
        val elapsed = REVEAL_START + 120f

        assertTrue(JarnSplashTimeline.tile(elapsed, 0) > JarnSplashTimeline.tile(elapsed, 2))
    }

    @Test
    fun `every tile is fully visible at the end`() {
        listOf(0, 5, 11, 500).forEach { order ->
            assertEquals(1f, JarnSplashTimeline.tile(END, order), 0f)
        }
    }
}
