package jr.brian.home.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.WidgetPageAppManager

val LocalWidgetPageAppManager = staticCompositionLocalOf<WidgetPageAppManager> {
    error("No WidgetPageAppManager provided")
}
