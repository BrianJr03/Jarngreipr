package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.HomeTabManager

val LocalHomeTabManager = staticCompositionLocalOf<HomeTabManager> {
    error("No HomeTabManager provided")
}
