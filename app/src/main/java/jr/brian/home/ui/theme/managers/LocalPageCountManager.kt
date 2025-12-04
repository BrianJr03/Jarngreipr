package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.PageCountManager

val LocalPageCountManager = staticCompositionLocalOf<PageCountManager> {
    error("No PageCountManager provided")
}
