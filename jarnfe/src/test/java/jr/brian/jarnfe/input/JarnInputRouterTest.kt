package jr.brian.jarnfe.input

import android.view.KeyEvent
import jr.brian.core.ui.components.JarnKey
import jr.brian.jarnfe.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarnInputRouterTest {

    private val router = JarnInputRouter()
    private val log = mutableListOf<String>()

    private fun down(code: Int, time: Long = 0, repeat: Int = 0, fallback: Boolean = false) =
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, time, repeat, isFallback = fallback))

    private fun up(code: Int, time: Long = 10, canceled: Boolean = false, fallback: Boolean = false) =
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, time, 0, canceled, fallback))

    private fun tap(code: Int) {
        down(code)
        up(code)
    }

    private fun layer(
        screen: JarnScreen = JarnScreen.Stage,
        mode: JarnLayerMode = JarnLayerMode.Passive,
        depth: Int = 0,
        declare: JarnBindingsBuilder.() -> Unit,
    ) = router.createLayer(screen, mode, depth).also {
        it.update(jarnBindings(declare))
        router.attach(it)
    }

    private fun JarnBindingsBuilder.logs(action: JarnAction, name: String) =
        on(action, R.string.jarnfe_hint_confirm) { log += name }

    @Test
    fun `a button that acts reports one press and ignores its repeat`() {
        var presses = 0
        router.onButtonPressed = { presses++ }
        layer { logs(JarnAction.Context, "context") }

        down(KeyEvent.KEYCODE_BUTTON_SELECT)
        down(KeyEvent.KEYCODE_BUTTON_SELECT, time = 5, repeat = 1)
        up(KeyEvent.KEYCODE_BUTTON_SELECT)

        assertEquals(1, presses)
    }

    @Test
    fun `an unhinted binding such as l1 tabs still reports its press`() {
        var presses = 0
        router.onButtonPressed = { presses++ }
        layer { on(JarnAction.TabNext) { log += "tab" } }

        tap(KeyEvent.KEYCODE_BUTTON_R1)

        assertEquals(1, presses)
        assertEquals(listOf("tab"), log)
    }

    @Test
    fun `a button that does nothing reports nothing`() {
        var presses = 0
        router.onButtonPressed = { presses++ }

        tap(KeyEvent.KEYCODE_BUTTON_SELECT)

        assertEquals(0, presses)
    }

    @Test
    fun `every d-pad press and repeat arms navigation and never counts as a button`() {
        var directions = 0
        var buttons = 0
        router.onDirectionPressed = { directions++ }
        router.onButtonPressed = { buttons++ }

        down(KeyEvent.KEYCODE_DPAD_RIGHT)
        down(KeyEvent.KEYCODE_DPAD_RIGHT, time = 50, repeat = 1)
        down(KeyEvent.KEYCODE_DPAD_RIGHT, time = 100, repeat = 2)
        up(KeyEvent.KEYCODE_DPAD_RIGHT, time = 120)

        assertEquals(3, directions)
        assertEquals(0, buttons)
    }

    @Test
    fun `holding x to swap reports one press`() {
        var presses = 0
        router.setGlobalBindings(
            jarnBindings {
                on(JarnAction.SwitchScreen) { log += "switch" }
                on(JarnAction.Swap) { log += "swap" }
            },
        )
        router.onButtonPressed = { presses++ }

        down(KeyEvent.KEYCODE_BUTTON_X)
        down(KeyEvent.KEYCODE_BUTTON_X, time = JARN_LONG_PRESS_MS + 10, repeat = 1)
        up(KeyEvent.KEYCODE_BUTTON_X, time = JARN_LONG_PRESS_MS + 50)

        assertEquals(listOf("swap"), log)
        assertEquals(2, presses)
    }

    @Test
    fun `full button map`() {
        val expected = mapOf(
            KeyEvent.KEYCODE_BUTTON_A to JarnAction.Confirm,
            KeyEvent.KEYCODE_DPAD_CENTER to JarnAction.Confirm,
            KeyEvent.KEYCODE_ENTER to JarnAction.Confirm,
            KeyEvent.KEYCODE_NUMPAD_ENTER to JarnAction.Confirm,
            KeyEvent.KEYCODE_BUTTON_B to JarnAction.Back,
            KeyEvent.KEYCODE_BACK to JarnAction.Back,
            KeyEvent.KEYCODE_ESCAPE to JarnAction.Back,
            KeyEvent.KEYCODE_BUTTON_SELECT to JarnAction.Context,
            KeyEvent.KEYCODE_BUTTON_Y to JarnAction.Search,
            KeyEvent.KEYCODE_BUTTON_START to JarnAction.Menu,
            KeyEvent.KEYCODE_BUTTON_X to JarnAction.SwitchScreen,
            KeyEvent.KEYCODE_BUTTON_L1 to JarnAction.TabPrevious,
            KeyEvent.KEYCODE_BUTTON_R1 to JarnAction.TabNext,
            KeyEvent.KEYCODE_BUTTON_L2 to JarnAction.BottomTabPrevious,
            KeyEvent.KEYCODE_BUTTON_R2 to JarnAction.BottomTabNext,
            KeyEvent.KEYCODE_BUTTON_THUMBL to JarnAction.Swap,
            KeyEvent.KEYCODE_BUTTON_THUMBR to JarnAction.Swap,
            KeyEvent.KEYCODE_DPAD_UP to JarnAction.MoveUp,
            KeyEvent.KEYCODE_DPAD_DOWN to JarnAction.MoveDown,
            KeyEvent.KEYCODE_DPAD_LEFT to JarnAction.MoveLeft,
            KeyEvent.KEYCODE_DPAD_RIGHT to JarnAction.MoveRight,
        )
        expected.forEach { (code, action) -> assertEquals(action, JarnButtonMap.actionFor(code)) }
    }

    @Test
    fun `home and system keys are never handled`() {
        listOf(KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_BUTTON_MODE).forEach {
            assertNull(JarnButtonMap.actionFor(it))
            assertFalse(down(it))
            assertFalse(up(it))
        }
    }

    @Test
    fun `release actions fire once on key up`() {
        layer { logs(JarnAction.Confirm, "confirm") }
        assertTrue(down(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(emptyList<String>(), log)
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(listOf("confirm"), log)
    }

    @Test
    fun `press actions fire once on key down and swallow the release`() {
        layer { logs(JarnAction.TabNext, "next") }
        assertTrue(down(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(listOf("next"), log)
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(listOf("next"), log)
    }

    @Test
    fun `held tab does not repeat`() {
        layer { logs(JarnAction.TabNext, "next") }
        down(KeyEvent.KEYCODE_BUTTON_R1)
        down(KeyEvent.KEYCODE_BUTTON_R1, time = 600, repeat = 1)
        up(KeyEvent.KEYCODE_BUTTON_R1, time = 700)
        assertEquals(listOf("next"), log)
    }

    @Test
    fun `bound direction repeats while held`() {
        layer { logs(JarnAction.MoveRight, "move") }
        down(KeyEvent.KEYCODE_DPAD_RIGHT)
        down(KeyEvent.KEYCODE_DPAD_RIGHT, time = 500, repeat = 1)
        down(KeyEvent.KEYCODE_DPAD_RIGHT, time = 550, repeat = 2)
        up(KeyEvent.KEYCODE_DPAD_RIGHT, time = 600)
        assertEquals(3, log.size)
    }

    @Test
    fun `long press fires once at threshold and suppresses tap`() {
        layer { on(JarnAction.Confirm, onLongPress = { log += "long" }) { log += "tap" } }
        down(KeyEvent.KEYCODE_BUTTON_A)
        down(KeyEvent.KEYCODE_BUTTON_A, time = 100, repeat = 1)
        assertEquals(emptyList<String>(), log)
        down(KeyEvent.KEYCODE_BUTTON_A, time = 500, repeat = 2)
        down(KeyEvent.KEYCODE_BUTTON_A, time = 550, repeat = 3)
        up(KeyEvent.KEYCODE_BUTTON_A, time = 600)
        assertEquals(listOf("long"), log)
    }

    @Test
    fun `long press is detected on release when the pad sends no repeats`() {
        layer { on(JarnAction.Context, onLongPress = { log += "long" }) { log += "tap" } }
        down(KeyEvent.KEYCODE_BUTTON_SELECT)
        up(KeyEvent.KEYCODE_BUTTON_SELECT, time = 700)
        assertEquals(listOf("long"), log)
    }

    @Test
    fun `short press with a long press binding is a tap`() {
        layer { on(JarnAction.Context, onLongPress = { log += "long" }) { log += "tap" } }
        down(KeyEvent.KEYCODE_BUTTON_SELECT)
        up(KeyEvent.KEYCODE_BUTTON_SELECT, time = 100)
        assertEquals(listOf("tap"), log)
    }

    @Test
    fun `key held from before the router saw it never fires`() {
        layer { logs(JarnAction.Confirm, "confirm") }
        assertTrue(down(KeyEvent.KEYCODE_BUTTON_A, time = 900, repeat = 5))
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_A, time = 950))
        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun `canceled release does not fire`() {
        layer { logs(JarnAction.Confirm, "confirm") }
        down(KeyEvent.KEYCODE_BUTTON_A)
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_A, canceled = true))
        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun `cancelPresses drops a pending press`() {
        layer { logs(JarnAction.Confirm, "confirm") }
        down(KeyEvent.KEYCODE_BUTTON_A)
        router.cancelPresses()
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun `L3 and R3 together swap once`() {
        router.setGlobalBindings(jarnBindings { on(JarnAction.Swap) { log += "swap" } })
        down(KeyEvent.KEYCODE_BUTTON_THUMBL)
        down(KeyEvent.KEYCODE_BUTTON_THUMBR)
        up(KeyEvent.KEYCODE_BUTTON_THUMBL)
        up(KeyEvent.KEYCODE_BUTTON_THUMBR)
        assertEquals(listOf("swap"), log)
    }

    @Test
    fun `unbound A and D-pad reach the focused composable on both phases`() {
        listOf(KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_DPAD_DOWN).forEach {
            assertFalse(down(it))
            assertFalse(up(it))
        }
    }

    @Test
    fun `A fallback key never acts a second time`() {
        layer { logs(JarnAction.Confirm, "confirm") }
        assertFalse(down(KeyEvent.KEYCODE_DPAD_CENTER, fallback = true))
        assertFalse(up(KeyEvent.KEYCODE_DPAD_CENTER, fallback = true))
        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun `stick fallback directions still route to a capture layer`() {
        layer(mode = JarnLayerMode.Capture) { logs(JarnAction.MoveLeft, "move") }
        assertTrue(down(KeyEvent.KEYCODE_DPAD_LEFT, fallback = true))
        assertEquals(listOf("move"), log)
    }

    @Test
    fun `unbound non-focus buttons are consumed so system fallbacks cannot fire`() {
        listOf(
            KeyEvent.KEYCODE_BUTTON_B,
            KeyEvent.KEYCODE_BUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_START,
            KeyEvent.KEYCODE_BUTTON_X,
            KeyEvent.KEYCODE_BUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_THUMBL,
        ).forEach {
            assertTrue(down(it))
            assertTrue(up(it))
        }
    }

    @Test
    fun `B pops exactly one layer per press`() {
        lateinit var overlay: JarnInputLayerHandle
        layer { on(JarnAction.Back) { log += "root" } }
        overlay = layer(depth = 1) {
            on(JarnAction.Back) {
                log += "overlay"
                router.detach(overlay)
            }
        }
        tap(KeyEvent.KEYCODE_BUTTON_B)
        assertEquals(listOf("overlay"), log)
        tap(KeyEvent.KEYCODE_BUTTON_B)
        assertEquals(listOf("overlay", "root"), log)
    }

    @Test
    fun `B at Stage root does nothing but is consumed`() {
        assertTrue(down(KeyEvent.KEYCODE_BUTTON_B))
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(JarnScreen.Stage, router.activeScreen)
    }

    @Test
    fun `B at Deck root returns to Stage`() {
        router.activate(JarnScreen.Deck)
        tap(KeyEvent.KEYCODE_BUTTON_B)
        assertEquals(JarnScreen.Stage, router.activeScreen)
    }

    @Test
    fun `system back follows the same layer path and does not double fire with the key`() {
        layer { on(JarnAction.Back) { log += "back" } }
        router.dispatchSystemBack()
        assertEquals(listOf("back"), log)
        down(KeyEvent.KEYCODE_BACK)
        router.dispatchSystemBack()
        up(KeyEvent.KEYCODE_BACK)
        assertEquals(listOf("back", "back"), log)
    }

    @Test
    fun `top layer gets the action first and passive layers fall through`() {
        layer { logs(JarnAction.Context, "low-x"); logs(JarnAction.Search, "low-y") }
        layer(depth = 1) { logs(JarnAction.Context, "top-x") }
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        tap(KeyEvent.KEYCODE_BUTTON_Y)
        assertEquals(listOf("top-x", "low-y"), log)
    }

    @Test
    fun `depth wins over registration order`() {
        layer(depth = 1) { logs(JarnAction.Context, "deep") }
        layer(depth = 0) { logs(JarnAction.Context, "shallow") }
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals(listOf("deep"), log)
    }

    @Test
    fun `modal blocks lower layers but lets focus input through`() {
        layer { logs(JarnAction.Context, "low") }
        router.setGlobalBindings(jarnBindings { on(JarnAction.Menu) { log += "menu" } })
        layer(mode = JarnLayerMode.Modal, depth = 1) { logs(JarnAction.Back, "close") }
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        tap(KeyEvent.KEYCODE_BUTTON_START)
        assertEquals(emptyList<String>(), log)
        assertFalse(down(KeyEvent.KEYCODE_DPAD_UP))
        assertFalse(up(KeyEvent.KEYCODE_DPAD_UP))
        assertFalse(down(KeyEvent.KEYCODE_BUTTON_A))
        assertFalse(up(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `modal without a Back binding does not fall through to Deck return`() {
        router.activate(JarnScreen.Deck)
        layer(JarnScreen.Deck, JarnLayerMode.Modal) { logs(JarnAction.Context, "x") }
        tap(KeyEvent.KEYCODE_BUTTON_B)
        assertEquals(JarnScreen.Deck, router.activeScreen)
    }

    @Test
    fun `capture owns every mapped button`() {
        layer { logs(JarnAction.Menu, "low") }
        layer(mode = JarnLayerMode.Capture, depth = 1) {
            on(JarnAction.TabPrevious) { log += "smaller" }
            on(JarnAction.TabNext) { log += "larger" }
            on(JarnAction.Context) { log += "axis" }
            on(JarnAction.Confirm) { log += "drop" }
            on(JarnAction.Back) { log += "cancel" }
            on(JarnAction.MoveLeft) { log += "move" }
        }
        tap(KeyEvent.KEYCODE_BUTTON_L1)
        tap(KeyEvent.KEYCODE_BUTTON_R1)
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        tap(KeyEvent.KEYCODE_DPAD_LEFT)
        tap(KeyEvent.KEYCODE_BUTTON_A)
        tap(KeyEvent.KEYCODE_BUTTON_B)
        tap(KeyEvent.KEYCODE_BUTTON_START)
        assertEquals(listOf("smaller", "larger", "axis", "move", "drop", "cancel"), log)
    }

    @Test
    fun `capture swallows unbound A and D-pad`() {
        layer(mode = JarnLayerMode.Capture) { logs(JarnAction.Back, "cancel") }
        assertTrue(down(KeyEvent.KEYCODE_DPAD_UP))
        assertTrue(up(KeyEvent.KEYCODE_DPAD_UP))
        assertTrue(down(KeyEvent.KEYCODE_BUTTON_A))
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `dialog above a capture layer can still move focus`() {
        layer(mode = JarnLayerMode.Capture) { logs(JarnAction.Back, "cancel") }
        layer(mode = JarnLayerMode.Modal, depth = 1) { logs(JarnAction.Back, "close") }
        assertFalse(down(KeyEvent.KEYCODE_DPAD_UP))
        assertFalse(up(KeyEvent.KEYCODE_DPAD_UP))
    }

    @Test
    fun `a lost pass-through release cannot block the same action on another key`() {
        layer(depth = 0) { hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm) }
        assertFalse(down(KeyEvent.KEYCODE_BUTTON_A))
        router.cancelPresses()
        assertFalse(down(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    @Test
    fun `swap passes through modal and capture`() {
        router.setGlobalBindings(jarnBindings { on(JarnAction.Swap) { log += "swap" } })
        layer(mode = JarnLayerMode.Modal) { logs(JarnAction.Back, "close") }
        layer(mode = JarnLayerMode.Capture, depth = 1) { logs(JarnAction.Back, "cancel") }
        tap(KeyEvent.KEYCODE_BUTTON_THUMBR)
        assertEquals(listOf("swap"), log)
    }

    @Test
    fun `input only reaches the active screen`() {
        layer(JarnScreen.Stage) { logs(JarnAction.Context, "stage") }
        layer(JarnScreen.Deck) { logs(JarnAction.Context, "deck") }
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        router.activate(JarnScreen.Deck)
        tap(KeyEvent.KEYCODE_BUTTON_SELECT)
        assertEquals(listOf("stage", "deck"), log)
    }

    @Test
    fun `a layer closed mid-press does not fire and its release does not hit the layer below`() {
        layer { logs(JarnAction.Confirm, "low") }
        val top = layer(depth = 1) { logs(JarnAction.Confirm, "top") }
        down(KeyEvent.KEYCODE_BUTTON_A)
        router.detach(top)
        assertTrue(up(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun `hints render only declared actions from the top of the stack`() {
        layer {
            on(JarnAction.Confirm, R.string.jarnfe_hint_confirm) {}
            on(JarnAction.Context, R.string.jarnfe_hint_context) {}
        }
        layer(depth = 1) { on(JarnAction.Confirm, R.string.jarnfe_hint_play) {} }
        assertEquals(
            listOf(
                JarnInputHint(JarnKey.A, R.string.jarnfe_hint_play),
                JarnInputHint(JarnKey.Select, R.string.jarnfe_hint_context),
            ),
            router.hintsFor(JarnScreen.Stage),
        )
    }

    @Test
    fun `modal hints hide blocked actions`() {
        layer { on(JarnAction.Context, R.string.jarnfe_hint_context) {} }
        layer(mode = JarnLayerMode.Modal, depth = 1) { on(JarnAction.Back, R.string.jarnfe_hint_back) {} }
        assertEquals(
            listOf(JarnInputHint(JarnKey.B, R.string.jarnfe_hint_back)),
            router.hintsFor(JarnScreen.Stage),
        )
    }

    @Test
    fun `hint-only declaration shows a label and leaves the press to focus`() {
        layer { hint(JarnAction.Confirm, R.string.jarnfe_hint_confirm) }
        assertEquals(
            listOf(JarnInputHint(JarnKey.A, R.string.jarnfe_hint_confirm)),
            router.hintsFor(JarnScreen.Stage),
        )
        assertFalse(down(KeyEvent.KEYCODE_BUTTON_A))
        assertFalse(up(KeyEvent.KEYCODE_BUTTON_A))
    }

    @Test
    fun `four directions collapse into one D-pad hint`() {
        layer {
            on(JarnAction.MoveUp, R.string.jarnfe_hint_move) {}
            on(JarnAction.MoveDown, R.string.jarnfe_hint_move) {}
            on(JarnAction.MoveLeft, R.string.jarnfe_hint_move) {}
            on(JarnAction.MoveRight, R.string.jarnfe_hint_move) {}
        }
        assertEquals(
            listOf(JarnInputHint(JarnKey.Dpad, R.string.jarnfe_hint_move)),
            router.hintsFor(JarnScreen.Stage),
        )
    }

    @Test
    fun `unlabeled bindings and swap show no hint`() {
        router.setGlobalBindings(jarnBindings { on(JarnAction.Swap, R.string.jarnfe_hint_swap) {} })
        layer { on(JarnAction.Search) {} }
        assertEquals(emptyList<JarnInputHint>(), router.hintsFor(JarnScreen.Stage))
    }

    @Test
    fun `Deck root advertises B because it returns to Stage`() {
        assertEquals(
            listOf(JarnInputHint(JarnKey.B, R.string.jarnfe_hint_back)),
            router.hintsFor(JarnScreen.Deck),
        )
        assertEquals(emptyList<JarnInputHint>(), router.hintsFor(JarnScreen.Stage))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `Back cannot be declared repeating`() {
        jarnBindings { on(JarnAction.Back, repeats = true) {} }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `hint-only is rejected for actions focus cannot perform`() {
        jarnBindings { hint(JarnAction.Search, R.string.jarnfe_hint_search) }
    }
}
