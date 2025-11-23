package jr.brian.home.ui.theme

import androidx.compose.runtime.compositionLocalOf
import jr.brian.home.data.AppVisibilityManager

val LocalAppVisibilityManager = compositionLocalOf<AppVisibilityManager> {
    error("No AppVisibilityManager provided")
}
