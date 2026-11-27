package jr.brian.jarnfe.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ViewSpec
import jr.brian.core.model.theme.forArea
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
            JarnBrowseView.Grid, JarnBrowseView.SameAsLayout -> grid()
            JarnBrowseView.Carousel -> shelf()
        }
    }
}

/**
 * Draws [kind]'s browse as [theme], crossfading when the theme changes. [grid], [shelf] and
 * [list] draw the browsable list at the geometry the theme resolves to. Decor layers follow
 * [focused], which is only read when the view has layers, so a theme without layers draws
 * exactly as the built-ins did. Built-ins keep [header] inside their list; spec-driven themes get it
 * as its own strip over the top [LIBRARY_HEADER_BAND] of the view (see [LocalLibraryHeaderInPrimary]).
 */
@Composable
internal fun LibraryThemedBrowse(
    theme: JarnThemeEntry,
    kind: LibraryShelfKind,
    cellMinSize: Dp,
    focused: () -> FocusedItem,
    grid: @Composable (LibraryGridGeometry) -> Unit,
    shelf: @Composable (LibraryShelfGeometry) -> Unit,
    list: @Composable (LibraryListGeometry) -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    LibraryBrowseCrossfade(theme) { shown ->
        val spec = shown.viewSpec(kind.themeView) ?: return@LibraryBrowseCrossfade
        val pins = shown.theme.pinsFor(kind.themeView)
        val headerInPrimary = shown.theme.pins.containsKey(kind.themeView)
        CompositionLocalProvider(LocalLibraryHeaderInPrimary provides headerInPrimary) {
            Box(Modifier.fillMaxSize()) {
                LibraryAreaView(spec) { view ->
                    val primary: @Composable () -> Unit = { LibraryThemePrimary(view.primary, pins, kind, cellMinSize, grid, shelf, list) }
                    if (view.layers.isEmpty()) {
                        JarnThemeLayout(view = view, primary = primary, layer = {}, insets = BrowseInsets)
                    } else {
                        JarnThemeDecorView(view = view, focused = focused(), primary = primary, themeDir = shown.themeDir, insets = BrowseInsets)
                    }
                }
                if (!headerInPrimary) header?.let { LibraryHeaderStrip(it) }
            }
        }
    }
}

/** [spec] as it lays out on this area, with its narrow patch on a narrow one. Views without a patch skip measuring. */
@Composable
private fun LibraryAreaView(spec: ViewSpec, content: @Composable (ViewSpec) -> Unit) {
    if (spec.narrow == null) {
        content(spec)
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        content(remember(spec, width, height) { spec.forArea(width, height) })
    }
}

/** A spec-driven view's header, at the top of the band the theme keeps clear for it. */
@Composable
private fun BoxScope.LibraryHeaderStrip(header: @Composable () -> Unit) {
    Box(
        Modifier
            .align(Alignment.TopStart)
            .padding(start = JarnTheme.space.stageMargin, end = JarnTheme.space.stageMargin, top = JarnTheme.space.md),
    ) { header() }
}

@Composable
private fun <T> LibraryBrowseCrossfade(target: T, content: @Composable (T) -> Unit) {
    val motion = JarnTheme.motion
    val token = if (motion.reduced) motion.panel else motion.small
    Crossfade(targetState = target, animationSpec = token.spec(), label = "browseView", content = content)
}

@Composable
private fun LibraryThemePrimary(
    primary: ThemePrimary,
    pins: LibraryPrimaryPins,
    kind: LibraryShelfKind,
    cellMinSize: Dp,
    grid: @Composable (LibraryGridGeometry) -> Unit,
    shelf: @Composable (LibraryShelfGeometry) -> Unit,
    list: @Composable (LibraryListGeometry) -> Unit,
) {
    when (primary) {
        is ThemePrimary.Shelf -> shelf(libraryShelfGeometry(primary, kind, pins.shelf))
        is ThemePrimary.Grid -> LibraryThemeGrid(primary, pins.grid, cellMinSize, grid)
        is ThemePrimary.List -> list(libraryListGeometry(primary))
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
