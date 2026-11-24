package jr.brian.jarnfe.library

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnBrowseView

/**
 * Flips between the grid and the shelf with a short crossfade. Both read the same focused item,
 * so the place is kept across the flip.
 */
@Composable
internal fun LibraryBrowseSwitch(
    view: JarnBrowseView,
    grid: @Composable () -> Unit,
    shelf: @Composable () -> Unit,
) {
    val motion = JarnTheme.motion
    val token = if (motion.reduced) motion.panel else motion.small
    Crossfade(targetState = view, animationSpec = token.spec(), label = "browseView") { shown ->
        when (shown) {
            JarnBrowseView.Grid -> grid()
            JarnBrowseView.Carousel -> shelf()
        }
    }
}
