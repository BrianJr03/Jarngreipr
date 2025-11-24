package jr.brian.home.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.AppDisplayPreferenceManager

val LocalAppDisplayPreferenceManager = staticCompositionLocalOf<AppDisplayPreferenceManager> {
    error("No AppDisplayPreferenceManager provided")
}
