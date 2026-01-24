package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.RecentAppsCacheManager

val LocalRecentAppsCacheManager = staticCompositionLocalOf<RecentAppsCacheManager> {
    error("No RecentAppsCacheManager provided")
}
