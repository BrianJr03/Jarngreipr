package jr.brian.frontend.ui.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.core.model.bus.FrontendBus
import jr.brian.core.model.bus.FrontendEvent
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the Games list and Detail screens. Owns the selected system
 * id as its own state so Compose can navigate between
 * `games/{systemId}` destinations without re-fetching.
 *
 * Launch isn't performed here — the `onLaunch` composable emits a
 * [FrontendEvent.GameLaunched] on the shared bus; :app's existing
 * RomGameLauncher listens and performs the actual intent build.
 * Phase 4 will migrate that into :frontend.
 */
@HiltViewModel
class GamesViewModel @Inject constructor(
    private val repository: LibraryRepository,
    private val bus: FrontendBus,
) : ViewModel() {

    private val systemId = MutableStateFlow<String?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GamesUiState> = systemId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(GamesUiState.Loading)
            else combine(
                repository.gamesForSystem(id),
                repository.findSystemFlow(id),
            ) { games, system ->
                GamesUiState.Loaded(system = system, games = games)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GamesUiState.Loading,
        )

    fun bind(systemId: String) {
        this.systemId.value = systemId
    }

    /**
     * Publishes a launch request on the bus. :app's RomGameLauncher
     * (unchanged in Phase 3) picks it up and builds the actual
     * intent via the existing EmulatorRegistry path.
     */
    fun requestLaunch(game: GameEntity) {
        viewModelScope.launch {
            bus.publishEvent(FrontendEvent.GameLaunched(gameId = game.id.toString()))
        }
    }

    /** One-shot game lookup for the detail screen. */
    suspend fun findGame(gameId: Long): GameEntity? = repository.findGameById(gameId)
}

sealed interface GamesUiState {
    data object Loading : GamesUiState
    data class Loaded(val system: SystemEntity?, val games: List<GameEntity>) : GamesUiState
}

/**
 * Extension on LibraryRepository exposing a flow for one system —
 * kept here as a thin adapter so the repository's public surface
 * doesn't need a per-screen method.
 */
private fun LibraryRepository.findSystemFlow(id: String): kotlinx.coroutines.flow.Flow<SystemEntity?> =
    kotlinx.coroutines.flow.flow { emit(findSystemById(id)) }
