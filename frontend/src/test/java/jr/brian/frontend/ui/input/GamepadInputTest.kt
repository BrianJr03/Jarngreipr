package jr.brian.frontend.ui.input

import android.app.Application
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Pure translation + routing. These tests pin plan §3.2's default
 * key mapping (B=back, Y=search, X=sort, START=settings) so a later
 * remap change can't silently reshape the default UX.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GamepadInputTest {

    @Test
    fun `every default binding resolves to the right action`() {
        assertEquals(GamepadAction.Confirm, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(GamepadAction.Confirm, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_DPAD_CENTER))
        assertEquals(GamepadAction.Back, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(GamepadAction.Back, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_ESCAPE))
        assertEquals(GamepadAction.Back, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BACK))
        assertEquals(GamepadAction.Search, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_Y))
        assertEquals(GamepadAction.Sort, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_X))
        assertEquals(GamepadAction.Settings, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(GamepadAction.PageLeft, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_L1))
        assertEquals(GamepadAction.PageRight, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_R1))
        assertEquals(GamepadAction.JumpStart, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_L2))
        assertEquals(GamepadAction.JumpEnd, GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_BUTTON_R2))
    }

    @Test
    fun `unmapped key codes return null`() {
        assertNull(GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_DPAD_UP))
        assertNull(GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_A))
        assertNull(GamepadKeyRouter.actionFor(KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun `routeKeyEvent dispatches a semantic action on key up`() {
        var seen: GamepadAction? = null
        val handled = routeKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_B),
        ) { action -> seen = action; true }
        assertTrue(handled)
        assertEquals(GamepadAction.Back, seen)
    }

    @Test
    fun `routeKeyEvent ignores key downs so holds do not double-fire`() {
        var seen: GamepadAction? = null
        val handled = routeKeyEvent(
            KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BUTTON_B),
        ) { action -> seen = action; true }
        assertFalse(handled)
        assertNull(seen)
    }

    @Test
    fun `routeKeyEvent returns the handler's own value for consumption`() {
        val handled = routeKeyEvent(
            KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BUTTON_B),
        ) { false }
        assertFalse(handled)
    }
}
