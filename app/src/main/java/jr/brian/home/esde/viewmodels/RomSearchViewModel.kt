package jr.brian.home.esde.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.RomListParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RomSearchViewModel @Inject constructor(
    private val esdePreferencesManager: ESDEPreferencesManager,
    private val setupPreferences: SetupPreferences,
    private val store: RomSearchStateHolder
) : ViewModel() {

    val query: StateFlow<String> = store.query.asStateFlow()
    val allGames: StateFlow<List<GameInfo>> = store.allGames.asStateFlow()
    val isLoading: StateFlow<Boolean> = store.isLoading.asStateFlow()
    val focusedGame: StateFlow<GameInfo?> = store.focusedGame.asStateFlow()
    val keyboardVisible: StateFlow<Boolean> = store.keyboardVisible.asStateFlow()

    private val esdeRootPath: String?
        get() = File(setupPreferences.scriptsPath).parentFile?.absolutePath

    private val mediaPath: String
        get() = esdePreferencesManager.state.value.customMediaPath ?: setupPreferences.mediaPath

    fun updateQuery(q: String) {
        store.query.value = q
    }

    fun dismiss() {
        store.query.value = ""
        store.focusedGame.value = null
        store.dismissSignal.tryEmit(Unit)
    }

    fun clearState() {
        store.query.value = ""
        store.focusedGame.value = null
    }

    fun resetKeyboardVisibility() {
        store.keyboardVisible.value = true
    }

    fun loadGames() {
        if (store.isLoading.value || store.allGames.value.isNotEmpty()) return
        val rootPath = esdeRootPath ?: return
        store.isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val prefsState = esdePreferencesManager.state.value
            val games = RomListParser.parseAllSystems(
                esdeRootPath = rootPath,
                mediaPath = mediaPath,
                romsPaths = prefsState.romsPaths,
                systemEmulatorMap = prefsState.systemAppMap
            )
            store.allGames.value = games.sortedWith(
                compareBy({ it.name.lowercase() }, { it.systemName })
            )
            store.isLoading.value = false
        }
    }
}
