package jr.brian.home.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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