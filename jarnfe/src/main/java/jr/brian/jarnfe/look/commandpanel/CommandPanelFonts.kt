package jr.brian.jarnfe.look.commandpanel

import android.database.Cursor
import androidx.compose.runtime.Immutable
import jr.brian.core.ui.theme.jarnCpFontFace
import org.json.JSONObject

/** Version 2 of Command Panel's shared theme contract: the applied look's fonts. Mirrors `SharedFontsContract`. */
internal object CommandPanelFontsContract {
    const val VERSION = 2
    const val PATH = "fonts"
    const val FILE_PATH = "fonts"

    const val COLUMN_VERSION = "version"
    const val COLUMN_LOOK_ID = "look_id"
    const val COLUMN_DISPLAY_FONT = "display_font"
    const val COLUMN_DISPLAY_FONT_SHA256 = "display_font_sha256"
    const val COLUMN_LABEL_FONT = "label_font"
    const val COLUMN_LABEL_FONT_SHA256 = "label_font_sha256"

    const val PREFIX_BUNDLED = "bundled:"
    const val PREFIX_USER = "user:"
    const val CUSTOM_LOOK_PREFIX = "custom:"

    val COLUMNS = listOf(
        COLUMN_VERSION, COLUMN_LOOK_ID,
        COLUMN_DISPLAY_FONT, COLUMN_DISPLAY_FONT_SHA256,
        COLUMN_LABEL_FONT, COLUMN_LABEL_FONT_SHA256,
    )
}

/** One font role from the row. */
@Immutable
internal sealed interface CommandPanelFont {
    /** [key] is the bare bundled key (`mono`), which may be one this build doesn't know. */
    data class Bundled(val key: String) : CommandPanelFont

    /** A font the user imported into Command Panel, cached by [sha256]. */
    data class Imported(val id: String, val sha256: String) : CommandPanelFont
}

@Immutable
internal data class CommandPanelFonts(
    val lookId: String,
    val display: CommandPanelFont?,
    val label: CommandPanelFont?,
) {
    val imported: List<CommandPanelFont.Imported>
        get() = listOfNotNull(display, label).filterIsInstance<CommandPanelFont.Imported>().distinct()

    /** True when at least one role draws in something other than Jarn's own font. */
    val hasUsableFont: Boolean get() = display.isUsable() || label.isUsable()
}

private fun CommandPanelFont?.isUsable(): Boolean = when (this) {
    null -> false
    is CommandPanelFont.Bundled -> jarnCpFontFace(key) != null
    is CommandPanelFont.Imported -> true
}

private val FONT_ID = Regex("[0-9A-Fa-f-]{1,64}")
private val SHA256 = Regex("[0-9a-f]{64}")

/** Null for a malformed value. An unknown prefix is a font this build can't draw, so it's no font rather than an error. */
private fun fontOf(value: String?, sha256: String?): Result<CommandPanelFont?> = when {
    value == null -> Result.success(null)
    value.startsWith(CommandPanelFontsContract.PREFIX_BUNDLED) ->
        Result.success(CommandPanelFont.Bundled(value.removePrefix(CommandPanelFontsContract.PREFIX_BUNDLED)))
    value.startsWith(CommandPanelFontsContract.PREFIX_USER) -> {
        val id = value.removePrefix(CommandPanelFontsContract.PREFIX_USER)
        if (FONT_ID.matches(id) && sha256 != null && SHA256.matches(sha256)) {
            Result.success(CommandPanelFont.Imported(id, sha256))
        } else {
            Result.failure(IllegalArgumentException("bad imported font"))
        }
    }
    else -> Result.success(null)
}

/** The one gate for provider and cache values. Null unless the row is a valid version 2 row. */
internal fun commandPanelFontsOf(
    version: Long?,
    lookId: String?,
    display: String?,
    displaySha256: String?,
    label: String?,
    labelSha256: String?,
): CommandPanelFonts? {
    if (version != CommandPanelFontsContract.VERSION.toLong()) return null
    if (lookId.isNullOrBlank()) return null
    val displayFont = fontOf(display, displaySha256).getOrElse { return null }
    val labelFont = fontOf(label, labelSha256).getOrElse { return null }
    return CommandPanelFonts(lookId, displayFont, labelFont)
}

/** Returns null unless the cursor holds exactly one valid version 2 row. Columns are read by name. */
internal fun parseCommandPanelFontsRow(cursor: Cursor): CommandPanelFonts? {
    if (cursor.count != 1 || !cursor.moveToFirst()) return null
    val versionIndex = cursor.getColumnIndex(CommandPanelFontsContract.COLUMN_VERSION)
    if (versionIndex < 0 || cursor.getType(versionIndex) != Cursor.FIELD_TYPE_INTEGER) return null
    val strings = CommandPanelFontsContract.COLUMNS.drop(1).map { column ->
        val index = cursor.getColumnIndex(column)
        if (index < 0) return null
        when (cursor.getType(index)) {
            Cursor.FIELD_TYPE_NULL -> null
            Cursor.FIELD_TYPE_STRING -> cursor.getString(index)
            else -> return null
        }
    }
    return commandPanelFontsOf(cursor.getLong(versionIndex), strings[0], strings[1], strings[2], strings[3], strings[4])
}

private fun CommandPanelFont?.encodeValue(): String? = when (this) {
    null -> null
    is CommandPanelFont.Bundled -> CommandPanelFontsContract.PREFIX_BUNDLED + key
    is CommandPanelFont.Imported -> CommandPanelFontsContract.PREFIX_USER + id
}

internal fun encodeCommandPanelFonts(fonts: CommandPanelFonts): String = JSONObject().apply {
    put(CommandPanelFontsContract.COLUMN_VERSION, CommandPanelFontsContract.VERSION)
    put(CommandPanelFontsContract.COLUMN_LOOK_ID, fonts.lookId)
    putOpt(CommandPanelFontsContract.COLUMN_DISPLAY_FONT, fonts.display.encodeValue())
    putOpt(CommandPanelFontsContract.COLUMN_DISPLAY_FONT_SHA256, (fonts.display as? CommandPanelFont.Imported)?.sha256)
    putOpt(CommandPanelFontsContract.COLUMN_LABEL_FONT, fonts.label.encodeValue())
    putOpt(CommandPanelFontsContract.COLUMN_LABEL_FONT_SHA256, (fonts.label as? CommandPanelFont.Imported)?.sha256)
}.toString()

internal fun decodeCommandPanelFonts(text: String): CommandPanelFonts? = runCatching {
    val json = JSONObject(text)
    fun string(column: String): String? = if (json.isNull(column)) null else json.getString(column)
    commandPanelFontsOf(
        json.getLong(CommandPanelFontsContract.COLUMN_VERSION),
        string(CommandPanelFontsContract.COLUMN_LOOK_ID),
        string(CommandPanelFontsContract.COLUMN_DISPLAY_FONT),
        string(CommandPanelFontsContract.COLUMN_DISPLAY_FONT_SHA256),
        string(CommandPanelFontsContract.COLUMN_LABEL_FONT),
        string(CommandPanelFontsContract.COLUMN_LABEL_FONT_SHA256),
    )
}.getOrNull()
