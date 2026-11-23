package jr.brian.core.model

import androidx.compose.runtime.Immutable

/** Command Panel's resolved overlay colors as opaque ARGB ints. */
@Immutable
data class CommandPanelPalette(
    val isDark: Boolean,
    val bg: Int,
    val rail: Int,
    val surface: Int,
    val surfaceHigh: Int,
    val border: Int,
    val accent: Int,
    val accentSoft: Int,
    val accentFocused: Int,
    val onAccent: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textTertiary: Int,
    val danger: Int,
    val success: Int,
)
