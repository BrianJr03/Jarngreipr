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
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BLUR_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BLUR_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_DIMMING_LEVEL
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ESDE_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LAST_SELECTED_SYSTEM
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_ALIGNMENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_ONLY_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_OFFSET_X
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_OFFSET_Y

import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_SYSTEM_IMAGES_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_SYSTEM_LOGOS_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_SYSTEM_IMAGE_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_SYSTEM_LOGO_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_GAME_IMAGE_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SINGLE_GAME_LOGO_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_HEIGHT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_WIDTH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_GAME_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_SCREENSAVER_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_SYSTEM_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_VIDEO_BEHAVIOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_VOLUME
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_USE_SYSTEM_SPECIFIC
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_LOOP_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MUSIC_IGNORE_AUDIO_FOCUS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_APP_DRAWER_OPACITY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_PRESS_SHORTCUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_VISIBLE_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_DESCRIPTION_OVERLAY_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_MARQUEE_FOR_SYSTEM
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_MARQUEE_FOR_GAME
import jr.brian.home.model.Shortcut
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_DIMMING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BACKGROUND_DIMMING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_ON_GAME_LAUNCH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_LOGO_BRIGHTNESS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_PERSIST_BACKGROUND_BRIGHTNESS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_POWER_EVENTS_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_RANDOM_SYSTEM_IMAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSAVER_BEHAVIOR
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_SYSTEM_LOGO
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_AUDIO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_DELAY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BACKGROUND_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_MEDIA_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_EXCLUDE_EFFECTS_FROM_HOME
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDE_UI_FOR_GAME_BROWSING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_POSITION_LOCKED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ANDROID_GAMES_BACKGROUND_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_MIN_WIDTH_PERCENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_OVERLAY_MEDIA_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SELECT_BUTTON_WALLPAPER_TOGGLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_WALLPAPER_TOGGLE_TARGET
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
            when (behaviorName) {
                "Blackout", "DimOverlay" -> ScreensaverBehavior.PowerOff
                else -> ScreensaverBehavior.valueOf(behaviorName)
            }
        } catch (_: IllegalArgumentException) {
            ScreensaverBehavior.ShowContent
        }

        val musicVideoBehaviorName = prefs.getString(KEY_MUSIC_VIDEO_BEHAVIOR, MusicVideoBehavior.Duck.value)
        val musicVideoBehavior = MusicVideoBehavior.fromValue(musicVideoBehaviorName ?: MusicVideoBehavior.Duck.value)

        val overlayMediaTypeName = prefs.getString(KEY_OVERLAY_MEDIA_TYPE, OverlayMediaType.Marquees.name)
        val overlayMediaType = OverlayMediaType.fromValue(overlayMediaTypeName ?: OverlayMediaType.Marquees.name)

        val wallpaperToggleTargetName = prefs.getString(KEY_WALLPAPER_TOGGLE_TARGET, WallpaperToggleTarget.SystemWallpaper.name)
        val wallpaperToggleTarget = WallpaperToggleTarget.fromName(wallpaperToggleTargetName ?: WallpaperToggleTarget.SystemWallpaper.name)

        val videoScaleModeName = prefs.getString(KEY_VIDEO_SCALE_MODE, VideoScaleMode.FillScreen.name)
        val videoScaleMode = try {
            VideoScaleMode.valueOf(videoScaleModeName ?: VideoScaleMode.FillScreen.name)
        } catch (_: IllegalArgumentException) {
            VideoScaleMode.FillScreen
        }

        val systemBackgroundScaleModeName = prefs.getString(KEY_SYSTEM_BACKGROUND_SCALE_MODE, BackgroundScaleMode.Crop.name)
        val systemBackgroundScaleMode = try {
            BackgroundScaleMode.valueOf(systemBackgroundScaleModeName ?: BackgroundScaleMode.Crop.name)
        } catch (_: IllegalArgumentException) {
            BackgroundScaleMode.Crop
        }

        val gameBackgroundScaleModeName = prefs.getString(KEY_GAME_BACKGROUND_SCALE_MODE, BackgroundScaleMode.Crop.name)
        val gameBackgroundScaleMode = try {
            BackgroundScaleMode.valueOf(gameBackgroundScaleModeName ?: BackgroundScaleMode.Crop.name)
        } catch (_: IllegalArgumentException) {
            BackgroundScaleMode.Crop
        }

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

        val descriptionOverlayEnabledPagesString = prefs.getString(KEY_DESCRIPTION_OVERLAY_PAGES, null)
        val descriptionOverlayEnabledPages = descriptionOverlayEnabledPagesString
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()

        val legacyBlurLevel = prefs.getInt(KEY_BLUR_LEVEL, 0)
        val systemBlurLevel = if (prefs.contains(KEY_SYSTEM_BLUR_LEVEL)) {
            prefs.getInt(KEY_SYSTEM_BLUR_LEVEL, 0)
        } else {
            legacyBlurLevel
        }
        val gameBlurLevel = if (prefs.contains(KEY_GAME_BLUR_LEVEL)) {
            prefs.getInt(KEY_GAME_BLUR_LEVEL, 0)
        } else {
            legacyBlurLevel
        }

        return ESDEPrefsState(
            animationStyle = animationStyle,
            animationDuration = prefs.getInt(KEY_ANIMATION_DURATION, 300),
            animationScale = prefs.getInt(KEY_ANIMATION_SCALE, 90).toFloat() / 100f,
            blurLevel = legacyBlurLevel,
            systemBlurLevel = systemBlurLevel,
            gameBlurLevel = gameBlurLevel,
            dimmingLevel = prefs.getInt(KEY_DIMMING_LEVEL, 20).coerceAtMost(70),
            backgroundColor = prefs.getInt(KEY_BACKGROUND_COLOR, Color.Black.toArgb()),
            videoEnabled = prefs.getBoolean(KEY_VIDEO_ENABLED, false),
            videoDelaySeconds = prefs.getInt(KEY_VIDEO_DELAY, 3),
            videoAudioEnabled = prefs.getBoolean(KEY_VIDEO_AUDIO_ENABLED, false),
            videoScaleMode = videoScaleMode,
            systemBackgroundScaleMode = systemBackgroundScaleMode,
            gameBackgroundScaleMode = gameBackgroundScaleMode,
            esdeEnabled = prefs.getBoolean(KEY_ESDE_ENABLED, false),
            lastSelectedSystem = prefs.getString(KEY_LAST_SELECTED_SYSTEM, null),
            systemImageType = systemImageType,
            gameImageType = gameImageType,
            showSystemLogo = prefs.getBoolean(KEY_SHOW_SYSTEM_LOGO, true),
            logoAlignment = logoAlignment,
            logoOffsetX = prefs.getFloat(KEY_LOGO_OFFSET_X, 0f),
            logoOffsetY = prefs.getFloat(KEY_LOGO_OFFSET_Y, 0f),
            randomSystemImage = prefs.getBoolean(KEY_RANDOM_SYSTEM_IMAGE, false),
            powerEventsEnabled = prefs.getBoolean(KEY_POWER_EVENTS_ENABLED, true),
            persistOnGameLaunch = prefs.getBoolean(KEY_PERSIST_ON_GAME_LAUNCH, false),
            persistBackgroundBrightness = prefs.getInt(KEY_PERSIST_BACKGROUND_BRIGHTNESS, 80),
            persistLogoBrightness = prefs.getInt(KEY_PERSIST_LOGO_BRIGHTNESS, 100),
            customSystemLogosPath = prefs.getString(KEY_CUSTOM_SYSTEM_LOGOS_PATH, null),
            customSystemImagesPath = prefs.getString(KEY_CUSTOM_SYSTEM_IMAGES_PATH, null),
            singleSystemImagePath = prefs.getString(KEY_SINGLE_SYSTEM_IMAGE_PATH, null),
            singleSystemLogoPath = prefs.getString(KEY_SINGLE_SYSTEM_LOGO_PATH, null),
            singleGameImagePath = prefs.getString(KEY_SINGLE_GAME_IMAGE_PATH, null),
            singleGameLogoPath = prefs.getString(KEY_SINGLE_GAME_LOGO_PATH, null),
            marqueeWidth = prefs.getInt(KEY_MARQUEE_WIDTH, 300),
            marqueeHeight = prefs.getInt(KEY_MARQUEE_HEIGHT, 150),
            screensaverBehavior = screensaverBehavior,
            musicEnabled = prefs.getBoolean(KEY_MUSIC_ENABLED, false),
            musicPath = prefs.getString(KEY_MUSIC_PATH, null),
            musicSystemEnabled = prefs.getBoolean(KEY_MUSIC_SYSTEM_ENABLED, true),
            musicGameEnabled = prefs.getBoolean(KEY_MUSIC_GAME_ENABLED, true),
            musicScreensaverEnabled = prefs.getBoolean(KEY_MUSIC_SCREENSAVER_ENABLED, true),
            musicVideoBehavior = musicVideoBehavior,
            musicVolume = prefs.getInt(KEY_MUSIC_VOLUME, 100),
            musicUseSystemSpecific = prefs.getBoolean(KEY_MUSIC_USE_SYSTEM_SPECIFIC, true),
            musicLoopEnabled = prefs.getBoolean(KEY_MUSIC_LOOP_ENABLED, true),
            musicIgnoreAudioFocus = prefs.getBoolean(KEY_MUSIC_IGNORE_AUDIO_FOCUS, false),
            appDrawerOpacity = prefs.getInt(KEY_APP_DRAWER_OPACITY, 100),
            marqueePressShortcut = marqueePressShortcut,
            marqueePressShortcutAppPackage = prefs.getString(KEY_MARQUEE_PRESS_SHORTCUT_APP_PACKAGE, null),
            marqueeHiddenPages = marqueeHiddenPages,
            descriptionOverlayEnabledPages = descriptionOverlayEnabledPages,
            showMarqueeForSystem = prefs.getBoolean(KEY_SHOW_MARQUEE_FOR_SYSTEM, true),
            showMarqueeForGame = prefs.getBoolean(KEY_SHOW_MARQUEE_FOR_GAME, true),
            gameBackgroundDimming = prefs.getInt(KEY_GAME_BACKGROUND_DIMMING, 20).coerceAtMost(70),
            systemBackgroundDimming = prefs.getInt(KEY_SYSTEM_BACKGROUND_DIMMING, 20).coerceAtMost(70),
            customMediaPath = prefs.getString(KEY_CUSTOM_MEDIA_PATH, null),
            excludeEffectsFromHome = prefs.getBoolean(KEY_EXCLUDE_EFFECTS_FROM_HOME, false),
            hideUIForGameBrowsing = prefs.getBoolean(KEY_HIDE_UI_FOR_GAME_BROWSING, false),
            marqueePositionLocked = prefs.getBoolean(KEY_MARQUEE_POSITION_LOCKED, false),
            androidGamesBackgroundScale = prefs.getFloat(KEY_ANDROID_GAMES_BACKGROUND_SCALE, 0.5f),
            marqueeMinWidthPercent = prefs.getFloat(KEY_MARQUEE_MIN_WIDTH_PERCENT, 0.5f),
            overlayMediaType = overlayMediaType,
            logoOnlyMode = prefs.getBoolean(KEY_LOGO_ONLY_MODE, false),
            selectButtonWallpaperToggle = prefs.getBoolean(KEY_SELECT_BUTTON_WALLPAPER_TOGGLE, false),
            wallpaperToggleTarget = wallpaperToggleTarget
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

    fun setSystemBlurLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 25)
        _state.value = _state.value.copy(systemBlurLevel = coercedLevel)
        prefs.edit { putInt(KEY_SYSTEM_BLUR_LEVEL, coercedLevel) }
    }

    fun setGameBlurLevel(level: Int) {
        val coercedLevel = level.coerceIn(0, 25)
        _state.value = _state.value.copy(gameBlurLevel = coercedLevel)
        prefs.edit { putInt(KEY_GAME_BLUR_LEVEL, coercedLevel) }
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

    fun setVideoScaleMode(mode: VideoScaleMode) {
        _state.value = _state.value.copy(videoScaleMode = mode)
        prefs.edit { putString(KEY_VIDEO_SCALE_MODE, mode.name) }
    }

    fun setSystemBackgroundScaleMode(mode: BackgroundScaleMode) {
        _state.value = _state.value.copy(systemBackgroundScaleMode = mode)
        prefs.edit { putString(KEY_SYSTEM_BACKGROUND_SCALE_MODE, mode.name) }
    }

    fun setGameBackgroundScaleMode(mode: BackgroundScaleMode) {
        _state.value = _state.value.copy(gameBackgroundScaleMode = mode)
        prefs.edit { putString(KEY_GAME_BACKGROUND_SCALE_MODE, mode.name) }
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

    fun setLogoOffset(x: Float, y: Float) {
        _state.value = _state.value.copy(logoOffsetX = x, logoOffsetY = y)
        prefs.edit {
            putFloat(KEY_LOGO_OFFSET_X, x)
            putFloat(KEY_LOGO_OFFSET_Y, y)
        }
    }

    fun setRandomSystemImage(random: Boolean) {
        _state.value = _state.value.copy(randomSystemImage = random)
        prefs.edit { putBoolean(KEY_RANDOM_SYSTEM_IMAGE, random) }
    }

    fun setPowerEventsEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(powerEventsEnabled = enabled)
        prefs.edit { putBoolean(KEY_POWER_EVENTS_ENABLED, enabled) }
    }

    fun setPersistOnGameLaunch(persist: Boolean) {
        _state.value = _state.value.copy(persistOnGameLaunch = persist)
        prefs.edit { putBoolean(KEY_PERSIST_ON_GAME_LAUNCH, persist) }
    }

    fun setPersistBackgroundBrightness(brightness: Int) {
        val coercedBrightness = brightness.coerceIn(30, 100)
        _state.value = _state.value.copy(persistBackgroundBrightness = coercedBrightness)
        prefs.edit { putInt(KEY_PERSIST_BACKGROUND_BRIGHTNESS, coercedBrightness) }
    }

    fun setPersistLogoBrightness(brightness: Int) {
        val coercedBrightness = brightness.coerceIn(30, 100)
        _state.value = _state.value.copy(persistLogoBrightness = coercedBrightness)
        prefs.edit { putInt(KEY_PERSIST_LOGO_BRIGHTNESS, coercedBrightness) }
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

    fun setSingleSystemImagePath(path: String?) {
        _state.value = _state.value.copy(singleSystemImagePath = path)
        if (path != null) {
            prefs.edit { putString(KEY_SINGLE_SYSTEM_IMAGE_PATH, path) }
        } else {
            prefs.edit { remove(KEY_SINGLE_SYSTEM_IMAGE_PATH) }
        }
    }

    fun setSingleSystemLogoPath(path: String?) {
        _state.value = _state.value.copy(singleSystemLogoPath = path)
        if (path != null) {
            prefs.edit { putString(KEY_SINGLE_SYSTEM_LOGO_PATH, path) }
        } else {
            prefs.edit { remove(KEY_SINGLE_SYSTEM_LOGO_PATH) }
        }
    }

    fun setSingleGameImagePath(path: String?) {
        _state.value = _state.value.copy(singleGameImagePath = path)
        if (path != null) {
            prefs.edit { putString(KEY_SINGLE_GAME_IMAGE_PATH, path) }
        } else {
            prefs.edit { remove(KEY_SINGLE_GAME_IMAGE_PATH) }
        }
    }

    fun setSingleGameLogoPath(path: String?) {
        _state.value = _state.value.copy(singleGameLogoPath = path)
        if (path != null) {
            prefs.edit { putString(KEY_SINGLE_GAME_LOGO_PATH, path) }
        } else {
            prefs.edit { remove(KEY_SINGLE_GAME_LOGO_PATH) }
        }
    }

    fun setMarqueeWidth(width: Int) {
        val coercedWidth = width.coerceIn(40, 600)
        _state.value = _state.value.copy(marqueeWidth = coercedWidth)
        prefs.edit { putInt(KEY_MARQUEE_WIDTH, coercedWidth) }
    }

    fun setMarqueeHeight(height: Int) {
        val coercedHeight = height.coerceIn(40, 600)
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

    fun setMusicVolume(volume: Int) {
        val coercedVolume = volume.coerceIn(0, 100)
        _state.value = _state.value.copy(musicVolume = coercedVolume)
        prefs.edit { putInt(KEY_MUSIC_VOLUME, coercedVolume) }
    }

    fun setMusicUseSystemSpecific(useSystemSpecific: Boolean) {
        _state.value = _state.value.copy(musicUseSystemSpecific = useSystemSpecific)
        prefs.edit { putBoolean(KEY_MUSIC_USE_SYSTEM_SPECIFIC, useSystemSpecific) }
    }

    fun setMusicLoopEnabled(loopEnabled: Boolean) {
        _state.value = _state.value.copy(musicLoopEnabled = loopEnabled)
        prefs.edit { putBoolean(KEY_MUSIC_LOOP_ENABLED, loopEnabled) }
    }

    fun setMusicIgnoreAudioFocus(ignoreAudioFocus: Boolean) {
        _state.value = _state.value.copy(musicIgnoreAudioFocus = ignoreAudioFocus)
        prefs.edit { putBoolean(KEY_MUSIC_IGNORE_AUDIO_FOCUS, ignoreAudioFocus) }
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
            hiddenPages - pageIndex
        } else {
            hiddenPages + pageIndex
        }
        
        _state.value = _state.value.copy(marqueeHiddenPages = newPages)
        if (newPages.isEmpty()) {
            prefs.edit { remove(KEY_MARQUEE_VISIBLE_PAGES) }
        } else {
            prefs.edit { putString(KEY_MARQUEE_VISIBLE_PAGES, newPages.joinToString(",")) }
        }
    }

    fun toggleDescriptionOverlayPage(pageIndex: Int) {
        val enabledPages = _state.value.descriptionOverlayEnabledPages
        val newPages = if (enabledPages.contains(pageIndex)) {
            enabledPages - pageIndex
        } else {
            enabledPages + pageIndex
        }
        
        _state.value = _state.value.copy(descriptionOverlayEnabledPages = newPages)
        if (newPages.isEmpty()) {
            prefs.edit { remove(KEY_DESCRIPTION_OVERLAY_PAGES) }
        } else {
            prefs.edit { putString(KEY_DESCRIPTION_OVERLAY_PAGES, newPages.joinToString(",")) }
        }
    }

    fun setGameBackgroundDimming(level: Int) {
        val coercedLevel = level.coerceIn(0, 70)
        _state.value = _state.value.copy(gameBackgroundDimming = coercedLevel)
        prefs.edit { putInt(KEY_GAME_BACKGROUND_DIMMING, coercedLevel) }
    }

    fun setSystemBackgroundDimming(level: Int) {
        val coercedLevel = level.coerceIn(0, 70)
        _state.value = _state.value.copy(systemBackgroundDimming = coercedLevel)
        prefs.edit { putInt(KEY_SYSTEM_BACKGROUND_DIMMING, coercedLevel) }
    }

    fun setCustomMediaPath(path: String?) {
        _state.value = _state.value.copy(customMediaPath = path)
        if (path != null) {
            prefs.edit { putString(KEY_CUSTOM_MEDIA_PATH, path) }
        } else {
            prefs.edit { remove(KEY_CUSTOM_MEDIA_PATH) }
        }
    }

    fun setExcludeEffectsFromHome(exclude: Boolean) {
        _state.value = _state.value.copy(excludeEffectsFromHome = exclude)
        prefs.edit { putBoolean(KEY_EXCLUDE_EFFECTS_FROM_HOME, exclude) }
    }

    fun setShowLogoForSystem(show: Boolean) {
        _state.value = _state.value.copy(showMarqueeForSystem = show)
        prefs.edit { putBoolean(KEY_SHOW_MARQUEE_FOR_SYSTEM, show) }
    }

    fun setShowLogoForGame(show: Boolean) {
        _state.value = _state.value.copy(showMarqueeForGame = show)
        prefs.edit { putBoolean(KEY_SHOW_MARQUEE_FOR_GAME, show) }
    }

    fun setHideUIForGameBrowsing(hide: Boolean) {
        _state.value = _state.value.copy(hideUIForGameBrowsing = hide)
        prefs.edit { putBoolean(KEY_HIDE_UI_FOR_GAME_BROWSING, hide) }
    }

    fun setLogoPositionLocked(locked: Boolean) {
        _state.value = _state.value.copy(marqueePositionLocked = locked)
        prefs.edit { putBoolean(KEY_MARQUEE_POSITION_LOCKED, locked) }
    }

    fun toggleLogoPositionLocked() {
        setLogoPositionLocked(!_state.value.marqueePositionLocked)
    }

    fun setAndroidGamesBackgroundScale(scale: Float) {
        val coercedScale = scale.coerceIn(0.2f, 1.0f)
        _state.value = _state.value.copy(androidGamesBackgroundScale = coercedScale)
        prefs.edit { putFloat(KEY_ANDROID_GAMES_BACKGROUND_SCALE, coercedScale) }
    }

    fun setMarqueeMinWidthPercent(percent: Float) {
        val coercedPercent = percent.coerceIn(0.3f, 1.0f)
        _state.value = _state.value.copy(marqueeMinWidthPercent = coercedPercent)
        prefs.edit { putFloat(KEY_MARQUEE_MIN_WIDTH_PERCENT, coercedPercent) }
    }

    fun setOverlayMediaType(type: OverlayMediaType) {
        _state.value = _state.value.copy(overlayMediaType = type)
        prefs.edit { putString(KEY_OVERLAY_MEDIA_TYPE, type.name) }
    }

    fun setLogoOnlyMode(enabled: Boolean) {
        _state.value = _state.value.copy(logoOnlyMode = enabled)
        prefs.edit { putBoolean(KEY_LOGO_ONLY_MODE, enabled) }
    }

    fun setSelectButtonWallpaperToggle(enabled: Boolean) {
        _state.value = _state.value.copy(selectButtonWallpaperToggle = enabled)
        prefs.edit { putBoolean(KEY_SELECT_BUTTON_WALLPAPER_TOGGLE, enabled) }
    }

    fun setWallpaperToggleTarget(target: WallpaperToggleTarget) {
        _state.value = _state.value.copy(wallpaperToggleTarget = target)
        prefs.edit { putString(KEY_WALLPAPER_TOGGLE_TARGET, target.name) }
    }
}