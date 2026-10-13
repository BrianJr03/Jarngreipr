package jr.brian.home.service

/**
 * Pure gesture detector for the hardware Home button. Translates a sequence
 * of `ACTION_DOWN` / `ACTION_UP` events (plus two timer callbacks owned by
 * [HomeInterceptorService]) into a single [Gesture] value.
 *
 * Kept state-only — the service wires the two timers (`Handler.postDelayed`)
 * and feeds the resulting ticks back into [onLongPressTimer] /
 * [onDoubleTapTimer]. That split keeps this class unit-testable without
 * pulling in `Looper` or Robolectric handler scheduling.
 *
 * The three gestures are translated by the service's routing layer into:
 *
 *  - [Gesture.SINGLE_PRESS] → [jr.brian.home.model.HomeTarget.BOTTOM]
 *    (launcher on the bottom display)
 *  - [Gesture.DOUBLE_PRESS] → FrontEndActivity launched on the primary (top)
 *    display, bypassing [jr.brian.home.model.HomeTarget] so MainActivity
 *    (which is `singleTask`) is not yanked off the bottom display
 *  - [Gesture.LONG_PRESS]   → [jr.brian.home.model.HomeTarget.BOTH]
 *    (launcher + frontend on both displays, with focus governed by the
 *    user's main-screen setting)
 */
internal class HomeGestureStateMachine {
    enum class Gesture { SINGLE_PRESS, DOUBLE_PRESS, LONG_PRESS }

    private var holding: Boolean = false
    private var longPressFired: Boolean = false
    private var pendingSingleTap: Boolean = false

    /** Reports a hardware Home `ACTION_DOWN`. */
    fun onDown() {
        holding = true
        longPressFired = false
    }

    /**
     * The long-press timer scheduled by the service has elapsed. Returns
     * [Gesture.LONG_PRESS] when the user is still holding the button;
     * `null` when the UP has already fired (short tap — timer lost the race).
     */
    fun onLongPressTimer(): Gesture? {
        if (!holding) return null
        longPressFired = true
        holding = false
        pendingSingleTap = false
        return Gesture.LONG_PRESS
    }

    /**
     * Reports a hardware Home `ACTION_UP`. Returns:
     *  - [Gesture.DOUBLE_PRESS] when this UP closes a tap already waiting in
     *    the double-tap window.
     *  - `null` when the preceding long-press timer has already routed, or
     *    when this is a first tap and the service should schedule the
     *    double-tap window (and later call [onDoubleTapTimer]).
     */
    fun onUp(): Gesture? {
        holding = false
        if (longPressFired) {
            longPressFired = false
            return null
        }
        if (pendingSingleTap) {
            pendingSingleTap = false
            return Gesture.DOUBLE_PRESS
        }
        pendingSingleTap = true
        return null
    }

    /**
     * The double-tap window scheduled after a first tap has elapsed with no
     * second tap. Returns [Gesture.SINGLE_PRESS] when a tap is still pending;
     * `null` when it was already consumed by a double-tap in the meantime.
     */
    fun onDoubleTapTimer(): Gesture? {
        if (!pendingSingleTap) return null
        pendingSingleTap = false
        return Gesture.SINGLE_PRESS
    }

    companion object {
        /**
         * Standard Android long-press timeout. Matches
         * `ViewConfiguration.getLongPressTimeout()` so a hold that would feel
         * like a long press anywhere else in the system feels the same here.
         */
        const val LONG_PRESS_MS = 500L

        /** Window within which a second tap is treated as a double press. */
        const val DOUBLE_TAP_WINDOW_MS = 300L
    }
}
