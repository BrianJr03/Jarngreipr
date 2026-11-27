package jr.brian.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

const val JARN_READABLE_CONTRAST = 4.5
const val JARN_FOCUS_CONTRAST = 3.0
private const val JARN_FOCUS_LIFT = 0.4f
private const val JARN_SELECTED_TINT_ALPHA = 0.14f
private const val JARN_REPAIR_STEPS = 100

enum class JarnPaletteMatch { FullPalette, AccentOnSavedLook, SavedLook }

@Immutable
data class JarnAdaptedLook(val look: JarnLook, val match: JarnPaletteMatch)

/**
 * Picks the best look an outside palette can give while keeping Jarn's contrast rules:
 * [fullColors] when they passed repair, else [accent] on this look's colors, else this look.
 * Only colors change: the id, name, tile shape and card style carry over.
 */
fun JarnLook.adaptedTo(fullColors: JarnColors?, accent: Color?): JarnAdaptedLook = when {
    fullColors != null -> JarnAdaptedLook(withAdaptedColors(fullColors), JarnPaletteMatch.FullPalette)
    accent != null -> JarnAdaptedLook(
        withAdaptedColors(resolvedColors.withAccent(accent)),
        JarnPaletteMatch.AccentOnSavedLook,
    )
    else -> JarnAdaptedLook(this, JarnPaletteMatch.SavedLook)
}

/** Clears [JarnLook.focusColor], which would otherwise override the derived focus color. */
private fun JarnLook.withAdaptedColors(adapted: JarnColors): JarnLook =
    copy(colors = adapted, focusColor = null)

/**
 * Raw base colors as Jarn colors, or null when their text can't be read as given.
 * Accent, danger and success are lifted to the text rule; on-colors, focus and the art scrim are derived.
 */
fun readableJarnColors(
    bg: Color,
    surface: Color,
    raised: Color,
    line: Color,
    textHigh: Color,
    textMid: Color,
    textLow: Color,
    accent: Color,
    danger: Color,
    success: Color,
): JarnColors? {
    val tonal = listOf(bg, surface, raised)
    val textIsReadable = listOf(textHigh, textMid, textLow).all { it.isLightTextOn(tonal) }
    val selectedTint = jarnBlend(textHigh.copy(alpha = JARN_SELECTED_TINT_ALPHA), surface)
    if (!textIsReadable || jarnContrastRatio(textHigh, selectedTint) < JARN_READABLE_CONTRAST) return null

    val dangerColor = danger.liftedToContrast(JARN_READABLE_CONTRAST, tonal)
    return JarnColors(
        bg = bg,
        surface = surface,
        raised = raised,
        line = line,
        textHigh = textHigh,
        textMid = textMid,
        textLow = textLow,
        accent = bg,
        onAccent = bg,
        focus = bg,
        focusGap = JarnFocusGapColor,
        danger = dangerColor,
        onDanger = readableContentOn(dangerColor),
        success = success.liftedToContrast(JARN_READABLE_CONTRAST, tonal),
        artScrim = readableArtScrim(bg, textHigh, textMid),
    ).withAccent(accent)
}

/** Accent also draws as text in Jarn, so it has to meet the text rule on every tonal surface. */
fun JarnColors.withAccent(rawAccent: Color): JarnColors {
    val tonal = listOf(bg, surface, raised)
    val readableAccent = rawAccent.liftedToContrast(JARN_READABLE_CONTRAST, tonal)
    return copy(
        accent = readableAccent,
        onAccent = readableContentOn(readableAccent),
        focus = lerp(readableAccent, Color.White, JARN_FOCUS_LIFT)
            .liftedToContrast(JARN_FOCUS_CONTRAST, tonal + focusGap),
    )
}

fun Color.isLightTextOn(backgrounds: List<Color>): Boolean {
    val luminance = jarnRelativeLuminance(this)
    return backgrounds.all {
        luminance > jarnRelativeLuminance(it) && jarnContrastRatio(this, it) >= JARN_READABLE_CONTRAST
    }
}

/** The closest color to this one, moving toward white, that meets [target] on every background. */
fun Color.liftedToContrast(target: Double, backgrounds: List<Color>): Color =
    (0..JARN_REPAIR_STEPS).asSequence()
        .map { step -> lerp(this, Color.White, step / JARN_REPAIR_STEPS.toFloat()) }
        .firstOrNull { candidate -> backgrounds.all { jarnContrastRatio(candidate, it) >= target } }
        ?: Color.White

/** [jarnOnColor]'s near-black can land just under 4.5:1 on mid tones, where pure black still passes. */
fun readableContentOn(container: Color): Color =
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
