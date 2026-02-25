package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.GameKonfettiManager

val LocalGameKonfettiManager = staticCompositionLocalOf<GameKonfettiManager> {
    error("No GameKonfettiManager provided")
}
