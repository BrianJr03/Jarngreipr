package jr.brian.jarnfe.look.commandpanel

/** Version 1 of Command Panel's shared theme contract. Only the release identity is trusted, in every Jarn build. */
internal object CommandPanelThemeContract {
    const val VERSION = 1
    const val PACKAGE = "jr.brian.commandpannel"
    const val AUTHORITY = "$PACKAGE.theme"
    const val PATH = "palette"
    const val CERT_SHA256 = "8ebd8e6ff20a29019a857fe25a98532649d66bf3fd778a64150db8085d988927"

    const val COLUMN_VERSION = "version"
    const val COLUMN_IS_DARK = "is_dark"

    val COLOR_COLUMNS = listOf(
        "bg", "rail", "surface", "surface_high", "border",
        "accent", "accent_soft", "accent_focused", "on_accent",
        "text_primary", "text_secondary", "text_tertiary",
        "danger", "success",
    )

    val COLUMNS = listOf(COLUMN_VERSION, COLUMN_IS_DARK) + COLOR_COLUMNS
}
