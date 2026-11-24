package jr.brian.jarnfe.transition

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import jr.brian.core.ui.theme.JarnMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class JarnDepthPushLogicTest {
    private val motion = JarnMotion()

    @Test
    fun `the push grows from the picked tile`() {
        val origin = jarnDepthOrigin(Rect(100f, 50f, 300f, 150f), Rect(0f, 0f, 800f, 400f))
        assertEquals(0.25f, origin.pivotFractionX, 0.0001f)
        assertEquals(0.25f, origin.pivotFractionY, 0.0001f)
    }

    @Test
    fun `without a tile the push grows from the center`() {
        assertEquals(TransformOrigin.Center, jarnDepthOrigin(null, Rect(0f, 0f, 800f, 400f)))
        assertEquals(TransformOrigin.Center, jarnDepthOrigin(Rect(0f, 0f, 10f, 10f), null))
    }

    @Test
    fun `a tile scrolled past the edge pins the origin to that edge`() {
        val origin = jarnDepthOrigin(Rect(900f, -200f, 1000f, -100f), Rect(0f, 0f, 800f, 400f))
        assertEquals(1f, origin.pivotFractionX, 0f)
        assertEquals(0f, origin.pivotFractionY, 0f)
    }

    @Test
    fun `deeper opens, shallower goes back, anything else is instant`() {
        assertSame(motion.depthOpen, jarnDepthChangeBetween(motion, 0, 1))
        assertSame(motion.depthBack, jarnDepthChangeBetween(motion, 1, 0))
        assertNull(jarnDepthChangeBetween(motion, 0, null))
        assertNull(jarnDepthChangeBetween(motion, null, 1))
        assertNull(jarnDepthChangeBetween(motion, 1, 1))
    }

    @Test
    fun `the slide follows the system order`() {
        assertEquals(1, jarnStepDirection(2, 3))
        assertEquals(-1, jarnStepDirection(3, 1))
        assertEquals(0, jarnStepDirection(-1, 2))
        assertEquals(0, jarnStepDirection(2, 2))
    }
}
