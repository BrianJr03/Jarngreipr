package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.AppDrawerFabManager

val LocalAppDrawerFabManager = staticCompositionLocalOf<AppDrawerFabManager> {
    error("No AppDrawerFabManager provided")
}
