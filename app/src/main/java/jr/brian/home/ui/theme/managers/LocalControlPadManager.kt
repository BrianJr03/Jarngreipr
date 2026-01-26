package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.ControlPadManager

val LocalControlPadManager = staticCompositionLocalOf<ControlPadManager> {
    error("No ControlPadManager provided")
}
