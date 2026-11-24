package jr.brian.core.ui

import jr.brian.core.ui.focus.JarnFocusFloatAmplitude
import jr.brian.core.ui.focus.JarnFocusFloatMaxGrowth
import jr.brian.core.ui.focus.JarnFocusLift
import jr.brian.core.ui.focus.JarnFocusMaxGrowth
import jr.brian.core.ui.focus.JarnFocusStyle
import jr.brian.core.ui.focus.jarnFocusFloatOffset
import jr.brian.core.ui.focus.jarnFocusScale
import jr.brian.core.ui.theme.JarnSpacing
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnFocusScaleTest {
    private val growth = 10f

    private fun focused(side: Int) = jarnFocusScale(JarnFocusStyle.Scale, 1f, 1f, side, growth)

    @Test
    fun `small items use the full scale`() = assertEquals(1.08f, focused(100), 0.001f)

    @Test
    fun `no side of any size moves past the growth limit`() {
        listOf(40, 100, 250, 400, 900, 1920, 4000).forEach { side ->
            assertTrue((focused(side) - 1f) * side / 2f <= growth + 0.01f)
        }
    }

    @Test
    fun `growth stays inside the bring into view margin`() =
        assertTrue(JarnFocusMaxGrowth < JarnSpacing().focusEdge)

    @Test
    fun `an inactive screen shows no growth`() =
        assertEquals(1f, jarnFocusScale(JarnFocusStyle.Scale, 1f, 0f, 100, growth), 0.001f)

    @Test
    fun `ring items never scale`() =
        assertEquals(1f, jarnFocusScale(JarnFocusStyle.Ring, 1f, 1f, 100, growth), 0.001f)

    @Test
    fun `a floating tile stays inside the bring into view margin`() {
        val reach = JarnFocusFloatMaxGrowth + JarnFocusLift + JarnFocusFloatAmplitude
        assertTrue(reach <= 15.dp)
        assertTrue(reach < JarnSpacing().focusEdge)
    }

    @Test
    fun `a focused tile lifts and floats`() {
        assertEquals(-9f, jarnFocusFloatOffset(JarnFocusStyle.Float, -1f, 1f, 1f, 6f, 3f), 0.001f)
        assertEquals(-3f, jarnFocusFloatOffset(JarnFocusStyle.Float, 1f, 1f, 1f, 6f, 3f), 0.001f)
    }

    @Test
    fun `only floating tiles on the active screen move`() {
        assertEquals(0f, jarnFocusFloatOffset(JarnFocusStyle.Float, 1f, 1f, 0f, 6f, 3f), 0.001f)
        assertEquals(0f, jarnFocusFloatOffset(JarnFocusStyle.Scale, 1f, 1f, 1f, 6f, 3f), 0.001f)
    }
}
