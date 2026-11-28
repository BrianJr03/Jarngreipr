package jr.brian.jarnfe.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class JarnStickFilterTest {
    @Test
    fun `inside the dead zone is centered`() {
        val filter = JarnStickFilter()
        assertEquals(JarnStickVector.Centered, filter.update(0.2f, 0f))
        assertEquals(JarnStickVector.Centered, filter.update(0.1f, -0.15f))
        assertEquals(JarnStickVector.Centered, filter.update(Float.NaN, 0f))
    }

    @Test
    fun `movement starts at zero just past the dead zone`() {
        val filter = JarnStickFilter(smoothing = 1f)
        val nudge = filter.update(0.25f, 0f)
        assertTrue(nudge.x > 0f && nudge.x < 0.1f)
    }

    @Test
    fun `the dead zone is radial so diagonals keep their direction`() {
        val v = JarnStickFilter(smoothing = 1f).update(0.6f, 0.6f)
        assertEquals(v.x, v.y, 0.0001f)
        assertTrue(hypot(v.x, v.y) <= 1f)
    }

    @Test
    fun `smoothing eases in, reaches full deflection, and snaps to zero on release`() {
        val filter = JarnStickFilter()
        val first = filter.update(1f, 0f).x
        val second = filter.update(1f, 0f).x
        assertTrue(first in 0.4f..0.6f)
        assertTrue(second > first)
        repeat(10) { filter.update(1f, 0f) }
        assertEquals(1f, filter.value.x)

        assertEquals(JarnStickVector.Centered, filter.update(0f, 0f))
    }

    @Test
    fun `values never leave -1 to 1`() {
        val v = JarnStickFilter(smoothing = 1f).update(-1.4f, 1.4f)
        assertTrue(v.x >= -1f && v.y <= 1f)
    }
}
