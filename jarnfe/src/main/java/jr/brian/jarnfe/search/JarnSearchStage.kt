package jr.brian.jarnfe.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnTabs
import jr.brian.core.ui.library.JarnGameTile
import jr.brian.core.ui.library.JarnSystemTile
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnApps
import jr.brian.jarnfe.apps.jarnAppsDataOrNull
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibraryCoverArt
import jr.brian.jarnfe.library.LibraryFocusGrid
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.library.LibrarySystemArt
import jr.brian.jarnfe.library.gameCountText
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.StageRoute

private val SearchGameCellMinSize = 112.dp
private val SearchSystemCellMinSize = 148.dp

@Composable
internal fun JarnSearchTabs(search: JarnSearchStore = JarnSearch) {
    val results = search.results
    val tabs = JarnSearchTab.entries
    JarnTabs(
        labels = listOf(
            stringResource(R.string.jarnfe_search_tab_games, results.games.size),
            stringResource(R.string.jarnfe_search_tab_systems, results.systems.size),
            stringResource(R.string.jarnfe_search_tab_apps, results.apps.size),
        ),
        selectedIndex = tabs.indexOf(results.visibleTab(search.tab)),
        onSelect = { index -> search.selectTab(tabs[index]) },
    )
}

@Composable
internal fun JarnSearchResultsPanel(state: JarnSessionState, search: JarnSearchStore = JarnSearch) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnLibraryDataOrNull() }
    val appsData = remember(context) { context.jarnAppsDataOrNull() }
    LaunchedEffect(data) { data?.let { search.observe(it, JarnLibrary) } }
    LaunchedEffect(appsData) { appsData?.let { JarnApps.observe(it) } }
    LaunchedEffect(state.activeScreen) {
        if (state.activeScreen == JarnScreen.Stage) search.showKeyboard()
    }
    val results = search.results
    when {
        search.query.isBlank() -> SearchMessage(R.string.jarnfe_search_prompt_title, R.string.jarnfe_search_prompt_message)
        results.isEmpty -> JarnEmptyState(
            title = stringResource(R.string.jarnfe_search_empty_title, search.query.trim()),
            message = stringResource(R.string.jarnfe_search_empty_message),
        )
        state.deckPanelOpen -> SearchMessage(R.string.jarnfe_search_prompt_title, R.string.jarnfe_search_typing_message)
        else -> key(results.visibleTab(search.tab)) { SearchResultGrid(results, search.tab) }
    }
}

@Composable
private fun SearchMessage(titleRes: Int, messageRes: Int) {
    JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
}

@Composable
private fun SearchResultGrid(results: JarnSearchResults, tab: JarnSearchTab) {
    when (results.visibleTab(tab)) {
        JarnSearchTab.Games -> SearchGameGrid(results.games)
        JarnSearchTab.Systems -> SearchSystemGrid(results.systems)
        JarnSearchTab.Apps -> SearchAppGrid(results.apps)
    }
}

@Composable
private fun SearchGameGrid(games: List<JarnLibraryGame>) {
    val selected = (JarnLibrary.state.searchSelection as? LibrarySelection.Game)?.game?.id
    LibraryFocusGrid(
        items = games,
        itemKey = JarnLibraryGame::id,
        focusedKey = selected?.takeIf { id -> games.any { it.id == id } },
        onFocused = { id ->
            games.firstOrNull { it.id == id }?.let { game ->
                JarnLibrary.update { focusSearchResult(LibrarySelection.Game(game)) }
            }
        },
        cellMinSize = SearchGameCellMinSize,
        refocusSignal = JarnLibrary.stageRefocus,
    ) { game, modifier ->
        JarnGameTile(
            title = game.title,
            onClick = { JarnLibrary.play(game) },
            modifier = modifier.fillMaxWidth(),
        ) { LibraryCoverArt(game) }
    }
}

@Composable
private fun SearchSystemGrid(systems: List<JarnLibrarySystem>) {
    val selected = (JarnLibrary.state.searchSelection as? LibrarySelection.System)?.system?.id
    LibraryFocusGrid(
        items = systems,
        itemKey = JarnLibrarySystem::id,
        focusedKey = selected?.takeIf { id -> systems.any { it.id == id } },
        onFocused = { id ->
            systems.firstOrNull { it.id == id }?.let { system ->
                JarnLibrary.update { focusSearchResult(LibrarySelection.System(system)) }
            }
        },
        cellMinSize = SearchSystemCellMinSize,
        refocusSignal = JarnLibrary.stageRefocus,
    ) { system, modifier ->
        JarnSystemTile(
            title = system.title,
            subtitle = gameCountText(system.gameCount),
            onClick = { openSystemResult(system) },
            modifier = modifier.fillMaxWidth(),
        ) { LibrarySystemArt(system) }
    }
}

private fun openSystemResult(system: JarnLibrarySystem) {
    JarnLibrary.update { openSystem(system.id) }
    JarnSession.update { openRoute(StageRoute.GameList) }
}
