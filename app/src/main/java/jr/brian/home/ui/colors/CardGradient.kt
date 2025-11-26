package jr.brian.home.ui.colors

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import jr.brian.home.ui.theme.LocalOledModeManager
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun cardGradient(isFocused: Boolean): Brush {
    val oledManager = LocalOledModeManager.current

    return if (oledManager.isOledModeEnabled) {
        // In OLED mode, use pure black with white accents when focused
        Brush.linearGradient(
            colors = if (isFocused) {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.2f),
                    ThemeSecondaryColor.copy(alpha = 0.1f),
                )
            } else {
                listOf(
                    OledCardColor,
                    OledCardColor,
                )
            }
        )
    } else {
        // Normal mode with original behavior
        Brush.linearGradient(
            colors = if (isFocused) {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.8f),
                    ThemeSecondaryColor.copy(alpha = 0.6f),
                )
            } else {
                listOf(
                    OledCardLightColor,
                    OledCardColor,
                )
            }
        )
    }
}