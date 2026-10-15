package jr.brian.frontend.ui.settings

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Backs the Settings screen. Phase 3f lands with three live toggles
 * (favoritesOnTop, showHidden, gamelistDecoration) persisted to
 * `frontend_settings_prefs` — a frontend-owned prefs file so we're
 * not writing into :app's esde_prefs blob that Phase 6 wants to
 * collapse.
 *
 * Later phases add more rows; later-phase scope should prefer this
 * class growing with new fields over splitting into a dozen tiny
 * per-section view models.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext context: Context,
) : ViewModel() {

    private val prefs = context.getSharedPreferences("frontend_settings_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    fun setFavoritesOnTop(v: Boolean) {
        _state.value = _state.value.copy(favoritesOnTop = v)
        prefs.edit { putBoolean(KEY_FAV_ON_TOP, v) }
    }

    fun setShowHidden(v: Boolean) {
        _state.value = _state.value.copy(showHidden = v)
        prefs.edit { putBoolean(KEY_SHOW_HIDDEN, v) }
    }

    fun setGamelistDecoration(v: Boolean) {
        _state.value = _state.value.copy(gamelistDecoration = v)
        prefs.edit { putBoolean(KEY_GAMELIST_DECO, v) }
    }

    private fun load(): SettingsUiState = SettingsUiState(
        favoritesOnTop = prefs.getBoolean(KEY_FAV_ON_TOP, true),
        showHidden = prefs.getBoolean(KEY_SHOW_HIDDEN, false),
        gamelistDecoration = prefs.getBoolean(KEY_GAMELIST_DECO, true),
    )

    private companion object {
        const val KEY_FAV_ON_TOP = "favorites_on_top"
        const val KEY_SHOW_HIDDEN = "show_hidden"
        const val KEY_GAMELIST_DECO = "gamelist_decoration"
    }
}

data class SettingsUiState(
    val favoritesOnTop: Boolean,
    val showHidden: Boolean,
    val gamelistDecoration: Boolean,
)
