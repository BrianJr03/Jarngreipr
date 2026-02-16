package jr.brian.home.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import jr.brian.home.ui.theme.AlmostBlack
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor

/**
 * Shared color options used across the app for consistent theming.
 * Used by Dock settings, FAB settings, and other customizable components.
 */
object ColorOptions {
    /**
     * Standard color palette for UI elements like dock, FAB, etc.
     * Includes theme colors and common grayscale options.
     */
    @Composable
    fun standardColors(): List<Color> = listOf(
        ThemePrimaryColor,
        ThemeSecondaryColor,
        Color.White,
        Color.LightGray,
        Color.DarkGray,
        AlmostBlack,
        Color.Black,
        Color.Transparent
    )
}
