package jr.brian.jarnfe.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.jarnfe.launch.JarnGameFileStatus
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.StageRoute
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Shared by the Stage and Deck windows, which live in one process. */
class JarnLibraryStore(source: JarnLibrarySource = EmptyJarnLibrarySource) {
    var source by mutableStateOf(source)
        private set

    var state by mutableStateOf(JarnLibraryState())
        private set

    /** Null until game launching is connected. While null, A on a game does nothing and shows no hint. */
    var onPlay by mutableStateOf<((JarnLibraryGame) -> Unit)?>(null)

    val canPlay: Boolean get() = onPlay != null

    /** True while the Continue playing banner holds focus, so hints describe the banner. */
    var heroFocused by mutableStateOf(false)

    /** Null until launching is connected. Checks a game's file without launching it. */
    var checkFile by mutableStateOf<(suspend (JarnLibraryGame) -> JarnGameFileStatus)?>(null)

    /** What Now and game actions follow: the Continue playing game while its banner has focus. */
    fun nowSelection(route: StageRoute, section: LibrarySection): LibrarySelection =
        state.resumeSelection(route, section, heroFocused) ?: state.selection(route, section)

    /** A move the open game list makes on its own, cleared once it lands. */
    internal var jump by mutableStateOf<LibraryJump?>(null)
        private set
    private var jumps = 0L

    internal fun requestJump(systemId: String, gameId: String, spin: Boolean) {
        jump = LibraryJump(systemId, gameId, spin, ++jumps)
    }

    /** Focus and Now move only here, at the end of the motion. */
    internal fun landJump(gameId: String) {
        jump = null
        update { focusOpenGame(gameId) }
        requestStageRefocus()
    }

    /** Grows each time Stage should take focus back on its current game. */
    var stageRefocus by mutableIntStateOf(0)
        private set

    fun requestStageRefocus() {
        stageRefocus += 1
    }

    /** The Collections tab's Manage sheet, opened by Select on a tile. */
    internal var collectionSheet by mutableStateOf<CollectionSheet?>(null)

    /** Grows when a closed sheet hands focus back to a collection tile, even from the tab strip. */
    internal var collectionFocusTake by mutableIntStateOf(0)
        private set

    internal fun takeCollectionFocus() {
        collectionFocusTake += 1
    }

    /** Set when the Continue playing card should take focus once it is on screen. */
    var heroFocusPending by mutableStateOf(false)
        private set

    fun requestHeroFocus() {
        heroFocusPending = true
    }

    fun consumeHeroFocus() {
        heroFocusPending = false
    }

    private val openSystemId = MutableStateFlow<String?>(null)
    private val openCollectionId = MutableStateFlow<Long?>(null)
    private val retries = MutableStateFlow(0)

    /** All games loads the first time its tab shows, then stays live, so startup never pays for it. */
    private val allGamesWanted = MutableStateFlow(false)

    fun wantAllGames() {
        allGamesWanted.value = true
    }
    private var restoreConsumed = false

    fun connect(newSource: JarnLibrarySource) {
        if (newSource === source) return
        source = newSource
        state = JarnLibraryState(
            openSystemId = state.openSystemId,
            focusedSystemId = state.focusedSystemId,
            focusedFavoriteId = state.focusedFavoriteId,
            focusedRecentId = state.focusedRecentId,
            focusedAllGameId = state.focusedAllGameId,
            focusedGameBySystem = state.focusedGameBySystem,
            focusedCollectionId = state.focusedCollectionId,
            openCollectionId = state.openCollectionId,
            focusedGameByCollection = state.focusedGameByCollection,
        )
    }

    fun update(transform: JarnLibraryState.() -> JarnLibraryState) {
        restoreConsumed = true
        state = state.transform()
        openSystemId.value = state.openSystemId
        openCollectionId.value = state.openCollectionId
    }

    /** Only a new process restores, so a recreated activity never rolls back a live session. */
    fun restoreIfFresh(systemId: String?) {
        if (restoreConsumed) return
        restoreConsumed = true
        if (systemId != null) update { openSystem(systemId) }
    }

    /** Grows on every press of Play, so a preview behind Now stops as the game opens. */
    var launches by mutableIntStateOf(0)
        private set

    fun play(game: JarnLibraryGame) {
        launches += 1
        onPlay?.invoke(game)
    }

    /** Resubscribes too, because a feed that threw has already ended. */
    fun retry() {
        source.refresh()
        retries.value += 1
    }

    /** Runs until cancelled. Call from the Stage's composition so it follows that window's lifetime. */
    suspend fun observe(from: JarnLibrarySource = source) = coroutineScope {
        launch { from.observeSystems().keepLast({ systems }) { copy(systems = it) } }
        launch { from.observeFavorites().keepLast({ favorites }) { copy(favorites = it) } }
        launch { from.observeRecent().keepLast({ recent }) { copy(recent = it) } }
        launch { from.collections.observeCollections().keepLast({ collections }) { copy(collections = it) } }
        launch {
            openCollectionId.collectLatest { collectionId ->
                if (collectionId != null) from.observeCollectionGames(collectionId)
            }
        }
        launch {
            allGamesWanted.collectLatest { wanted ->
                if (wanted) from.observeAllGames().keepLast({ allGames }) { copy(allGames = it) }
            }
        }
        launch {
            openSystemId.collectLatest { systemId ->
                if (systemId != null) from.observeSystemGames(systemId)
            }
        }
    }

    private suspend fun JarnLibrarySource.observeCollectionGames(collectionId: Long) {
        collections.observeCollectionGames(collectionId).keepLast(
            read = { gamesByCollection[collectionId] ?: JarnLibraryFeed() },
            write = { copy(gamesByCollection = gamesByCollection + (collectionId to it)) },
        )
    }

    private suspend fun JarnLibrarySource.observeSystemGames(systemId: String) {
        observeGames(systemId).keepLast(
            read = { gamesBySystem[systemId] ?: JarnLibraryFeed() },
            write = { copy(gamesBySystem = gamesBySystem + (systemId to it)) },
        )
    }

    private suspend fun <T> Flow<JarnLibraryFeed<T>>.keepLast(
        read: JarnLibraryState.() -> JarnLibraryFeed<T>,
        write: JarnLibraryState.(JarnLibraryFeed<T>) -> JarnLibraryState,
    ) {
        val feed = this
        retries.collectLatest {
            feed.catch { emit(state.read().copy(status = JarnLibraryStatus.Unavailable)) }
                .collect { state = state.write(it) }
        }
    }
}

val JarnLibrary = JarnLibraryStore()
