package jr.brian.frontend.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.frontend.library.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the first-run setup wizard. Steps:
 *   1. Welcome / permissions prompt (Phase 3g lands the UI; wiring
 *      MANAGE_EXTERNAL_STORAGE / SAF comes later as a one-liner).
 *   2. Add a ROM folder (hands a path to [LibraryRepository.addSource]).
 *   3. Scan (kicks [LibraryRepository.scanAll]).
 *   4. Done, with "optionally import ES-DE gamelists" callout the
 *      Phase 2d migration already handled on upgrade.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<SetupState>(SetupState.Welcome)
    val state: StateFlow<SetupState> = _state.asStateFlow()

    fun next() {
        _state.value = when (val current = _state.value) {
            SetupState.Welcome -> SetupState.AddSource
            is SetupState.AddSource -> SetupState.Scan
            SetupState.Scan -> SetupState.Done
            SetupState.Done -> SetupState.Done
            is SetupState.Working -> current // ignore
        }
    }

    fun addSource(path: String, displayPath: String? = null) {
        viewModelScope.launch {
            _state.value = SetupState.Working("Adding source…")
            repository.addSource(treeUri = path, displayPath = displayPath ?: path)
            _state.value = SetupState.Scan
        }
    }

    fun scan() {
        viewModelScope.launch {
            _state.value = SetupState.Working("Scanning…")
            repository.scanAll()
            // repository.state transitions back to Idle via its own
            // invokeOnCompletion; a simple UI pass is enough here.
            _state.value = SetupState.Done
        }
    }

    fun restart() {
        _state.value = SetupState.Welcome
    }
}

sealed interface SetupState {
    data object Welcome : SetupState
    data object AddSource : SetupState
    data object Scan : SetupState
    data object Done : SetupState
    data class Working(val message: String) : SetupState
}
