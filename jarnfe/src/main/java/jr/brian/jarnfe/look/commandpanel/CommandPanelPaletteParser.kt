package jr.brian.jarnfe.look.commandpanel

import android.database.Cursor
import jr.brian.core.model.CommandPanelPalette

private const val OPAQUE = 0xFF shl 24
private const val COLOR_COUNT = 14
private const val ACCENT_SOFT_INDEX = 6
private const val SURFACE_INDEX = 2

/** Returns null unless the cursor holds exactly one valid version 1 row. */
internal fun parseCommandPanelRow(cursor: Cursor): CommandPanelPalette? {
    if (cursor.count != 1 || cursor.columnCount != CommandPanelThemeContract.COLUMNS.size) return null
    if (!cursor.moveToFirst()) return null
    val version = cursor.integerOrNull(CommandPanelThemeContract.COLUMN_VERSION) ?: return null
    val isDark = cursor.integerOrNull(CommandPanelThemeContract.COLUMN_IS_DARK) ?: return null
    val colors = CommandPanelThemeContract.COLOR_COLUMNS.map { cursor.integerOrNull(it) ?: return null }
    return commandPanelPaletteOf(version, isDark, colors)
}

private fun Cursor.integerOrNull(column: String): Long? {
    val index = getColumnIndex(column)
    if (index < 0 || getType(index) != Cursor.FIELD_TYPE_INTEGER) return null
    return getLong(index)
}

/** The one gate for provider and cache values. The contract's translucent accent soft is flattened onto surface. */
internal fun commandPanelPaletteOf(version: Long, isDark: Long, colors: List<Long>): CommandPanelPalette? {
    if (version != CommandPanelThemeContract.VERSION.toLong()) return null
    if (isDark != 0L && isDark != 1L) return null
    if (colors.size != COLOR_COUNT) return null
    if (colors.any { it < Int.MIN_VALUE || it > Int.MAX_VALUE }) return null
    val raw = colors.map { it.toInt() }
    val opaque = raw.map { it or OPAQUE }.toMutableList()
    opaque[ACCENT_SOFT_INDEX] = flattenOnto(raw[ACCENT_SOFT_INDEX], opaque[SURFACE_INDEX])
    return CommandPanelPalette(
        isDark = isDark == 1L,
        bg = opaque[0],
        rail = opaque[1],
        surface = opaque[2],
        surfaceHigh = opaque[3],
        border = opaque[4],
        accent = opaque[5],
        accentSoft = opaque[6],
        accentFocused = opaque[7],
        onAccent = opaque[8],
        textPrimary = opaque[9],
        textSecondary = opaque[10],
        textTertiary = opaque[11],
        danger = opaque[12],
        success = opaque[13],
    )
}

internal fun flattenOnto(color: Int, opaqueBackground: Int): Int {
    val alpha = color ushr 24
    fun channel(shift: Int): Int {
        val top = (color ushr shift) and 0xFF
        val bottom = (opaqueBackground ushr shift) and 0xFF
        return (top * alpha + bottom * (255 - alpha) + 127) / 255
    }
    return OPAQUE or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

internal fun encodeCommandPanelPalette(palette: CommandPanelPalette): String = with(palette) {
    listOf(
        CommandPanelThemeContract.VERSION, if (isDark) 1 else 0,
        bg, rail, surface, surfaceHigh, border,
        accent, accentSoft, accentFocused, onAccent,
        textPrimary, textSecondary, textTertiary,
        danger, success,
    ).joinToString(",")
}

internal fun decodeCommandPanelPalette(text: String): CommandPanelPalette? {
    val values = text.trim().split(",").map { it.toLongOrNull() ?: return null }
    if (values.size != CommandPanelThemeContract.COLUMNS.size) return null
    return commandPanelPaletteOf(values[0], values[1], values.drop(2))
}
