package jr.brian.home.esde.preferences

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.model.Shortcut

/**
 * Defines the preferred media type for system background images
 */
enum class SystemImageType(val folderName: String?) {
    None(null),
    All(null),
    Fanart("fanart"),
    Screenshots("screenshots"),
    TitleScreens("titlescreens");

    companion object {
        /** Returns all types that have actual media folders (excludes None and All) */
        fun randomizableTypes(): List<SystemImageType> =
            entries.filter { it.folderName != null }
    }
}

/**
 * Defines the preferred media type for game background images
 */
enum class GameImageType(val folderName: String?) {
    None(null),
    All(null),
    Screenshots("screenshots"),
    Fanart("fanart"),
    TitleScreens("titlescreens"),
    Covers("covers"),
    MixImages("miximages"),
    Description(null);

    companion object {
        /** Returns all types that have actual media folders (excludes None and All) */
        fun randomizableTypes(): List<GameImageType> =
            entries.filter { it.folderName != null }
    }
}

/**
 * Defines the alignment position for the system logo
 */
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

/**
 * Defines the behavior of the companion screen when ES-DE screensaver is active
 */
enum class ScreensaverBehavior {
    /** Show companion content with screensaver game artwork */
    ShowContent,
    /** Power off screen - shows clock and battery info */
    PowerOff
}

/**
 * Defines the behavior of music when a video starts playing
 */
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

/**
 * Defines how videos are scaled to fit the screen
 */
enum class VideoScaleMode {
    /** Fill the screen, cropping edges if necessary */
    FillScreen,
    /** Fit the entire video, showing black bars on unused areas */
    FitVideo
}

/**
 * Defines how background images are scaled to fit the screen
 */
enum class BackgroundScaleMode {
    /** Crop to fill the screen (zoomed effect) */
    Crop,
    /** Fit the entire image, showing background color in unused areas */
    Fit
}

/**
 * Defines the media type to use for the overlay (marquee/logo) display
 */
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
    val systemBackgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop,
    val gameBackgroundScaleMode: BackgroundScaleMode = BackgroundScaleMode.Crop,
    
    val esdeEnabled: Boolean = false,
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
    
    val musicEnabled: Boolean = false,
    val musicPath: String? = null,
    val musicSystemEnabled: Boolean = true,
    val musicGameEnabled: Boolean = true,
    val musicScreensaverEnabled: Boolean = true,
    val musicVideoBehavior: MusicVideoBehavior = MusicVideoBehavior.Duck,
    val musicVolume: Int = 100,
    val musicUseSystemSpecific: Boolean = true,
    val musicLoopEnabled: Boolean = true,
    
    val appDrawerOpacity: Int = 100,
    
    val gameBackgroundDimming: Int = 20,
    val systemBackgroundDimming: Int = 20,
    
    val customMediaPath: String? = null,
    
    val excludeEffectsFromHome: Boolean = false,
    
    val hideUIForGameBrowsing: Boolean = false,
    
    val marqueePositionLocked: Boolean = false,
    
    val androidGamesBackgroundScale: Float = 0.5f,
    
    val marqueeMinWidthPercent: Float = 0.5f,
    
    val overlayMediaType: OverlayMediaType = OverlayMediaType.Marquees,
    val logoOnlyMode: Boolean = false,
    val selectButtonWallpaperToggle: Boolean = false
) {
    val dimmingLevelFloat: Float get() = dimmingLevel / 100f
    val appDrawerOpacityFloat: Float get() = appDrawerOpacity / 100f
    val musicVolumeFloat: Float get() = musicVolume / 100f
    val gameBackgroundDimmingFloat: Float get() = gameBackgroundDimming / 100f
    val systemBackgroundDimmingFloat: Float get() = systemBackgroundDimming / 100f
    
    /**
     * Check if marquee should be visible on a specific page.
     * Page is visible if NOT in the hidden set.
     */
    fun isMarqueeVisibleOnPage(pageIndex: Int): Boolean {
        return !marqueeHiddenPages.contains(pageIndex)
    }

    fun isAndroidGamesSelected() = lastSelectedSystem == "androidgames"
    
    fun isDescriptionOverlayOnPage(pageIndex: Int): Boolean {
        return descriptionOverlayEnabledPages.contains(pageIndex)
    }

    fun isLogoFreePosEnabled() : Boolean {
        return logoAlignment == LogoAlignment.FreePosition
    }
}