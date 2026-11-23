package jr.brian.jarnfe.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.LibraryGame
import jr.brian.frontend.library.art.VisibleList
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.JarnLibraryStore
import jr.brian.jarnfe.library.JarnLibrarySystem
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.library.toJarnGame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

private const val SEARCH_SETTLE_MS = 120L
private const val MAX_GAME_RESULTS = 120
internal const val MAX_QUERY_LENGTH = 60

internal enum class JarnSearchTab { Games, Systems }

@Immutable
internal data class JarnSearchResults(
    val games: List<JarnLibraryGame> = emptyList(),
    val systems: List<JarnLibrarySystem> = emptyList(),
) {
    val isEmpty: Boolean get() = games.isEmpty() && systems.isEmpty()

    /** Falls over to the other tab so a query with matches never lands on an empty one. */
    fun visibleTab(selected: JarnSearchTab): JarnSearchTab = when {
        selected == JarnSearchTab.Games && games.isEmpty() && systems.isNotEmpty() -> JarnSearchTab.Systems
        selected == JarnSearchTab.Systems && systems.isEmpty() && games.isNotEmpty() -> JarnSearchTab.Games
        else -> selected
    }

    fun firstSelection(tab: JarnSearchTab): LibrarySelection = when (visibleTab(tab)) {
        JarnSearchTab.Games -> games.firstOrNull()?.let(LibrarySelection::Game)
        JarnSearchTab.Systems -> systems.firstOrNull()?.let(LibrarySelection::System)
    } ?: LibrarySelection.None

    fun contains(selection: LibrarySelection): Boolean = when (selection) {
        LibrarySelection.None -> false
        is LibrarySelection.Game -> games.any { it.id == selection.game.id }
        is LibrarySelection.System -> systems.any { it.id == selection.system.id }
    }

    /** Keeps the selected result but with its latest art, so the Deck updates live. */
    fun refreshed(selection: LibrarySelection, tab: JarnSearchTab): LibrarySelection = when {
        !contains(selection) -> firstSelection(tab)
        selection is LibrarySelection.Game ->
            LibrarySelection.Game(games.first { it.id == selection.game.id })
        else -> selection
    }
}

/** Shared by the Stage and Deck windows, which live in one process. */
internal class JarnSearchStore {
    var query by mutableStateOf("")
        private set
    var tab by mutableStateOf(JarnSearchTab.Games)
        private set
    var scopeSystemId by mutableStateOf<String?>(null)
        private set
    var results by mutableStateOf(JarnSearchResults())
        private set

    /** True while the Deck shows a result's actions in place of the keyboard. */
    var showingActions by mutableStateOf(false)
        private set

    fun begin(scope: String?) {
        query = ""
        tab = JarnSearchTab.Games
        scopeSystemId = scope
        results = JarnSearchResults()
        showingActions = false
    }

    fun type(text: String) {
        query = (query + text).take(MAX_QUERY_LENGTH)
    }

    fun deleteLast() {
        query = query.dropLast(1)
    }

    fun clearQuery() {
        query = ""
    }

    fun clearScope() {
        scopeSystemId = null
    }

    fun showKeyboard() {
        showingActions = false
    }

    fun showActions() {
        showingActions = true
    }

    fun stepTab(delta: Int) {
        val current = results.visibleTab(tab)
        val entries = JarnSearchTab.entries
        tab = entries[(entries.indexOf(current) + delta).coerceIn(entries.indices)]
    }

    /** Runs while Search is on screen. Newer input cancels any search still in progress. */
    suspend fun observe(data: JarnLibraryData, library: JarnLibraryStore) {
        combine(
            snapshotFlow { query to scopeSystemId },
            data.allGames,
            snapshotFlow { library.state.systems.items },
            data.artChoices,
        ) { (text, scope), games, systems, _ -> SearchInput(text, scope, games.items, systems) }
            .collectLatest { input ->
                delay(SEARCH_SETTLE_MS)
                val found = withContext(Dispatchers.Default) { input.run(data) }
                results = found
                val next = found.refreshed(library.state.searchSelection, tab)
                if (next != library.state.searchSelection) {
                    library.update { focusSearchResult(next) }
                }
            }
    }

    private data class SearchInput(
        val text: String,
        val scope: String?,
        val games: List<LibraryGame>,
        val systems: List<JarnLibrarySystem>,
    ) {
        suspend fun run(data: JarnLibraryData): JarnSearchResults {
            val parsed = parseJarnSearchQuery(text)
            val allowed = allowedSystemIds(parsed, systems, scope)
            val matches = searchGames(games, parsed, allowed, MAX_GAME_RESULTS, LibraryGame::systemId, LibraryGame::title)
            val withArt = if (matches.isEmpty()) {
                emptyList()
            } else {
                data.withArt(flowOf(VisibleList(matches))).first().items.map { it.toJarnGame() }
            }
            return JarnSearchResults(withArt, searchSystems(systems, parsed, allowed))
        }
    }
}

internal val JarnSearch = JarnSearchStore()
