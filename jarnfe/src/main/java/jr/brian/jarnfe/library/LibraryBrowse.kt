package jr.brian.jarnfe.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.jarnfe.themekit.JarnThemeLayout
import jr.brian.jarnfe.themekit.ThemeInsets

/** Browse sits inside the stage, which already keeps clear of the bars and the hint bar. */
private val BrowseInsets = ThemeInsets()

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
    LibraryBrowseCrossfade(view) { shown ->
        when (shown) {
            JarnBrowseView.Grid -> grid()
            JarnBrowseView.Carousel -> shelf()
        }
    }
}

/**
 * Draws [kind]'s browse as the built-in theme for the saved [view], crossfading between them.
 * [grid] and [shelf] draw the list at the geometry the theme resolves to.
 */
@Composable
internal fun LibraryThemedBrowse(
    view: JarnBrowseView,
    kind: LibraryShelfKind,
    cellMinSize: Dp,
    grid: @Composable (LibraryGridGeometry) -> Unit,
    shelf: @Composable (LibraryShelfGeometry) -> Unit,
) {
    LibraryBrowseCrossfade(view) { shown ->
        val theme = LibraryBuiltInThemes.forView(shown)
        val spec = theme.spec.views[kind.themeView] ?: return@LibraryBrowseCrossfade
        val pins = theme.pinsFor(kind.themeView)
        JarnThemeLayout(
            view = spec,
            primary = { LibraryThemePrimary(spec.primary, pins, kind, cellMinSize, grid, shelf) },
            layer = {},
            insets = BrowseInsets,
        )
    }
}

@Composable
private fun LibraryBrowseCrossfade(view: JarnBrowseView, content: @Composable (JarnBrowseView) -> Unit) {
    val motion = JarnTheme.motion
    val token = if (motion.reduced) motion.panel else motion.small
    Crossfade(targetState = view, animationSpec = token.spec(), label = "browseView", content = content)
}

/** Lists the format has no Jarn component for yet fall back to the grid. */
@Composable
private fun LibraryThemePrimary(
    primary: ThemePrimary,
    pins: LibraryPrimaryPins,
    kind: LibraryShelfKind,
    cellMinSize: Dp,
    grid: @Composable (LibraryGridGeometry) -> Unit,
    shelf: @Composable (LibraryShelfGeometry) -> Unit,
) {
    when (primary) {
        is ThemePrimary.Shelf -> shelf(libraryShelfGeometry(primary, kind, pins.shelf))
        is ThemePrimary.Grid -> LibraryThemeGrid(primary, pins.grid, cellMinSize, grid)
        is ThemePrimary.List -> grid(LibraryGridGeometry(cellMinSize, rowSpacing = LibraryGridRowSpacing))
    }
}

/** A pinned grid needs no box width, so it skips measuring for one. */
@Composable
private fun LibraryThemeGrid(
    primary: ThemePrimary.Grid,
    pinned: LibraryGridPins?,
    cellMinSize: Dp,
    grid: @Composable (LibraryGridGeometry) -> Unit,
) {
    if (pinned != null) {
        grid(libraryGridGeometry(primary, cellMinSize, Dp.Unspecified, pinned))
        return
    }
    BoxWithConstraints { grid(libraryGridGeometry(primary, cellMinSize, maxWidth)) }
}
