package jr.brian.home.esde.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.esde.animation.AnimationStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.core.content.edit
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANIMATION_DURATION
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANIMATION_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANIMATION_STYLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_BACKGROUND_COLOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_BLUR_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_DIMMING_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ESDE_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LAST_SELECTED_SYSTEM
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_ALIGNMENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDE_CONTENT_ON_VIDEO
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_SYSTEM_IMAGES_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_SYSTEM_LOGOS_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_HEIGHT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_WIDTH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_GAME_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_SCREENSAVER_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_SYSTEM_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_VIDEO_BEHAVIOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_APP_DRAWER_OPACITY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_PRESS_SHORTCUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_VISIBLE_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_OVERLAY_PAGES
import jr.brian.home.model.Shortcut
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_BRIGHTNESS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_DIMMING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_ON_GAME_LAUNCH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_POWER_EVENTS_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_RANDOM_SYSTEM_IMAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSAVER_BEHAVIOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_SYSTEM_LOGO
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_AUDIO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_DELAY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.PREFS_NAME

class ESDEPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<ESDEPrefsState> = _state.asStateFlow()

    private fun loadState(): ESDEPrefsState {
        val styleName = prefs.getString(KEY_ANIMATION_STYLE, AnimationStyle.Fade.name)
        val animationStyle = try {
            AnimationStyle.valueOf(styleName ?: AnimationStyle.Fade.name)
        } catch (_: IllegalArgumentException) {
            AnimationStyle.Fade
        }
        
        val systemImageTypeName = prefs.getString(KEY_SYSTEM_IMAGE_TYPE, SystemImageType.Fanart.name)
        val systemImageType = try {
            SystemImageType.valueOf(systemImageTypeName ?: SystemImageType.Fanart.name)
        } catch (_: IllegalArgumentException) {
            SystemImageType.Fanart
        }
        
        val gameImageTypeName = prefs.getString(KEY_GAME_IMAGE_TYPE, GameImageType.Screenshots.name)
        val gameImageType = try {
            GameImageType.valueOf(gameImageTypeName ?: GameImageType.Screenshots.name)
        } catch (_: IllegalArgumentException) {
            GameImageType.Screenshots
        }
        
        val logoAlignmentName = prefs.getString(KEY_LOGO_ALIGNMENT, LogoAlignment.Center.name)
        val logoAlignment = try {
            LogoAlignment.valueOf(logoAlignmentName ?: LogoAlignment.Center.name)
        } catch (_: IllegalArgumentException) {
            LogoAlignment.Center
        }
        
        val screensaverBehaviorName = prefs.getString(KEY_SCREENSAVER_BEHAVIOR, ScreensaverBehavior.ShowContent.name)
        val screensaverBehavior = try {
            val behaviorName = screensaverBehaviorName ?: ScreensaverBehavior.ShowContent.name
            // Migrate old Blackout and DimOverlay to PowerOff
            when (behaviorName) {
                "Blackout", "DimOverlay" -> ScreensaverBehavior.PowerOff
                else -> ScreensaverBehavior.valueOf(behaviorName)
            }
        } catch (_: IllegalArgumentException) {
            ScreensaverBehavior.ShowContent
        }

        val musicVideoBehaviorName = prefs.getString(KEY_MUSIC_VIDEO_BEHAVIOR, MusicVideoBehavior.Duck.value)
        val musicVideoBehavior = MusicVideoBehavior.fromValue(musicVideoBehaviorName ?: MusicVideoBehavior.Duck.value)

        val marqueePressShortcutName = prefs.getString(KEY_MARQUEE_PRESS_SHORTCUT, Shortcut.NONE.name)
        val marqueePressShortcut = try {
            Shortcut.valueOf(marqueePressShortcutName ?: Shortcut.NONE.name)
        } catch (_: IllegalArgumentException) {
            Shortcut.NONE
        }

        val marqueeHiddenPagesString = prefs.getString(KEY_MARQUEE_VISIBLE_PAGES, null)
        val marqueeHiddenPages = marqueeHiddenPagesString
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

        val marqueeOverlayDisabledPagesString = prefs.getString(KEY_MARQUEE_OVERLAY_PAGES, null)
        val marqueeOverlayDisabledPages = marqueeOverlayDisabledPagesString
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

        return ESDEPrefsState(
            animationStyle = animationStyle,
            animationDuration = prefs.getInt(KEY_ANIMATION_DURATION, 300),
            animationScale = prefs.getInt(KEY_ANIMATION_SCALE, 90).toFloat() / 100f,
            blurLevel = prefs.getInt(KEY_BLUR_LEVEL, 0),
            dimmingLevel = prefs.getInt(KEY_DIMMING_LEVEL, 20).coerceAtMost(70),
            backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.Black.toArgb()),
            videoEnabled = prefs.getBoolean(KEY_VIDEO_ENABLED, false),
            videoDelaySeconds = prefs.getInt(KEY_VIDEO_DELAY, 3),
            videoAudioEnabled = prefs.getBoolean(KEY_VIDEO_AUDIO_ENABLED, false),
            esdeEnabled = prefs.getBoolean(KEY_ESDE_ENABLED, false),
            lastSelectedSystem = prefs.getString(KEY_LAST_SELECTED_SYSTEM, null),
            systemImageType = systemImageType,
            gameImageType = gameImageType,
            showSystemLogo = prefs.getBoolean(KEY_SHOW_SYSTEM_LOGO, true),
            logoAlignment = logoAlignment,
            randomSystemImage = prefs.getBoolean(KEY_RANDOM_SYSTEM_IMAGE, false),
            hideContentOnVideo = prefs.getBoolean(KEY_HIDE_CONTENT_ON_VIDEO, false),
            powerEventsEnabled = prefs.getBoolean(KEY_POWER_EVENTS_ENABLED, true),
            persistOnGameLaunch = prefs.getBoolean(KEY_PERSIST_ON_GAME_LAUNCH, false),
            customSystemLogosPath = prefs.getString(KEY_CUSTOM_SYSTEM_LOGOS_PATH, null),
            customSystemImagesPath = prefs.getString(KEY_CUSTOM_SYSTEM_IMAGES_PATH, null),
            marqueeWidth = prefs.getInt(KEY_MARQUEE_WIDTH, 300),
            marqueeHeight = prefs.getInt(KEY_MARQUEE_HEIGHT, 150),
            screensaverBehavior = screensaverBehavior,
            musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, false),
            musicPath = prefs.getString(KEY_MUSIC_PATH, null),
            musicSystemEnabled = prefs.getBoolean(KEY_MUSIC_SYSTEM_ENABLED, true),
            musicGameEnabled = prefs.getBoolean(KEY_MUSIC_GAME_ENABLED, true),
            musicScreensaverEnabled = prefs.getBoolean(KEY_MUSIC_SCREENSAVER_ENABLED, true),
            musicVideoBehavior = musicVideoBehavior,
            appDrawerOpacity = prefs.getInt(KEY_APP_DRAWER_OPACITY, 100),
            marqueePressShortcut = marqueePressShortcut,
            marqueePressShortcutAppPackage = prefs.getString(KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE, null),
            marqueeHiddenPages = marqueeHiddenPages,
            marqueeOverlayDisabledPages = marqueeOverlayDisabledPages,
            logoBrightness = prefs.getInt(KEY_LOGO_BRIGHTNESS, 100),
            gameBackgroundDimming = prefs.getInt(KEY_GAME_BACKGROUND_DIMMING, 20)
        )
    }

    fun setAnimationStyle(style: AnimationStyle) {
        _state.value = _state.value.copy(animationStyle = style)
        prefs.edit { putString(KEY_ANIMATION_STYLE, style.name) }
    }

    fun setAnimationDuration(duration: Int) {
        _state.value = _state.value.copy(animationDuration = duration)
        prefs.edit { putInt(KEY_ANIMATION_DURATION, duration) }
    }

    fun setAnimationScale(scale: Float) {
        _state.value = _state.value.copy(animationScale = scale)
        prefs.edit { putInt(KEY_ANIMATION_SCALE, (scale * 100).toInt()) }
    }

    fun setBlurLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 25)
        _state.value = _state.value.copy(blurLevel = coercedLevel)
        prefs.edit { putInt(KEY_BLUR_LEVEL, coercedLevel) }
    }

    fun setDimmingLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 70)
        _state.value = _state.value.copy(dimmingLevel = coercedLevel)
        prefs.edit { putInt(KEY_DIMMING_LEVEL, coercedLevel) }
    }

    fun setBackgroundColor(color: Int) {
        _state.value = _state.value.copy(backgroundColor = color)
        prefs.edit { putInt(KEY_BACKGROUND_COLOR, color) }
    }

    fun setVideoEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(videoEnabled = enabled)
        prefs.edit { putBoolean(KEY_VIDEO_ENABLED, enabled) }
    }

    fun setVideoDelaySeconds(seconds: Int) {
        _state.value = _state.value.copy(videoDelaySeconds = seconds)
        prefs.edit { putInt(KEY_VIDEO_DELAY, seconds) }
    }

    fun setVideoAudioEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(videoAudioEnabled = enabled)
        prefs.edit { putBoolean(KEY_VIDEO_AUDIO_ENABLED, enabled) }
    }


    fun setLastSelectedSystem(systemName: String?) {
        _state.value = _state.value.copy(lastSelectedSystem = systemName)
        if (systemName != null) {
            prefs.edit { putString(KEY_LAST_SELECTED_SYSTEM, systemName) }
        } else {
            prefs.edit { remove(KEY_LAST_SELECTED_SYSTEM) }
        }
    }

    fun setSystemImageType(type: SystemImageType) {
        _state.value = _state.value.copy(systemImageType = type)
        prefs.edit { putString(KEY_SYSTEM_IMAGE_TYPE, type.name) }
    }

    fun setGameImageType(type: GameImageType) {
        _state.value = _state.value.copy(gameImageType = type)
        prefs.edit { putString(KEY_GAME_IMAGE_TYPE, type.name) }
    }

    fun setLogoAlignment(alignment: LogoAlignment) {
        _state.value = _state.value.copy(logoAlignment = alignment)
        prefs.edit { putString(KEY_LOGO_ALIGNMENT, alignment.name) }
    }

    fun setRandomSystemImage(random: Boolean) {
        _state.value = _state.value.copy(randomSystemImage = random)
        prefs.edit { putBoolean(KEY_RANDOM_SYSTEM_IMAGE, random) }
    }

    fun setHideContentOnVideo(hide: Boolean) {
        _state.value = _state.value.copy(hideContentOnVideo = hide)
        prefs.edit { putBoolean(KEY_HIDE_CONTENT_ON_VIDEO, hide) }
    }

    fun setPowerEventsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(powerEventsEnabled = enabled)
        prefs.edit { putBoolean(KEY_POWER_EVENTS_ENABLED, enabled) }
    }

    fun setPersistOnGameLaunch(persist: Boolean) {
        _state.value = _state.value.copy(persistOnGameLaunch = persist)
        prefs.edit { putBoolean(KEY_PERSIST_ON_GAME_LAUNCH, persist) }
    }

    fun setCustomSystemLogosPath(path: String?) {
        _state.value = _state.value.copy(customSystemLogosPath = path)
        if (path != null) {
            prefs.edit { putString(KEY_CUSTOM_SYSTEM_LOGOS_PATH, path) }
        } else {
            prefs.edit { remove(KEY_CUSTOM_SYSTEM_LOGOS_PATH) }
        }
    }

    fun setCustomSystemImagesPath(path: String?) {
        _state.value = _state.value.copy(customSystemImagesPath = path)
        if (path != null) {
            prefs.edit { putString(KEY_CUSTOM_SYSTEM_IMAGES_PATH, path) }
        } else {
            prefs.edit { remove(KEY_CUSTOM_SYSTEM_IMAGES_PATH) }
        }
    }

    fun setMarqueeWidth(width: Int) {
        val coercedWidth = width.coerceIn(100, 600)
        _state.value = _state.value.copy(marqueeWidth = coercedWidth)
        prefs.edit { putInt(KEY_MARQUEE_WIDTH, coercedWidth) }
    }

    fun setMarqueeHeight(height: Int) {
        val coercedHeight = height.coerceIn(50, 400)
        _state.value = _state.value.copy(marqueeHeight = coercedHeight)
        prefs.edit { putInt(KEY_MARQUEE_HEIGHT, coercedHeight) }
    }

    fun setScreensaverBehavior(behavior: ScreensaverBehavior) {
        _state.value = _state.value.copy(screensaverBehavior = behavior)
        prefs.edit { putString(KEY_SCREENSAVER_BEHAVIOR, behavior.name) }
    }

    fun setMusicEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(musicEnabled = enabled)
        prefs.edit { putBoolean(KEY_MUSIC_ENABLED, enabled) }
    }

    fun setMusicPath(path: String?) {
        _state.value = _state.value.copy(musicPath = path)
        if (path != null) {
            prefs.edit { putString(KEY_MUSIC_PATH, path) }
        } else {
            prefs.edit { remove(KEY_MUSIC_PATH) }
        }
    }

    fun setMusicSystemEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(musicSystemEnabled = enabled)
        prefs.edit { putBoolean(KEY_MUSIC_SYSTEM_ENABLED, enabled) }
    }

    fun setMusicGameEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(musicGameEnabled = enabled)
        prefs.edit { putBoolean(KEY_MUSIC_GAME_ENABLED, enabled) }
    }

    fun setMusicScreensaverEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(musicScreensaverEnabled = enabled)
        prefs.edit { putBoolean(KEY_MUSIC_SCREENSAVER_ENABLED, enabled) }
    }

    fun setMusicVideoBehavior(behavior: MusicVideoBehavior) {
        _state.value = _state.value.copy(musicVideoBehavior = behavior)
        prefs.edit { putString(KEY_MUSIC_VIDEO_BEHAVIOR, behavior.value) }
    }

    fun setAppDrawerOpacity(opacity: Int) {
        val coercedOpacity = opacity.coerceIn(0, 100)
        _state.value = _state.value.copy(appDrawerOpacity = coercedOpacity)
        prefs.edit { putInt(KEY_APP_DRAWER_OPACITY, coercedOpacity) }
    }

    fun setMarqueePressShortcut(shortcut: Shortcut) {
        _state.value = _state.value.copy(marqueePressShortcut = shortcut)
        prefs.edit { putString(KEY_MARQUEE_PRESS_SHORTCUT, shortcut.name) }
    }

    fun setMarqueePressShortcutAppPackage(packageName: String?) {
        _state.value = _state.value.copy(marqueePressShortcutAppPackage = packageName)
        if (packageName != null) {
            prefs.edit { putString(KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE, packageName) }
        } else {
            prefs.edit { remove(KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE) }
        }
    }

    fun toggleMarqueePageVisibility(pageIndex: Int) {
        val hiddenPages = _state.value.marqueeHiddenPages
        val newPages = if (hiddenPages.contains(pageIndex)) {
            // Currently hidden, remove from set to show
            hiddenPages - pageIndex
        } else {
            // Currently visible, add to set to hide
            hiddenPages + pageIndex
        }
        
        _state.value = _state.value.copy(marqueeHiddenPages = newPages)
        if (newPages.isEmpty()) {
            prefs.edit { remove(KEY_MARQUEE_VISIBLE_PAGES) }
        } else {
            prefs.edit { putString(KEY_MARQUEE_VISIBLE_PAGES, newPages.joinToString(",")) }
        }
    }

    fun toggleMarqueeOverlayPage(pageIndex: Int) {
        val disabledPages = _state.value.marqueeOverlayDisabledPages
        val newPages = if (disabledPages.contains(pageIndex)) {
            // Currently disabled, remove from set to enable
            disabledPages - pageIndex
        } else {
            // Currently enabled, add to set to disable
            disabledPages + pageIndex
        }
        
        _state.value = _state.value.copy(marqueeOverlayDisabledPages = newPages)
        if (newPages.isEmpty()) {
            prefs.edit { remove(KEY_MARQUEE_OVERLAY_PAGES) }
        } else {
            prefs.edit { putString(KEY_MARQUEE_OVERLAY_PAGES, newPages.joinToString(",")) }
        }
    }

    fun setLogoBrightness(brightness: Int) {
        val coercedBrightness = brightness.coerceIn(0, 100)
        _state.value = _state.value.copy(logoBrightness = coercedBrightness)
        prefs.edit { putInt(KEY_LOGO_BRIGHTNESS, coercedBrightness) }
    }

    fun setGameBackgroundDimming(level: Int) {
        val coercedLevel = level.coerceIn(0, 70)
        _state.value = _state.value.copy(gameBackgroundDimming = coercedLevel)
        prefs.edit { putInt(KEY_GAME_BACKGROUND_DIMMING, coercedLevel) }
    }
}