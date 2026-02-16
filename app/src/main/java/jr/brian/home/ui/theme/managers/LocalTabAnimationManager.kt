package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.compositionLocalOf

val LocalTabAnimationManager =
    compositionLocalOf<TabAnimationManager> {
        error("TabAnimationManager not provided")
    }
