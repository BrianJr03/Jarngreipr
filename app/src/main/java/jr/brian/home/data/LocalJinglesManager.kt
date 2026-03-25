package jr.brian.home.data

import androidx.compose.runtime.staticCompositionLocalOf

val LocalJinglesManager = staticCompositionLocalOf<JinglesManager> {
    error("JinglesManager not provided")
}
