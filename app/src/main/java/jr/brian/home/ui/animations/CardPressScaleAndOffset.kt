package jr.brian.home.ui.animations

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun onPressScaleAndOffset(isPressed: Boolean) : Pair<Float, Dp> {
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "pressScale"
    )
    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = tween(durationMillis = 100),
        label = "offsetY"
    )
    return Pair(pressScale, offsetY)
}