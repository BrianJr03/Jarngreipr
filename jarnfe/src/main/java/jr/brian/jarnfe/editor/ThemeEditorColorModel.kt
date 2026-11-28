package jr.brian.jarnfe.editor

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.theme.JARN_READABLE_CONTRAST
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.jarnContrastRatio
import jr.brian.core.ui.theme.liftedToContrast
import jr.brian.jarnfe.R
import jr.brian.jarnfe.themes.toReadableJarnColors
import kotlin.math.abs

private const val REPAIR_STEPS = 40

/** Backgrounds leave room under white text, so every text shade can still be lifted to pass. */
private const val TONAL_TARGET = 7.0
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val CHANNEL_MAX = 255f
private const val SECTORS = 6f
private const val DEGREES_PER_SECTOR = 60f
private const val FULL_TURN = 360f

/** The theme's named colors, in the order the Colors tab lists them. */
internal enum class ThemeEditorColor(@param:StringRes val labelRes: Int) {
    Background(R.string.jarnfe_editor_color_bg),
    Panel(R.string.jarnfe_editor_color_surface),
    Raised(R.string.jarnfe_editor_color_raised),
    Text(R.string.jarnfe_editor_color_text_high),
    SoftText(R.string.jarnfe_editor_color_text_mid),
    FaintText(R.string.jarnfe_editor_color_text_low),
    Accent(R.string.jarnfe_editor_color_accent),
    Warning(R.string.jarnfe_editor_color_danger),
    Success(R.string.jarnfe_editor_color_success),
}

internal fun ThemeColors.get(color: ThemeEditorColor): ThemeColor = when (color) {
    ThemeEditorColor.Background -> bg
    ThemeEditorColor.Panel -> surface
    ThemeEditorColor.Raised -> raised
    ThemeEditorColor.Text -> textHigh
    ThemeEditorColor.SoftText -> textMid
    ThemeEditorColor.FaintText -> textLow
    ThemeEditorColor.Accent -> accent
    ThemeEditorColor.Warning -> danger
    ThemeEditorColor.Success -> success
}

internal fun ThemeColors.with(color: ThemeEditorColor, value: ThemeColor): ThemeColors = when (color) {
    ThemeEditorColor.Background -> copy(bg = value)
    ThemeEditorColor.Panel -> copy(surface = value)
    ThemeEditorColor.Raised -> copy(raised = value)
    ThemeEditorColor.Text -> copy(textHigh = value)
    ThemeEditorColor.SoftText -> copy(textMid = value)
    ThemeEditorColor.FaintText -> copy(textLow = value)
    ThemeEditorColor.Accent -> copy(accent = value)
    ThemeEditorColor.Warning -> copy(danger = value)
    ThemeEditorColor.Success -> copy(success = value)
}

/** Hue (0 to 360), saturation and lightness (0 to 1). */
internal data class ThemeHsl(val hue: Float, val saturation: Float, val lightness: Float)

internal val ThemeColor.hsl: ThemeHsl
    get() {
        val r = channel(RED_SHIFT)
        val g = channel(GREEN_SHIFT)
        val b = channel(0)
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val lightness = (max + min) / 2
        val chroma = max - min
        if (chroma == 0f) return ThemeHsl(0f, 0f, lightness)
        val saturation = chroma / (1 - abs(2 * lightness - 1))
        val sector = when (max) {
            r -> ((g - b) / chroma).mod(SECTORS)
            g -> (b - r) / chroma + 2
            else -> (r - g) / chroma + 4
        }
        return ThemeHsl(sector * DEGREES_PER_SECTOR, saturation.coerceIn(0f, 1f), lightness)
    }

internal fun ThemeHsl.toThemeColor(): ThemeColor {
    val chroma = (1 - abs(2 * lightness - 1)) * saturation
    val sector = (hue.mod(FULL_TURN)) / DEGREES_PER_SECTOR
    val second = chroma * (1 - abs(sector.mod(2f) - 1))
    val (r, g, b) = when (sector.toInt()) {
        0 -> Triple(chroma, second, 0f)
        1 -> Triple(second, chroma, 0f)
        2 -> Triple(0f, chroma, second)
        3 -> Triple(0f, second, chroma)
        4 -> Triple(second, 0f, chroma)
        else -> Triple(chroma, 0f, second)
    }
    val base = lightness - chroma / 2
    return ThemeColor(Color(r + base, g + base, b + base).toArgb())
}

private fun ThemeColor.channel(shift: Int): Float = ((argb shr shift) and CHANNEL_MASK) / CHANNEL_MAX

/** Jarn colors as the theme's own, for starting from the player's Look. */
internal fun JarnColors.toThemeColors(): ThemeColors = ThemeColors(
    bg = ThemeColor(bg.toArgb()),
    surface = ThemeColor(surface.toArgb()),
    raised = ThemeColor(raised.toArgb()),
    textHigh = ThemeColor(textHigh.toArgb()),
    textMid = ThemeColor(textMid.toArgb()),
    textLow = ThemeColor(textLow.toArgb()),
    accent = ThemeColor(accent.toArgb()),
    danger = ThemeColor(danger.toArgb()),
    success = ThemeColor(success.toArgb()),
    line = ThemeColor(line.toArgb()),
)

/** False when Jarn's contrast rules would throw these colors away and fall back to the Look. */
internal val ThemeColors.isReadable: Boolean get() = toReadableJarnColors() != null

/**
 * The nearest readable set: backgrounds darken until white text reads on them, then each text
 * color lightens just enough. Accents are left alone, since Jarn lifts them on its own.
 */
internal fun ThemeColors.fixedForReading(): ThemeColors {
    val tonal = listOf(bg, surface, raised).map { it.darkenedForText() }
    val backs = tonal.map { Color(it.argb) }
    fun lifted(text: ThemeColor) = ThemeColor(Color(text.argb).copy(alpha = 1f).liftedToContrast(JARN_READABLE_CONTRAST, backs).toArgb())
    val fixed = copy(
        bg = tonal[0],
        surface = tonal[1],
        raised = tonal[2],
        textHigh = lifted(textHigh),
        textMid = lifted(textMid),
        textLow = lifted(textLow),
    )
    return if (fixed.isReadable) fixed else fixed.copy(textHigh = ThemeColor(Color.White.toArgb()))
}

private fun ThemeColor.darkenedForText(): ThemeColor {
    val start = Color(argb).copy(alpha = 1f)
    val passing = (0..REPAIR_STEPS).asSequence()
        .map { step -> lerp(start, Color.Black, step / REPAIR_STEPS.toFloat()) }
        .first { jarnContrastRatio(Color.White, it) >= TONAL_TARGET || it == Color.Black }
    return ThemeColor(passing.toArgb())
}
