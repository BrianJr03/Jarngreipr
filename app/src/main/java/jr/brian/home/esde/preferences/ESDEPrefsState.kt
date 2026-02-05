package jr.brian.home.esde.preferences

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import jr.brian.home.esde.animation.AnimationStyle

/**
 * Defines the preferred media type for system background images
 */
enum class SystemImageType(val folderName: String?) {
    None(null),
    Fanart("fanart"),
    Screenshots("screenshots"),
    TitleScreens("titlescreens")
}

/**
 * Defines the preferred media type for game background images
 */
enum class GameImageType(val folderName: String?) {
    None(null),
    Screenshots("screenshots"),
    Fanart("fanart"),
    TitleScreens("titlescreens"),
    Covers("covers"),
    MixImages("miximages")
}

/**
 * Defines the alignment position for the system logo
 */
enum class LogoAlignment {
    Top,
    Center,
    Bottom
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
            return values().find { it.value == value } ?: Continue
        }
    }
}

data class ESDEPrefsState(
    val animationStyle: AnimationStyle = AnimationStyle.Fade,
    val animationDuration: Int = 300,
    val animationScale: Float = 0.9f,
    val blurLevel: Int = 0,
    val dimmingLevel: Int = 20,
    val backgroundColor: Int = Color.Black.toArgb(),
    val videoEnabled: Boolean = false,
    val videoDelaySeconds: Int = 3,
    val videoAudioEnabled: Boolean = false,
    val esdeEnabled: Boolean = false,
    val lastSelectedSystem: String? = null,
    val systemImageType: SystemImageType = SystemImageType.Fanart,
    val gameImageType: GameImageType = GameImageType.Screenshots,
    val showSystemLogo: Boolean = true,
    val logoAlignment: LogoAlignment = LogoAlignment.Center,
    val randomSystemImage: Boolean = false,
    val hideContentOnVideo: Boolean = false,
    val powerEventsEnabled: Boolean = true,
    val persistOnGameLaunch: Boolean = false,
    val customSystemLogosPath: String? = null,
    val customSystemImagesPath: String? = null,
    val marqueeWidth: Int = 300,
    val marqueeHeight: Int = 150,
    val screensaverBehavior: ScreensaverBehavior = ScreensaverBehavior.ShowContent,
    // Music settings
    val musicEnabled: Boolean = false,
    val musicPath: String? = null,
    val musicSystemEnabled: Boolean = true,
    val musicGameEnabled: Boolean = true,
    val musicScreensaverEnabled: Boolean = true,
    val musicVideoBehavior: MusicVideoBehavior = MusicVideoBehavior.Duck,
    // App Drawer settings
    val appDrawerOpacity: Int = 100
) {
    val dimmingLevelFloat: Float get() = dimmingLevel / 100f
    val appDrawerOpacityFloat: Float get() = appDrawerOpacity / 100f
}