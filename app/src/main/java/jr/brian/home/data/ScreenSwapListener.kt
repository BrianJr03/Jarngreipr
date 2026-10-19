package jr.brian.home.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.bus.FrontendBus
import jr.brian.core.model.bus.FrontendCommand
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import jr.brian.home.ui.components.SWAP_LAYOUT_FLIP_BUFFER_MS
import jr.brian.home.ui.components.SWAP_LEAVE_MS
import jr.brian.home.ui.util.routeHomeFromLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * :app's Phase 5 glue. Listens for [FrontendCommand.SwapScreens],
 * flips the persisted [ScreenAssignmentStore], and re-fires
 * `routeHomeFromLauncher` so the new assignment lands immediately.
 *
 * The animation choreography (plan §3.6 steps 2–4) is driven by
 * [SwapCoordinator.phase] on each activity's own side — this
 * listener only owns the persistence + routing, not the Compose
 * fade/scale animation.
 *
 * Must be started once at app boot; [JarngreiprApplication] holds
 * the only reference so GC doesn't drop it mid-swap.
 */
@Singleton
class ScreenSwapListener @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val bus: FrontendBus,
    private val swap: SwapCoordinator,
    private val store: ScreenAssignmentStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    fun start() {
        scope.launch {
            bus.commands.filterIsInstance<FrontendCommand.SwapScreens>().collect {
                handleSwap()
            }
        }
    }

    private suspend fun handleSwap() {
        if (swap.phase.first() !is SwapPhase.Idle && swap.phase.first() !is SwapPhase.Leaving) {
            Log.d(TAG, "Ignoring swap: phase=${swap.phase.first()}")
            return
        }

        val next = store.assignment.flipped()
        // Start Leaving first, then wait one fade duration before
        // flipping the pref + routing. Layout changes (row ↔ grid
        // when the user has different layouts configured per display)
        // happen while both activities are at alpha 0, so the user
        // never sees the layout transition in-frame.
        swap.beginLeaving(next)
        delay((SWAP_LEAVE_MS + SWAP_LAYOUT_FLIP_BUFFER_MS).toLong())
        swap.resetArrivingReady()
        store.setAssignment(next)
        ScreenFocusLockWriter.syncToAssignment(context, next)
        routeHomeFromLauncher(context, frontendEnabled = true)
        // Wait for both freshly-attached activities to report first-frame
        // readiness before flipping to Arriving. Without this gate,
        // markArriving() races the window reparent — the enter tween
        // consumes against a blank display and the slide-in snaps in
        // partway through. Fall through on timeout so a stuck activity
        // can't permanently block the swap.
        val ready = swap.awaitArrivingReady(expected = 2, timeoutMs = ARRIVING_READY_TIMEOUT_MS)
        if (!ready) Log.w(TAG, "arriving-ready timed out, proceeding")
        // Short vsync-aligned settle so both displays start their
        // slide-in on the same frame instead of staggering by whatever
        // the handoff happened to cost.
        delay(SWAP_SETTLE_MS)
        swap.markArriving()
    }

    private companion object {
        const val TAG = "ScreenSwapListener"
        const val ARRIVING_READY_TIMEOUT_MS = 400L
        const val SWAP_SETTLE_MS = 32L
    }
}
