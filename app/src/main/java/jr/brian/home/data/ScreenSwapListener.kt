package jr.brian.home.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.bus.FrontendBus
import jr.brian.core.model.bus.FrontendCommand
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import jr.brian.home.ui.util.routeHomeFromLauncher
import kotlinx.coroutines.CoroutineScope
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
        swap.beginLeaving(next)
        store.setAssignment(next)

        // Fire the two-activity launch plan with the new assignment.
        // The current activities finish themselves via normal flow;
        // the arriving activity's observer transitions SwapPhase to
        // Arriving once it hits onStart.
        routeHomeFromLauncher(context, frontendEnabled = true)
        swap.markArriving()
    }

    private companion object {
        const val TAG = "ScreenSwapListener"
    }
}
