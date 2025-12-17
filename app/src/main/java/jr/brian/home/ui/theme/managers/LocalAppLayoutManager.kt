package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.AppLayoutManager

val LocalAppLayoutManager = staticCompositionLocalOf<AppLayoutManager> {
    error("No AppLayoutManager provided")
}
