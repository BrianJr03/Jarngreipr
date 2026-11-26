package jr.brian.jarnfe.library

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnTheme

private val BACKDROP_BLUR = 28.dp
private const val BACKDROP_DIM_ALPHA = 0.45f

/**
 * One scene's art behind a fitted preview. Set by the scene's own player, so a preview on one
 * screen never blurs the other screen's art. View state only: never saved and never backed up.
 */
@Stable
internal class NowVideoBackdrop {
    var blurred by mutableStateOf(false)
}

/** Called by the preview player with whether a fitted video is on screen over the art. */
@Composable
internal fun NowVideoBackdropReport(backdrop: NowVideoBackdrop?, blurred: Boolean) {
    if (backdrop == null) return
    DisposableEffect(backdrop, blurred) {
        backdrop.blurred = blurred
        onDispose { backdrop.blurred = false }
    }
}

/**
 * Blurs and dims the art so the bars around a fitted video read as part of it. Reuses the held
 * art, so it costs no second decoder. minSdk 33 always has RenderEffect blur.
 */
@Composable
internal fun Modifier.nowVideoBackdrop(blurred: Boolean): Modifier {
    val motion = JarnTheme.motion
    val amount by animateFloatAsState(
        targetValue = if (blurred) 1f else 0f,
        animationSpec = nowBackdropSpec(motion, blurred),
        label = "NowVideoBackdrop",
    )
    if (amount == 0f) return this
    val dim = JarnTheme.colors.bg.copy(alpha = BACKDROP_DIM_ALPHA * amount)
    return blur(BACKDROP_BLUR * amount, BlurredEdgeTreatment.Rectangle)
        .drawWithContent {
            drawContent()
            drawRect(dim)
        }
}

/** Reduced motion jumps straight to the blur, so the art never visibly swims. */
private fun nowBackdropSpec(motion: JarnMotion, blurred: Boolean): AnimationSpec<Float> = when {
    motion.reduced -> snap()
    blurred -> motion.videoReveal.spec()
    else -> motion.focusOut.spec()
}
