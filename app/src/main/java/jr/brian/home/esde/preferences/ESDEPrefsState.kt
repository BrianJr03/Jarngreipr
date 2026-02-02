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
    val systemImageType: SystemImageType = SystemImageType.Fanart
) {
    val dimmingLevelFloat: Float get() = dimmingLevel / 100f
}