package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.R
import jr.brian.jarnfe.deck.nowPlayingDimLabel

/** What the bottom screen does while a game runs on the top one. Two screens only. */
@Composable
internal fun PlayingScreenGroup(store: JarnLookStore, display: JarnDisplayPrefs) {
    SettingsGroup(R.string.jarnfe_settings_playing_title) {
        PlayingScreenRow(store, display.playingScreenVisible)
        PlayingDimRow(store, display.playingDim, enabled = display.playingScreenVisible)
    }
}

@Composable
private fun PlayingScreenRow(store: JarnLookStore, visible: Boolean) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_settings_playing_show),
        checked = visible,
        onCheckedChange = { show -> store.updateDisplay { it.copy(playingScreenVisible = show) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_playing_show_hint),
    )
}

@Composable
private fun PlayingDimRow(store: JarnLookStore, dim: JarnPlayingDim, enabled: Boolean) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_settings_playing_dim),
        value = nowPlayingDimLabel(dim.seconds),
        onClick = { store.updateDisplay { it.copy(playingDim = dim.next()) } },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        focusableWhenDisabled = true,
    )
}

internal fun JarnPlayingDim.next(): JarnPlayingDim =
    JarnPlayingDim.entries[(ordinal + 1) % JarnPlayingDim.entries.size]
