package jr.brian.jarnfe.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.R

@Composable
internal fun JarnPauseOnBatterySaverSetting(store: JarnLookStore) {
    val display by store.displayState.collectAsState()
    JarnPauseOnBatterySaverRow(display.pauseOnBatterySaver) { pause ->
        store.updateDisplay { it.copy(pauseOnBatterySaver = pause) }
    }
}

/** Off keeps wallpapers and video previews moving on Battery Saver. Reduce motion still stops them. */
@Composable
internal fun JarnPauseOnBatterySaverRow(checked: Boolean, onChange: (Boolean) -> Unit) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_wallpaper_pause_battery_saver),
        checked = checked,
        onCheckedChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_wallpaper_pause_battery_saver_hint),
    )
}
