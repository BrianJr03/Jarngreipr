package jr.brian.jarnfe.transition

import androidx.compose.animation.core.Easing
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.jarnfe.input.JarnScreen
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One window's Jarn label moving from [from] to [to] presence, where 1 rests in the header and 0
 * is fully gone. Every value is pure so the handoff can be tested.
 */
internal data class JarnMarkLeg(
    val from: Float,
    val to: Float,
    val delayMillis: Int,
    val durationMillis: Int,
    val easing: Easing,
) {
    val totalMillis: Int get() = delayMillis + durationMillis

    fun presenceAt(elapsedMillis: Float): Float {
        if (durationMillis == 0) return if (elapsedMillis >= delayMillis) to else from
        val raw = ((elapsedMillis - delayMillis) / durationMillis).coerceIn(0f, 1f)
        return from + (to - from) * easing.transform(raw)
    }
}

internal object JarnMarkHandoffTimeline {
    /**
     * Starts wherever the label is now, so an interrupted handoff turns around without snapping.
     * Time and the arrival's wait shrink with the distance left to cover.
     */
    fun leg(from: Float, arriving: Boolean, motion: JarnMotion): JarnMarkLeg {
        val to = if (arriving) 1f else 0f
        val token = if (arriving) motion.markArrive else motion.markLeave
        val remaining = abs(to - from)
        val delay = if (arriving) motion.markArriveDelayMillis * remaining else 0f
        return JarnMarkLeg(
            from = from,
            to = to,
            delayMillis = delay.roundToInt(),
            durationMillis = (token.durationMillis * remaining).roundToInt(),
            easing = token.easing,
        )
    }

    /**
     * How open the label's slot is, so the header row slides over to fill it. It closes with the
     * leaving label and opens across the whole arrival, landing with the label. Reduced motion
     * snaps it once the label has faded out, or before the label fades in.
     */
    fun slotLeg(from: Float, arriving: Boolean, motion: JarnMotion): JarnMarkLeg {
        val to = if (arriving) 1f else 0f
        val remaining = abs(to - from)
        val token = if (arriving) motion.markArrive else motion.markLeave
        val span = token.durationMillis + if (arriving) motion.markArriveDelayMillis else 0
        val delay = if (motion.reduced && !arriving) motion.markLeave.durationMillis * remaining else 0f
        return JarnMarkLeg(
            from = from,
            to = to,
            delayMillis = delay.roundToInt(),
            durationMillis = if (motion.reduced) 0 else (span * remaining).roundToInt(),
            easing = token.easing,
        )
    }

    /** Offset from rest in [JarnMotion.markTravel] units, positive downward. */
    fun travel(presence: Float, towardOtherScreen: Float): Float = (1f - presence) * towardOtherScreen
}

/** The label waits on Stage while the splash docks it, then follows control. */
internal fun jarnMarkHolder(cue: JarnSwitchCue, splashIntro: Boolean, activeScreen: JarnScreen): JarnScreen =
    if (cue == JarnSwitchCue.JarnLabel && !splashIntro) activeScreen else JarnScreen.Stage

/**
 * 1 moves down, toward the bottom screen, and -1 moves up. Stage is the top screen unless the
 * roles are swapped, so the label always travels toward the physical screen it is heading for.
 */
internal fun jarnMarkTowardOtherScreen(role: JarnScreen, rolesSwapped: Boolean): Float {
    val onTop = (role == JarnScreen.Stage) != rolesSwapped
    return if (onTop) 1f else -1f
}
