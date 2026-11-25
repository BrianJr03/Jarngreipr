package jr.brian.jarnfe.settings

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.theme.LocalJarnScreenActive
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.JarnFontPreview
import jr.brian.jarnfe.look.commandpanel.FontMatchAvailability
import jr.brian.jarnfe.look.commandpanel.JarnFontMatchRow
import jr.brian.jarnfe.look.commandpanel.resolveJarnFontMatchRow

@Composable
internal fun rememberJarnFontMatchRow(store: JarnLookStore, availability: FontMatchAvailability): JarnFontMatchRow {
    val enabled by store.matchCommandPanelFontsState.collectAsState()
    return remember(enabled, availability) { resolveJarnFontMatchRow(enabled, availability) }
}

@Composable
internal fun JarnMatchCommandPanelFontsRow(row: JarnFontMatchRow, onCheckedChange: (Boolean) -> Unit) {
    if (!row.isShown) return
    val interactions = remember { MutableInteractionSource() }
    val focused by interactions.collectIsFocusedAsState()
    LiveFontPreview(focused && row.canPreview)
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_look_match_cp_fonts),
        checked = row.enabled,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = fontMatchStatus(row),
        enabled = row.canToggle,
        focusableWhenDisabled = true,
        interactionSource = interactions,
    )
}

/** Both screens show Command Panel's fonts while the row has focus, and go back once it loses focus. */
@Composable
private fun LiveFontPreview(focused: Boolean) {
    val active = focused && LocalJarnScreenActive.current
    DisposableEffect(active) {
        JarnFontPreview.active = active
        onDispose { JarnFontPreview.active = false }
    }
}

@Composable
private fun fontMatchStatus(row: JarnFontMatchRow): String = when (val availability = row.availability) {
    FontMatchAvailability.Hidden,
    FontMatchAvailability.Checking,
    -> stringResource(R.string.jarnfe_look_match_cp_checking)
    FontMatchAvailability.Waiting -> stringResource(R.string.jarnfe_look_match_cp_waiting)
    is FontMatchAvailability.Available -> availableStatus(row.enabled, availability.lookName)
    is FontMatchAvailability.NoFonts -> noFontsStatus(availability.lookName)
    FontMatchAvailability.Missing -> stringResource(R.string.jarnfe_look_match_cp_unavailable)
    FontMatchAvailability.Unverified -> stringResource(R.string.jarnfe_look_match_cp_unverified)
    FontMatchAvailability.Unreadable -> stringResource(R.string.jarnfe_look_match_cp_fonts_unreadable)
}

@Composable
private fun availableStatus(enabled: Boolean, lookName: String?): String = when {
    enabled && lookName != null -> stringResource(R.string.jarnfe_look_match_cp_fonts_on_named, lookName)
    enabled -> stringResource(R.string.jarnfe_look_match_cp_fonts_on)
    lookName != null -> stringResource(R.string.jarnfe_look_match_cp_fonts_available_named, lookName)
    else -> stringResource(R.string.jarnfe_look_match_cp_fonts_available)
}

@Composable
private fun noFontsStatus(lookName: String?): String =
    if (lookName != null) {
        stringResource(R.string.jarnfe_look_match_cp_fonts_none_named, lookName)
    } else {
        stringResource(R.string.jarnfe_look_match_cp_fonts_none)
    }
