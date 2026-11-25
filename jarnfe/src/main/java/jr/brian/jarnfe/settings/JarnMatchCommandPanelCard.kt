package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnLookSwatches
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.JarnColors
import jr.brian.core.ui.theme.JarnLook
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.commandpanel.JarnMatchCard
import jr.brian.jarnfe.look.commandpanel.JarnMatchCardState
import jr.brian.jarnfe.look.commandpanel.rememberCommandPanelPalette
import jr.brian.jarnfe.look.commandpanel.rememberCommandPanelProbe
import jr.brian.jarnfe.look.commandpanel.resolveJarnMatchCard

@Composable
internal fun rememberJarnMatchCard(store: JarnLookStore, savedLook: JarnLook): JarnMatchCard {
    val enabled by store.matchCommandPanelState.collectAsState()
    val probe by rememberCommandPanelProbe()
    val applied by rememberCommandPanelPalette(enabled)
    var wasShown by remember { mutableStateOf(false) }
    val card = remember(enabled, probe, applied, savedLook, wasShown) {
        resolveJarnMatchCard(enabled, probe, applied, savedLook, wasShown)
    }
    if (card.state != JarnMatchCardState.Hidden && !wasShown) SideEffect { wasShown = true }
    return card
}

@Composable
internal fun JarnMatchCommandPanelCard(card: JarnMatchCard, onCheckedChange: (Boolean) -> Unit) {
    if (card.state == JarnMatchCardState.Hidden) return
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_look_match_cp),
        checked = card.enabled,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(card.state.statusText),
        enabled = card.enabled || card.state.canTurnOn,
        focusableWhenDisabled = true,
    )
}

/** Shown after both Command Panel toggles, so the two rows stay together. */
@Composable
internal fun JarnMatchCommandPanelSwatches(card: JarnMatchCard) {
    if (card.state == JarnMatchCardState.Hidden) return
    card.preview?.let { JarnMatchPreview(it) }
}

@Composable
private fun JarnMatchPreview(colors: JarnColors) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.bg, JarnTheme.radius.smShape),
    ) {
        JarnLookSwatches(colors)
    }
}

@get:StringRes
private val JarnMatchCardState.statusText: Int
    get() = when (this) {
        JarnMatchCardState.Hidden,
        JarnMatchCardState.Checking,
        -> R.string.jarnfe_look_match_cp_checking
        JarnMatchCardState.Available -> R.string.jarnfe_look_match_cp_available
        JarnMatchCardState.AvailableAccentOnly,
        JarnMatchCardState.MatchingAccentOnly,
        -> R.string.jarnfe_look_match_cp_accent_only
        JarnMatchCardState.Waiting -> R.string.jarnfe_look_match_cp_waiting
        JarnMatchCardState.MatchingFull -> R.string.jarnfe_look_match_cp_full
        JarnMatchCardState.Unavailable -> R.string.jarnfe_look_match_cp_unavailable
        JarnMatchCardState.Unverified -> R.string.jarnfe_look_match_cp_unverified
        JarnMatchCardState.Unreadable -> R.string.jarnfe_look_match_cp_unreadable
    }
