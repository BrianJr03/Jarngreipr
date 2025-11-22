package jr.brian.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun borderBrush(
    isFocused: Boolean,
    colors: List<Color> = listOf(ThemePrimaryColor, ThemeSecondaryColor),
) = Brush.linearGradient(
    colors = if (isFocused) colors else listOf(Color.Transparent, Color.Transparent),
)