package jr.brian.home.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.core.model.ScreenSlot
import jr.brian.core.model.SlotContent
import jr.brian.core.model.SwapTransitionCoordinator
import jr.brian.core.model.SwapTransitionPhase
import jr.brian.home.data.ScreenAssignmentStore

/**
 * Root composable for a host activity under the pinned-slot swap
 * model (PR 2). Supplies [LocalScreenSlot] to descendants so layout
 * branches read a static slot instead of peeking at the current
 * display, composes the [SwapTransition] cross-fade, and swallows
 * input while the coordinator is in [SwapTransitionPhase.Swapping]
 * so taps mid-animation can't reach a composable that is partway
 * through fading out.
 *
 * PR 2a (this change) is additive — nothing in :app calls this yet.
 * PR 2b will swap both activity `setContent` blocks over.
 */
@Composable
fun ScreenSlotHost(
    slot: ScreenSlot,
    coordinator: SwapTransitionCoordinator,
    store: ScreenAssignmentStore,
    launcherContent: @Composable () -> Unit,
    frontendContent: @Composable () -> Unit,
) {
    val assignment by store.state.collectAsStateWithLifecycle()
    val phase by coordinator.phase.collectAsStateWithLifecycle()
    val swapping = phase is SwapTransitionPhase.Swapping

    CompositionLocalProvider(LocalScreenSlot provides slot) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (swapping) Modifier.consumeAllPointerEvents() else Modifier),
        ) {
            SwapTransition(
                coordinator = coordinator,
                slot = slot,
                assignment = assignment,
            ) { which ->
                when (which) {
                    SlotContent.Launcher -> launcherContent()
                    SlotContent.Frontend -> frontendContent()
                }
            }
        }
    }
}

/**
 * Eats every pointer change on the Initial pass so a tap landing
 * mid-swap can't commit against the alpha-fading composable
 * underneath. The gate only applies while this modifier is in the
 * tree (toggled off when the swap finishes).
 */
private fun Modifier.consumeAllPointerEvents(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
        }
    }
}
