package jr.brian.core.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

val JarnEasingStandard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val JarnEasingEmphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val JarnEasingExit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

private const val REDUCED_FADE_MILLIS = 120

@Immutable
data class JarnMotionToken(val durationMillis: Int, val easing: Easing) {
    fun <T> spec(delayMillis: Int = 0): TweenSpec<T> =
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
}

private val Instant = JarnMotionToken(0, LinearEasing)
private val ReducedFade = JarnMotionToken(REDUCED_FADE_MILLIS, LinearEasing)

/**
 * [reduced] follows the in-app toggle or Android animations being off, for UI movement.
 * [ambientReduced] follows only the in-app toggle, for ambient motion such as live wallpapers
 * and GIFs, which many handheld players keep while turning Android animations off.
 */
@Immutable
data class JarnMotion(val reduced: Boolean = false, val ambientReduced: Boolean = reduced) {
    val focusIn = if (reduced) Instant else JarnMotionToken(90, JarnEasingStandard)
    val focusOut = if (reduced) Instant else JarnMotionToken(70, JarnEasingExit)
    val press = if (reduced) Instant else JarnMotionToken(80, JarnEasingStandard)
    val small = if (reduced) Instant else JarnMotionToken(160, JarnEasingStandard)
    val panel = if (reduced) ReducedFade else JarnMotionToken(240, JarnEasingEmphasized)
    val screen = if (reduced) ReducedFade else JarnMotionToken(320, JarnEasingEmphasized)
    val art = if (reduced) ReducedFade else JarnMotionToken(400, LinearEasing)

    val pressScale: Float = if (reduced) 1f else 0.97f
    val depthOffset: Dp = if (reduced) 0.dp else 24.dp
    val deckFollowDelayMillis: Int = if (reduced) 0 else 40
    val artDebounceMillis: Int = 150
    val animateNowPlaying: Boolean = !reduced

    fun panelEnter(offsetPx: Int): EnterTransition {
        val fade = fadeIn(panel.spec())
        return if (reduced) fade else fade + slideInVertically(panel.spec<IntOffset>()) { offsetPx }
    }

    fun panelExit(offsetPx: Int): ExitTransition {
        val fade = fadeOut(panel.spec())
        return if (reduced) fade else fade + slideOutVertically(panel.spec<IntOffset>()) { offsetPx }
    }
}

fun jarnShouldReduceMotion(inAppToggle: Boolean, animatorDurationScale: Float): Boolean =
    inAppToggle || animatorDurationScale == 0f
