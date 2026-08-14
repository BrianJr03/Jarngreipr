package jr.brian.home.service

import android.view.KeyEvent

/**
 * Pure decision logic for the accessibility-based hardware Home interceptor.
 *
 * Kept separate from [HomeInterceptorService] so it can be unit-tested without
 * pulling in the full AccessibilityService lifecycle.
 *
 * Contract used by the service:
 * - [shouldConsume]  → return value of `onKeyEvent`. Consuming BOTH ACTION_DOWN
 *   and ACTION_UP is required; some ROMs act on DOWN when only UP is swallowed.
 * - [shouldRouteHome] → true on the ACTION_UP that follows a matching ACTION_DOWN,
 *   and only if the debounce window has elapsed since the last routed press.
 *
 * Match criteria: `keyCode == KEYCODE_HOME && scanCode == 102`. The scanCode check
 * separates the *physical* KEY_HOME button (Linux input event code 102) from
 * synthesized Home events emitted by the nav bar, `performGlobalAction`, or our
 * own [jr.brian.home.ui.util.routeHome] follow-up, and so prevents feedback loops.
 */
internal class HomeKeyEventFilter(
    private val debounceMs: Long = DEFAULT_DEBOUNCE_MS,
    private val nowMs: () -> Long = { System.currentTimeMillis() }
) {
    private var lastRoutedAtMs: Long = 0L

    /**
     * @param enabled the current value of the user toggle. When false the filter
     * never consumes and never routes, so Home behaves normally.
     */
    fun shouldConsume(event: KeyEvent, enabled: Boolean): Boolean {
        if (!enabled) return false
        return matches(event)
    }

    /**
     * @return true iff [event] is the ACTION_UP of a real hardware Home press
     * outside the debounce window. Callers should invoke [routeHome] immediately
     * after seeing `true` here so [lastRoutedAtMs] moves forward before the next
     * press is evaluated.
     */
    fun shouldRouteHome(event: KeyEvent, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (event.action != KeyEvent.ACTION_UP) return false
        if (!matches(event)) return false
        val now = nowMs()
        if (now - lastRoutedAtMs < debounceMs) return false
        lastRoutedAtMs = now
        return true
    }

    private fun matches(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.KEYCODE_HOME && event.scanCode == HARDWARE_HOME_SCAN_CODE

    companion object {
        /** Linux `KEY_HOME` scan code — the value the Thor firmware surfaces. */
        const val HARDWARE_HOME_SCAN_CODE = 102
        const val DEFAULT_DEBOUNCE_MS = 300L
    }
}
