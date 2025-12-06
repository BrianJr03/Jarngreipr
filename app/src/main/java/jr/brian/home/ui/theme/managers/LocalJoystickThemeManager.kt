package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.compositionLocalOf
import jr.brian.home.data.JoystickThemeManager

val LocalJoystickThemeManager =
    compositionLocalOf<JoystickThemeManager> {
        error("JoystickThemeManager not provided")
    }
