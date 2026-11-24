package jr.brian.jarnfe.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private const val LEAVE_MILLIS = 150
private const val ARRIVE_DELAY_MILLIS = 110L
private const val ARRIVE_MILLIS = 190
private const val BAND_HEIGHT_FRACTION = 0.34f
private const val BAND_ALPHA = 0.55f
private const val OFFSCREEN_MARGIN = 0.2f
private const val LEAVE_TRAVEL = 0.7f
private const val ARRIVE_TRAVEL = 1.4f

/** The band's center as a fraction of screen height. It always travels through the hinge. */
internal fun jarnSweepCenter(progress: Float, leaving: Boolean, isTopWindow: Boolean): Float = when {
    leaving && isTopWindow -> 0.5f + LEAVE_TRAVEL * progress
    leaving -> 0.5f - LEAVE_TRAVEL * progress
    isTopWindow -> 1f + OFFSCREEN_MARGIN - ARRIVE_TRAVEL * progress
    else -> -OFFSCREEN_MARGIN + ARRIVE_TRAVEL * progress
}

internal fun jarnSweepAlpha(progress: Float, leaving: Boolean): Float =
    BAND_ALPHA * if (leaving) 1f - progress else sin(PI.toFloat() * progress)

/** A band of accent light that leaves one screen and enters the other as control moves. */
@Composable
internal fun JarnScreenSwitchSweep(screen: JarnScreen, state: JarnSessionState) {
    if (!state.deckDisplayAvailable || JarnTheme.motion.reduced) return
    val isLit = state.isLit(screen)
    val isTopWindow = state.roleOn(JarnScreen.Stage) == screen
    val accent = JarnTheme.colors.accent
    val progress = remember { Animatable(1f) }
    val wasLit = remember { booleanArrayOf(isLit) }
    var leaving by remember { mutableStateOf(false) }
    LaunchedEffect(isLit) {
        if (wasLit[0] == isLit) return@LaunchedEffect
        wasLit[0] = isLit
        leaving = !isLit
        progress.snapTo(0f)
        if (isLit) delay(ARRIVE_DELAY_MILLIS)
        val duration = if (isLit) ARRIVE_MILLIS else LEAVE_MILLIS
        progress.animateTo(1f, tween(duration, easing = FastOutSlowInEasing))
    }
    Spacer(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val value = progress.value
                if (value < 1f) drawSweepBand(accent, value, leaving, isTopWindow)
            },
    )
}

private fun DrawScope.drawSweepBand(accent: Color, progress: Float, leaving: Boolean, isTopWindow: Boolean) {
    val bandHeight = size.height * BAND_HEIGHT_FRACTION
    val top = size.height * jarnSweepCenter(progress, leaving, isTopWindow) - bandHeight / 2f
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, accent, Color.Transparent),
            startY = top,
            endY = top + bandHeight,
        ),
        topLeft = Offset(0f, top),
        size = Size(size.width, bandHeight),
        alpha = jarnSweepAlpha(progress, leaving),
    )
}
