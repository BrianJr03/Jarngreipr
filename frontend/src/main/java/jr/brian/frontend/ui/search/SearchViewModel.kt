package jr.brian.frontend.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Backs the Search surface. Filters [LibraryRepository.allGames] by
 * a user-provided query against the game's display name. The query
 * is a StateFlow so the on-screen keyboard can edit it without
 * recomposing the whole screen.
 *
 * Minimum-viable for Phase 3e: substring match, case-insensitive,
 * no system filter chip, no "@hidden" / "@platform" prefixes. Those
 * come later once we move RomSearchQueryState from :app.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: LibraryRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val results: StateFlow<List<GameEntity>> = combine(
        repository.allGames,
        _query,
    ) { all, q ->
        if (q.isBlank()) emptyList()
        else all.filter { it.displayName.contains(q, ignoreCase = true) }.take(MAX_RESULTS)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun setQuery(q: String) {
        _query.value = q
    }

    fun appendChar(c: Char) {
        _query.value = _query.value + c
    }

    fun backspace() {
        _query.value = _query.value.dropLast(1)
    }

    fun clear() {
        _query.value = ""
    }

    private companion object {
        const val MAX_RESULTS = 100
    }
}
