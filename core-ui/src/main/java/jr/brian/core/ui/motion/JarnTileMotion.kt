package jr.brian.core.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.core.ui.theme.LocalJarnMotion

private const val TILE_ENTER_FROM_SCALE = 0.95f

/** Presses inside [content] use the tile press: 0.96 over 100ms, none with Reduce motion. */
@Composable
fun JarnTilePress(content: @Composable () -> Unit) {
    val motion = JarnTheme.motion
    val tileMotion = remember(motion) { motion.copy(tilePress = true) }
    CompositionLocalProvider(LocalJarnMotion provides tileMotion, content = content)
}

/** A one-time fade and slight grow when an item first appears, without a bounce. Off with Reduce motion. */
@Composable
fun rememberJarnTileEnter(): Modifier {
    val motion = JarnTheme.motion
    val progress = remember { Animatable(if (motion.reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (motion.reduced) progress.snapTo(1f) else progress.animateTo(1f, motion.small.spec())
    }
    return remember(progress) {
        Modifier.graphicsLayer {
            val value = progress.value
            val scale = lerp(TILE_ENTER_FROM_SCALE, 1f, value)
            alpha = value
            scaleX = scale
            scaleY = scale
        }
    }
}
