package jr.brian.core.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the single-flight, idempotent transition contract of
 * [SwapTransitionCoordinator]. The content-swap composition is
 * driven entirely by `phase` — any regression (double-trigger not
 * dropped, Idle not reached, from/to swapped) would corrupt the
 * running animation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwapTransitionCoordinatorTest {

    private val totalMs: Long =
        SwapTransitionCoordinator.SWAP_MS.toLong() +
            SwapTransitionCoordinator.SWAP_SETTLE_MS.toLong()

    @Test
    fun `starts Idle`() = runTest {
        val coordinator = SwapTransitionCoordinator()

        assertEquals(SwapTransitionPhase.Idle, coordinator.phase.first())
    }

    @Test
    fun `run enters Swapping with the from and to and returns to Idle`() = runTest {
        val coordinator = SwapTransitionCoordinator()

        val job = launch {
            coordinator.run(
                from = ScreenAssignment.FRONTEND_ON_TOP,
                to = ScreenAssignment.FRONTEND_ON_BOTTOM,
            )
        }

        advanceTimeBy(1)
        val mid = coordinator.phase.value
        assertTrue("expected Swapping, got $mid", mid is SwapTransitionPhase.Swapping)
        assertEquals(
            ScreenAssignment.FRONTEND_ON_TOP,
            (mid as SwapTransitionPhase.Swapping).from,
        )
        assertEquals(ScreenAssignment.FRONTEND_ON_BOTTOM, mid.to)

        advanceTimeBy(totalMs)
        job.join()
        assertEquals(SwapTransitionPhase.Idle, coordinator.phase.value)
    }

    @Test
    fun `second run while Swapping is dropped`() = runTest {
        val coordinator = SwapTransitionCoordinator()

        val first = launch {
            coordinator.run(
                from = ScreenAssignment.FRONTEND_ON_TOP,
                to = ScreenAssignment.FRONTEND_ON_BOTTOM,
            )
        }
        advanceTimeBy(1)
        val duringFirst = coordinator.phase.value as SwapTransitionPhase.Swapping

        coordinator.run(
            from = ScreenAssignment.FRONTEND_ON_BOTTOM,
            to = ScreenAssignment.FRONTEND_ON_TOP,
        )

        val afterSecond = coordinator.phase.value as SwapTransitionPhase.Swapping
        assertEquals(duringFirst.from, afterSecond.from)
        assertEquals(duringFirst.to, afterSecond.to)

        advanceTimeBy(totalMs)
        first.join()
    }

    @Test
    fun `run with same from and to is a no-op`() = runTest {
        val coordinator = SwapTransitionCoordinator()

        coordinator.run(
            from = ScreenAssignment.FRONTEND_ON_TOP,
            to = ScreenAssignment.FRONTEND_ON_TOP,
        )

        assertEquals(SwapTransitionPhase.Idle, coordinator.phase.value)
    }

    @Test
    fun `reset forces the phase back to Idle`() = runTest {
        val coordinator = SwapTransitionCoordinator()

        val job = launch {
            coordinator.run(
                from = ScreenAssignment.FRONTEND_ON_TOP,
                to = ScreenAssignment.FRONTEND_ON_BOTTOM,
            )
        }
        advanceTimeBy(1)
        assertTrue(coordinator.phase.value is SwapTransitionPhase.Swapping)

        coordinator.reset()

        assertEquals(SwapTransitionPhase.Idle, coordinator.phase.value)
        advanceUntilIdle()
        job.join()
    }
}
