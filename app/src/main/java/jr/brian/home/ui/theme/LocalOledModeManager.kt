package jr.brian.home.ui.theme

import androidx.compose.runtime.compositionLocalOf

val LocalOledModeManager =
    compositionLocalOf<OledModeManager> {
        error("OledModeManager not provided")
    }
