package jr.brian.jarnfe.themes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import jr.brian.core.model.theme.ThemeColor
import jr.brian.core.model.theme.ThemeColors
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.adaptedTo
import jr.brian.core.ui.theme.readableJarnColors

/** Where a missing line color sits between the surface and mid text. */
private const val LINE_FALLBACK_MIX = 0.3f

/**
 * [ThemeColors] through the same repair Command Panel colors take, or null when its text can't be
 * read. Focus is always derived, so a theme's own focus color is not used.
 */
internal fun ThemeColors.toReadableJarnColors(): JarnColors? {
    val surface = surface.color
    return readableJarnColors(
        bg = bg.color,
        surface = surface,
        raised = raised.color,
        line = line?.color ?: lerp(surface, textMid.color, LINE_FALLBACK_MIX),
        textHigh = textHigh.color,
        textMid = textMid.color,
        textLow = textLow.color,
        accent = accent.color,
        danger = danger.color,
        success = success.color,
    )
}

/**
 * This look in a theme's own colors: the full palette when it passes repair, else its accent on
 * this look's colors. Null [colors] (a theme that follows the Look) returns this look.
 */
internal fun JarnLook.withThemeColors(colors: ThemeColors?): JarnLook =
    colors?.let { adaptedTo(it.toReadableJarnColors(), it.accent.color).look } ?: this

private val ThemeColor.color: Color get() = Color(argb).copy(alpha = 1f)
