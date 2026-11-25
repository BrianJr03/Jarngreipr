package jr.brian.jarnfe.look.commandpanel

import androidx.compose.runtime.Immutable

/**
 * Whether the active Command Panel look can lend Jarn its fonts. Until a fonts reader exists,
 * [fromColorCard] derives it from the colors probe and reports [Available] with no look name.
 * The reader replaces that seam with [NoFonts] and real look names.
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

    companion object {
        fun fromColorCard(state: JarnMatchCardState): FontMatchAvailability = when (state) {
            JarnMatchCardState.Hidden -> Hidden
            JarnMatchCardState.Checking -> Checking
            JarnMatchCardState.Waiting -> Waiting
            JarnMatchCardState.Available,
            JarnMatchCardState.AvailableAccentOnly,
            JarnMatchCardState.MatchingFull,
            JarnMatchCardState.MatchingAccentOnly,
            -> Available()
            JarnMatchCardState.Unavailable -> Missing
            JarnMatchCardState.Unverified -> Unverified
            JarnMatchCardState.Unreadable -> Unreadable
        }
    }
}

@Immutable
internal data class JarnFontMatchRow(
    val availability: FontMatchAvailability,
    val enabled: Boolean,
) {
    val isShown: Boolean get() = availability != FontMatchAvailability.Hidden

    /** Always lets a user turn it off, even when Command Panel can no longer lend its fonts. */
    val canToggle: Boolean get() = enabled || availability is FontMatchAvailability.Available
}

/** A toggle that is on never hides, so a user can always find it to turn it off. */
internal fun resolveJarnFontMatchRow(enabled: Boolean, availability: FontMatchAvailability): JarnFontMatchRow {
    val shown = if (enabled && availability == FontMatchAvailability.Hidden) FontMatchAvailability.Missing else availability
    return JarnFontMatchRow(shown, enabled)
}
