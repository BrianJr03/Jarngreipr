package jr.brian.home.esde.wallpaper

import androidx.compose.ui.graphics.Color
import jr.brian.home.esde.animation.AnimationStyle
import jr.brian.home.esde.preferences.LogoAlignment
import jr.brian.home.esde.preferences.ScreensaverBehavior

data class WallpaperState(
    val currentImagePath: String? = null,
    val marqueePath: String? = null,
    val videoPath: String? = null,
    val isVideoPlaying: Boolean = false,
    val videoAudioEnabled: Boolean = false,
    val videoDelaySeconds: Int = 3,
    val dimmingLevel: Float = 0.2f,
    val blurLevel: Float = 0f,
    val animationStyle: AnimationStyle = AnimationStyle.Fade,
    val animationDuration: Int = 300,
    val animationScale: Float = 0.9f,
    val backgroundColor: Color = Color.Black,
    val showSystemLogo: Boolean = true,
    val logoAlignment: LogoAlignment = LogoAlignment.Center,
    val hideContentOnVideo: Boolean = false,
    val marqueeWidth: Int = 300,
    val marqueeHeight: Int = 150,
    val isScreensaverActive: Boolean = false,
    val screensaverBehavior: ScreensaverBehavior = ScreensaverBehavior.PowerOff,
    val isGameRunning: Boolean = false,
    val gameDescription: String? = null
)