package jr.brian.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.JarnEasingEmphasized
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val PAGE_ENTER_MS = 240
private const val PAGE_FADE_REDUCED_MS = 120
private val PageEnterShift = 24.dp

/**
 * Plays [transition] each time [page] changes, from the side the new page came from. Reduce
 * motion makes every preset a short fade, and Android animations off makes it instant.
 */
@Composable
fun rememberJarnPageEnter(page: Int, transition: JarnPageTransition): Modifier {
    val motion = JarnTheme.motion
    val shiftPx = with(LocalDensity.current) { PageEnterShift.toPx() }
    val current by rememberUpdatedState(transition)
    val alpha = remember { Animatable(1f) }
    val shift = remember { Animatable(0f) }
    val travel = remember { Animatable(0f) }
    var shown by remember { mutableIntStateOf(page) }
    var direction by remember { mutableIntStateOf(0) }
    LaunchedEffect(page) {
        direction = (page - shown).coerceIn(-1, 1)
        shown = page
        if (direction == 0 || motion.instant) {
            alpha.snapTo(1f)
            shift.snapTo(0f)
            travel.snapTo(0f)
            return@LaunchedEffect
        }
        val preset = if (motion.reduced) JarnPageTransition.Fade else current
        if (motion.reduced || preset == JarnPageTransition.JarnSlide) {
            travel.snapTo(0f)
            playJarnSlide(alpha, shift, if (motion.reduced) 0f else shiftPx * direction, motion.reduced)
        } else {
            alpha.snapTo(1f)
            shift.snapTo(0f)
            travel.snapTo(1f)
            travel.animateTo(0f, tween(PAGE_ENTER_MS, easing = JarnEasingEmphasized))
        }
    }
    return remember(alpha, shift, travel) {
        Modifier.graphicsLayer {
            val progress = travel.value
            val preset = if (progress > 0f) current else null
            val layer = preset?.arrivingLayer(-direction * progress, size.width, size.height, density)
            if (layer != null) {
                layer.applyTo(this)
            } else {
                this.alpha = alpha.value
                translationX = shift.value
            }
        }
    }
}

private suspend fun playJarnSlide(alpha: Animatable<Float, *>, shift: Animatable<Float, *>, fromShift: Float, reduced: Boolean) {
    alpha.snapTo(0f)
    shift.snapTo(fromShift)
    coroutineScope {
        launch {
            alpha.animateTo(
                1f,
                if (reduced) tween(PAGE_FADE_REDUCED_MS, easing = LinearEasing) else tween(PAGE_ENTER_MS, easing = JarnEasingEmphasized),
            )
        }
        launch { shift.animateTo(0f, tween(PAGE_ENTER_MS, easing = JarnEasingEmphasized)) }
    }
}
