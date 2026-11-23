package jr.brian.core.ui

import jr.brian.core.ui.focus.jarnLazyItemDistance
import org.junit.Assert.assertEquals
import org.junit.Test

class JarnLazyRevealTest {
    private fun distance(start: Float, toTop: Float? = null, toBottom: Float? = null) =
        jarnLazyItemDistance(start, itemSize = 100f, container = 500f, margin = 20f, toTop, toBottom)

    @Test
    fun `an item clear of the edges does not scroll`() = assertEquals(0f, distance(200f), 0f)

    @Test
    fun `the whole item is kept off the top edge`() = assertEquals(-30f, distance(-10f), 0f)

    @Test
    fun `the first item returns all the way to the top`() =
        assertEquals(-12f, distance(40f, toTop = -12f), 0f)

    @Test
    fun `the last item reaches the very bottom`() =
        assertEquals(25f, distance(390f, toBottom = 25f), 0f)
}
