package jr.brian.jarnfe.backdrop

import androidx.compose.ui.graphics.Color
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.jarnContrastRatio
import jr.brian.core.ui.theme.jarnRelativeLuminance
import kotlin.math.pow

/** The other tone must beat the current one by this contrast factor, so mid-grey never flickers. */
internal const val JARN_TONE_SWITCH_MARGIN = 1.25

/** Header text either keeps the look's colors or swaps to its dark ones over a light backdrop. */
internal enum class JarnTextTone { Theme, Inverse }

/** The look's own darks become text and its text color the chip fill, so key caps stay legible too. */
internal fun jarnTextColorsFor(colors: JarnColors, tone: JarnTextTone): JarnColors = when (tone) {
    JarnTextTone.Theme -> colors
    JarnTextTone.Inverse -> colors.copy(
        textHigh = colors.bg,
        textMid = colors.surface,
        textLow = colors.raised,
        raised = colors.textMid,
    )
}

/** The color a faint shadow takes behind text of [tone], the opposite end from the text. */
internal fun jarnTextShadowColor(colors: JarnColors, tone: JarnTextTone): Color = when (tone) {
    JarnTextTone.Theme -> colors.bg
    JarnTextTone.Inverse -> colors.textHigh
}

/** The lowest contrast [text] has against any of [backgrounds], given as relative luminances. */
internal fun jarnWorstContrast(text: Color, backgrounds: List<Double>): Double {
    val textLuminance = jarnRelativeLuminance(text)
    return backgrounds.minOfOrNull { jarnContrastRatio(textLuminance, it) } ?: Double.MAX_VALUE
}

/** Keeps [current] unless the other tone's worst contrast beats it by [margin]. */
internal fun jarnPickTextTone(
    current: JarnTextTone,
    themeContrast: Double,
    inverseContrast: Double,
    margin: Double = JARN_TONE_SWITCH_MARGIN,
): JarnTextTone = when (current) {
    JarnTextTone.Theme -> if (inverseContrast > themeContrast * margin) JarnTextTone.Inverse else current
    JarnTextTone.Inverse -> if (themeContrast > inverseContrast * margin) JarnTextTone.Theme else current
}

/** The tone for text over [backgrounds], with [current] held by the hysteresis margin. */
internal fun jarnTextToneOver(colors: JarnColors, backgrounds: List<Double>, current: JarnTextTone): JarnTextTone {
    if (backgrounds.isEmpty()) return current
    val theme = jarnWorstContrast(jarnTextColorsFor(colors, JarnTextTone.Theme).textHigh, backgrounds)
    val inverse = jarnWorstContrast(jarnTextColorsFor(colors, JarnTextTone.Inverse).textHigh, backgrounds)
    return jarnPickTextTone(current, theme, inverse)
}

/** sRGB channel to linear light, so a pixel's luminance costs one table lookup per channel. */
private val LinearChannel = DoubleArray(256) { level ->
    val value = level / 255.0
    if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

/** WCAG relative luminance of an ARGB pixel, seen over the black behind every screen. */
internal fun jarnPixelLuminance(argb: Int): Double {
    val alpha = argb ushr 24
    fun over(channel: Int) = LinearChannel[(channel * alpha + OPAQUE / 2) / OPAQUE]
    return 0.2126 * over(argb shr 16 and 0xFF) + 0.7152 * over(argb shr 8 and 0xFF) + 0.0722 * over(argb and 0xFF)
}

private const val OPAQUE = 255

/** Spaces samples at least [intervalMillis] apart, however long each one takes. */
internal class JarnSampleThrottle(private val intervalMillis: Long) {
    private var lastMillis = Long.MIN_VALUE

    fun waitMillis(nowMillis: Long): Long =
        if (lastMillis == Long.MIN_VALUE) 0L else (lastMillis + intervalMillis - nowMillis).coerceAtLeast(0L)

    fun mark(nowMillis: Long) {
        lastMillis = nowMillis
    }
}
