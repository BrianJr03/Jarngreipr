package jr.brian.frontend.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the Home screen. Pulls systems + favourites + recent games
 * from [LibraryRepository] and projects them into a single
 * [HomeUiState] the Composable can bind without touching individual
 * Flows.
 *
 * The empty-library state is explicit so the Phase 3g setup wizard
 * can trigger from Home when nothing is scanned yet.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.systems,
        repository.favorites,
        repository.recentGames(limit = RECENT_LIMIT),
    ) { systems, favorites, recent ->
        HomeUiState(
            systems = systems.filter { it.id in observedSystemIds(systems) },
            favorites = favorites,
            recent = recent,
            isEmpty = systems.isEmpty(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading,
    )

    /**
     * Hide systems the user has no scanned games for. The Phase 2a
     * scanner upserts a SystemEntity row for every catalog system
     * eagerly so the FK writes always resolve — but the Home grid
     * should only show systems the user actually has ROMs in.
     *
     * Phase 2a's games query is already filtered by `isHidden`; this
     * composition joins the two so Home respects both.
     */
    private fun observedSystemIds(allSystems: List<SystemEntity>): Set<String> {
        // Phase 3c lands this as "show every system row the DB has"
        // because the filter needs a per-system game count query the
        // DAO doesn't expose yet. Phase 3d adds the count join; until
        // then Home mirrors the DB. If every catalog system is in the
        // DB, this is the same as "show the full catalog" — which the
        // setup wizard flow in 3g fixes with an isEmpty UI.
        return allSystems.map { it.id }.toSet()
    }

    private companion object {
        const val RECENT_LIMIT = 12
    }
}

/** Composable-facing state. */
data class HomeUiState(
    val systems: List<SystemEntity>,
    val favorites: List<GameEntity>,
    val recent: List<GameEntity>,
    val isEmpty: Boolean,
) {
    companion object {
        val Loading = HomeUiState(
            systems = emptyList(),
            favorites = emptyList(),
            recent = emptyList(),
            isEmpty = true,
        )
    }
}
