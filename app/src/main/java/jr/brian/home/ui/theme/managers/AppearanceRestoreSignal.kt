package jr.brian.home.ui.theme.managers

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The theme managers live in each activity's composition, so a restore can't reach them directly.
 * It bumps this count instead, and every open [jr.brian.home.ui.theme.LauncherTheme] reloads.
 */
object AppearanceRestoreSignal {
    private val _restores = MutableStateFlow(0)
    val restores: StateFlow<Int> = _restores.asStateFlow()

    fun notifyRestored() = _restores.update { it + 1 }
}
