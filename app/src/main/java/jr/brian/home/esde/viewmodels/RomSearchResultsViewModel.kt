package jr.brian.home.esde.viewmodels

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class RomSearchResultsViewModel @Inject constructor(
    private val store: RomSearchStateHolder
) : ViewModel() {
    val query: StateFlow<String> = store.query.asStateFlow()
    val allGames: StateFlow<List<GameInfo>> = store.allGames.asStateFlow()
    val isLoading: StateFlow<Boolean> = store.isLoading.asStateFlow()
    val dismissSignal: SharedFlow<Unit> = store.dismissSignal.asSharedFlow()
    val currentRoute: StateFlow<FrontendRoute> = store.currentRoute.asStateFlow()

    fun updateQuery(q: String) {
        store.query.value = q
    }

    fun updateFocusedGame(game: GameInfo?) {
        store.focusedGame.value = game
    }

    fun navigateTo(route: FrontendRoute) {
        store.currentRoute.value = route
    }

    fun clearState() {
        store.query.value = ""
        store.focusedGame.value = null
        store.currentRoute.value = FrontendRoute.Search
    }
}
