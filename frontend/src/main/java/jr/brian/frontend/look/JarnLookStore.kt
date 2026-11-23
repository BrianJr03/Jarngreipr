package jr.brian.frontend.look

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.ui.theme.JarnLooks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted Jarn FE look and reduced-motion flag. The id is kept as written, so a backup from a
 * newer build keeps its look; [JarnLooks.fromId] shows the default until that look exists here.
 */
@Singleton
class JarnLookStore @Inject constructor(
    @param:ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _lookId = MutableStateFlow(
        prefs.getString(KEY_LOOK, null) ?: JarnLooks.Default.id,
    )
    val lookIdState: StateFlow<String> = _lookId.asStateFlow()
    val lookId: String get() = _lookId.value

    private val _reducedMotion = MutableStateFlow(prefs.getBoolean(KEY_REDUCED_MOTION, false))
    val reducedMotionState: StateFlow<Boolean> = _reducedMotion.asStateFlow()
    val reducedMotion: Boolean get() = _reducedMotion.value

    private val _display = MutableStateFlow(readDisplay())
    val displayState: StateFlow<JarnDisplayPrefs> = _display.asStateFlow()
    val display: JarnDisplayPrefs get() = _display.value

    fun updateDisplay(transform: (JarnDisplayPrefs) -> JarnDisplayPrefs) {
        val next = transform(_display.value).coerced()
        _display.value = next
        prefs.edit {
            putFloat(KEY_SYSTEM_TILE_SCALE, next.systemTileScale)
            putFloat(KEY_GAME_TILE_SCALE, next.gameTileScale)
            putBoolean(KEY_HINTS_VISIBLE, next.hintsVisible)
            putBoolean(KEY_BUTTON_HAPTICS, next.buttonHaptics)
        }
    }

    private fun readDisplay() = JarnDisplayPrefs(
        systemTileScale = prefs.getFloat(KEY_SYSTEM_TILE_SCALE, JarnDisplayPrefs.DEFAULT_TILE_SCALE),
        gameTileScale = prefs.getFloat(KEY_GAME_TILE_SCALE, JarnDisplayPrefs.DEFAULT_TILE_SCALE),
        hintsVisible = prefs.getBoolean(KEY_HINTS_VISIBLE, true),
        buttonHaptics = prefs.getBoolean(KEY_BUTTON_HAPTICS, true),
    ).coerced()

    fun setLookId(id: String) {
        _lookId.value = id
        prefs.edit { putString(KEY_LOOK, id) }
    }

    fun setReducedMotion(enabled: Boolean) {
        _reducedMotion.value = enabled
        prefs.edit { putBoolean(KEY_REDUCED_MOTION, enabled) }
    }

    private companion object {
        const val PREFS_NAME = "jarnfe_look_prefs"
        const val KEY_LOOK = "look_id"
        const val KEY_REDUCED_MOTION = "reduced_motion"
        const val KEY_SYSTEM_TILE_SCALE = "system_tile_scale"
        const val KEY_GAME_TILE_SCALE = "game_tile_scale"
        const val KEY_HINTS_VISIBLE = "hints_visible"
        const val KEY_BUTTON_HAPTICS = "button_haptics"
    }
}
