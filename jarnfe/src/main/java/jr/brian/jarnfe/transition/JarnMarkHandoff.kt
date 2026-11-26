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
import androidx.compose.ui.layout.layout
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.splash.JarnSplash
import kotlin.math.roundToInt

/**
 * [value] is drawn only, so a label on its way never holds back input on either screen. [slot]
 * changes layout, so once it settles the tabs sit and take touches exactly where they show.
 */
@Stable
internal class JarnMarkPresence(initial: Float) {
    var value by mutableFloatStateOf(initial)
        private set
    var slot by mutableFloatStateOf(initial)
        private set

    /** Each window runs its own legs from the same switch. A new switch cancels these mid-leg. */
    suspend fun moveTo(arriving: Boolean, motion: JarnMotion) {
        val label = JarnMarkHandoffTimeline.leg(value, arriving, motion)
        val row = JarnMarkHandoffTimeline.slotLeg(slot, arriving, motion)
        val total = maxOf(label.totalMillis, row.totalMillis)
        if (total == 0) {
            value = label.to
            slot = row.to
            return
        }
        val startedAt = withFrameMillis { it }
        var done = false
        while (!done) {
            withFrameMillis { now ->
                val elapsed = (now - startedAt).toFloat()
                value = label.presenceAt(elapsed)
                slot = row.presenceAt(elapsed)
                done = elapsed >= total
            }
        }
    }
}

/** [follows] is true with two screens, so the Deck keeps a slot for the label. */
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
internal fun rememberJarnMarkHandoff(screen: JarnScreen, twoScreens: Boolean, state: JarnSessionState): JarnMarkHandoff {
    val motion = JarnTheme.motion
    val holds = jarnMarkHolder(twoScreens, JarnSplash.isIntro, state.activeScreen) == screen
    val presence = remember(screen) { JarnMarkPresence(if (holds) 1f else 0f) }
    LaunchedEffect(presence, holds, motion) { presence.moveTo(holds, motion) }
    val follows = twoScreens
    val toward = jarnMarkTowardOtherScreen(screen, state.rolesSwapped)
    return remember(presence, follows, toward) { JarnMarkHandoff(follows, presence, toward) }
}

/** Reports the label plus [gapPx] scaled by how open the slot is, keeping the row's height. */
internal fun Modifier.jarnMarkSlot(handoff: JarnMarkHandoff, gapPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val width = ((placeable.width + gapPx) * handoff.presence.slot).roundToInt()
    layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), placeable.height) {
        placeable.placeRelative(0, 0)
    }
}

internal fun Modifier.jarnMarkHandoff(handoff: JarnMarkHandoff, travelPx: Float): Modifier = graphicsLayer {
    val presence = handoff.presence.value
    alpha = presence
    translationY = JarnMarkHandoffTimeline.travel(presence, handoff.towardOtherScreen) * travelPx
}
