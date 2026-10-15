package jr.brian.home.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import kotlinx.coroutines.delay

const val SWAP_FADE_MS = 300
private const val SWAP_SCALE_LEAVING = 0.94f

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
    val initialProgress = remember { if (coordinator.phase.value is SwapPhase.Arriving) 0f else 1f }
    val target = remember { mutableFloatStateOf(initialProgress) }
    LaunchedEffect(phase) {
        target.floatValue = when (phase) {
            is SwapPhase.Leaving -> 0f
            else -> 1f
        }
        if (phase is SwapPhase.Arriving) {
            delay(SWAP_FADE_MS.toLong())
            coordinator.markArrived()
        }
    }
    val progress by animateFloatAsState(
        targetValue = target.floatValue,
        animationSpec = tween(SWAP_FADE_MS, easing = FastOutSlowInEasing),
        label = "swap-fade",
    )
    // progress 1 = fully shown, 0 = fully leaving. Alpha tracks it directly;
    // scale lerps between SWAP_SCALE_LEAVING and 1 so the content gently
    // pulls back as it fades out and settles in as it fades back in.
    val scale = SWAP_SCALE_LEAVING + (1f - SWAP_SCALE_LEAVING) * progress
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}
