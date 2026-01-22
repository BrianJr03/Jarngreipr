package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.AppUpdateManager

val LocalAppUpdateManager = staticCompositionLocalOf<AppUpdateManager> {
    error("No AppUpdateManager provided")
}
