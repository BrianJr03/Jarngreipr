package jr.brian.home.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import jr.brian.home.ui.theme.ThemePrimaryColor

@Composable
fun IconBox(
    isFocused: Boolean = false,
    icon: @Composable () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isFocused) ThemePrimaryColor.copy(alpha = 0.3f) else Color.Black.copy(
            alpha = 0.75f
        ),
        label = "iconBoxBackgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isFocused) ThemePrimaryColor else Color.White.copy(alpha = 0.2f),
        label = "iconBoxBorderColor"
    )

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        content = { icon() }
    )
}