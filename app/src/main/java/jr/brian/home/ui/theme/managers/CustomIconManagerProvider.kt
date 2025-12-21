package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.CustomIconManager

val LocalCustomIconManager = staticCompositionLocalOf<CustomIconManager> {
    error("CustomIconManager not provided")
}
