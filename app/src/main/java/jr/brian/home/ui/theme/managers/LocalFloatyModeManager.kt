package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.FloatyModeManager

val LocalFloatyModeManager = staticCompositionLocalOf<FloatyModeManager> {
    error("No FloatyModeManager provided")
}
