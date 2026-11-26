package jr.brian.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

internal val JarnDarkOnColor = Color(0xFF14100C)
internal val JarnLightOnColor = Color(0xFFFFFFFF)

fun jarnRelativeLuminance(color: Color): Double {
    fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
}

fun jarnContrastRatio(first: Color, second: Color): Double =
    jarnContrastRatio(jarnRelativeLuminance(first), jarnRelativeLuminance(second))

/** The WCAG ratio between two relative luminances, from 1 to 21. */
fun jarnContrastRatio(firstLuminance: Double, secondLuminance: Double): Double =
    (max(firstLuminance, secondLuminance) + 0.05) / (min(firstLuminance, secondLuminance) + 0.05)

fun jarnBlend(foreground: Color, background: Color): Color {
    val alpha = foreground.alpha
    return Color(
        red = foreground.red * alpha + background.red * (1f - alpha),
        green = foreground.green * alpha + background.green * (1f - alpha),
        blue = foreground.blue * alpha + background.blue * (1f - alpha),
        alpha = 1f,
    )
}

fun jarnOnColor(background: Color): Color =
    if (jarnContrastRatio(JarnDarkOnColor, background) >= jarnContrastRatio(JarnLightOnColor, background)) {
        JarnDarkOnColor
    } else {
        JarnLightOnColor
    }
