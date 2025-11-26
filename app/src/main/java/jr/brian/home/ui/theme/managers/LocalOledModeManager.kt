package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.compositionLocalOf

val LocalOledModeManager =
    compositionLocalOf<OledModeManager> {
        error("OledModeManager not provided")
    }
