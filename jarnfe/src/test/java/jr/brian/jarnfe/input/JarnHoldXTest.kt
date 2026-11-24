package jr.brian.jarnfe.input

import android.view.KeyEvent
import jr.brian.core.ui.components.JarnKey
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JarnHoldXTest {
    private val router = JarnInputRouter()
    private val log = mutableListOf<String>()

    private fun down(code: Int, time: Long = 0, repeat: Int = 0) =
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, time, repeat))

    private fun up(code: Int, time: Long) = router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, time))

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
                on(JarnAction.SwitchScreen, R.string.jarnfe_hint_switch_screen) { log += "switch" }
                on(JarnAction.Swap) { log += "swap" }
            },
        )
    }

    @Test
    fun `x switches screens and select is the options button`() {
        assertEquals(JarnAction.SwitchScreen, JarnButtonMap.actionFor(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(JarnAction.Context, JarnButtonMap.actionFor(KeyEvent.KEYCODE_BUTTON_SELECT))
        assertEquals(JarnKey.X, JarnAction.SwitchScreen.hintKey)
        assertEquals(JarnKey.Select, JarnAction.Context.hintKey)
    }

    @Test
    fun `a tap of x switches on release`() {
        down(KeyEvent.KEYCODE_BUTTON_X)
        assertEquals(emptyList<String>(), log)
        up(KeyEvent.KEYCODE_BUTTON_X, 80)
        assertEquals(listOf("switch"), log)
    }

    @Test
    fun `holding x swaps the screens and never also switches`() {
        hold(KeyEvent.KEYCODE_BUTTON_X, 800)
        assertEquals(listOf("swap"), log)
    }

    @Test
    fun `a long press with no repeats still swaps on release`() {
        down(KeyEvent.KEYCODE_BUTTON_X)
        up(KeyEvent.KEYCODE_BUTTON_X, JARN_LONG_PRESS_MS + 10)
        assertEquals(listOf("swap"), log)
    }

    @Test
    fun `holding x swaps from inside a layer that binds its own switch`() {
        router.attach(
            router.createLayer(JarnScreen.Stage, JarnLayerMode.Modal, depth = 1).apply {
                update(jarnBindings { on(JarnAction.SwitchScreen) { log += "layer switch" } })
            },
        )
        hold(KeyEvent.KEYCODE_BUTTON_X, 700)
        assertEquals(listOf("swap"), log)
    }

    @Test
    fun `a layer's own long press on x is kept`() {
        router.attach(
            router.createLayer(JarnScreen.Stage, depth = 1).apply {
                update(
                    jarnBindings {
                        on(JarnAction.SwitchScreen, onLongPress = { log += "layer hold" }) { log += "layer tap" }
                    },
                )
            },
        )
        hold(KeyEvent.KEYCODE_BUTTON_X, 700)
        assertEquals(listOf("layer hold"), log)
    }

    @Test
    fun `l3 keeps swapping`() {
        down(KeyEvent.KEYCODE_BUTTON_THUMBL)
        assertEquals(listOf("swap"), log)
    }
}
