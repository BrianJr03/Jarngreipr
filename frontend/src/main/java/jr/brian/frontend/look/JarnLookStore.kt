package jr.brian.frontend.look

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.ui.motion.JarnPageTransition
import jr.brian.core.ui.theme.JarnLooks
import jr.brian.core.ui.theme.JarnSwitchCue
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

    private val _matchCommandPanelFonts = MutableStateFlow(prefs.getBoolean(KEY_MATCH_COMMAND_PANEL_FONTS, false))

    /** Independent of [matchCommandPanelState], so fonts and colors can be matched one at a time. */
    val matchCommandPanelFontsState: StateFlow<Boolean> = _matchCommandPanelFonts.asStateFlow()
    val matchCommandPanelFonts: Boolean get() = _matchCommandPanelFonts.value

    private val _fontsPromptShown = MutableStateFlow(prefs.getBoolean(KEY_FONTS_PROMPT_SHOWN, false))

    /** The one-time "use Command Panel fonts" prompt was answered. Never turns back off. */
    val fontsPromptShownState: StateFlow<Boolean> = _fontsPromptShown.asStateFlow()
    val fontsPromptShown: Boolean get() = _fontsPromptShown.value

    fun markFontsPromptShown() {
        _fontsPromptShown.value = true
        prefs.edit { putBoolean(KEY_FONTS_PROMPT_SHOWN, true) }
    }

    private val _esdeImportNoticeShown = MutableStateFlow(prefs.getBoolean(KEY_ESDE_IMPORT_NOTICE_SHOWN, false))

    /** The one-time "ES-DE found" notice after skipping the tour was shown. Never turns back off. */
    val esdeImportNoticeShown: Boolean get() = _esdeImportNoticeShown.value

    fun markEsdeImportNoticeShown() {
        _esdeImportNoticeShown.value = true
        prefs.edit { putBoolean(KEY_ESDE_IMPORT_NOTICE_SHOWN, true) }
    }

    private val _videoHintShown = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_HINT_SHOWN, false))

    /** The one-time "this game has a video preview" hint was shown. Never turns back off. */
    val videoHintShown: Boolean get() = _videoHintShown.value

    fun markVideoHintShown() {
        _videoHintShown.value = true
        prefs.edit { putBoolean(KEY_VIDEO_HINT_SHOWN, true) }
    }

    private val _liveWallpaperId = MutableStateFlow(
        prefs.getString(KEY_LIVE_WALLPAPER, null).withoutOffValue(),
    )

    /** Kept as written, so a wallpaper from a newer build returns once this build has it. */
    val liveWallpaperIdState: StateFlow<String?> = _liveWallpaperId.asStateFlow()
    val liveWallpaperId: String? get() = _liveWallpaperId.value

    private val _sameWallpaperOnBoth = MutableStateFlow(prefs.getBoolean(KEY_SAME_WALLPAPER_ON_BOTH, true))

    /** True shows the frontend wallpaper on both screens. False gives the bottom screen its own. */
    val sameWallpaperOnBothState: StateFlow<Boolean> = _sameWallpaperOnBoth.asStateFlow()
    val sameWallpaperOnBoth: Boolean get() = _sameWallpaperOnBoth.value

    private val _bottomWallpaperUri = MutableStateFlow(prefs.getString(KEY_BOTTOM_WALLPAPER_URI, null))

    /** The bottom screen's own image or GIF, used only while [sameWallpaperOnBoth] is off. */
    val bottomWallpaperUriState: StateFlow<String?> = _bottomWallpaperUri.asStateFlow()
    val bottomWallpaperUri: String? get() = _bottomWallpaperUri.value

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
            putString(KEY_SWITCH_CUE, next.switchCue.id)
            putString(KEY_SYSTEM_VIEW, next.systemView.id)
            putString(KEY_GAME_VIEW, next.gameView.id)
            putString(KEY_PAGE_TRANSITION, next.pageTransition.id)
            putBoolean(KEY_SYSTEM_NAMES_VISIBLE, next.systemNamesVisible)
            putBoolean(KEY_RANDOM_VISIBLE, next.randomVisible)
            putBoolean(KEY_LAST_PLAYED_VISIBLE, next.lastPlayedVisible)
            putBoolean(KEY_SYSTEM_LOGO_VISIBLE, next.systemLogoVisible)
            putBoolean(KEY_GAME_COUNT_VISIBLE, next.gameCountVisible)
            putBoolean(KEY_LAST_PLAYED_TEXT_VISIBLE, next.lastPlayedTextVisible)
            putBoolean(KEY_GAME_ROW_VISIBLE, next.gameRowVisible)
            putBoolean(KEY_SYSTEM_BACKGROUND_VISIBLE, next.systemBackgroundVisible)
            putBoolean(KEY_PLAYING_SCREEN_VISIBLE, next.playingScreenVisible)
            putString(KEY_PLAYING_DIM, next.playingDim.id)
            putBoolean(KEY_PLAYING_SCREEN_OFF, next.playingScreenOff)
            putBoolean(KEY_VIDEO_PREVIEWS, next.videoPreviews)
            putBoolean(KEY_VIDEO_PREVIEW_SOUND, next.videoPreviewSound)
            putString(KEY_VIDEO_FIT, next.videoFit.id)
            putBoolean(KEY_PAUSE_ON_BATTERY_SAVER, next.pauseOnBatterySaver)
            putString(KEY_TAB_FOCUS, next.tabFocus.id)
            putString(KEY_LIBRARY_TAB_ORDER, next.librarySectionOrder.joinToString(ORDER_SEPARATOR))
            putString(KEY_DECK_TAB_ORDER, next.deckTabOrder.joinToString(ORDER_SEPARATOR))
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
        switchCue = JarnSwitchCue.fromId(prefs.getString(KEY_SWITCH_CUE, null)),
        systemView = JarnBrowseView.fromId(prefs.getString(KEY_SYSTEM_VIEW, null)),
        gameView = JarnBrowseView.fromId(prefs.getString(KEY_GAME_VIEW, null)),
        pageTransition = JarnPageTransition.fromId(prefs.getString(KEY_PAGE_TRANSITION, null)),
        systemNamesVisible = prefs.getBoolean(KEY_SYSTEM_NAMES_VISIBLE, true),
        randomVisible = prefs.getBoolean(KEY_RANDOM_VISIBLE, true),
        lastPlayedVisible = prefs.getBoolean(KEY_LAST_PLAYED_VISIBLE, true),
        systemLogoVisible = prefs.getBoolean(KEY_SYSTEM_LOGO_VISIBLE, true),
        gameCountVisible = prefs.getBoolean(KEY_GAME_COUNT_VISIBLE, true),
        lastPlayedTextVisible = prefs.getBoolean(KEY_LAST_PLAYED_TEXT_VISIBLE, true),
        gameRowVisible = prefs.getBoolean(KEY_GAME_ROW_VISIBLE, true),
        systemBackgroundVisible = prefs.getBoolean(KEY_SYSTEM_BACKGROUND_VISIBLE, true),
        playingScreenVisible = prefs.getBoolean(KEY_PLAYING_SCREEN_VISIBLE, true),
        playingDim = JarnPlayingDim.fromId(prefs.getString(KEY_PLAYING_DIM, null)) ?: JarnPlayingDim.ThirtySeconds,
        playingScreenOff = prefs.getBoolean(KEY_PLAYING_SCREEN_OFF, false),
        videoPreviews = prefs.getBoolean(KEY_VIDEO_PREVIEWS, false),
        videoPreviewSound = prefs.getBoolean(KEY_VIDEO_PREVIEW_SOUND, false),
        videoFit = JarnVideoFit.fromId(prefs.getString(KEY_VIDEO_FIT, null)) ?: JarnVideoFit.Fit,
        pauseOnBatterySaver = prefs.getBoolean(KEY_PAUSE_ON_BATTERY_SAVER, true),
        tabFocus = JarnTabFocus.fromId(prefs.getString(KEY_TAB_FOCUS, null)) ?: JarnTabFocus.Both,
        librarySectionOrder = prefs.getString(KEY_LIBRARY_TAB_ORDER, null).orderIds(),
        deckTabOrder = prefs.getString(KEY_DECK_TAB_ORDER, null).orderIds(),
    ).coerced()

    private fun String?.orderIds(): List<String> =
        this?.split(ORDER_SEPARATOR)?.map(String::trim)?.filter(String::isNotEmpty).orEmpty()

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

    fun setMatchCommandPanelFonts(enabled: Boolean) {
        _matchCommandPanelFonts.value = enabled
        prefs.edit { putBoolean(KEY_MATCH_COMMAND_PANEL_FONTS, enabled) }
    }

    fun setLiveWallpaperId(id: String?) {
        val next = id.withoutOffValue()
        _liveWallpaperId.value = next
        prefs.edit {
            if (next == null) remove(KEY_LIVE_WALLPAPER) else putString(KEY_LIVE_WALLPAPER, next)
        }
    }

    fun setSameWallpaperOnBoth(same: Boolean) {
        _sameWallpaperOnBoth.value = same
        prefs.edit { putBoolean(KEY_SAME_WALLPAPER_ON_BOTH, same) }
    }

    fun setBottomWallpaperUri(uri: String?) {
        _bottomWallpaperUri.value = uri
        prefs.edit {
            if (uri == null) remove(KEY_BOTTOM_WALLPAPER_URI) else putString(KEY_BOTTOM_WALLPAPER_URI, uri)
        }
    }

    private fun String?.withoutOffValue() = takeUnless { it == JARN_LIVE_WALLPAPER_OFF }

    private companion object {
        const val PREFS_NAME = "jarnfe_look_prefs"
        const val KEY_LOOK = "look_id"
        const val KEY_REDUCED_MOTION = "reduced_motion"
        const val KEY_MATCH_COMMAND_PANEL = "match_command_panel"
        const val KEY_MATCH_COMMAND_PANEL_FONTS = "match_command_panel_fonts"
        const val KEY_FONTS_PROMPT_SHOWN = "fonts_prompt_shown"
        const val KEY_ESDE_IMPORT_NOTICE_SHOWN = "esde_import_notice_shown"
        const val KEY_LIVE_WALLPAPER = "live_wallpaper_id"
        const val KEY_SAME_WALLPAPER_ON_BOTH = "wallpaper_same_on_both"
        const val KEY_BOTTOM_WALLPAPER_URI = "bottom_wallpaper_uri"
        const val KEY_SYSTEM_TILE_SCALE = "system_tile_scale"
        const val KEY_GAME_TILE_SCALE = "game_tile_scale"
        const val KEY_HINTS_VISIBLE = "hints_visible"
        const val KEY_BUTTON_HAPTICS = "button_haptics"
        const val KEY_TAP_CONTROL = "tap_control"
        const val KEY_APP_NAMES_VISIBLE = "app_names_visible"
        const val KEY_UNDO_COUNTDOWN_VISIBLE = "undo_countdown_visible"
        const val KEY_SWITCH_CUE = "switch_cue"
        const val KEY_SYSTEM_VIEW = "system_view"
        const val KEY_GAME_VIEW = "game_view"
        const val KEY_PAGE_TRANSITION = "page_transition"
        const val KEY_SYSTEM_NAMES_VISIBLE = "system_names_visible"
        const val KEY_RANDOM_VISIBLE = "now_random_visible"
        const val KEY_LAST_PLAYED_VISIBLE = "now_last_played_visible"
        const val KEY_SYSTEM_LOGO_VISIBLE = "now_system_logo_visible"
        const val KEY_GAME_COUNT_VISIBLE = "now_game_count_visible"
        const val KEY_LAST_PLAYED_TEXT_VISIBLE = "now_last_played_text_visible"
        const val KEY_GAME_ROW_VISIBLE = "now_game_row_visible"
        const val KEY_SYSTEM_BACKGROUND_VISIBLE = "now_system_background_visible"
        const val KEY_PLAYING_SCREEN_VISIBLE = "playing_screen_visible"
        const val KEY_PLAYING_DIM = "playing_dim"
        const val KEY_PLAYING_SCREEN_OFF = "playing_screen_off"
        const val KEY_VIDEO_PREVIEWS = "video_previews"
        const val KEY_VIDEO_PREVIEW_SOUND = "video_preview_sound"
        const val KEY_VIDEO_FIT = "video_fit"
        const val KEY_PAUSE_ON_BATTERY_SAVER = "pause_on_battery_saver"
        const val KEY_TAB_FOCUS = "tab_focus"
        const val KEY_LIBRARY_TAB_ORDER = "library_tab_order"
        const val KEY_DECK_TAB_ORDER = "deck_tab_order"
        const val ORDER_SEPARATOR = ","
        const val KEY_VIDEO_HINT_SHOWN = "video_hint_shown"
        const val KEY_FIRST_RUN_DONE = "first_run_done"
        const val KEY_FIRST_RUN_STEP = "first_run_step"
    }
}
