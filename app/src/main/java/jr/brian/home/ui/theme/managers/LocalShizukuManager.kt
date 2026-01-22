package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.ShizukuManager

val LocalShizukuManager = staticCompositionLocalOf<ShizukuManager> {
    error("No ShizukuManager provided")
}
