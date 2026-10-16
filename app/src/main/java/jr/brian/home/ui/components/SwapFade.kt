package jr.brian.home.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import kotlinx.coroutines.delay

const val SWAP_FADE_MS = 300

/**
 * Phase 5 handoff animation. Observes [SwapCoordinator.phase]: each
 * activity's content slides toward the other display during Leaving
 * and drops back in from the far side during Arriving. The top-screen
 * activity slides down and re-enters from above; the bottom-screen
 * activity slides up and re-enters from below. Together the two
 * screens visually swap places.
 *
 * [onTopDisplay] picks the direction. The caller computes it from
 * [jr.brian.home.ui.util.rememberPhysicalAssignment] plus its own
 * role (frontend vs launcher), so during Leaving the direction
 * follows the pre-reparent position and during Arriving it follows
 * the post-reparent position.
 *
 * A non-animated black backdrop sits behind the moving content so the
 * system wallpaper never flashes through the one-frame gap between
 * leaving and arriving.
 *
 * The composable keeps its `SwapFade` name for call-site compatibility;
 * the animation is a slide now.
 */
@Composable
fun SwapFade(
    coordinator: SwapCoordinator,
    onTopDisplay: Boolean,
    content: @Composable () -> Unit,
) {
    val phase by coordinator.phase.collectAsStateWithLifecycle()
    val initialProgress = remember { if (coordinator.phase.value is SwapPhase.Arriving) 0f else 1f }
    val target = remember { mutableFloatStateOf(initialProgress) }

    // Nudge Compose's frame clock each time the lifecycle enters
    // STARTED (which fires on wake). Right after the screen comes back
    // on, the first vsync tick can arrive late because the display
    // driver and GPU are warming up — if a swap happens before that
    // tick lands, the slide stalls on its first frame. Pulling two
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
        label = "swap-slide",
    )
    // Resolve the slide distance from the current configuration so the
    // animation doesn't rely on a BoxWithConstraints re-measure every
    // frame. Right after a wake-up the GPU/display driver takes a
    // beat to settle, and the per-frame re-measure was slowing the
    // first swap. LocalConfiguration.screenHeightDp is available
    // synchronously without a layout pass.
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val heightPx = remember(density, configuration.screenHeightDp) {
        with(density) { configuration.screenHeightDp.dp.toPx() }
    }
    // Top display: content slides DOWN out of frame and the fresh
    //   copy drops back IN from above.
    // Bottom display: content slides UP out of frame and the fresh
    //   copy rises back IN from below.
    // Each activity passes its own [onTopDisplay] based on the pre-
    // reparent position during Leaving and the post-reparent
    // position during Arriving, which keeps the direction correct
    // whichever side of the swap we're on.
    val leavingDistance = if (onTopDisplay) heightPx else -heightPx
    val arrivingDistance = if (onTopDisplay) -heightPx else heightPx
    val translation = when (phase) {
        is SwapPhase.Leaving -> (1f - progress) * leavingDistance
        is SwapPhase.Arriving -> (1f - progress) * arrivingDistance
        SwapPhase.Idle -> 0f
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = translation }
        ) {
            content()
        }
    }
}
