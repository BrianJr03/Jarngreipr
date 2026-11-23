package jr.brian.jarnfe.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnSliderRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.frontend.look.JarnLookStore
import jr.brian.frontend.look.JarnTapControl
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import kotlin.math.roundToInt

private const val PERCENT = 100

@Composable
internal fun JarnDisplaySection() {
    val context = LocalContext.current
    val store = remember(context) { context.jarnLookStoreOrNull() } ?: return
    val display by store.displayState.collectAsState()
    SettingsSectionTitle(R.string.jarnfe_settings_tiles_title)
    TileScaleRow(R.string.jarnfe_settings_system_tile_size, display.systemTileScale) { scale ->
        store.updateDisplay { it.copy(systemTileScale = scale) }
    }
    TileScaleRow(R.string.jarnfe_settings_game_tile_size, display.gameTileScale) { scale ->
        store.updateDisplay { it.copy(gameTileScale = scale) }
    }
    SettingsSectionTitle(R.string.jarnfe_settings_controls_title)
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_settings_show_hints),
        checked = display.hintsVisible,
        onCheckedChange = { visible -> store.updateDisplay { it.copy(hintsVisible = visible) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_show_hints_hint),
    )
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_settings_haptics),
        checked = display.buttonHaptics,
        onCheckedChange = { enabled -> store.updateDisplay { it.copy(buttonHaptics = enabled) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_haptics_hint),
    )
    UndoCountdownRow(store, display.undoCountdownVisible)
    if (JarnSession.state.deckDisplayAvailable) TapControlRow(store, display.tapControl)
}

@Composable
private fun UndoCountdownRow(store: JarnLookStore, visible: Boolean) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_settings_undo_countdown),
        checked = visible,
        onCheckedChange = { show -> store.updateDisplay { it.copy(undoCountdownVisible = show) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_undo_countdown_hint),
    )
}

@Composable
private fun TapControlRow(store: JarnLookStore, choice: JarnTapControl) {
    JarnValueRow(
        label = stringResource(R.string.jarnfe_settings_tap_control),
        value = stringResource(choice.labelRes),
        onClick = { store.updateDisplay { it.copy(tapControl = choice.next()) } },
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_settings_tap_control_hint),
    )
}

@get:StringRes
private val JarnTapControl.labelRes: Int
    get() = when (this) {
        JarnTapControl.FollowTap -> R.string.jarnfe_settings_tap_control_follow
        JarnTapControl.AlwaysTop -> R.string.jarnfe_settings_tap_control_top
        JarnTapControl.AlwaysBottom -> R.string.jarnfe_settings_tap_control_bottom
    }

private fun JarnTapControl.next(): JarnTapControl =
    JarnTapControl.entries[(ordinal + 1) % JarnTapControl.entries.size]

@Composable
private fun TileScaleRow(@StringRes labelRes: Int, scale: Float, onChange: (Float) -> Unit) {
    JarnSliderRow(
        label = stringResource(labelRes),
        value = scale,
        valueText = stringResource(R.string.jarnfe_settings_percent, (scale * PERCENT).roundToInt()),
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        valueRange = JarnDisplayPrefs.MIN_TILE_SCALE..JarnDisplayPrefs.MAX_TILE_SCALE,
        steps = JarnDisplayPrefs.TILE_SCALE_STEPS,
    )
}
