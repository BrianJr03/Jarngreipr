package jr.brian.home.ui.util

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun animatedColor(
    firstSeen: Boolean = true,
    fallbackColor: Color = Color.White
) : Color {
    val romIconTransition = rememberInfiniteTransition(label = "romIconGradient")
    val romIconColor1 by romIconTransition.animateColor(
        initialValue = ThemePrimaryColor,
        targetValue = ThemeAccentColor,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "romIconColor1"
    )
    val romIconColor2 by romIconTransition.animateColor(
        initialValue = ThemeAccentColor,
        targetValue = ThemeSecondaryColor,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "romIconColor2"
    )
    return if (firstSeen) {
        lerp(romIconColor1, romIconColor2, 0.5f)
    } else {
        fallbackColor
    }
}