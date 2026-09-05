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
 * Guards the two scan-code / debounce / gate invariants of the interceptor:
 *
 *  - Only a real hardware Home press (`keyCode == KEYCODE_HOME && scanCode == 102`)
 *    is ever consumed. Any other key, and any synthesized Home event (scanCode 0),
 *    falls through so we don't feedback-loop with `performGlobalAction` or the nav bar.
 *  - The routing side-effect fires on ACTION_UP only, and is debounced so a rapidly
 *    double-tapped physical button routes once, not twice.
 *  - When the user toggle is off the filter never consumes and never routes —
 *    turning off Home Button Interception restores default system behaviour without
 *    needing the user to revoke the accessibility permission.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class HomeKeyEventFilterTest {

    private var currentTime: Long = 1_000L
    private val clock: () -> Long = { currentTime }

    private fun hardwareHome(action: Int): KeyEvent {
        val downTime = currentTime
        val eventTime = currentTime
        // The (deviceId, scanCode, ...) constructor gives us direct control over scanCode.
        return KeyEvent(
            /* downTime  */ downTime,
            /* eventTime */ eventTime,
            /* action    */ action,
            /* code      */ KeyEvent.KEYCODE_HOME,
            /* repeat    */ 0,
            /* metaState */ 0,
            /* deviceId  */ 0,
            /* scancode  */ HomeKeyEventFilter.HARDWARE_HOME_SCAN_CODE,
            /* flags     */ 0
        )
    }

    private fun syntheticHome(action: Int, scanCode: Int = 0): KeyEvent = KeyEvent(
        currentTime, currentTime, action, KeyEvent.KEYCODE_HOME,
        0, 0, 0, scanCode, 0
    )

    private fun hardwareOtherKey(action: Int, keyCode: Int): KeyEvent = KeyEvent(
        currentTime, currentTime, action, keyCode,
        0, 0, 0, HomeKeyEventFilter.HARDWARE_HOME_SCAN_CODE, 0
    )

    @Test
    fun `hardware Home down and up are both consumed`() {
        val filter = HomeKeyEventFilter(nowMs = clock)

        assertTrue(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_DOWN), enabled = true))
        assertTrue(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_UP), enabled = true))
    }

    @Test
    fun `hardware Home routes exactly once, on ACTION_UP`() {
        val filter = HomeKeyEventFilter(nowMs = clock)

        assertFalse(
            "ACTION_DOWN must be consumed but must NOT trigger the route",
            filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_DOWN), enabled = true)
        )
        assertTrue(
            "ACTION_UP fires the route",
            filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_UP), enabled = true)
        )
    }

    @Test
    fun `synthesized Home events fall through completely`() {
        // scanCode == 0 is what performGlobalAction, nav bar taps, and our own
        // routeHome() follow-up look like — critical that none of them retrigger us.
        val filter = HomeKeyEventFilter(nowMs = clock)

        val down = syntheticHome(KeyEvent.ACTION_DOWN)
        val up = syntheticHome(KeyEvent.ACTION_UP)

        assertFalse(filter.shouldConsume(down, enabled = true))
        assertFalse(filter.shouldConsume(up, enabled = true))
        assertFalse(filter.shouldRouteHome(up, enabled = true))
    }

    @Test
    fun `non-home keys with the home scan code still fall through`() {
        val filter = HomeKeyEventFilter(nowMs = clock)
        val event = hardwareOtherKey(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MENU)

        assertFalse(filter.shouldConsume(event, enabled = true))
        assertFalse(filter.shouldRouteHome(event, enabled = true))
    }

    @Test
    fun `disabled flag makes the filter a pass-through`() {
        val filter = HomeKeyEventFilter(nowMs = clock)

        assertFalse(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_DOWN), enabled = false))
        assertFalse(filter.shouldConsume(hardwareHome(KeyEvent.ACTION_UP), enabled = false))
        assertFalse(filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_UP), enabled = false))
    }

    @Test
    fun `two presses within the debounce window route once`() {
        val filter = HomeKeyEventFilter(debounceMs = 300L, nowMs = clock)

        currentTime = 1_000L
        assertTrue(filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_UP), enabled = true))

        currentTime = 1_100L // 100ms later, well inside the 300ms window
        assertFalse(filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_UP), enabled = true))
    }

    @Test
    fun `two presses outside the debounce window route twice`() {
        val filter = HomeKeyEventFilter(debounceMs = 300L, nowMs = clock)

        currentTime = 1_000L
        assertTrue(filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_UP), enabled = true))

        currentTime = 1_400L // 400ms later, past the 300ms window
        assertTrue(filter.shouldRouteHome(hardwareHome(KeyEvent.ACTION_UP), enabled = true))
    }

    @Test
    fun `debounce window default is 300ms`() {
        assertEquals(300L, HomeKeyEventFilter.DEFAULT_DEBOUNCE_MS)
    }

    @Test
    fun `hardware home scan code is Linux KEY_HOME`() {
        assertEquals(102, HomeKeyEventFilter.HARDWARE_HOME_SCAN_CODE)
    }
}
