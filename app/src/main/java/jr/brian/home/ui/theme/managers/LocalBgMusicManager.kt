package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.BgMusicManager

val LocalBgMusicManager = staticCompositionLocalOf<BgMusicManager> {
    error("No BgMusicManager provided")
}
