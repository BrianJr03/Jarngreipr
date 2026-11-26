package jr.brian.jarnfe.input

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JarnStickMuteInputTest {
    private val router = JarnInputRouter()
    private val log = mutableListOf<String>()

    private fun down(code: Int, time: Long = 0, repeat: Int = 0) =
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, time, repeat))

    private fun up(code: Int, time: Long = 0) = router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, time))

    private fun press(code: Int) {
        down(code)
        up(code, 60)
    }

    private fun hold(code: Int, millis: Long) {
        down(code)
        var time = 50L
        var repeat = 1
        while (time < millis) {
            down(code, time, repeat++)
            time += 50
        }
        up(code, millis)
    }

    @Before
    fun setUp() {
        router.setGlobalBindings(
            jarnBindings {
                on(JarnAction.SwitchScreen) { log += "switch" }
                on(JarnAction.Swap) { log += "swap" }
                on(JarnAction.MuteJingles) { log += "jingles" }
                on(JarnAction.MuteMusic) { log += "music" }
            },
        )
    }

    @Test
    fun `the map sends L3 and R3 to swap unless the setting is on`() {
        val l3 = KeyEvent.KEYCODE_BUTTON_THUMBL
        val r3 = KeyEvent.KEYCODE_BUTTON_THUMBR
        assertEquals(JarnAction.Swap, JarnButtonMap.actionFor(l3))
        assertEquals(JarnAction.Swap, JarnButtonMap.actionFor(r3))
        assertEquals(JarnAction.MuteJingles, JarnButtonMap.actionFor(l3, sticksMute = true))
        assertEquals(JarnAction.MuteMusic, JarnButtonMap.actionFor(r3, sticksMute = true))
    }

    @Test
    fun `with the setting off L3 and R3 swap as before`() {
        press(KeyEvent.KEYCODE_BUTTON_THUMBL)
        press(KeyEvent.KEYCODE_BUTTON_THUMBR)
        assertEquals(listOf("swap", "swap"), log)
    }

    @Test
    fun `with the setting on L3 mutes jingles and R3 mutes music, once per press`() {
        router.sticksMute = true
        press(KeyEvent.KEYCODE_BUTTON_THUMBL)
        press(KeyEvent.KEYCODE_BUTTON_THUMBR)
        assertEquals(listOf("jingles", "music"), log)
    }

    @Test
    fun `holding X still swaps with the setting on`() {
        router.sticksMute = true
        hold(KeyEvent.KEYCODE_BUTTON_X, 800)
        assertEquals(listOf("swap"), log)
    }

    @Test
    fun `the mutes reach the app through a capture layer, like swap`() {
        router.sticksMute = true
        val layer = router.createLayer(JarnScreen.Stage, JarnLayerMode.Capture)
        layer.update(jarnBindings { on(JarnAction.Confirm) { log += "confirm" } })
        router.attach(layer)
        press(KeyEvent.KEYCODE_BUTTON_THUMBL)
        press(KeyEvent.KEYCODE_BUTTON_THUMBR)
        assertEquals(listOf("jingles", "music"), log)
        assertTrue(JarnAction.MuteJingles.isAppWide && JarnAction.MuteMusic.isAppWide)
    }
}
