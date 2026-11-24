package jr.brian.core.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable

/** One side of a route change: a fade, and a scale when [fromScale] and [toScale] differ. */
@Immutable
data class JarnRouteLeg(
    val delayMillis: Int,
    val durationMillis: Int,
    val easing: Easing,
    val fromScale: Float = 1f,
    val toScale: Float = 1f,
) {
    val endMillis: Int get() = delayMillis + durationMillis
    val scales: Boolean get() = fromScale != toScale

    fun <T> spec(): TweenSpec<T> =
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
}

/** The outgoing and incoming sides of one route change. */
@Immutable
data class JarnRouteChange(val exit: JarnRouteLeg, val enter: JarnRouteLeg) {
    val totalMillis: Int get() = maxOf(exit.endMillis, enter.endMillis)
}

/** A sideways step between neighbors, moving by [offsetFraction] of the width. */
@Immutable
data class JarnRouteStep(val exit: JarnRouteLeg, val enter: JarnRouteLeg, val offsetFraction: Float) {
    val totalMillis: Int get() = maxOf(exit.endMillis, enter.endMillis)
}

/** The other screen fading its content out and back in, [delayMillis] behind the active one. */
@Immutable
data class JarnRouteFollow(val delayMillis: Int, val outMillis: Int, val inMillis: Int) {
    val fadeMillis: Int get() = outMillis + inMillis
}

private const val DEPTH_SHRINK = 0.97f
private const val DEPTH_GROW = 1.03f
private const val STEP_OFFSET = 0.014f
private const val REDUCED_CROSSFADE_MILLIS = 120
private const val REDUCED_HALF_MILLIS = 60

private val InstantLeg = JarnRouteLeg(0, 0, LinearEasing)
private val ReducedCrossfade = JarnRouteLeg(0, REDUCED_CROSSFADE_MILLIS, LinearEasing)

/**
 * Systems to games (opening) and back. Opening sinks the systems and brings the games in from
 * just in front; back is the mirror. Reduced motion is a plain crossfade, and with Android
 * animations off ([instant]) nothing animates.
 */
fun jarnDepthChange(opening: Boolean, reduced: Boolean, instant: Boolean): JarnRouteChange = when {
    instant -> JarnRouteChange(InstantLeg, InstantLeg)
    reduced -> JarnRouteChange(ReducedCrossfade, ReducedCrossfade)
    opening -> JarnRouteChange(
        exit = JarnRouteLeg(0, 140, JarnEasingExit, toScale = DEPTH_SHRINK),
        enter = JarnRouteLeg(60, 220, JarnEasingEmphasized, fromScale = DEPTH_GROW),
    )
    else -> JarnRouteChange(
        exit = JarnRouteLeg(0, 120, JarnEasingExit, toScale = DEPTH_GROW),
        enter = JarnRouteLeg(40, 220, JarnEasingEmphasized, fromScale = DEPTH_SHRINK),
    )
}

/** L1 or R1 to the next system inside a game list: out, then in from the other side. */
fun jarnSystemStep(reduced: Boolean, instant: Boolean): JarnRouteStep = when {
    instant -> JarnRouteStep(InstantLeg, InstantLeg, 0f)
    reduced -> JarnRouteStep(
        exit = JarnRouteLeg(0, REDUCED_HALF_MILLIS, LinearEasing),
        enter = JarnRouteLeg(REDUCED_HALF_MILLIS, REDUCED_HALF_MILLIS, LinearEasing),
        offsetFraction = 0f,
    )
    else -> JarnRouteStep(
        exit = JarnRouteLeg(0, 80, JarnEasingExit),
        enter = JarnRouteLeg(80, 120, JarnEasingStandard),
        offsetFraction = STEP_OFFSET,
    )
}

/** The Deck behind a Stage route change, so both screens never move at once. */
fun jarnDeckFollow(reduced: Boolean, instant: Boolean): JarnRouteFollow = when {
    instant -> JarnRouteFollow(0, 0, 0)
    reduced -> JarnRouteFollow(0, REDUCED_HALF_MILLIS, REDUCED_HALF_MILLIS)
    else -> JarnRouteFollow(40, 100, 140)
}

/** The Stage header (tabs or the game list title) crossfading during a depth push. */
fun jarnHeaderSwap(reduced: Boolean, instant: Boolean): JarnRouteLeg = when {
    instant -> InstantLeg
    reduced -> ReducedCrossfade
    else -> JarnRouteLeg(40, 160, LinearEasing)
}
