package jr.brian.jarnfe.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    private val openSystemId = MutableStateFlow<String?>(null)
    private val retries = MutableStateFlow(0)
    private var restoreConsumed = false

    fun connect(newSource: JarnLibrarySource) {
        if (newSource === source) return
        source = newSource
        state = JarnLibraryState(
            openSystemId = state.openSystemId,
            focusedSystemId = state.focusedSystemId,
            focusedFavoriteId = state.focusedFavoriteId,
            focusedRecentId = state.focusedRecentId,
            focusedGameBySystem = state.focusedGameBySystem,
        )
    }

    fun update(transform: JarnLibraryState.() -> JarnLibraryState) {
        restoreConsumed = true
        state = state.transform()
        openSystemId.value = state.openSystemId
    }

    /** Only a new process restores, so a recreated activity never rolls back a live session. */
    fun restoreIfFresh(systemId: String?) {
        if (restoreConsumed) return
        restoreConsumed = true
        if (systemId != null) update { openSystem(systemId) }
    }

    fun play(game: JarnLibraryGame) {
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
        launch {
            openSystemId.collectLatest { systemId ->
                if (systemId != null) from.observeSystemGames(systemId)
            }
        }
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
