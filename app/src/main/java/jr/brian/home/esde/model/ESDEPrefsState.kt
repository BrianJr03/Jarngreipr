package jr.brian.home.esde.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.model.Shortcut

enum class SystemImageType(val folderName: String?) {
    None(null),
    All(null),
    Fanart("fanart"),
    Screenshots("screenshots"),
    TitleScreens("titlescreens");

    companion object {
        fun randomizableTypes(): List<SystemImageType> =
            entries.filter { it.folderName != null }
    }
}

enum class GameImageType(val folderName: String?) {
    None(null),
    All(null),
    Screenshots("screenshots"),
    Fanart("fanart"),
    TitleScreens("titlescreens"),
    Covers("covers"),
    Marquee("marquees"),
    MixImages("miximages"),
    Description(null);

    companion object {
        fun randomizableTypes(): List<GameImageType> =
            entries.filter { it.folderName != null }
    }
}

enum class LogoAlignment {
    TopLeft,
    Top,
    TopRight,
    Center,
    BottomLeft,
    Bottom,
    BottomRight,
    FreePosition
}

enum class ScreensaverBehavior {
    ShowContent,
    PowerOff,
    Floaty
}

enum class MusicVideoBehavior(val value: String) {
    Continue("continue"),
    Duck("duck"),
    Pause("pause");

    companion object {
        fun fromValue(value: String): MusicVideoBehavior {
            return MusicVideoBehavior.entries.find { it.value == value } ?: Continue
        }
    }
}

enum class VideoScaleMode {
    FillScreen,
    FitVideo
}

enum class BackgroundScaleMode {
    Crop,
    Fit
}

enum class WallpaperToggleTarget(val displayName: String) {
    SystemWallpaper("Wallpaper"),
    SavedImage("Image"),
    SavedGif("GIF"),
    SavedVideo("Video"),
    Default("Default");

    companion object {
        fun fromName(name: String): WallpaperToggleTarget {
            return entries.find { it.name == name } ?: SystemWallpaper
        }
    }
}

enum class SystemLaunchTrigger(val displayName: String) {
    NoAction("No Action"),
    GameStart("Game Start"),
    GameSelect("Game Select"),
    SystemSelect("System Select");

    companion object {
        fun fromName(name: String): SystemLaunchTrigger {
            return entries.find { it.name == name } ?: NoAction
        }
    }
}

enum class OverlayMediaType(val folderName: String, val displayName: String) {
    Marquees("marquees", "Marquees"),
    ThreeDBoxes("3dboxes", "3D Boxes"),
    Covers("covers", "Covers"),
    Screenshots("screenshots", "Screenshots"),
    Fanart("fanart", "Fanart"),
    MixImages("miximages", "Miximages");

    companion object {
        fun fromValue(value: String): OverlayMediaType {
            return entries.find { it.name == value } ?: Marquees
        }
    }
}

data class ESDEPrefsState(
    val animationStyle: AnimationStyle = AnimationStyle.Fade,
    val animationDuration: Int = 300,
    val animationScale: Float = 0.9f,
    
    val blurLevel: Int = 0,
    val systemBlurLevel: Int = 0,
    val gameBlurLevel: Int = 0,
    
    val dimmingLevel: Int = 20,
    val backgroundColor: Int = Color.Black.toArgb(),
    
    val videoEnabled: Boolean = false,
    val videoDelaySeconds: Int = 3,
    val videoAudioEnabled: Boolean = false,
    val videoScaleMode: VideoScaleMode = VideoScaleMode.FillScreen,
    val videoOverlayEnabled: Boolean = false,
    val systemBackgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop,
    val gameBackgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop,
    
    val lastSelectedSystem: String? = null,
    val systemImageType: SystemImageType = SystemImageType.Fanart,
    val gameImageType: GameImageType = GameImageType.Screenshots,
    val showSystemLogo: Boolean = true,
    val logoAlignment: LogoAlignment = LogoAlignment.Center,
    val logoOffsetX: Float = 0f,
    val logoOffsetY: Float = 0f,
    val randomSystemImage: Boolean = false,
    val powerEventsEnabled: Boolean = true,
    val persistOnGameLaunch: Boolean = false,
    val persistBackgroundBrightness: Int = 80,
    val persistLogoBrightness: Int = 100,
    val customSystemLogosPath: String? = null,
    val customSystemImagesPath: String? = null,
    val singleSystemImagePath: String? = null,
    val singleSystemLogoPath: String? = null,
    val singleGameImagePath: String? = null,
    val singleGameLogoPath: String? = null,
    
    val marqueeWidth: Int = 300,
    val marqueeHeight: Int = 150,
    val marqueePressShortcut: Shortcut = Shortcut.NONE,
    val marqueePressShortcutAppPackage: String? = null,
    val marqueeHiddenPages: Set<Int> = emptySet(),
    val descriptionOverlayEnabledPages: Set<Int> = emptySet(),
    val showMarqueeForSystem: Boolean = true,
    val showMarqueeForGame: Boolean = true,
    
    val screensaverBehavior: ScreensaverBehavior = ScreensaverBehavior.ShowContent,
    val screensaverFloatyAppCount: Int = 7,
    
    val musicEnabled: Boolean = false,
    val musicPath: String? = null,
    val musicSystemEnabled: Boolean = true,
    val musicGameEnabled: Boolean = true,
    val musicScreensaverEnabled: Boolean = true,
    val musicVideoBehavior: MusicVideoBehavior = MusicVideoBehavior.Duck,
    val musicVolume: Int = 100,
    val musicUseSystemSpecific: Boolean = true,
    val musicLoopEnabled: Boolean = true,
    val musicIgnoreAudioFocus: Boolean = false,
    
    val appDrawerOpacity: Int = 100,
    
    val gameBackgroundDimming: Int = 20,
    val systemBackgroundDimming: Int = 20,
    
    val customMediaPath: String? = null,
    
    val effectsExcludedPages: Set<Int> = emptySet(),
    
    val hideUIForGameBrowsing: Boolean = false,
    
    val marqueePositionLocked: Boolean = false,
    
    val gameBackgroundScale: Float = 0.5f,
    
    val marqueeMinWidthPercent: Float = 0.5f,
    
    val overlayMediaType: OverlayMediaType = OverlayMediaType.Marquees,
    val logoOnlyMode: Boolean = false,
    val selectButtonWallpaperToggle: Boolean = false,
    val wallpaperToggleTarget: WallpaperToggleTarget = WallpaperToggleTarget.SystemWallpaper,

    val romsPaths: List<String> = emptyList(),
    val systemAppMap: Map<String, String?> = emptyMap(),
    val systemLaunchTriggerMap: Map<String, SystemLaunchTrigger> = emptyMap(),
    val systemTopScreenSet: Set<String> = emptySet(),
    val systemBgVideoMuted: Boolean = true,
    val systemBgVideoLooping: Boolean = true,
    val gameEmulatorMap: Map<String, String> = emptyMap(),
    val gameCommandMap: Map<String, String> = emptyMap(),
    val gameCoreMap: Map<String, String> = emptyMap(),
    val hiddenGames: Set<String> = emptySet(),
    val romSearchUseWallpaper: Boolean = true
) {
    val dimmingLevelFloat: Float get() = dimmingLevel / 100f
    val appDrawerOpacityFloat: Float get() = appDrawerOpacity / 100f
    val musicVolumeFloat: Float get() = musicVolume / 100f
    val gameBackgroundDimmingFloat: Float get() = gameBackgroundDimming / 100f
    val systemBackgroundDimmingFloat: Float get() = systemBackgroundDimming / 100f
    val persistBackgroundBrightnessFloat: Float get() = persistBackgroundBrightness / 100f
    val persistLogoBrightnessFloat: Float get() = persistLogoBrightness / 100f
    
    fun isMarqueeVisibleOnPage(pageIndex: Int): Boolean {
        return !marqueeHiddenPages.contains(pageIndex)
    }

    fun isEffectsExcludedOnPage(pageIndex: Int): Boolean {
        return effectsExcludedPages.contains(pageIndex)
    }

    fun isAndroidGamesSelected() = lastSelectedSystem == "androidgames"
    
    fun isDescriptionOverlayOnPage(pageIndex: Int): Boolean {
        return descriptionOverlayEnabledPages.contains(pageIndex)
    }

    fun isLogoFreePosEnabled() : Boolean {
        return logoAlignment == LogoAlignment.FreePosition
    }
}
