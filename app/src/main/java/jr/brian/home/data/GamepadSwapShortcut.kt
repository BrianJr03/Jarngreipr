package jr.brian.home.data

import android.content.Context
import android.view.KeyEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.SwapCoordinator
import jr.brian.home.ui.components.SWAP_FADE_MS
import jr.brian.home.ui.util.routeHomeFromLauncher
import jr.brian.home.util.ThorDetection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * L3 or R3 (either thumbstick click) fires a screen swap on Thor
 * hardware. Matches the same action the "Swap screens" row in
 * Frontend Settings → Dual-screen performs — flips
 * [ScreenAssignmentStore] and re-fires the two-activity launch plan.
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
     * Run the full swap choreography: begin the Leaving phase (so
     * SwapFade animates out), flip [ScreenAssignmentStore], re-fire
     * the launch plan, then transition to Arriving (so the new
     * activities animate in). Same sequence [ScreenSwapListener]
     * runs — this just shortcuts the FrontendBus hop for callers
     * that already live inside :app. Thor-only — a single-display
     * device has no "other" screen for the swap to land on.
     */
    fun fireSwap(context: Context) {
        if (!ThorDetection.isThor()) return
        val entryPoint = EntryPointAccessors
            .fromApplication(context.applicationContext, SwapShortcutEntryPoint::class.java)
        val store = entryPoint.screenAssignmentStore()
        val coordinator = entryPoint.swapCoordinator()
        val scope = entryPoint.applicationScope()
        val next = store.assignment.flipped()
        // Start the Leaving fade first, then wait a full fade duration
        // before flipping the pref + routing. That means the layout
        // change happens while both activities are at alpha 0, so the
        // user never sees the row ↔ grid transition in-frame.
        coordinator.beginLeaving(next)
        scope.launch {
            delay(SWAP_FADE_MS.toLong())
            store.setAssignment(next)
            ScreenFocusLockWriter.syncToAssignment(context, next)
            routeHomeFromLauncher(context, frontendEnabled = true)
            coordinator.markArriving()
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    internal interface SwapShortcutEntryPoint {
        fun screenAssignmentStore(): ScreenAssignmentStore
        fun swapCoordinator(): SwapCoordinator
        @ApplicationScope fun applicationScope(): CoroutineScope
    }
}
