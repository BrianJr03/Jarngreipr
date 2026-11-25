package jr.brian.jarnfe.look.commandpanel

import androidx.compose.runtime.Immutable

/**
 * Whether the active Command Panel look can lend Jarn its fonts, from the `/fonts` row ([fontMatchAvailability]).
 * [Available] and [NoFonts] carry the look's name from the row.
 */
@Immutable
internal sealed interface FontMatchAvailability {
    data object Hidden : FontMatchAvailability
    data object Checking : FontMatchAvailability
    data object Waiting : FontMatchAvailability
    data class Available(val lookName: String? = null) : FontMatchAvailability
    data class NoFonts(val lookName: String? = null) : FontMatchAvailability
    data object Missing : FontMatchAvailability
    data object Unverified : FontMatchAvailability
    data object Unreadable : FontMatchAvailability
}

/**
 * [read] null means not read yet. A provider that doesn't resolve hides the row, like the colors card; one too old to
 * share fonts asks for an update.
 */
internal fun fontMatchAvailability(
    read: CommandPanelFontsRead?,
    providerExists: Boolean,
): FontMatchAvailability = when (read) {
    null -> FontMatchAvailability.Checking
    is CommandPanelFontsRead.Fresh -> {
        val name = read.fonts.lookName
        if (read.fonts.hasUsableFont) FontMatchAvailability.Available(name) else FontMatchAvailability.NoFonts(name)
    }
    CommandPanelFontsRead.Locked,
    CommandPanelFontsRead.Stopped,
    CommandPanelFontsRead.Unavailable,
    -> FontMatchAvailability.Waiting
    CommandPanelFontsRead.Untrusted ->
        if (providerExists) FontMatchAvailability.Unverified else FontMatchAvailability.Hidden
    CommandPanelFontsRead.Unsupported -> FontMatchAvailability.Missing
    CommandPanelFontsRead.Rejected -> FontMatchAvailability.Unreadable
}

@Immutable
internal data class JarnFontMatchRow(
    val availability: FontMatchAvailability,
    val enabled: Boolean,
) {
    val isShown: Boolean get() = availability != FontMatchAvailability.Hidden

    /** Only worth previewing while off: when on, both screens already use the fonts. */
    val canPreview: Boolean get() = !enabled && availability is FontMatchAvailability.Available

    /** Always lets a user turn it off, even when Command Panel can no longer lend its fonts. */
    val canToggle: Boolean get() = enabled || availability is FontMatchAvailability.Available
}

/** A toggle that is on never hides, so a user can always find it to turn it off. */
internal fun resolveJarnFontMatchRow(enabled: Boolean, availability: FontMatchAvailability): JarnFontMatchRow {
    val shown = if (enabled && availability == FontMatchAvailability.Hidden) FontMatchAvailability.Missing else availability
    return JarnFontMatchRow(shown, enabled)
}
