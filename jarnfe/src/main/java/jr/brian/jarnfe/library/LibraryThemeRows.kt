package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import jr.brian.jarnfe.themekit.FocusedItem

/** The systems as a theme's text list, with the Continue playing card above, as in the grid. */
@Composable
internal fun LibrarySystemRows(
    library: JarnLibraryStore,
    geometry: LibraryListGeometry,
    onFocused: (String) -> Unit,
    onOpenSystem: (String) -> Unit,
) {
    val state = library.state
    LibraryThemeList(
        items = state.systems.items,
        content = systemListContent(onOpenSystem),
        geometry = geometry,
        focusedKey = state.focusedSystem?.id,
        onFocused = onFocused,
        header = librarySystemsHeader(library),
        letterJumpEnabled = false,
    )
}

private fun systemListContent(onOpenSystem: (String) -> Unit) = LibraryListContent<JarnLibrarySystem>(
    itemKey = JarnLibrarySystem::id,
    itemTitle = JarnLibrarySystem::title,
    itemFocus = FocusedItem::System,
    onOpen = { onOpenSystem(it.id) },
    itemModifier = { Modifier.librarySystemAnchor(it.id) },
)

/** Games as a theme's text list. A game's own system comes along, for system fields on its second line. */
@Composable
internal fun rememberGameListContent(library: JarnLibraryStore): LibraryListContent<JarnLibraryGame> {
    val systemItems = library.state.systems.items
    return remember(library, systemItems) {
        val systems = systemItems.associateBy(JarnLibrarySystem::id)
        LibraryListContent(
            itemKey = JarnLibraryGame::id,
            itemTitle = JarnLibraryGame::title,
            itemFocus = { game -> FocusedItem.Game(game, systems[game.systemId]) },
            onOpen = library::play,
        )
    }
}
