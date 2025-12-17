package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.IconPackManager

val LocalIconPackManager = staticCompositionLocalOf<IconPackManager> {
    error("No IconPackManager provided")
}
