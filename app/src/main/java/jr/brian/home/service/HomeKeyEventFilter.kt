package jr.brian.home.service

import android.view.KeyEvent

/**
 * Match / consume predicate for the accessibility-based hardware Home
 * interceptor. Routing decisions (single / double / long press) live in
 * [HomeGestureStateMachine] — this filter only decides whether an event
 * belongs to the hardware Home button at all.
 *
 * Match criteria: `keyCode == KEYCODE_HOME && scanCode == 102`. The scanCode
 * check separates the *physical* KEY_HOME button (Linux input event code 102)
 * from synthesized Home events emitted by the nav bar, `performGlobalAction`,
 * or our own [jr.brian.home.ui.util.routeHome] follow-up, and so prevents
 * feedback loops.
 */
internal class HomeKeyEventFilter {

    /**
     * @param enabled the current value of the user toggle. When false the
     * filter never consumes, so Home behaves normally.
     */
    fun shouldConsume(event: KeyEvent, enabled: Boolean): Boolean {
        if (!enabled) return false
        return matches(event)
    }

    private fun matches(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.KEYCODE_HOME && event.scanCode == HARDWARE_HOME_SCAN_CODE

    companion object {
        /** Linux `KEY_HOME` scan code — the value the Thor firmware surfaces. */
        const val HARDWARE_HOME_SCAN_CODE = 102
    }
}
