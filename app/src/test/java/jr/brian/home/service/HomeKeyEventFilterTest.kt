package jr.brian.home.service

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the scan-code / gate invariants of the interceptor's match predicate.
 * Gesture classification (single / double / long press) lives in
 * [HomeGestureStateMachine] and is tested separately.
 *
 *  - Only a real hardware Home press (`keyCode == KEYCODE_HOME && scanCode == 102`)
 *    is ever consumed. Any other key, and any synthesized Home event
 *    (`scanCode == 0`), falls through so we don't feedback-loop with
 *    `performGlobalAction`, the nav bar, or our own routing follow-up.
 *  - When the user toggle is off the filter never consumes — turning off
 *    Home Button Interception restores default system behaviour without
 *    needing the user to revoke the accessibility permission.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class HomeKeyEventFilterTest {

    private fun hardwareHome(action: Int): KeyEvent = KeyEvent(
        /* downTime  */ 1_000L,
        /* eventTime */ 1_000L,
        /* action    */ action,
        /* code      */ KeyEvent.KEYCODE_HOME,
        /* repeat    */ 0,
        /* metaState */ 0,
        /* deviceId  */ 0,
        /* scancode  */ HomeKeyEventFilter.HARDWARE_HOME_SCAN_CODE,
        /* flags     */ 0
    )

    private fun syntheticHome(action: Int, scanCode: Int = 0): KeyEvent = KeyEvent(
        1_000L, 1_000L, action, KeyEvent.KEYCODE_HOME,
        0, 0, 0, scanCode, 0
    )

    private fun hardwareOtherKey(action: Int, keyCode: Int): KeyEvent = KeyEvent(
        1_000L, 1_000L, action, keyCode,
        0, 0, 0, HomeKeyEventFilter.HARDWARE_HOME_SCAN_CODE, 0
    )

    @Test
    fun `hardware Home down and up are both consumed`() {
        val filter = HomeKeyEventFilter()

        assertTrue(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_DOWN), enabled = true))
        assertTrue(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_UP), enabled = true))
    }

    @Test
    fun `synthesized Home events fall through completely`() {
        // scanCode == 0 is what performGlobalAction, nav bar taps, and our own
        // routeHome() follow-up look like — critical that none of them retrigger us.
        val filter = HomeKeyEventFilter()

        assertFalse(filter.shouldConsume(syntheticHome(KeyEvent.ACTION_DOWN), enabled = true))
        assertFalse(filter.shouldConsume(syntheticHome(KeyEvent.ACTION_UP), enabled = true))
    }

    @Test
    fun `non-home keys with the home scan code still fall through`() {
        val filter = HomeKeyEventFilter()
        val event = hardwareOtherKey(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)

        assertFalse(filter.shouldConsume(event, enabled = true))
    }

    @Test
    fun `disabled flag makes the filter a pass-through`() {
        val filter = HomeKeyEventFilter()

        assertFalse(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_DOWN), enabled = false))
        assertFalse(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_UP), enabled = false))
    }

    @Test
    fun `hardware home scan code is Linux KEY_HOME`() {
        assertEquals(102, HomeKeyEventFilter.HARDWARE_HOME_SCAN_CODE)
    }
}
