package jr.brian.home.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

@Composable
fun animatedFocusedScale(scaleIf: Boolean): Float {
    val scale by animateFloatAsState(
        targetValue = if (scaleIf) 1.05f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "scale",
    )
    return scale
}