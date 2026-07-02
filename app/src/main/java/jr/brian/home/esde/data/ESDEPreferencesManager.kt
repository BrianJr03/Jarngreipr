package jr.brian.home.esde.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.esde.model.AnimationStyle
import jr.brian.home.esde.model.BackgroundScaleMode
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.FrontendLayout
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.esde.model.LogoAlignment
import jr.brian.home.esde.model.MusicVideoBehavior
import jr.brian.home.esde.model.OverlayMediaType
import jr.brian.home.esde.model.RomSearchCardMediaType
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.esde.model.PlatformImageFolderType
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.model.SystemImageType
import jr.brian.home.esde.model.SystemLaunchTrigger
import jr.brian.home.esde.model.VideoScaleMode
import jr.brian.home.esde.model.WallpaperToggleTarget
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SCREENSAVER_FLOATY_APP_COUNT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SHOW_SYSTEM_LOGO
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_IMAGE_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_AUDIO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_DELAY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_VIDEO_OVERLAY_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BACKGROUND_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_SCALE_MODE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CUSTOM_MEDIA_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_EXCLUDE_EFFECTS_FROM_HOME
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_EFFECTS_EXCLUDED_PAGES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDE_UI_FOR_GAME_BROWSING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_POSITION_LOCKED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_BACKGROUND_SCALE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_MARQUEE_MIN_WIDTH_PERCENT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_OVERLAY_MEDIA_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SELECT_BUTTON_WALLPAPER_TOGGLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_WALLPAPER_TOGGLE_TARGET
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROMS_PATH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROMS_PATHS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_APP_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_AUTO_LAUNCH
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_LAUNCH_TRIGGER_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_TOP_SCREEN
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BG_VIDEO_MUTED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_BG_VIDEO_LOOPING
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_EMULATOR_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_COMMAND_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_CORE_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_HIDDEN_GAMES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SAF_TREE_URIS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_USE_WALLPAPER
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_CARD_MEDIA_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_GAME_MEDIA_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_HIDE_NO_METADATA
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_HIDE_NO_IMAGE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_FOCUS_ANIMATION_SPIN
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_BLACK_BACKGROUND
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_VISIBILITY_ANIMATION
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_LOGO_CHANGE_ANIMATION
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_FOCUS_ANIMATION_DISABLED_GAMES
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_SHOW_ALL_ANDROID_APPS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_AUTO_FILTER
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_FOCUS_ANIMATION_DELAY_MS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_IMAGES_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_URI
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_TYPE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_DETAIL_IMAGE_HEIGHT_DP
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_GAME_LAYOUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SECONDARY_MEDIA_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_LAYOUT
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_CUSTOMIZATIONS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_SYSTEM_ORDER
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_HINTS_VISIBLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FLOAT_INTENSITY
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_FRONTEND_FOCUS_HAPTIC_ENABLED
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_CANVAS_CONTINUOUS_SPIN_ROMS
import jr.brian.home.esde.util.ESDEPreferencesConstants.KEY_ROM_SEARCH_HINTS_KB_VISIBLE
import jr.brian.home.esde.util.ESDEPreferencesConstants.PREFS_NAME
import org.json.JSONArray
import org.json.JSONObject

class ESDEPreferencesManager(context: Context) {
    internal val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    internal val customizationJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    internal val _state = MutableStateFlow(loadState())
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

        val romsPathsJson = prefs.getString(KEY_ROMS_PATHS, null)
        val romsPaths: List<String> = if (!romsPathsJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(romsPathsJson)
                (0 until arr.length()).map { arr.getString(it) }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            val legacyPath = prefs.getString(KEY_ROMS_PATH, null)
            if (legacyPath != null) {
                val migrated = listOf(legacyPath)
                prefs.edit {
                    putString(KEY_ROMS_PATHS, JSONArray(migrated).toString())
                    remove(KEY_ROMS_PATH)
                }
                migrated
            } else {
                emptyList()
            }
        }

        val systemAppMapJson = prefs.getString(KEY_SYSTEM_APP_MAP, null)
        val systemAppMap: Map<String, String?> = if (!systemAppMapJson.isNullOrEmpty()) {
            try {
                val json = JSONObject(systemAppMapJson)
                val result = mutableMapOf<String, String?>()
                json.keys().forEach { key -> result[key] = if (json.isNull(key)) null else json.getString(key) }
                result
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        val triggerMapJson = prefs.getString(KEY_SYSTEM_LAUNCH_TRIGGER_MAP, null)
        val systemLaunchTriggerMap: Map<String, SystemLaunchTrigger> = if (!triggerMapJson.isNullOrEmpty()) {
            try {
                val json = JSONObject(triggerMapJson)
                val result = mutableMapOf<String, SystemLaunchTrigger>()
                json.keys().forEach { key -> result[key] = SystemLaunchTrigger.fromName(json.getString(key)) }
                result
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            // Migrate legacy auto-launch set to trigger map
            val autoLaunchJson = prefs.getString(KEY_SYSTEM_AUTO_LAUNCH, null)
            if (!autoLaunchJson.isNullOrEmpty()) {
                try {
                    val arr = JSONArray(autoLaunchJson)
                    val migrated = mutableMapOf<String, SystemLaunchTrigger>()
                    (0 until arr.length()).forEach { migrated[arr.getString(it)] = SystemLaunchTrigger.GameStart }
                    // Persist migration
                    val json = JSONObject()
                    migrated.forEach { (key, value) -> json.put(key, value.name) }
                    prefs.edit {
                        putString(KEY_SYSTEM_LAUNCH_TRIGGER_MAP, json.toString())
                        remove(KEY_SYSTEM_AUTO_LAUNCH)
                    }
                    migrated
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }
        }

        val topScreenJson = prefs.getString(KEY_SYSTEM_TOP_SCREEN, null)
        val systemTopScreenSet: Set<String> = if (!topScreenJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(topScreenJson)
                (0 until arr.length()).map { arr.getString(it) }.toSet()
            } catch (_: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }

        val hiddenGamesJson = prefs.getString(KEY_HIDDEN_GAMES, null)
        val hiddenGames: Set<String> = if (!hiddenGamesJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(hiddenGamesJson)
                (0 until arr.length()).map { arr.getString(it) }.toSet()
            } catch (_: Exception) {
                emptySet()
            }
        } else {
            emptySet()
        }

        // Migrate legacy excludeEffectsFromHome boolean to per-page set
        val effectsExcludedPagesString = prefs.getString(KEY_EFFECTS_EXCLUDED_PAGES, null)
        val effectsExcludedPages: Set<Int> = if (effectsExcludedPagesString != null) {
            effectsExcludedPagesString
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .toSet()
        } else if (prefs.getBoolean(KEY_EXCLUDE_EFFECTS_FROM_HOME, false)) {
            // Migrate: old boolean was true, so mark home tab (index 0) as excluded
            val migrated = setOf(0)
            prefs.edit {
                putString(KEY_EFFECTS_EXCLUDED_PAGES, migrated.joinToString(","))
                remove(KEY_EXCLUDE_EFFECTS_FROM_HOME)
            }
            migrated
        } else {
            emptySet()
        }

        val systemCustomizations: Map<String, SystemCustomization> = prefs.getString(KEY_SYSTEM_CUSTOMIZATIONS, null)
            ?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { customizationJson.decodeFromString<Map<String, SystemCustomization>>(it) }.getOrNull() }
            ?: emptyMap()

        val systemOrder: List<String> = prefs.getString(KEY_SYSTEM_ORDER, null)
            ?.takeIf { it.isNotEmpty() }
            ?.let { runCatching { customizationJson.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList()

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
            videoOverlayEnabled = prefs.getBoolean(KEY_VIDEO_OVERLAY_ENABLED, true),
            systemBackgroundScaleMode = systemBackgroundScaleMode,
            gameBackgroundScaleMode = gameBackgroundScaleMode,
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
            screensaverFloatyAppCount = prefs.getInt(KEY_SCREENSAVER_FLOATY_APP_COUNT, 7).coerceIn(0, 100),
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
            effectsExcludedPages = effectsExcludedPages,
            hideUIForGameBrowsing = prefs.getBoolean(KEY_HIDE_UI_FOR_GAME_BROWSING, false),
            marqueePositionLocked = prefs.getBoolean(KEY_MARQUEE_POSITION_LOCKED, false),
            gameBackgroundScale = prefs.getFloat(KEY_GAME_BACKGROUND_SCALE, 0.5f),
            marqueeMinWidthPercent = prefs.getFloat(KEY_MARQUEE_MIN_WIDTH_PERCENT, 0.5f),
            overlayMediaType = overlayMediaType,
            logoOnlyMode = prefs.getBoolean(KEY_LOGO_ONLY_MODE, false),
            selectButtonWallpaperToggle = prefs.getBoolean(KEY_SELECT_BUTTON_WALLPAPER_TOGGLE, false),
            wallpaperToggleTarget = wallpaperToggleTarget,
            romsPaths = romsPaths,
            systemAppMap = systemAppMap,
            systemLaunchTriggerMap = systemLaunchTriggerMap,
            systemTopScreenSet = systemTopScreenSet,
            systemBgVideoMuted = prefs.getBoolean(KEY_SYSTEM_BG_VIDEO_MUTED, true),
            systemBgVideoLooping = prefs.getBoolean(KEY_SYSTEM_BG_VIDEO_LOOPING, true),
            hiddenGames = hiddenGames,
            gameEmulatorMap = prefs.getString(KEY_GAME_EMULATOR_MAP, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val obj = JSONObject(json)
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    } catch (_: Exception) { emptyMap() }
                } ?: emptyMap(),
            gameCommandMap = prefs.getString(KEY_GAME_COMMAND_MAP, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val obj = JSONObject(json)
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    } catch (_: Exception) { emptyMap() }
                } ?: emptyMap(),
            gameCoreMap = prefs.getString(KEY_GAME_CORE_MAP, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val obj = JSONObject(json)
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    } catch (_: Exception) { emptyMap() }
                } ?: emptyMap(),
            romSearchUseWallpaper = prefs.getBoolean(KEY_ROM_SEARCH_USE_WALLPAPER, true),
            romSearchCardMediaType = prefs.getString(KEY_ROM_SEARCH_CARD_MEDIA_TYPE, null)
                ?.let { runCatching { RomSearchCardMediaType.valueOf(it) }.getOrNull() }
                ?: RomSearchCardMediaType.PhysicalMedia,
            romSearchGameMediaMap = prefs.getString(KEY_ROM_SEARCH_GAME_MEDIA_MAP, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val obj = JSONObject(json)
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    } catch (_: Exception) { emptyMap() }
                } ?: emptyMap(),
            systemMediaMap = prefs.getString(KEY_ROM_SEARCH_SYSTEM_MEDIA_MAP, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val obj = JSONObject(json)
                        obj.keys().asSequence().associateWith { obj.getString(it) }
                    } catch (_: Exception) { emptyMap() }
                } ?: emptyMap(),
            romSearchHideNoMetadata = prefs.getBoolean(KEY_ROM_SEARCH_HIDE_NO_METADATA, false),
            romSearchHideNoImage = prefs.getBoolean(KEY_ROM_SEARCH_HIDE_NO_IMAGE, false),
            romSearchDiscSpin = prefs.getBoolean(KEY_ROM_FOCUS_ANIMATION_SPIN, false),
            romSearchBlackBackground = prefs.getBoolean(KEY_ROM_SEARCH_BLACK_BACKGROUND, true),
            romSearchFocusAnimationDisabledGames = prefs.getString(KEY_ROM_SEARCH_FOCUS_ANIMATION_DISABLED_GAMES, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val arr = JSONArray(json)
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    } catch (_: Exception) { emptySet() }
                } ?: emptySet(),
            logoVisibilityAnimation = prefs.getBoolean(KEY_LOGO_VISIBILITY_ANIMATION, false),
            logoChangeAnimation = prefs.getBoolean(KEY_LOGO_CHANGE_ANIMATION, false),
            romSearchShowAllAndroidApps = prefs.getBoolean(KEY_ROM_SEARCH_SHOW_ALL_ANDROID_APPS, false),
            romSearchPlatformAutoFilter = prefs.getBoolean(KEY_ROM_SEARCH_PLATFORM_AUTO_FILTER, false),
            romSearchFocusAnimationDelayMs = prefs.getInt(KEY_ROM_SEARCH_FOCUS_ANIMATION_DELAY_MS, 150),
            romSearchPlatformImagesEnabled = prefs.getBoolean(KEY_ROM_SEARCH_PLATFORM_IMAGES_ENABLED, false),
            romSearchPlatformImagesFolderUri = prefs.getString(KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_URI, null),
            romSearchPlatformImagesFolderType = prefs.getString(KEY_ROM_SEARCH_PLATFORM_IMAGES_FOLDER_TYPE, null)
                ?.let { runCatching { PlatformImageFolderType.valueOf(it) }.getOrNull() }
                ?: PlatformImageFolderType.Default,
            romSearchDetailImageHeightDp = prefs.getInt(KEY_ROM_SEARCH_DETAIL_IMAGE_HEIGHT_DP, 240),
            romSearchHintsKbVisible = prefs.getBoolean(KEY_ROM_SEARCH_HINTS_KB_VISIBLE, true),
            frontendEnabled = prefs.getBoolean(KEY_FRONTEND_ENABLED, false),
            secondaryMediaEnabled = prefs.getBoolean(KEY_SECONDARY_MEDIA_ENABLED, true),
            systemLayout = prefs.getString(KEY_SYSTEM_LAYOUT, null)
                ?.let { runCatching { FrontendLayout.valueOf(it) }.getOrNull() }
                ?: FrontendLayout.Grid,
            gameLayout = prefs.getString(KEY_GAME_LAYOUT, null)
                ?.let { runCatching { FrontendLayout.valueOf(it) }.getOrNull() }
                ?: FrontendLayout.Grid,
            systemCustomizations = systemCustomizations,
            systemOrder = systemOrder,
            frontendHintsVisible = prefs.getBoolean(KEY_FRONTEND_HINTS_VISIBLE, true),
            frontendFloatIntensity = prefs.getFloat(KEY_FRONTEND_FLOAT_INTENSITY, 1f).coerceIn(0f, 3f),
            frontendFocusHapticEnabled = prefs.getBoolean(KEY_FRONTEND_FOCUS_HAPTIC_ENABLED, true),
            canvasContinuousSpinRoms = prefs.getString(KEY_CANVAS_CONTINUOUS_SPIN_ROMS, null)
                ?.takeIf { it.isNotEmpty() }
                ?.let { json ->
                    try {
                        val arr = JSONArray(json)
                        (0 until arr.length()).map { arr.getString(it) }.toSet()
                    } catch (_: Exception) { emptySet() }
                } ?: emptySet()
        )
    }

}
