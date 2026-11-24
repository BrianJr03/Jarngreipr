package jr.brian.jarnfe.shell

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import jr.brian.core.ui.theme.JarnEasingStandard
import jr.brian.core.ui.theme.JarnMotion
import jr.brian.core.ui.theme.JarnSwitchCue
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SCREEN_DIM_ALPHA = 0.14f
private const val DIM_LIFT_MILLIS = 240
private const val DIM_SETTLE_MILLIS = 320
private const val DIM_DIRECTION_GAP_MILLIS = 170L

/** 1 rests dimmed. Needs a real second window, so a lone Stage never dims. */
internal fun jarnDimTarget(cue: JarnSwitchCue, isLit: Boolean, twoScreens: Boolean): Float =
    if (cue == JarnSwitchCue.Dim && twoScreens && !isLit) 1f else 0f

/** A tab mark would stay lit forever with no other screen to hand control to. */
internal fun jarnShownCue(cue: JarnSwitchCue, twoScreens: Boolean): JarnSwitchCue =
    if (cue == JarnSwitchCue.TabMark && !twoScreens) JarnSwitchCue.Pulse else cue

/** Limits direction changes, so fast presses can't make a whole screen flicker. */
internal class JarnDimGate(private var shown: Float, private val gapMillis: Long = DIM_DIRECTION_GAP_MILLIS) {
    private var changedAt: Long? = null

    fun waitMillis(target: Float, nowMillis: Long): Long {
        val last = changedAt
        return if (target == shown || last == null) 0L else (last + gapMillis - nowMillis).coerceAtLeast(0L)
    }

    fun commit(target: Float, nowMillis: Long) {
        if (target == shown) return
        shown = target
        changedAt = nowMillis
    }
}

private fun dimSpec(target: Float, motion: JarnMotion): AnimationSpec<Float> = when {
    motion.reduced -> motion.panel.spec()
    target > 0f -> tween(DIM_SETTLE_MILLIS, easing = JarnEasingStandard)
    else -> tween(DIM_LIFT_MILLIS, easing = JarnEasingStandard)
}

/** Separate from screen activation, which snaps to 0 the moment a screen loses control. */
@Composable
internal fun rememberJarnScreenDim(target: Float): State<Float> {
    val motion = JarnTheme.motion
    val dim = remember { Animatable(target) }
    val gate = remember { JarnDimGate(target) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(target) {
        delay(gate.waitMillis(target, SystemClock.uptimeMillis()))
        gate.commit(target, SystemClock.uptimeMillis())
        scope.launch { dim.animateTo(target, dimSpec(target, motion)) }
    }
    return remember(dim) { dim.asState() }
}

internal fun Modifier.jarnScreenDim(dim: State<Float>): Modifier = drawWithContent {
    drawContent()
    val amount = dim.value
    if (amount > 0f) drawRect(Color.Black, alpha = amount * SCREEN_DIM_ALPHA)
}
