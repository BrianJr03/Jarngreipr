package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.HomeButtonManager

val LocalHomeButtonManager = staticCompositionLocalOf<HomeButtonManager> {
    error("No HomeButtonManager provided")
}
