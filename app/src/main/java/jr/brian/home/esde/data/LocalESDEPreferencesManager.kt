package jr.brian.home.esde.data

import androidx.compose.runtime.staticCompositionLocalOf

val LocalESDEPreferencesManager = staticCompositionLocalOf<ESDEPreferencesManager> {
    error("ESDEPreferencesManager not provided")
}
