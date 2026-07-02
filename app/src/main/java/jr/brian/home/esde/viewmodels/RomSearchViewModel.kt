package jr.brian.home.esde.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.RomSearchStateHolder
import jr.brian.home.esde.data.SetupPreferences
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.util.RomListParser
import jr.brian.home.esde.util.mediaRoots
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    val isLoading: StateFlow<Boolean> = store.isLoading.asStateFlow()
    val focusedGame: StateFlow<GameInfo?> = store.focusedGame.asStateFlow()
    val hintAndKbVisible: StateFlow<Boolean> = store.hintAndKbVisible.asStateFlow()
    val screenDismissSignal: SharedFlow<Unit> = store.screenDismissSignal.asSharedFlow()
    val pendingRomForPin: StateFlow<Pair<Int, GameInfo>?> = store.pendingRomForPin.asStateFlow()
    val stateHolder: RomSearchStateHolder = store

    fun enterSelectMode(pageIndex: Int) {
        store.isSelectMode.value = true
        store.pendingSelectPageIndex.value = pageIndex
    }

    fun clearPendingRomForPin() {
        store.pendingRomForPin.value = null
    }

    fun requestRomLaunch(rom: PinnedRomInfo) {
        store.pendingRomToLaunch.value = rom
    }

    private val esdeRootPath: String?
        get() = File(setupPreferences.scriptsPath).parentFile?.absolutePath

    private val mediaPaths: List<String>
        get() {
            val state = esdePreferencesManager.state.value
            val primary = state.customMediaPath ?: setupPreferences.mediaPath
            val secondary = if (state.secondaryMediaEnabled)
                SetupPreferences.DEFAULT_SECONDARY_MEDIA_PATH else null
            return mediaRoots(primary, secondary)
        }

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

    fun resetHintAndKbVisibility() {
        store.hintAndKbVisible.value = true
    }

    fun loadGames() {
        if (store.isLoading.value || store.allGames.value.isNotEmpty()) return
        val rootPath = esdeRootPath ?: return
        store.isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val prefsState = esdePreferencesManager.state.value
            val games = RomListParser.parseAllSystems(
                esdeRootPath = rootPath,
                mediaPaths = mediaPaths,
                romsPaths = prefsState.romsPaths,
                systemEmulatorMap = prefsState.systemAppMap
            )
            store.allGames.value = games.sortedWith(
                compareBy({ it.name.lowercase() }, { it.systemName.trim() })
            )
            store.isLoading.value = false
        }
    }
}
