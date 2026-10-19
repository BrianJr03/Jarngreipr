package jr.brian.home.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import kotlinx.coroutines.delay

const val SWAP_LEAVE_MS = 150
const val SWAP_ARRIVE_MS = 300

// Buffer between beginLeaving and the layout-flip call so the overlay
// has actually rendered its final alpha=1 frame before the assignment
// changes. animateFloatAsState adds one vsync of state propagation on
// top of the tween, so delaying by this plus [SWAP_LEAVE_MS] keeps
// the row ↔ grid flip hidden behind the opaque overlay.
const val SWAP_LAYOUT_FLIP_BUFFER_MS = 48

/**
 * Phase 5 handoff animation. Observes [SwapCoordinator.phase] and
 * animates an opaque black overlay on TOP of [content] during a swap:
 *
 * - **Leaving** — overlay fades in to full black over [SWAP_LEAVE_MS].
 *   Once opaque, the orchestrator flips the screen assignment and
 *   reparents the activities. Any layout change on the content below
 *   is invisible because the overlay fully covers it.
 * - **Arriving** — overlay fades back out over [SWAP_ARRIVE_MS],
 *   revealing the settled new content.
 *
 * This is more robust than animating the content's own alpha: a
 * dropped frame or a vsync-timing slip during the leave animation
 * can't leak the layout flip, because the overlay sits above the
 * content and reaches full opacity before anything underneath moves.
 */
@Composable
fun SwapFade(
    coordinator: SwapCoordinator,
    content: @Composable () -> Unit,
) {
    val phase by coordinator.phase.collectAsStateWithLifecycle()
    val composedDuringLeaving = remember { coordinator.phase.value is SwapPhase.Leaving }

    // If this SwapFade was composed while the swap was already in
    // its Leaving window, we're the freshly-attached arriving
    // activity. Signal first-frame readiness so the orchestrator
    // can defer markArriving() until both displays actually have
    // content to draw — otherwise Arriving fires during the dead
    // reparent gap and the overlay reveal starts mid-tween.
    LaunchedEffect(composedDuringLeaving) {
        if (composedDuringLeaving) {
            withFrameNanos { }
            coordinator.notifyArrivingReady()
        }
    }

    // Nudge Compose's frame clock each time the lifecycle enters
    // STARTED (which fires on wake). Right after the screen comes back
    // on, the first vsync tick can arrive late because the display
    // driver and GPU are warming up — if a swap happens before that
    // tick lands, the overlay stalls on its first frame. Pulling two
    // frames here gets Choreographer running before the user has a
    // chance to trigger the first post-wake swap.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withFrameNanos { }
            withFrameNanos { }
        }
    }
    LaunchedEffect(phase) {
        if (phase is SwapPhase.Arriving) {
            delay(SWAP_ARRIVE_MS.toLong())
            coordinator.markArrived()
        }
    }
    val targetOverlayAlpha = if (phase is SwapPhase.Leaving) 1f else 0f
    val overlayAlpha by animateFloatAsState(
        targetValue = targetOverlayAlpha,
        animationSpec = tween(
            durationMillis = if (phase is SwapPhase.Leaving) SWAP_LEAVE_MS else SWAP_ARRIVE_MS,
            easing = if (phase is SwapPhase.Leaving) LinearEasing else FastOutSlowInEasing,
        ),
        label = "swap-overlay",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = overlayAlpha }
                .background(Color.Black)
        )
    }
}
