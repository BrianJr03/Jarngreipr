package jr.brian.home.esde.data

import jr.brian.home.esde.model.FrontendRoute
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RomSearchStateHolder @Inject constructor() {
    val query = MutableStateFlow("")
    val allGames = MutableStateFlow<List<GameInfo>>(emptyList())
    val isLoading = MutableStateFlow(false)
    val dismissSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val screenDismissSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gameLaunchSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val focusedGame = MutableStateFlow<GameInfo?>(null)
    val hintAndKbVisible = MutableStateFlow(true)

    val isSelectMode = MutableStateFlow(false)
    val pendingSelectPageIndex = MutableStateFlow(-1)
    val pendingRomForPin = MutableStateFlow<Pair<Int, GameInfo>?>(null)
    val pendingRomToLaunch = MutableStateFlow<PinnedRomInfo?>(null)

    val currentRoute = MutableStateFlow<FrontendRoute>(FrontendRoute.Search)
}
