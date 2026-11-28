package jr.brian.jarnfe.input

import android.app.Application
import android.view.InputDevice
import android.view.MotionEvent
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnAnalogInputTest {
    private val router = JarnInputRouter()
    private val received = mutableListOf<JarnSticks>()

    private fun layer(
        screen: JarnScreen = JarnScreen.Stage,
        mode: JarnLayerMode = JarnLayerMode.Passive,
        depth: Int = 0,
        sticks: Boolean = false,
    ) = router.createLayer(screen, mode, depth).also {
        it.update(jarnBindings { on(JarnAction.Confirm, R.string.jarnfe_hint_confirm) {} })
        if (sticks) it.onSticks = { s -> received += s }
        router.attach(it)
    }

    private fun motion(
        axes: Map<Int, Float>,
        source: Int = InputDevice.SOURCE_JOYSTICK,
        action: Int = MotionEvent.ACTION_MOVE,
        time: Long = 100,
    ): MotionEvent {
        val properties = arrayOf(MotionEvent.PointerProperties().apply { id = 0 })
        val coords = arrayOf(MotionEvent.PointerCoords().apply { axes.forEach { (axis, v) -> setAxisValue(axis, v) } })
        return MotionEvent.obtain(0, time, action, 1, properties, coords, 0, 0, 1f, 1f, 0, 0, source, 0)
    }

    private fun raw(lx: Float = 0f, ly: Float = 0f, rx: Float = 0f, ry: Float = 0f, hat: Boolean = false) =
        router.dispatchSticks(JarnStickRaw(lx, ly, rx, ry, eventTimeMs = 0, hatPressed = hat))

    @Test
    fun `reads both sticks from a joystick MotionEvent`() {
        val event = motion(
            mapOf(MotionEvent.AXIS_X to 0.5f, MotionEvent.AXIS_Y to -1f, MotionEvent.AXIS_Z to 1f, MotionEvent.AXIS_RZ to 0.3f),
        )
        assertEquals(JarnStickRaw(0.5f, -1f, 1f, 0.3f, eventTimeMs = 100), event.jarnStickRaw())
    }

    @Test
    fun `right stick on RX and RY is read when Z and RZ are still`() {
        val event = motion(mapOf(MotionEvent.AXIS_RX to -0.8f, MotionEvent.AXIS_RY to 0.6f))
        val raw = event.jarnStickRaw()!!
        assertEquals(-0.8f, raw.rightX)
        assertEquals(0.6f, raw.rightY)
    }

    @Test
    fun `non joystick or non move events are ignored`() {
        assertNull(motion(mapOf(MotionEvent.AXIS_X to 1f), source = InputDevice.SOURCE_TOUCHSCREEN).jarnStickRaw())
        assertNull(motion(mapOf(MotionEvent.AXIS_X to 1f), action = MotionEvent.ACTION_DOWN).jarnStickRaw())
        assertFalse(router.dispatch(motion(mapOf(MotionEvent.AXIS_X to 1f), source = InputDevice.SOURCE_MOUSE)))
    }

    @Test
    fun `without an opted in layer the stick is left to Android as a D-pad`() {
        layer()
        assertFalse(router.dispatch(motion(mapOf(MotionEvent.AXIS_X to 1f))))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `an opted in layer gets the sticks and the event is consumed`() {
        layer(sticks = true)

        assertTrue(router.dispatch(motion(mapOf(MotionEvent.AXIS_X to 1f, MotionEvent.AXIS_RZ to -1f))))

        val sticks = received.single()
        assertTrue(sticks.left.x > 0f && sticks.left.y == 0f)
        assertTrue(sticks.right.y < 0f && sticks.right.x == 0f)
    }

    @Test
    fun `a hat D-pad in the same event still reaches Android`() {
        layer(sticks = true)
        assertFalse(router.dispatch(motion(mapOf(MotionEvent.AXIS_X to 1f, MotionEvent.AXIS_HAT_X to 1f))))
        assertEquals(1, received.size)
    }

    @Test
    fun `small drift inside the dead zone reports nothing but is still consumed`() {
        layer(sticks = true)
        assertTrue(raw(lx = 0.15f, ly = -0.1f, rx = 0.19f))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `a sheet above the stick layer takes the stick back to D-pad behavior`() {
        layer(sticks = true)
        layer(mode = JarnLayerMode.Capture, depth = 1)
        assertFalse(raw(lx = 1f))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `a passive layer above does not block, and the other screen never receives`() {
        layer(sticks = true)
        layer(depth = 1)
        layer(screen = JarnScreen.Deck, depth = 2)
        assertTrue(raw(lx = 1f))
        assertEquals(1, received.size)

        router.activate(JarnScreen.Deck)
        assertFalse(raw(lx = -1f))
    }

    @Test
    fun `losing the window or detaching centers the sticks for the layer`() {
        val stickLayer = layer(sticks = true)
        raw(lx = 1f)
        router.cancelPresses()
        assertTrue(received.last().isCentered)

        raw(ry = 1f)
        router.detach(stickLayer)
        assertTrue(received.last().isCentered)
        assertFalse(raw(ry = 1f))
    }

    @Test
    fun `repeated identical positions are delivered once`() {
        layer(sticks = true)
        repeat(30) { raw(lx = 1f) }
        val count = received.size
        repeat(5) { raw(lx = 1f) }
        assertEquals(count, received.size)
        assertEquals(1f, received.last().left.x)
    }

    @Test
    fun `a hat D-pad press and its release both reach Android`() {
        layer(sticks = true)
        assertFalse(raw(lx = 1f, hat = true))
        assertFalse(raw(lx = 1f))
        assertTrue(raw(lx = 1f))
    }
}
