package jr.brian.jarnfe.shell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState
import kotlin.math.PI
import kotlin.math.sin

internal const val JARN_SWITCH_BORDER_MILLIS = 300
private val SwitchBorderWidth = 6.dp

/** Rises and falls once, so the border is gone when the animation ends. */
internal fun jarnSwitchBorderAlpha(progress: Float): Float =
    sin(PI.toFloat() * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)

/** Outlines the screen that just received control. */
@Composable
internal fun JarnScreenSwitchBorder(screen: JarnScreen, state: JarnSessionState) {
    if (!state.deckDisplayAvailable || JarnTheme.motion.reduced) return
    val isLit = state.isLit(screen)
    val accent = JarnTheme.colors.accent
    val progress = remember { Animatable(1f) }
    val wasLit = remember { booleanArrayOf(isLit) }
    LaunchedEffect(isLit) {
        val received = isLit && !wasLit[0]
        wasLit[0] = isLit
        if (received) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(JARN_SWITCH_BORDER_MILLIS, easing = LinearEasing))
        } else if (!isLit) {
            progress.snapTo(1f)
        }
    }
    Spacer(
        Modifier
            .fillMaxSize()
            .drawBehind {
                val value = progress.value
                if (value < 1f) {
                    val width = SwitchBorderWidth.toPx()
                    drawRect(
                        color = accent,
                        topLeft = Offset(width / 2f, width / 2f),
                        size = Size(size.width - width, size.height - width),
                        alpha = jarnSwitchBorderAlpha(value),
                        style = Stroke(width),
                    )
                }
            },
    )
}
