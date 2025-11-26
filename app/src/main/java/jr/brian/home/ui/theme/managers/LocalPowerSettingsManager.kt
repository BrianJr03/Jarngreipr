package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.PowerSettingsManager

val LocalPowerSettingsManager = staticCompositionLocalOf<PowerSettingsManager> {
    error("No PowerSettingsManager provided")
}
