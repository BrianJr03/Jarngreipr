package jr.brian.jarnfe.look.commandpanel

import androidx.compose.runtime.Immutable
import jr.brian.core.model.CommandPanelPalette
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnPaletteMatch
import jr.brian.core.ui.theme.adaptCommandPanelPalette
import jr.brian.core.ui.theme.resolvedColors

internal enum class JarnMatchCardState(val isMatching: Boolean = false, val canTurnOn: Boolean = false) {
    Hidden,
    Checking,
    Available(canTurnOn = true),
    AvailableAccentOnly(canTurnOn = true),
    Waiting(canTurnOn = true),
    MatchingFull(isMatching = true),
    MatchingAccentOnly(isMatching = true),
    Unavailable,
    Unverified,
    Unreadable,
}

@Immutable
internal data class JarnMatchCard(
    val state: JarnMatchCardState,
    val enabled: Boolean,
    val preview: JarnColors? = null,
)

/**
 * [wasShown] keeps a card the user is focused on from vanishing when they turn it off,
 * which would drop gamepad focus.
 */
internal fun resolveJarnMatchCard(
    enabled: Boolean,
    probe: CommandPanelProbe,
    applied: CommandPanelPalette?,
    savedLook: JarnLook,
    wasShown: Boolean,
): JarnMatchCard {
    fun blocked(state: JarnMatchCardState) = JarnMatchCard(state, enabled)
    when (probe.availability) {
        CommandPanelAvailability.Missing -> return blocked(
            if (enabled || wasShown) JarnMatchCardState.Unavailable else JarnMatchCardState.Hidden,
        )
        CommandPanelAvailability.Untrusted -> return blocked(JarnMatchCardState.Unverified)
        CommandPanelAvailability.BadData -> return blocked(JarnMatchCardState.Unreadable)
        CommandPanelAvailability.Checking,
        CommandPanelAvailability.Ready,
        CommandPanelAvailability.Waiting,
        -> Unit
    }
    val palette = if (enabled) applied else probe.palette
    val adapted = adaptCommandPanelPalette(palette, savedLook)
    val probeHasColors = adaptCommandPanelPalette(probe.palette, savedLook).match != JarnPaletteMatch.SavedLook
    val state = when (adapted.match) {
        JarnPaletteMatch.FullPalette ->
            if (enabled) JarnMatchCardState.MatchingFull else JarnMatchCardState.Available
        JarnPaletteMatch.AccentOnSavedLook ->
            if (enabled) JarnMatchCardState.MatchingAccentOnly else JarnMatchCardState.AvailableAccentOnly
        JarnPaletteMatch.SavedLook -> noColorsState(enabled, probe.availability, probeHasColors, wasShown)
    }
    val preview = adapted.look.resolvedColors.takeUnless { adapted.match == JarnPaletteMatch.SavedLook }
    return JarnMatchCard(state, enabled, preview)
}

private fun noColorsState(
    enabled: Boolean,
    availability: CommandPanelAvailability,
    probeHasColors: Boolean,
    wasShown: Boolean,
): JarnMatchCardState = when (availability) {
    CommandPanelAvailability.Ready ->
        if (enabled && probeHasColors) JarnMatchCardState.Checking else JarnMatchCardState.Unreadable
    CommandPanelAvailability.Waiting -> JarnMatchCardState.Waiting
    else -> if (enabled || wasShown) JarnMatchCardState.Checking else JarnMatchCardState.Hidden
}
