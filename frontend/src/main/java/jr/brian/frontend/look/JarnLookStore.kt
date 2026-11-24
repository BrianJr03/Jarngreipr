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

/** Written to backups when no live wallpaper is chosen, so a restore can turn one off. */
const val JARN_LIVE_WALLPAPER_OFF = "off"

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

    private val _matchCommandPanel = MutableStateFlow(prefs.getBoolean(KEY_MATCH_COMMAND_PANEL, false))

    /** Separate from the look id, so turning it off returns the saved look exactly. */
    val matchCommandPanelState: StateFlow<Boolean> = _matchCommandPanel.asStateFlow()
    val matchCommandPanel: Boolean get() = _matchCommandPanel.value

    private val _liveWallpaperId = MutableStateFlow(
        prefs.getString(KEY_LIVE_WALLPAPER, null).withoutOffValue(),
    )

    /** Kept as written, so a wallpaper from a newer build returns once this build has it. */
    val liveWallpaperIdState: StateFlow<String?> = _liveWallpaperId.asStateFlow()
    val liveWallpaperId: String? get() = _liveWallpaperId.value

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
            putString(KEY_TAP_CONTROL, next.tapControl.id)
            putBoolean(KEY_APP_NAMES_VISIBLE, next.appNamesVisible)
            putBoolean(KEY_UNDO_COUNTDOWN_VISIBLE, next.undoCountdownVisible)
            putInt(KEY_SCREEN_SWITCH_MILLIS, next.screenSwitchMillis)
        }
    }

    private val _firstRunDone = MutableStateFlow(prefs.getBoolean(KEY_FIRST_RUN_DONE, false))
    val firstRunDoneState: StateFlow<Boolean> = _firstRunDone.asStateFlow()
    val firstRunDone: Boolean get() = _firstRunDone.value

    /** The tour's place on this device. Never backed up, since it only matters mid-tour. */
    val firstRunStep: String? get() = prefs.getString(KEY_FIRST_RUN_STEP, null)

    fun saveFirstRunStep(step: String) {
        prefs.edit { putString(KEY_FIRST_RUN_STEP, step) }
    }

    fun markFirstRunDone() {
        _firstRunDone.value = true
        prefs.edit {
            putBoolean(KEY_FIRST_RUN_DONE, true)
            remove(KEY_FIRST_RUN_STEP)
        }
    }

    private fun readDisplay() = JarnDisplayPrefs(
        systemTileScale = prefs.getFloat(KEY_SYSTEM_TILE_SCALE, JarnDisplayPrefs.DEFAULT_TILE_SCALE),
        gameTileScale = prefs.getFloat(KEY_GAME_TILE_SCALE, JarnDisplayPrefs.DEFAULT_TILE_SCALE),
        hintsVisible = prefs.getBoolean(KEY_HINTS_VISIBLE, true),
        buttonHaptics = prefs.getBoolean(KEY_BUTTON_HAPTICS, true),
        tapControl = JarnTapControl.fromId(prefs.getString(KEY_TAP_CONTROL, null)) ?: JarnTapControl.FollowTap,
        appNamesVisible = prefs.getBoolean(KEY_APP_NAMES_VISIBLE, true),
        undoCountdownVisible = prefs.getBoolean(KEY_UNDO_COUNTDOWN_VISIBLE, true),
        screenSwitchMillis = prefs.getInt(KEY_SCREEN_SWITCH_MILLIS, JarnDisplayPrefs.DEFAULT_SCREEN_SWITCH_MILLIS),
    ).coerced()

    fun setLookId(id: String) {
        _lookId.value = id
        prefs.edit { putString(KEY_LOOK, id) }
    }

    fun setReducedMotion(enabled: Boolean) {
        _reducedMotion.value = enabled
        prefs.edit { putBoolean(KEY_REDUCED_MOTION, enabled) }
    }

    fun setMatchCommandPanel(enabled: Boolean) {
        _matchCommandPanel.value = enabled
        prefs.edit { putBoolean(KEY_MATCH_COMMAND_PANEL, enabled) }
    }

    fun setLiveWallpaperId(id: String?) {
        val next = id.withoutOffValue()
        _liveWallpaperId.value = next
        prefs.edit {
            if (next == null) remove(KEY_LIVE_WALLPAPER) else putString(KEY_LIVE_WALLPAPER, next)
        }
    }

    private fun String?.withoutOffValue() = takeUnless { it == JARN_LIVE_WALLPAPER_OFF }

    private companion object {
        const val PREFS_NAME = "jarnfe_look_prefs"
        const val KEY_LOOK = "look_id"
        const val KEY_REDUCED_MOTION = "reduced_motion"
        const val KEY_MATCH_COMMAND_PANEL = "match_command_panel"
        const val KEY_LIVE_WALLPAPER = "live_wallpaper_id"
        const val KEY_SYSTEM_TILE_SCALE = "system_tile_scale"
        const val KEY_GAME_TILE_SCALE = "game_tile_scale"
        const val KEY_HINTS_VISIBLE = "hints_visible"
        const val KEY_BUTTON_HAPTICS = "button_haptics"
        const val KEY_TAP_CONTROL = "tap_control"
        const val KEY_APP_NAMES_VISIBLE = "app_names_visible"
        const val KEY_UNDO_COUNTDOWN_VISIBLE = "undo_countdown_visible"
        const val KEY_SCREEN_SWITCH_MILLIS = "screen_switch_millis"
        const val KEY_FIRST_RUN_DONE = "first_run_done"
        const val KEY_FIRST_RUN_STEP = "first_run_step"
    }
}
