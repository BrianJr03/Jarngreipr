package jr.brian.home.esde.model

import androidx.compose.ui.graphics.Color
import jr.brian.home.esde.model.AnimationStyle
import jr.brian.home.esde.model.LogoAlignment
import jr.brian.home.esde.model.ScreensaverBehavior

data class WallpaperState(
    val currentImagePath: String? = null,
    val logoPath: String? = null,
    val videoPath: String? = null,
    val isVideoPlaying: Boolean = false,
    val systemBackgroundVideoPath: String? = null,
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
    val logoOffsetX: Float = 0f,
    val logoOffsetY: Float = 0f,
    val marqueeWidth: Int = 300,
    val marqueeHeight: Int = 150,
    val isScreensaverActive: Boolean = false,
    val screensaverBehavior: ScreensaverBehavior = ScreensaverBehavior.PowerOff,
    val isGameRunning: Boolean = false,
    val logoBrightness: Float = 1f,
    val gameDescription: String? = null,
    val isShowingGameBackground: Boolean = false,
    val systemBgVideoMuted: Boolean = true,
    val systemBgVideoLooping: Boolean = true
)