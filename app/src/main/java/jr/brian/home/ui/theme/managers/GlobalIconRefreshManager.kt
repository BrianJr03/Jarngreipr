package jr.brian.home.ui.theme.managers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalIconRefreshManager @Inject constructor() {
    private var _refreshCounter by mutableIntStateOf(0)
    val refreshCounter: Int get() = _refreshCounter

    fun triggerRefresh() {
        _refreshCounter++
    }
}

val LocalGlobalIconRefreshManager = staticCompositionLocalOf<GlobalIconRefreshManager?> {
    null
}