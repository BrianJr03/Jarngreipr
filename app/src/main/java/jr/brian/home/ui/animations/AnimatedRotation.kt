package jr.brian.home.ui.animations

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun animatedRotation(
    isFocused: Boolean,
    durationMillis: Int = 800,
): Float {
    var rotationCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            rotationCount++
        }
    }

    val rotation by animateFloatAsState(
        targetValue = rotationCount * 360f,
        animationSpec =
            tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing,
            ),
        label = "rotation",
    )
    return rotation
}

@Composable
fun animatedFlip(
    isFocused: Boolean,
    durationMillis: Int = 800
): Float {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, animationSpec = tween(durationMillis, easing = EaseInOutCubic))
        } else {
            rotation.snapTo(0f)
        }
    }

    return rotation.value
}