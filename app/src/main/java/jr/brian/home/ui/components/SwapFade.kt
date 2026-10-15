package jr.brian.home.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import kotlinx.coroutines.delay

const val SWAP_FADE_MS = 180

/**
 * Phase 5 handoff animation. Observes [SwapCoordinator.phase]:
 * fades out during Leaving (just before the activity finishes itself
 * as part of the swap), fades back in during Arriving on the new
 * display, and signals [SwapCoordinator.markArrived] once the enter
 * animation completes so the coordinator returns to Idle.
 *
 * Shared between legacy's `FrontEndActivity` and the launcher's
 * `MainActivity` so both sides play the same ~180ms fade in sync.
 * Dropping it into a new activity's root `setContent { … }` is
 * sufficient — no lifecycle wiring beyond [SwapCoordinator] injection.
 */
@Composable
fun SwapFade(
    coordinator: SwapCoordinator,
    content: @Composable () -> Unit,
) {
    val phase by coordinator.phase.collectAsStateWithLifecycle()
    val initialAlpha = remember { if (coordinator.phase.value is SwapPhase.Arriving) 0f else 1f }
    val targetAlpha = remember { mutableFloatStateOf(initialAlpha) }
    LaunchedEffect(phase) {
        targetAlpha.floatValue = when (phase) {
            is SwapPhase.Leaving -> 0f
            else -> 1f
        }
        if (phase is SwapPhase.Arriving) {
            delay(SWAP_FADE_MS.toLong())
            coordinator.markArrived()
        }
    }
    val animated by animateFloatAsState(
        targetValue = targetAlpha.floatValue,
        animationSpec = tween(SWAP_FADE_MS),
        label = "swap-fade",
    )
    Box(modifier = Modifier.fillMaxSize().alpha(animated)) {
        content()
    }
}
