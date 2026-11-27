package jr.brian.jarnfe.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnBrowseView
import jr.brian.jarnfe.themekit.FocusedItem
import jr.brian.jarnfe.themekit.JarnThemeDecorView
import jr.brian.jarnfe.themekit.JarnThemeLayout
import jr.brian.jarnfe.themes.JarnThemeEntry
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
 * Draws [kind]'s browse as [theme], crossfading when the theme changes. [grid] and [shelf] draw
 * the list at the geometry the theme resolves to. Decor layers follow [focused], which is only
 * read when the view has layers, so a theme without layers draws exactly as the built-ins did.
 */
@Composable
internal fun LibraryThemedBrowse(
    theme: JarnThemeEntry,
    kind: LibraryShelfKind,
    cellMinSize: Dp,
    focused: () -> FocusedItem,
    grid: @Composable (LibraryGridGeometry) -> Unit,
    shelf: @Composable (LibraryShelfGeometry) -> Unit,
) {
    LibraryBrowseCrossfade(theme) { shown ->
        val spec = shown.viewSpec(kind.themeView) ?: return@LibraryBrowseCrossfade
        val pins = shown.theme.pinsFor(kind.themeView)
        val primary: @Composable () -> Unit = { LibraryThemePrimary(spec.primary, pins, kind, cellMinSize, grid, shelf) }
        if (spec.layers.isEmpty()) {
            JarnThemeLayout(view = spec, primary = primary, layer = {}, insets = BrowseInsets)
        } else {
            JarnThemeDecorView(view = spec, focused = focused(), primary = primary, themeDir = shown.themeDir, insets = BrowseInsets)
        }
    }
}

@Composable
private fun <T> LibraryBrowseCrossfade(target: T, content: @Composable (T) -> Unit) {
    val motion = JarnTheme.motion
    val token = if (motion.reduced) motion.panel else motion.small
    Crossfade(targetState = target, animationSpec = token.spec(), label = "browseView", content = content)
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

internal fun JarnLibraryState.focusedSystemItem(): FocusedItem =
    focusedSystem?.let(FocusedItem::System) ?: FocusedItem.None

/** The game at [focusedKey] in [games], with its own system for system fields beside it. */
internal fun JarnLibraryState.focusedGameItem(games: List<JarnLibraryGame>, focusedKey: String?): FocusedItem {
    val game = games.firstOrNull { it.id == focusedKey } ?: return FocusedItem.None
    return FocusedItem.Game(game, systems.items.firstOrNull { it.id == game.systemId })
}
