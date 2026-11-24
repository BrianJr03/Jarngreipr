package jr.brian.core.ui.focus

import jr.brian.core.ui.library.jarnFitSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFocusBorderTest {
    @Test
    fun `a floating border reaches furthest up, where the float lifts it`() {
        val reach = jarnFocusBorderReach(120f, 160f)
        assertTrue(reach.top > reach.sides)
        assertTrue(reach.bottom < reach.sides)
        assertTrue(reach.innerGap > 0f)
    }

    @Test
    fun `the floating border stays inside the 20dp focus edge`() {
        listOf(60f, 112f, 240f, 600f).forEach { side ->
            assertTrue(jarnFocusBorderReach(side, side).top <= 20f)
        }
    }

    @Test
    fun `a scaling border has no lift, so every side reaches the same`() {
        val reach = jarnFocusBorderReach(800f, 160f, JarnFocusStyle.ScaleBorder)
        assertEquals(reach.sides, reach.top, 0f)
        assertEquals(reach.sides, reach.bottom, 0f)
    }

    @Test
    fun `art fits its slot without spilling over either side`() {
        assertEquals(100f to 75f, jarnFitSize(100f, 125f, 4f / 3f))
        assertEquals(60f to 120f, jarnFitSize(100f, 120f, 0.5f))
    }
}
