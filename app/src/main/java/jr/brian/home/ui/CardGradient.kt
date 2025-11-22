package jr.brian.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import jr.brian.home.ui.theme.AppCardDark
import jr.brian.home.ui.theme.AppCardLight
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun cardGradient(isFocused: Boolean): Brush =
    Brush.linearGradient(
        colors =
            if (isFocused) {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.8f),
                    ThemeSecondaryColor.copy(alpha = 0.6f),
                )
            } else {
                listOf(
                    AppCardLight,
                    AppCardDark,
                )
            },
    )