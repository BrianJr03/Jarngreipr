package jr.brian.home.data

import jr.brian.core.model.ApplicationScope
import jr.brian.core.model.bus.FrontendBus
import jr.brian.core.model.bus.FrontendCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges [FrontendCommand.SwapScreens] on the [FrontendBus] to
 * [ScreenSwapOrchestrator]. The orchestrator owns the actual
 * choreography — this listener only exists so the frontend module
 * (which doesn't link against :app) can request a swap over the bus.
 *
 * Must be started once at app boot; [jr.brian.home.JarngreiprApplication]
 * holds the only reference so GC doesn't drop it mid-collect.
 */
@Singleton
class ScreenSwapListener @Inject constructor(
    private val bus: FrontendBus,
    private val orchestrator: ScreenSwapOrchestrator,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    fun start() {
        scope.launch {
            bus.commands.filterIsInstance<FrontendCommand.SwapScreens>().collect {
                orchestrator.requestSwap()
            }
        }
    }
}
