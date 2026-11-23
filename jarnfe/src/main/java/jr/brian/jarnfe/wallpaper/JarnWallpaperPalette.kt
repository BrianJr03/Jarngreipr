package jr.brian.jarnfe.wallpaper

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private const val OPAQUE = 0xFF shl 24
private const val CONTRAST_TARGET = 4.5
private const val LUMINANCE_OFFSET = 0.05
private const val BODY_MARGIN = 0.9
private const val LINE_MARGIN = 0.92
private const val BAND_MARGIN = 0.94
private const val BAND_PADDING = 0.01
private const val SEARCH_STEPS = 20
private const val DECK_BODY_DIM = 0.2f

private val StageBandStops = floatArrayOf(0f, 0.115f, 0.19f, 0.83f, 0.915f, 1f)
private val DeckBandStops = floatArrayOf(0f, 0.1f, 0.17f, 0.84f, 0.91f, 1f)
private const val MID_GLOW_AMOUNT = 0.42

/** Every color is opaque ARGB. Marks stay under the cap that keeps text at 4.5:1. */
internal data class JarnWallpaperPalette(
    val bg: Int,
    val glow: Int,
    val line: Int,
    val midGlow: Int,
    val bandAlpha: Float,
)

internal fun jarnWallpaperPalette(
    bg: Int,
    accent: Int,
    focus: Int,
    textMid: Int,
    textLow: Int,
): JarnWallpaperPalette {
    val base = channels(bg)
    val bodyCap = luminanceCapFor(textMid) * BODY_MARGIN
    val bandCap = luminanceCapFor(textLow) * BAND_MARGIN
    val glow = capped(base, channels(accent), bodyCap)
    val line = capped(base, channels(focus), bodyCap * LINE_MARGIN)
    val peak = DoubleArray(3) { max(glow[it], line[it]) }
    return JarnWallpaperPalette(
        bg = argb(base),
        glow = argb(glow),
        line = argb(line),
        midGlow = argb(DoubleArray(3) { floor(base[it] + (glow[it] - base[it]) * MID_GLOW_AMOUNT) }),
        bandAlpha = bandAlphaFor(peak, base, bandCap),
    )
}

internal fun jarnWallpaperBandStops(isDeck: Boolean): FloatArray =
    if (isDeck) DeckBandStops else StageBandStops

internal fun jarnWallpaperBandColors(palette: JarnWallpaperPalette, isDeck: Boolean): IntArray {
    val edge = palette.bandAlpha
    val body = if (isDeck) DECK_BODY_DIM else 0f
    return floatArrayOf(edge, edge, body, body, edge, edge)
        .map { withAlpha(palette.bg, it) }
        .toIntArray()
}

/** Blends two opaque colors, rounding down so a mix never gets brighter than its inputs. */
internal fun jarnWallpaperMix(from: Int, to: Int, amount: Float): Int {
    val start = channels(from)
    val end = channels(to)
    return argb(DoubleArray(3) { floor(start[it] + (end[it] - start[it]) * amount) })
}

private fun bandAlphaFor(peak: DoubleArray, bg: DoubleArray, cap: Double): Float {
    var low = 0.0
    var high = 1.0
    repeat(SEARCH_STEPS) {
        val middle = (low + high) / 2
        if (luminance(mix(peak, bg, middle)) <= cap) high = middle else low = middle
    }
    return min(1.0, high + BAND_PADDING).toFloat()
}

private fun capped(from: DoubleArray, to: DoubleArray, cap: Double): DoubleArray {
    var low = 0.0
    var high = 1.0
    repeat(SEARCH_STEPS) {
        val middle = (low + high) / 2
        if (luminance(mix(from, to, middle)) <= cap) low = middle else high = middle
    }
    val result = mix(from, to, low)
    return DoubleArray(3) { floor(result[it]) }
}

private fun luminanceCapFor(text: Int): Double =
    (luminance(channels(text)) + LUMINANCE_OFFSET) / CONTRAST_TARGET - LUMINANCE_OFFSET

private fun luminance(rgb: DoubleArray): Double =
    0.2126 * linear(rgb[0]) + 0.7152 * linear(rgb[1]) + 0.0722 * linear(rgb[2])

private fun linear(channel: Double): Double {
    val value = channel / 255
    return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
}

private fun mix(from: DoubleArray, to: DoubleArray, amount: Double): DoubleArray =
    DoubleArray(3) { from[it] + (to[it] - from[it]) * amount }

private fun channels(color: Int): DoubleArray = doubleArrayOf(
    (color shr 16 and 0xFF).toDouble(),
    (color shr 8 and 0xFF).toDouble(),
    (color and 0xFF).toDouble(),
)

private fun argb(rgb: DoubleArray): Int =
    OPAQUE or (rgb[0].toInt() shl 16) or (rgb[1].toInt() shl 8) or rgb[2].toInt()

private fun withAlpha(color: Int, alpha: Float): Int =
    (color and 0x00FFFFFF) or ((alpha * 255).toInt().coerceIn(0, 255) shl 24)
