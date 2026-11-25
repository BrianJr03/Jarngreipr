package jr.brian.jarnfe.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.splash.JarnSplash

/** Drawing only, so a label on its way never holds back input on either screen. */
@Stable
internal class JarnMarkPresence(initial: Float) {
    var value by mutableFloatStateOf(initial)
        private set

    /** Each window runs its own leg from the same switch. A new switch cancels this one mid-leg. */
    suspend fun moveTo(arriving: Boolean, motion: JarnMotion) {
        val leg = JarnMarkHandoffTimeline.leg(value, arriving, motion)
        if (leg.totalMillis == 0) {
            value = leg.to
            return
        }
        val startedAt = withFrameMillis { it }
        var done = false
        while (!done) {
            withFrameMillis { now ->
                val elapsed = (now - startedAt).toFloat()
                value = leg.presenceAt(elapsed)
                done = elapsed >= leg.totalMillis
            }
        }
    }
}

/** [follows] is true while the Jarn label cue is on, so the Deck keeps a slot for the label. */
@Stable
internal class JarnMarkHandoff(
    val follows: Boolean,
    val presence: JarnMarkPresence,
    val towardOtherScreen: Float,
) {
    companion object {
        val Resting = JarnMarkHandoff(follows = false, presence = JarnMarkPresence(1f), towardOtherScreen = 1f)
    }
}

internal val LocalJarnMarkHandoff = staticCompositionLocalOf { JarnMarkHandoff.Resting }

@Composable
internal fun rememberJarnMarkHandoff(screen: JarnScreen, cue: JarnSwitchCue, state: JarnSessionState): JarnMarkHandoff {
    val motion = JarnTheme.motion
    val holds = jarnMarkHolder(cue, JarnSplash.isIntro, state.activeScreen) == screen
    val presence = remember(screen) { JarnMarkPresence(if (holds) 1f else 0f) }
    LaunchedEffect(presence, holds, motion) { presence.moveTo(holds, motion) }
    val follows = cue == JarnSwitchCue.JarnLabel
    val toward = jarnMarkTowardOtherScreen(screen, state.rolesSwapped)
    return remember(presence, follows, toward) { JarnMarkHandoff(follows, presence, toward) }
}

internal fun Modifier.jarnMarkHandoff(handoff: JarnMarkHandoff, travelPx: Float): Modifier = graphicsLayer {
    val presence = handoff.presence.value
    alpha = presence
    translationY = JarnMarkHandoffTimeline.travel(presence, handoff.towardOtherScreen) * travelPx
}
