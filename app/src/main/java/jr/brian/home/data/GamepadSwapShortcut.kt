package jr.brian.home.data

import android.content.Context
import android.view.KeyEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * L3 or R3 (either thumbstick click) fires a screen swap on Thor
 * hardware. Matches the same action the "Swap screens" row in
 * Frontend Settings → Dual-screen performs — both route through
 * [ScreenSwapOrchestrator], so every trigger animates identically.
 *
 * Both [jr.brian.home.MainActivity] and
 * [jr.brian.home.esde.ui.FrontEndActivity] route their
 * `dispatchKeyEvent` through [handle], so the shortcut works
 * regardless of which display has focus. Both activities also
 * consume L3 and R3 unconditionally (even on non-Thor devices) so
 * nothing else in the app can bind them — pressing either button
 * never leaks into a composable's own key handler.
 */
object GamepadSwapShortcut {

    /**
     * Returns true when [event] is an L3 or R3 press — either fires
     * [fire] (on Thor via `ACTION_DOWN`) or silently consumes the
     * key to keep the rest of the app from binding it. Activities
     * must return the result from their `dispatchKeyEvent` so the
     * press is consumed before any composable sees it.
     */
    fun handle(event: KeyEvent, fire: () -> Unit): Boolean {
        val code = event.keyCode
        if (code != KeyEvent.KEYCODE_BUTTON_THUMBL &&
            code != KeyEvent.KEYCODE_BUTTON_THUMBR
        ) return false

        // Fire once per press on ACTION_DOWN with no repeats so a
        // held click doesn't re-fire the swap on the gamepad driver's
        // repeat cadence. ACTION_UP is swallowed too so nothing else
        // can react to the key — it's reserved for the shortcut app-wide.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            fire()
        }
        return true
    }

    /**
     * Delegates to [ScreenSwapOrchestrator.requestSwap]. The orchestrator
     * owns the guards (Thor-only, phase-not-busy) so this stays a thin
     * entry point.
     */
    fun fireSwap(context: Context) {
        val entryPoint = EntryPointAccessors
            .fromApplication(context.applicationContext, SwapShortcutEntryPoint::class.java)
        entryPoint.screenSwapOrchestrator().requestSwap()
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface SwapShortcutEntryPoint {
        fun screenSwapOrchestrator(): ScreenSwapOrchestrator
    }
}
