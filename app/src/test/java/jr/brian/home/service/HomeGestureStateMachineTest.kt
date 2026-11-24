package jr.brian.home.service

import jr.brian.home.service.HomeGestureStateMachine.Gesture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Covers the three gesture paths the hardware Home interceptor exposes:
 *
 *  - single press  → BOTTOM
 *  - double press  → TOP
 *  - long press    → BOTH
 *
 * The service drives the state machine with two timers (long-press + double-
 * tap window); these tests simulate those timer ticks directly, so timing
 * never enters the pure logic under test.
 */
class HomeGestureStateMachineTest {

    @Test
    fun `single press — long-press timer loses the race, double-tap window elapses`() {
        val sm = HomeGestureStateMachine()
        sm.onDown()
        assertNull(sm.onUp())                     // first tap, waits for a second
        assertNull(sm.onLongPressTimer())         // service cancels in practice, but
                                                   // a late firing must not route anything
        assertEquals(Gesture.SINGLE_PRESS, sm.onDoubleTapTimer())
    }

    @Test
    fun `double press — second UP arrives inside the double-tap window`() {
        val sm = HomeGestureStateMachine()
        sm.onDown()
        assertNull(sm.onUp())                     // first tap pending
        sm.onDown()
        assertEquals(Gesture.DOUBLE_PRESS, sm.onUp())
        assertNull(sm.onDoubleTapTimer())         // nothing pending by now
    }

    @Test
    fun `long press — timer fires while the user is still holding`() {
        val sm = HomeGestureStateMachine()
        sm.onDown()
        assertEquals(Gesture.LONG_PRESS, sm.onLongPressTimer())
        // The UP that eventually arrives must not accidentally schedule a single-press.
        assertNull(sm.onUp())
        assertNull(sm.onDoubleTapTimer())
    }

    @Test
    fun `long-press timer fires AFTER the up is a no-op`() {
        val sm = HomeGestureStateMachine()
        sm.onDown()
        sm.onUp()                                 // tap released before 500ms
        assertNull(sm.onLongPressTimer())         // service should have cancelled, but
                                                   // a race-firing must stay silent
    }

    @Test
    fun `two separated single presses each produce SINGLE_PRESS`() {
        val sm = HomeGestureStateMachine()

        sm.onDown()
        sm.onUp()
        assertEquals(Gesture.SINGLE_PRESS, sm.onDoubleTapTimer())

        sm.onDown()
        sm.onUp()
        assertEquals(Gesture.SINGLE_PRESS, sm.onDoubleTapTimer())
    }

    @Test
    fun `UP without a prior DOWN never routes a long-press`() {
        // Service drops UP events on an unmatched hold (e.g. service bound mid-press),
        // but if one did sneak through the state machine must stay silent.
        val sm = HomeGestureStateMachine()
        assertNull(sm.onLongPressTimer())
        assertNull(sm.onDoubleTapTimer())
    }
}
