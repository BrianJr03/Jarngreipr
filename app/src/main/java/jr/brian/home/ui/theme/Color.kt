package jr.brian.home.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppRed = Color(0xFFE94560)
val AppBlue = Color(0xFF0F3460)

val AppDarkBlue = Color(0xFF1A1A2E)
val AppBackgroundDark = Color(0xFF0A0E27)
val AppCardDark = Color(0xFF1E1E2E)

val AppCardLight = Color(0xFF16213E)

@Composable
fun themePrimaryColor(): Color = LocalThemeManager.current.currentTheme.primaryColor

@Composable
fun themeSecondaryColor(): Color = LocalThemeManager.current.currentTheme.secondaryColor

@Composable
fun themeAccentColor(): Color = LocalThemeManager.current.currentTheme.lightTextColor

val ThemePrimaryColor @Composable get() = themePrimaryColor()
val ThemeSecondaryColor @Composable get() = themeSecondaryColor()
val ThemeAccentColor @Composable get() = themeAccentColor()