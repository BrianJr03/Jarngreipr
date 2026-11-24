package jr.brian.jarnfe.tools

import jr.brian.jarnfe.tools.JarnTrackpadGestureEvent.End
import jr.brian.jarnfe.tools.JarnTrackpadGestureEvent.Tap
import jr.brian.jarnfe.tools.JarnTrackpadGestureEvent.TwoFingerTap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnTrackpadModelTest {
    private fun point(id: Long, x: Float, y: Float, pressed: Boolean = true, time: Long = 0) =
        JarnTouchPoint(id, pressed, JarnTouchSample(x, y, time))

    @Test
    fun `movement follows the acceleration curve`() {
        val slow = JarnTrackpadProcessor(100f, 100f).process(1f, 0f, 1_000_000L).first
        val fast = JarnTrackpadProcessor(100f, 100f).process(100f, 0f, 1_000_000L).first
        assertEquals(0.01f / 2.5f * 1.24f, slow, 0.0001f)
        assertEquals(1f / 2.5f * 5.5f, fast, 0.0001f)
    }

    @Test
    fun `cursor stays on screen and reports pixels`() {
        val sent = mutableListOf<JarnPointerCommand>()
        val controller = JarnTrackpadController(
            JarnTrackpadProcessor(100f, 100f),
            JarnPointerBounds(1000f, 500f),
            sent::add,
        )
        repeat(100) { controller.step(JarnPadDirection.Right) }
        assertEquals(JarnPointerCommand.Move(1000f, 250f), sent.last())
        controller.click(secondary = true)
        assertEquals(JarnPointerCommand.Click(1000f, 250f, true), sent.last())
    }

    @Test
    fun `one finger without travel is a tap`() {
        val gesture = JarnTrackpadGesture(10f)
        gesture.onFrame(listOf(point(1, 5f, 5f)))
        assertEquals(listOf(Tap, End), gesture.onFrame(listOf(point(1, 6f, 5f, pressed = false))))
    }

    @Test
    fun `two fingers lifting in different frames is still a right click`() {
        val gesture = JarnTrackpadGesture(10f)
        gesture.onFrame(listOf(point(1, 5f, 5f)))
        gesture.onFrame(listOf(point(1, 5f, 5f), point(2, 50f, 5f)))
        assertTrue(gesture.onFrame(listOf(point(1, 5f, 5f, pressed = false), point(2, 50f, 5f))).isEmpty())
        assertEquals(listOf(TwoFingerTap, End), gesture.onFrame(listOf(point(2, 50f, 5f, pressed = false))))
    }

    @Test
    fun `a drag is not a tap and the leftover finger never moves the cursor`() {
        val gesture = JarnTrackpadGesture(10f)
        gesture.onFrame(listOf(point(1, 0f, 0f)))
        gesture.onFrame(listOf(point(1, 0f, 0f), point(2, 40f, 0f)))
        val scroll = gesture.onFrame(listOf(point(1, 0f, 30f), point(2, 40f, 30f)))
        assertEquals(listOf(JarnTrackpadGestureEvent.Scroll(30f)), scroll)
        assertTrue(gesture.onFrame(listOf(point(1, 0f, 30f, pressed = false), point(2, 60f, 60f))).isEmpty())
        assertEquals(listOf(End), gesture.onFrame(listOf(point(2, 60f, 60f, pressed = false))))
    }

    @Test
    fun `only the second slot can be empty`() {
        assertFalse(JarnBackShortcut.NONE in jarnBackShortcutOptions(JarnBackSlot.Primary))
        assertTrue(JarnBackShortcut.NONE in jarnBackShortcutOptions(JarnBackSlot.Secondary))
    }

    @Test
    fun `pointer is ready only when fully connected`() {
        assertFalse(JarnPointerStatus(available = true, permissionGranted = true).ready)
        assertTrue(JarnPointerStatus(available = true, permissionGranted = true, connected = true).ready)
    }
}
