package jr.brian.home.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.core.model.ScreenAssignment
import jr.brian.core.model.ScreenSlot
import jr.brian.core.model.SlotContent
import jr.brian.core.model.SwapCoordinator
import jr.brian.core.model.SwapPhase
import jr.brian.core.model.contentFor
import kotlin.math.PI
import kotlin.math.sin

/**
 * The cross-fade shown during a seamless screen swap. Both the
 * outgoing and incoming [SlotContent] are composed simultaneously
 * while the coordinator sits in [SwapPhase.Swapping];
 * input is blocked at [ScreenSlotHost] above so the user can't tap
 * into a composable that is partway through fading out.
 *
 * Motion is "shared axis through the hinge" (plan §2.4): the
 * outgoing layer translates toward the hinge (bottom edge for the
 * [ScreenSlot.TOP], top edge for the [ScreenSlot.BOTTOM]) while
 * scaling and fading out; the incoming layer starts on the hinge
 * side and converges to centre as it fades in. A short black scrim
 * sits between the two layers so the midpoint reads as a smooth
 * hand-off instead of a flash.
 *
 * ### Why the content is wrapped in [movableContentOf]
 *
 * Idle and Swapping are different branches of a `when`, and the
 * outgoing and incoming layers are different children of the
 * Swapping branch. Without [movableContentOf], each phase change
 * disposes the content in the old slot and composes it from scratch
 * in the new one: a single swap composed the incoming screen
 * **three** times per window (incoming layer, then again in the Idle
 * branch) and the outgoing screen twice, with every `remember`,
 * ViewModel collector and image request restarted each time. On a
 * full [jr.brian.home.esde.ui.frontend.FrontendScreen] that was the
 * transient heap spike that tipped the Thor's 256 MB heap over.
 * Moving the composition instead means one new composition per
 * swap per window, and the content keeps its state across the
 * animation.
 */
@Composable
fun SwapTransition(
    coordinator: SwapCoordinator,
    slot: ScreenSlot,
    assignment: ScreenAssignment,
    launcherContent: @Composable () -> Unit,
    frontendContent: @Composable () -> Unit,
) {
    val phase by coordinator.phase.collectAsStateWithLifecycle()

    // rememberUpdatedState so the hosts can pass fresh lambdas
    // (they capture activity-level flags such as navigateToThemeShare)
    // without the movable content being re-created — re-creating it
    // would defeat the whole point.
    val latestLauncher by rememberUpdatedState(launcherContent)
    val latestFrontend by rememberUpdatedState(frontendContent)
    val launcher = remember { movableContentOf { latestLauncher() } }
    val frontend = remember { movableContentOf { latestFrontend() } }
    val render: @Composable (SlotContent) -> Unit = { which ->
        when (which) {
            SlotContent.Launcher -> launcher()
            SlotContent.Frontend -> frontend()
        }
    }

    val progress by animateFloatAsState(
        targetValue = if (phase is SwapPhase.Swapping) 1f else 0f,
        animationSpec = tween(
            durationMillis = SwapCoordinator.SWAP_MS,
            easing = FastOutSlowInEasing,
        ),
        label = "swap-progress",
    )

    when (val p = phase) {
        SwapPhase.Idle -> {
            Box(modifier = Modifier.fillMaxSize()) {
                render(contentFor(slot, assignment))
            }
        }
        is SwapPhase.Swapping -> {
            val outgoing = contentFor(slot, p.from)
            val incoming = contentFor(slot, p.to)
            val hingeSign = if (slot == ScreenSlot.TOP) 1f else -1f

            Box(modifier = Modifier.fillMaxSize()) {
                SlotLayer(
                    translationFraction = SlotFraction.outgoing(progress) * hingeSign,
                    scale = SlotScale.outgoing(progress),
                    alpha = SlotAlpha.outgoing(progress),
                ) {
                    render(outgoing)
                }
                Scrim(progress)
                SlotLayer(
                    translationFraction = SlotFraction.incoming(progress) * hingeSign,
                    scale = SlotScale.incoming(progress),
                    alpha = SlotAlpha.incoming(progress),
                ) {
                    render(incoming)
                }
            }
        }
    }
}

@Composable
private fun SlotLayer(
    translationFraction: Float,
    scale: Float,
    alpha: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.translationY = translationFraction * size.height
            },
    ) {
        content()
    }
}

@Composable
private fun Scrim(progress: Float) {
    val scrimAlpha = (SCRIM_PEAK * sin(PI * progress)).toFloat().coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha)),
    )
}

private const val SCRIM_PEAK = 0.35

/** Per-layer per-channel progression. The 60% overlap is from plan §2.4. */
private object SlotFraction {
    private const val MAX = 0.12f
    fun outgoing(progress: Float): Float =
        (progress / 0.6f).coerceIn(0f, 1f) * MAX
    fun incoming(progress: Float): Float =
        (1f - ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)) * MAX
}

private object SlotScale {
    private const val MIN = 0.94f
    fun outgoing(progress: Float): Float =
        1f - ((progress / 0.6f).coerceIn(0f, 1f)) * (1f - MIN)
    fun incoming(progress: Float): Float =
        MIN + ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f) * (1f - MIN)
}

private object SlotAlpha {
    fun outgoing(progress: Float): Float =
        1f - (progress / 0.6f).coerceIn(0f, 1f)
    fun incoming(progress: Float): Float =
        ((progress - 0.4f) / 0.6f).coerceIn(0f, 1f)
}
