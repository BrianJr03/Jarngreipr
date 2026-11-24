package jr.brian.jarnfe.search

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.LibraryGameTitle
import jr.brian.frontend.library.art.VisibleList
import jr.brian.jarnfe.apps.JarnApps
import jr.brian.jarnfe.apps.jarnShownApps
import jr.brian.jarnfe.dock.JarnDockApp
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
private const val MAX_APP_RESULTS = 60
internal const val MAX_QUERY_LENGTH = 60

internal enum class JarnSearchTab { Games, Systems, Apps }

@Immutable
internal data class JarnSearchResults(
    val games: List<JarnLibraryGame> = emptyList(),
    val systems: List<JarnLibrarySystem> = emptyList(),
    val apps: List<JarnDockApp> = emptyList(),
) {
    val isEmpty: Boolean get() = games.isEmpty() && systems.isEmpty() && apps.isEmpty()

    fun count(tab: JarnSearchTab): Int = when (tab) {
        JarnSearchTab.Games -> games.size
        JarnSearchTab.Systems -> systems.size
        JarnSearchTab.Apps -> apps.size
    }

    /** Falls over to the first tab with matches, so a query with matches never lands on an empty one. */
    fun visibleTab(selected: JarnSearchTab): JarnSearchTab =
        if (count(selected) > 0) selected else JarnSearchTab.entries.firstOrNull { count(it) > 0 } ?: selected

    /** Skips tabs with no matches, so L1 and R1 never land on an empty one. */
    fun stepTab(selected: JarnSearchTab, delta: Int): JarnSearchTab {
        val shown = JarnSearchTab.entries.filter { count(it) > 0 }.ifEmpty { JarnSearchTab.entries }
        val index = shown.indexOf(visibleTab(selected)).coerceAtLeast(0)
        return shown[(index + delta).coerceIn(shown.indices)]
    }

    /** Apps aren't library items, so an app result leaves the library selection empty. */
    fun firstSelection(tab: JarnSearchTab): LibrarySelection = when (visibleTab(tab)) {
        JarnSearchTab.Games -> games.firstOrNull()?.let(LibrarySelection::Game)
        JarnSearchTab.Systems -> systems.firstOrNull()?.let(LibrarySelection::System)
        JarnSearchTab.Apps -> null
    } ?: LibrarySelection.None

    fun contains(selection: LibrarySelection): Boolean = when (selection) {
        LibrarySelection.None -> false
        is LibrarySelection.Game -> games.any { it.id == selection.game.id }
        is LibrarySelection.System -> systems.any { it.id == selection.system.id }
    }

    /** Keeps the selected result but with its latest art, so the Deck updates live. */
    fun refreshed(selection: LibrarySelection, tab: JarnSearchTab): LibrarySelection = when {
        visibleTab(tab) == JarnSearchTab.Apps -> LibrarySelection.None
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

    /** The focused app result, which Options and the hints act on. */
    var focusedApp by mutableStateOf<String?>(null)

    /** True while the Deck shows a result's actions in place of the keyboard. */
    var showingActions by mutableStateOf(false)
        private set

    fun begin(scope: String?) {
        query = ""
        tab = JarnSearchTab.Games
        scopeSystemId = scope
        results = JarnSearchResults()
        showingActions = false
        focusedApp = null
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

    fun selectTab(next: JarnSearchTab) {
        tab = next
    }

    fun stepTab(delta: Int) {
        tab = results.stepTab(tab, delta)
    }

    /** Runs while Search is on screen. Newer input cancels any search still in progress. */
    suspend fun observe(data: JarnLibraryData, library: JarnLibraryStore) {
        combine(
            snapshotFlow { query to scopeSystemId },
            data.searchTitles,
            snapshotFlow { library.state.systems.items },
            data.artChoices,
            snapshotFlow { JarnApps.snapshot },
        ) { (text, scope), games, systems, _, apps ->
            SearchInput(text, scope, games, systems, apps?.let(::jarnShownApps).orEmpty())
        }
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
        val games: List<LibraryGameTitle>,
        val systems: List<JarnLibrarySystem>,
        val apps: List<JarnDockApp>,
    ) {
        /** Titles are matched first, so only the shown games load in full. */
        suspend fun run(data: JarnLibraryData): JarnSearchResults {
            val parsed = parseJarnSearchQuery(text)
            val allowed = allowedSystemIds(parsed, systems, scope)
            val matches = searchGames(
                games,
                parsed,
                allowed,
                MAX_GAME_RESULTS,
                LibraryGameTitle::systemId,
                LibraryGameTitle::title,
            )
            val full = data.gamesById(matches.map { it.id })
            val withArt = if (full.isEmpty()) {
                emptyList()
            } else {
                data.withArt(flowOf(VisibleList(full))).first().items.map { it.toJarnGame() }
            }
            return JarnSearchResults(
                games = withArt,
                systems = searchSystems(systems, parsed, allowed),
                apps = searchApps(apps, parsed, allowed, MAX_APP_RESULTS, JarnDockApp::label),
            )
        }
    }
}

internal val JarnSearch = JarnSearchStore()
