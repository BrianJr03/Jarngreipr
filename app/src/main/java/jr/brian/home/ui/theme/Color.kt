package jr.brian.home.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import jr.brian.home.ui.theme.managers.LocalOledModeManager
import jr.brian.home.ui.theme.managers.LocalThemeManager

val AppRed = Color(0xFFE94560)
val AppBlue = Color(0xFF0F3460)

val AppDarkBlue = Color(0xFF1A1A2E)
val AppBackgroundDark = Color(0xFF0A0E27)
val AppCardDark = Color(0xFF1E1E2E)

val AppCardLight = Color(0xFF16213E)

val ThemeBlack = Color(0xFF000000)
val AlignmentGuideColor = Color(0xFFFFD700) // Gold/Yellow color for alignment guides

@Composable
fun themePrimaryColor(): Color = LocalThemeManager.current.currentTheme.primaryColor

@Composable
fun themeSecondaryColor(): Color = LocalThemeManager.current.currentTheme.secondaryColor

@Composable
fun themeAccentColor(): Color = LocalThemeManager.current.currentTheme.lightTextColor

val ThemePrimaryColor @Composable get() = themePrimaryColor()
val ThemeSecondaryColor @Composable get() = themeSecondaryColor()
val ThemeAccentColor @Composable get() = themeAccentColor()

@Composable
fun oledBackgroundColor(): Color {
    val oledManager = LocalOledModeManager.current
    return if (oledManager.isOledModeEnabled) ThemeBlack else AppBackgroundDark
}

@Composable
fun oledCardColor(): Color {
    val oledManager = LocalOledModeManager.current
    return if (oledManager.isOledModeEnabled) ThemeBlack else AppCardDark
}

@Composable
fun oledCardLightColor(): Color {
    val oledManager = LocalOledModeManager.current
    return if (oledManager.isOledModeEnabled) ThemeBlack else AppCardLight
}

val OledBackgroundColor @Composable get() = oledBackgroundColor()
val OledCardColor @Composable get() = oledCardColor()
val OledCardLightColor @Composable get() = oledCardLightColor()