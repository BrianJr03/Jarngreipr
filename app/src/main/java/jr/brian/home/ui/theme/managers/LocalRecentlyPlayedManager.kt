package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.RecentlyPlayedManager

val LocalRecentlyPlayedManager = staticCompositionLocalOf<RecentlyPlayedManager> {
    error("RecentlyPlayedManager not provided")
}
