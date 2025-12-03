package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.AppPositionManager

val LocalAppPositionManager = staticCompositionLocalOf<AppPositionManager> {
    error("No AppPositionManager provided")
}
