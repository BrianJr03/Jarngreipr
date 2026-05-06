package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.PinnedRomManager

val LocalPinnedRomManager = staticCompositionLocalOf<PinnedRomManager> {
    error("PinnedRomManager not provided")
}
