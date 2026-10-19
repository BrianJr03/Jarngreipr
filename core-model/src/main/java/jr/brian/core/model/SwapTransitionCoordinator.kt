package jr.brian.core.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * State owner for the pinned-slot content-swap animation (PR 2 of
 * the seamless-swap work). Both host activities render both
 * [SlotContent]s simultaneously during [SwapTransitionPhase.Swapping]
 * and cross-fade off this single phase so the two displays stay in
 * lockstep without needing a shared frame clock.
 *
 * This class is intentionally kept alongside the legacy
 * [SwapCoordinator] during PR 2a — the Compose glue in `:app`
 * targets this new type, nothing else references it yet, and PR 2b
 * will collapse the two by renaming this type to `SwapCoordinator`
 * and deleting the window-reparent choreography the old class owns.
 */
@Singleton
class SwapTransitionCoordinator @Inject constructor() {

    private val _phase = MutableStateFlow<SwapTransitionPhase>(SwapTransitionPhase.Idle)
    val phase: StateFlow<SwapTransitionPhase> = _phase.asStateFlow()

    /**
     * Enters [SwapTransitionPhase.Swapping] for [SWAP_MS] +
     * [SWAP_SETTLE_MS], then returns to Idle. The settle buffer
     * lets `animateFloatAsState` tween converge before the phase
     * flips back, so there's no one-frame flicker at the tail.
     *
     * Idempotent: a second [run] while already Swapping is dropped
     * (not queued), and a [run] with equal `from == to` is a no-op.
     */
    suspend fun run(from: ScreenAssignment, to: ScreenAssignment) {
        if (_phase.value !is SwapTransitionPhase.Idle) return
        if (from == to) return
        _phase.value = SwapTransitionPhase.Swapping(from, to)
        delay(SWAP_MS.toLong() + SWAP_SETTLE_MS.toLong())
        _phase.value = SwapTransitionPhase.Idle
    }

    /**
     * Force-reset to Idle. For the game-launch path, which finishes
     * the top host mid-swap — the coordinator must land back at
     * Idle so the next swap can begin.
     */
    fun reset() {
        _phase.value = SwapTransitionPhase.Idle
    }

    companion object {
        /** Duration of the content cross-fade. */
        const val SWAP_MS = 320

        /**
         * Grace period after [SWAP_MS] before transitioning back to
         * Idle, so the Compose tween commits its final value under
         * the final [SwapTransitionPhase.Swapping] frame.
         */
        const val SWAP_SETTLE_MS = 50
    }
}

/**
 * Phase observed by the content-swap composition. Idle while nothing
 * is happening; Swapping with both the previous and next
 * [ScreenAssignment] for the duration of the cross-fade.
 */
sealed interface SwapTransitionPhase {
    data object Idle : SwapTransitionPhase

    data class Swapping(
        val from: ScreenAssignment,
        val to: ScreenAssignment,
    ) : SwapTransitionPhase
}
