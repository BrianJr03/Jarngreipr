package jr.brian.core.ui

import jr.brian.core.ui.focus.jarnScrollDistance
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnBringIntoViewTest {
    private fun distance(offset: Float, size: Float = 100f) =
        jarnScrollDistance(offset, size, container = 500f, margin = 20f)

    @Test
    fun `item clear of both edges does not scroll`() {
        assertEquals(0f, distance(200f), 0f)
    }

    @Test
    fun `item at the exact margins does not scroll`() {
        assertEquals(0f, distance(20f), 0f)
        assertEquals(0f, distance(380f), 0f)
    }

    @Test
    fun `item near the top scrolls up to its margin`() {
        assertEquals(-15f, distance(5f), 0f)
    }

    @Test
    fun `item near the bottom scrolls down to its margin`() {
        assertEquals(30f, distance(410f), 0f)
    }

    @Test
    fun `item too large to fit lines up with the start`() {
        assertEquals(30f, distance(50f, size = 480f), 0f)
    }
}
