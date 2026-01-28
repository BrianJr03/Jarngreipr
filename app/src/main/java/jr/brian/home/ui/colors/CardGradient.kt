package jr.brian.home.ui.colors

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import jr.brian.home.ui.theme.managers.LocalOledModeManager
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.OledCardLightColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

@Composable
fun cardGradient(
    isFocused: Boolean = false,
    isSelected: Boolean = false,
    isPressed: Boolean = false
): Brush {
    val oledManager = LocalOledModeManager.current
    val active = isFocused || isSelected || isPressed
    return if (oledManager.isOledModeEnabled) {
        Brush.linearGradient(
            colors = if (active) {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.2f),
                    ThemeSecondaryColor.copy(alpha = 0.1f)
                )
            } else {
                listOf(OledCardColor, OledCardColor)
            }
        )
    } else {
        Brush.linearGradient(
            colors = if (active) {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.9f),
                    ThemeSecondaryColor.copy(alpha = 0.9f)
                )
            } else {
                listOf(
                    ThemePrimaryColor.copy(alpha = 0.4f),
                    ThemeSecondaryColor.copy(alpha = 0.3f)
                )
            }
        )
    }
}

@Composable
fun subtleCardGradient(isFocused: Boolean): Brush {
    return Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.6f),
                ThemeSecondaryColor.copy(alpha = 0.6f)
            )
        } else {
            listOf(
                OledCardLightColor.copy(alpha = 0.5f),
                OledCardColor.copy(alpha = 0.5f)
            )
        }
    )
}

@Composable
fun emptyStateGradient(isFocused: Boolean): Brush {
    return Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.9f),
                ThemeSecondaryColor.copy(alpha = 0.9f)
            )
        } else {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.4f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        }
    )
}
