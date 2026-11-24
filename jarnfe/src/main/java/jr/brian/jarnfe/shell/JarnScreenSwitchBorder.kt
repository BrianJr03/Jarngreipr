package jr.brian.jarnfe.shell

import android.graphics.Matrix
import android.graphics.SweepGradient
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSessionState

internal const val JARN_SWITCH_RING_MILLIS = 1500
private const val RING_ROTATION_DEGREES = 540f
private const val RING_FADE_FRACTION = 0.2f
private const val RING_MIDDLE_ALPHA = 0.8f
private val RingStops = floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f)
private val RingWidth = 4.dp

/** Fades in and out at the ends, so the ring never pops on or off. */
internal fun jarnSwitchRingAlpha(progress: Float): Float = when {
    progress <= 0f || progress >= 1f -> 0f
    progress < RING_FADE_FRACTION -> progress / RING_FADE_FRACTION
    progress > 1f - RING_FADE_FRACTION -> (1f - progress) / RING_FADE_FRACTION
    else -> 1f
}

internal fun jarnSwitchRingRotation(progress: Float): Float = progress * RING_ROTATION_DEGREES

/** A gradient that flows around the edge of the screen that just received control. */
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
            progress.animateTo(1f, tween(JARN_SWITCH_RING_MILLIS, easing = LinearEasing))
        } else if (!isLit) {
            progress.snapTo(1f)
        }
    }
    Spacer(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val clear = accent.copy(alpha = 0f).toArgb()
                val solid = accent.toArgb()
                val middle = accent.copy(alpha = RING_MIDDLE_ALPHA).toArgb()
                val colors = intArrayOf(clear, solid, middle, solid, clear)
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val shader = SweepGradient(centerX, centerY, colors, RingStops)
                val brush = ShaderBrush(shader)
                val rotation = Matrix()
                val width = RingWidth.toPx()
                val stroke = Stroke(width)
                val inset = Offset(width / 2f, width / 2f)
                val ringSize = Size(size.width - width, size.height - width)
                onDrawBehind {
                    val alpha = jarnSwitchRingAlpha(progress.value)
                    if (alpha > 0f) {
                        rotation.setRotate(jarnSwitchRingRotation(progress.value), centerX, centerY)
                        shader.setLocalMatrix(rotation)
                        drawRect(brush, inset, ringSize, alpha, stroke)
                    }
                }
            },
    )
}
