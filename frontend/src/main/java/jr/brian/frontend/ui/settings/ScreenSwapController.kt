package jr.brian.frontend.ui.settings

import android.content.Context
import jr.brian.core.model.ScreenAssignment
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.bus.FrontendBus
import jr.brian.core.model.bus.FrontendCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fires a screen-swap. Publishes [FrontendCommand.SwapScreens] on
 * the bus AND nudges [SwapCoordinator] into Leaving — the launcher
 * side (which owns [jr.brian.home.data.ScreenAssignmentStore])
 * listens to the bus, persists the flipped assignment, and runs the
 * routing. The actual choreography lives in :app's Phase 5 glue; the
 * frontend just asks.
 *
 * Phase 5 ships the "ask" path; the animation choreography is a
 * follow-up that reads [SwapCoordinator.phase] from both sides.
 */
@Singleton
class ScreenSwapController @Inject constructor(
    private val bus: FrontendBus,
    private val swap: SwapCoordinator,
) {
    /**
     * Called from the UI. The actual assignment transition is
     * computed on the launcher side so this call doesn't need to
     * know the current state — the swap listener there flips the
     * [ScreenAssignmentStore] value.
     */
    fun requestSwap(scope: CoroutineScope) {
        scope.launch {
            // The target is a placeholder; the launcher side computes
            // the real one against its store before firing routing.
            swap.beginLeaving(ScreenAssignment.DEFAULT)
            bus.sendCommand(FrontendCommand.SwapScreens)
        }
    }
}
