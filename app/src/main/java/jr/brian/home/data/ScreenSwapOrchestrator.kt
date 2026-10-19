package jr.brian.home.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import jr.brian.home.ui.components.SWAP_LAYOUT_FLIP_BUFFER_MS
import jr.brian.home.ui.components.SWAP_LEAVE_MS
import jr.brian.home.ui.util.routeHomeFromLauncher
import jr.brian.home.util.ThorDetection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of the screen-swap choreography. Replaces three
 * previously-duplicated inline sequences (bus listener, L3/R3 shortcut,
 * settings row) which had drifted apart — only the bus path waited for
 * the arriving activities to signal first-frame readiness before
 * revealing them, so the two most-used triggers raced the window
 * reparent.
 *
 * Order of operations (persist-first):
 *
 *  1. Guard: Thor-only; phase is Idle or Leaving (reentry during an
 *     in-flight Leaving is tolerated so bus and shortcut can fire on
 *     the same press without one wedging the other).
 *  2. Flip and persist [ScreenAssignmentStore]. Doing this before the
 *     animation means a crash inside the fade window leaves the pref
 *     matching what the displays will show on next boot.
 *  3. Sync AYN Focus Lock to the new assignment.
 *  4. [SwapCoordinator.beginLeaving] — both activities play the fade-out.
 *  5. Wait [SWAP_LEAVE_MS] + [SWAP_LAYOUT_FLIP_BUFFER_MS] under the
 *     alpha-0 veil so row ↔ grid layout changes are invisible.
 *  6. [SwapCoordinator.resetArrivingReady], then
 *     [routeHomeFromLauncher] fires the two startActivity IPCs that
 *     reparent each task to the flipped display.
 *  7. [SwapCoordinator.awaitArrivingReady] gates on both freshly-
 *     attached activities signalling first-frame readiness, with a
 *     [ARRIVING_READY_TIMEOUT_MS] fall-through so one stuck activity
 *     can't permanently block the swap.
 *  8. Short vsync-aligned settle so both displays start the slide-in
 *     on the same frame.
 *  9. [SwapCoordinator.markArriving] — both sides animate in.
 *
 * The animation portion (steps 4 → 9) is unchanged from the previous
 * [ScreenSwapListener] implementation. PR 2 replaces it with an in-
 * process cross-fade; this consolidation lands first so every trigger
 * animates the same way and the pref write never trails the window
 * relaunch.
 */
@Singleton
class ScreenSwapOrchestrator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val swap: SwapCoordinator,
    private val store: ScreenAssignmentStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    /**
     * Fire-and-forget entry point used by every trigger (bus, L3/R3,
     * settings row). Launches on the application scope so a settings-
     * row click doesn't tie the swap to the clicked activity's
     * lifecycle (that activity is about to be reparented).
     */
    fun requestSwap() {
        if (!ThorDetection.isThor()) return
        scope.launch { runSwap() }
    }

    private suspend fun runSwap() {
        val currentPhase = swap.phase.value
        if (currentPhase !is SwapPhase.Idle && currentPhase !is SwapPhase.Leaving) {
            Log.d(TAG, "Ignoring swap: phase=$currentPhase")
            return
        }

        val next = store.assignment.flipped()

        store.setAssignment(next)
        ScreenFocusLockWriter.syncToAssignment(context, next)

        swap.beginLeaving(next)
        delay((SWAP_LEAVE_MS + SWAP_LAYOUT_FLIP_BUFFER_MS).toLong())
        swap.resetArrivingReady()
        routeHomeFromLauncher(context, frontendEnabled = true)
        val ready = swap.awaitArrivingReady(expected = 2, timeoutMs = ARRIVING_READY_TIMEOUT_MS)
        if (!ready) Log.w(TAG, "arriving-ready timed out, proceeding")
        delay(SWAP_SETTLE_MS)
        swap.markArriving()
    }

    private companion object {
        const val TAG = "ScreenSwapOrchestrator"
        const val ARRIVING_READY_TIMEOUT_MS = 400L
        const val SWAP_SETTLE_MS = 32L
    }
}
