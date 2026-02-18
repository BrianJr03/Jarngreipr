package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.CustomAppNameManager

val LocalCustomAppNameManager = staticCompositionLocalOf<CustomAppNameManager> {
    error("CustomAppNameManager not provided")
}
