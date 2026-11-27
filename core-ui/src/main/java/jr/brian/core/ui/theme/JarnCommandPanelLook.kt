package jr.brian.core.ui.theme

import androidx.compose.ui.graphics.Color
import jr.brian.core.model.CommandPanelPalette

private const val OPAQUE_ALPHA_BITS = 0xFF shl 24

/**
 * Turns a published Command Panel palette into a look that keeps Jarn's contrast rules.
 * Only colors change: [savedLook]'s id, name, tile shape and card style carry over.
 */
fun adaptCommandPanelPalette(palette: CommandPanelPalette?, savedLook: JarnLook): JarnAdaptedLook {
    val usable = palette?.takeUnless { it.isMissingData() }
    return savedLook.adaptedTo(
        fullColors = usable?.toReadableColors(),
        accent = usable?.let { opaqueColor(it.accent) },
    )
}

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
    return readableJarnColors(
        bg = opaqueColor(bg),
        surface = opaqueColor(surface),
        raised = opaqueColor(surfaceHigh),
        line = opaqueColor(border),
        textHigh = opaqueColor(textPrimary),
        textMid = opaqueColor(textSecondary),
        textLow = opaqueColor(textTertiary),
        accent = opaqueColor(accent),
        danger = opaqueColor(danger),
        success = opaqueColor(success),
    )
}

private fun opaqueColor(argb: Int): Color = Color(argb).copy(alpha = 1f)
