package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.GridSettingsManager

val LocalGridSettingsManager = staticCompositionLocalOf<GridSettingsManager> {
    error("No GridSettingsManager provided")
}
