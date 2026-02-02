package jr.brian.home.esde.preferences

import androidx.compose.runtime.staticCompositionLocalOf

val LocalESDEPreferencesManager = staticCompositionLocalOf<ESDEPreferencesManager> {
    error("ESDEPreferencesManager not provided")
}
