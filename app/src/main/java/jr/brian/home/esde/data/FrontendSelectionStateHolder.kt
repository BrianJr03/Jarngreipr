package jr.brian.home.esde.data

import jr.brian.home.esde.model.FrontendSelection
import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide bridge that lets the frontend activity tell the bottom screen
 * (MainActivity's ESDEViewModel) which system or game is currently focused,
 * replacing the ES-DE file-event channel that drives [WallpaperState] during
 * normal ES-DE browsing.
 */
@Singleton
class FrontendSelectionStateHolder @Inject constructor() {
    val selection = MutableStateFlow<FrontendSelection?>(null)

    fun selectSystem(systemName: String) {
        selection.value = FrontendSelection.System(systemName)
    }

    fun selectGame(systemName: String, gameFilename: String) {
        selection.value = FrontendSelection.Game(systemName, gameFilename)
    }

    fun selectGame(game: GameInfo) {
        selection.value = FrontendSelection.Game(game.systemName, game.path)
    }

    fun clear() {
        selection.value = null
    }
}
