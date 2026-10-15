package jr.brian.core.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lives in :core-model (plan §3.6). Both activities observe
 * [phase] during a screen-swap handoff so each can play its
 * leave/enter animation in sync without importing the other
 * module.
 *
 * ### Lifecycle
 *
 * 1. The initiator calls [requestSwap] on this coordinator or
 *    emits `FrontendCommand.SwapScreens` on the bus. Both routes
 *    end up invoking [beginLeaving].
 * 2. Both activities observe `phase = Leaving` and play the
 *    ~180ms scale/fade-out animation on their content (wallpaper
 *    stays per plan).
 * 3. Each side calls [planHomeLaunches] with the flipped
 *    [ScreenAssignment] and starts the other activity on its own
 *    display, then finishes itself. The planner is already Phase 5-
 *    aware from the HomeLaunchPlanner changes.
 * 4. Arriving activities read `phase = Arriving` and play the
 *    mirrored enter animation, then call [markArrived] which
 *    returns [SwapPhase] to Idle.
 *
 * This class is deliberately dumb: it owns the state transitions
 * but doesn't start any activities. The real orchestration lives
 * in :app's Phase 5 glue (which also persists the new assignment
 * via [ScreenAssignmentStore]).
 */
@Singleton
class SwapCoordinator @Inject constructor() {

    private val _phase = MutableStateFlow<SwapPhase>(SwapPhase.Idle)
    val phase: StateFlow<SwapPhase> = _phase.asStateFlow()

    /** Idempotent — a second call during Leaving / Arriving is a no-op. */
    fun beginLeaving(targetAssignment: ScreenAssignment) {
        if (_phase.value !is SwapPhase.Idle) return
        _phase.value = SwapPhase.Leaving(targetAssignment)
    }

    /**
     * Called by the new side after it starts drawing. Transitions
     * from Leaving → Arriving so the arriving activity's observer
     * can play its enter animation.
     */
    fun markArriving() {
        val leaving = _phase.value as? SwapPhase.Leaving ?: return
        _phase.value = SwapPhase.Arriving(leaving.targetAssignment)
    }

    /** Called by the arriving activity once its enter animation ends. */
    fun markArrived() {
        if (_phase.value is SwapPhase.Arriving) {
            _phase.value = SwapPhase.Idle
        }
    }

    /**
     * Resets to Idle in response to a game-launch or user cancel.
     * The arriving animation may have been skipped; the state must
     * still land back at Idle so the next swap can begin.
     */
    fun reset() {
        _phase.value = SwapPhase.Idle
    }
}

/** Observable state the choreography depends on. */
sealed interface SwapPhase {
    data object Idle : SwapPhase

    /** Both sides play the leave animation. */
    data class Leaving(val targetAssignment: ScreenAssignment) : SwapPhase

    /** Both sides play the enter animation. */
    data class Arriving(val targetAssignment: ScreenAssignment) : SwapPhase
}
