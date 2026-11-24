package jr.brian.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import jr.brian.core.model.CommandPanelPalette

private const val JARN_READABLE_CONTRAST = 4.5
private const val JARN_FOCUS_CONTRAST = 3.0
private const val JARN_FOCUS_LIFT = 0.4f
private const val JARN_SELECTED_TINT_ALPHA = 0.14f
private const val JARN_REPAIR_STEPS = 100
private const val OPAQUE_ALPHA_BITS = 0xFF shl 24

enum class JarnPaletteMatch { FullPalette, AccentOnSavedLook, SavedLook }

@Immutable
data class JarnAdaptedLook(val look: JarnLook, val match: JarnPaletteMatch)

/**
 * Turns a published Command Panel palette into a look that keeps Jarn's contrast rules.
 * Only colors change: [savedLook]'s id, name, tile shape and card style carry over.
 */
fun adaptCommandPanelPalette(palette: CommandPanelPalette?, savedLook: JarnLook): JarnAdaptedLook {
    if (palette == null || palette.isMissingData()) {
        return JarnAdaptedLook(savedLook, JarnPaletteMatch.SavedLook)
    }
    val fullColors = palette.toReadableColors()
    if (fullColors != null) {
        return JarnAdaptedLook(savedLook.withAdaptedColors(fullColors), JarnPaletteMatch.FullPalette)
    }
    val accented = savedLook.resolvedColors.withAccent(opaqueColor(palette.accent))
    return JarnAdaptedLook(savedLook.withAdaptedColors(accented), JarnPaletteMatch.AccentOnSavedLook)
}

/** Clears [JarnLook.focusColor], which would otherwise override the derived focus color. */
private fun JarnLook.withAdaptedColors(adapted: JarnColors): JarnLook =
    copy(colors = adapted, focusColor = null)

/** An unset provider column reads as 0, so a zero accent or a single-valued row carries no theme. */
private fun CommandPanelPalette.isMissingData(): Boolean {
    val roles = listOf(
        bg, rail, surface, surfaceHigh, border, accent, accentSoft, accentFocused, onAccent,
        textPrimary, textSecondary, textTertiary, danger, success,
    )
    return accent == 0 || roles.map { it or OPAQUE_ALPHA_BITS }.distinct().size == 1
}

/** The full palette as Jarn colors, or null when it is light or its text can't be read as published. */
private fun CommandPanelPalette.toReadableColors(): JarnColors? {
    if (!isDark) return null
    val bgColor = opaqueColor(bg)
    val surfaceColor = opaqueColor(surface)
    val raisedColor = opaqueColor(surfaceHigh)
    val textHigh = opaqueColor(textPrimary)
    val textMid = opaqueColor(textSecondary)
    val textLow = opaqueColor(textTertiary)
    val tonal = listOf(bgColor, surfaceColor, raisedColor)
    val textIsReadable = listOf(textHigh, textMid, textLow).all { it.isLightTextOn(tonal) }
    val selectedTint = jarnBlend(textHigh.copy(alpha = JARN_SELECTED_TINT_ALPHA), surfaceColor)
    if (!textIsReadable || jarnContrastRatio(textHigh, selectedTint) < JARN_READABLE_CONTRAST) return null

    val dangerColor = opaqueColor(danger).liftedToContrast(JARN_READABLE_CONTRAST, tonal)
    return JarnColors(
        bg = bgColor,
        surface = surfaceColor,
        raised = raisedColor,
        line = opaqueColor(border),
        textHigh = textHigh,
        textMid = textMid,
        textLow = textLow,
        accent = bgColor,
        onAccent = bgColor,
        focus = bgColor,
        focusGap = JarnFocusGapColor,
        danger = dangerColor,
        onDanger = readableContentOn(dangerColor),
        success = opaqueColor(success).liftedToContrast(JARN_READABLE_CONTRAST, tonal),
        artScrim = readableArtScrim(bgColor, textHigh, textMid),
    ).withAccent(opaqueColor(accent))
}

/** Accent also draws as text in Jarn, so it has to meet the text rule on every tonal surface. */
private fun JarnColors.withAccent(rawAccent: Color): JarnColors {
    val tonal = listOf(bg, surface, raised)
    val readableAccent = rawAccent.liftedToContrast(JARN_READABLE_CONTRAST, tonal)
    return copy(
        accent = readableAccent,
        onAccent = readableContentOn(readableAccent),
        focus = lerp(readableAccent, Color.White, JARN_FOCUS_LIFT)
            .liftedToContrast(JARN_FOCUS_CONTRAST, tonal + focusGap),
    )
}

private fun opaqueColor(argb: Int): Color = Color(argb).copy(alpha = 1f)

private fun Color.isLightTextOn(backgrounds: List<Color>): Boolean {
    val luminance = jarnRelativeLuminance(this)
    return backgrounds.all {
        luminance > jarnRelativeLuminance(it) && jarnContrastRatio(this, it) >= JARN_READABLE_CONTRAST
    }
}

/** The closest color to this one, moving toward white, that meets [target] on every background. */
private fun Color.liftedToContrast(target: Double, backgrounds: List<Color>): Color =
    (0..JARN_REPAIR_STEPS).asSequence()
        .map { step -> lerp(this, Color.White, step / JARN_REPAIR_STEPS.toFloat()) }
        .firstOrNull { candidate -> backgrounds.all { jarnContrastRatio(candidate, it) >= target } }
        ?: Color.White

/** [jarnOnColor]'s near-black can land just under 4.5:1 on mid tones, where pure black still passes. */
private fun readableContentOn(container: Color): Color =
    jarnOnColor(container).takeIf { jarnContrastRatio(it, container) >= JARN_READABLE_CONTRAST } ?: Color.Black

/** The lightest scrim from Jarn's standard alpha up that keeps text readable over pure white art. */
private fun readableArtScrim(bg: Color, textHigh: Color, textMid: Color): Color =
    (0..JARN_REPAIR_STEPS).asSequence()
        .map { step ->
            val fraction = step / JARN_REPAIR_STEPS.toFloat()
            bg.copy(alpha = JARN_ART_SCRIM_ALPHA + (1f - JARN_ART_SCRIM_ALPHA) * fraction)
        }
        .firstOrNull { scrim ->
            val backing = jarnBlend(scrim, Color.White)
            jarnContrastRatio(textHigh, backing) >= JARN_READABLE_CONTRAST &&
                jarnContrastRatio(textMid, backing) >= JARN_READABLE_CONTRAST
        }
        ?: bg
