package jr.brian.home.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import jr.brian.home.util.ThorDetection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single owner of the screen-swap sequence under the pinned-slot
 * model (PR 2b). Both activities stay pinned to their displays; the
 * swap flips [ScreenAssignmentStore] and the
 * [SwapCoordinator] drives the cross-fade both windows
 * observe. No window reparent, no routeHomeFromLauncher, no
 * awaitArrivingReady.
 *
 * Order of operations:
 *
 *  1. Guard: Thor-only; coordinator must be [SwapPhase.Idle].
 *  2. Flip and persist [ScreenAssignmentStore]. Crash-safe —
 *     persist first means a crash during the animation leaves the
 *     pref matching what the displays would show on next boot.
 *  3. Sync AYN Focus Lock to the new assignment.
 *  4. [SwapCoordinator.run] from old → new — enters Swapping,
 *     both windows cross-fade in sync, then returns to Idle.
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
     * row click doesn't tie the swap to the clicked composable's
     * coroutine scope.
     */
    fun requestSwap() {
        if (!ThorDetection.isThor()) return
        scope.launch { runSwap() }
    }

    private suspend fun runSwap() {
        if (swap.phase.value !is SwapPhase.Idle) {
            Log.d(TAG, "Ignoring swap: already Swapping")
            return
        }

        val from = store.assignment
        val to = from.flipped()

        store.setAssignment(to)
        ScreenFocusLockWriter.syncToAssignment(context, to)

        swap.run(from = from, to = to)
    }

    private companion object {
        const val TAG = "ScreenSwapOrchestrator"
    }
}
