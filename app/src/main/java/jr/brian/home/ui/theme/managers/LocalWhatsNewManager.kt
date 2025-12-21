package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.WhatsNewManager

val LocalWhatsNewManager = staticCompositionLocalOf<WhatsNewManager> {
    error("No WhatsNewManager provided")
}
