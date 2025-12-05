package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.PageTypeManager

val LocalPageTypeManager = staticCompositionLocalOf<PageTypeManager> {
    error("No PageTypeManager provided")
}
