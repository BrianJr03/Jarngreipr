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
import jr.brian.frontend.look.JarnDisplayPrefs
import jr.brian.jarnfe.R
import jr.brian.jarnfe.look.jarnLookStoreOrNull
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
}

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
