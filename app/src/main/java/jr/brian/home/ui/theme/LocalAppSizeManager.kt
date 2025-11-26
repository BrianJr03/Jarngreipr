package jr.brian.home.ui.theme

import androidx.compose.runtime.compositionLocalOf
import jr.brian.home.data.AppSizeManager

val LocalAppSizeManager =
    compositionLocalOf<AppSizeManager> {
        error("AppSizeManager not provided")
    }
