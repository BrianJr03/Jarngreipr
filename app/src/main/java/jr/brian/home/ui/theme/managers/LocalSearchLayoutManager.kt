package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.SearchLayoutManager

val LocalSearchLayoutManager = staticCompositionLocalOf<SearchLayoutManager> {
    error("No SearchLayoutManager provided")
}
