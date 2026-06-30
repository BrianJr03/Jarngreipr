package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.staticCompositionLocalOf
import jr.brian.home.data.PageOrderCoordinator

val LocalPageOrderCoordinator = staticCompositionLocalOf<PageOrderCoordinator> {
    error("No PageOrderCoordinator provided")
}
