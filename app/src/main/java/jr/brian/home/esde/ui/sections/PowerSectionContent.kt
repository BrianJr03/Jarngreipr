package jr.brian.home.esde.ui.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting

@Composable
fun PowerSectionContent(
    prefsState: ESDEPrefsState,
    onPersistOnGameLaunchChange: (Boolean) -> Unit,
    onGameBackgroundDimmingChange: (Int) -> Unit,
    onPowerEventsEnabledChange: (Boolean) -> Unit
) {
    ToggleSetting(
        title = stringResource(R.string.esde_settings_persist_on_game_launch),
        description = stringResource(R.string.esde_settings_persist_on_game_launch_description),
        checked = prefsState.persistOnGameLaunch,
        onCheckedChange = { persist ->
            onPersistOnGameLaunchChange(persist)
        }
    )

    AnimatedVisibility(prefsState.persistOnGameLaunch) {
        SliderSetting(
            title = stringResource(R.string.esde_settings_game_background_dimming),
            value = prefsState.gameBackgroundDimming.toFloat(),
            valueRange = 0f..70f,
            steps = 13,
            valueText = "${prefsState.gameBackgroundDimming}%",
            onValueChange = { dimming ->
                onGameBackgroundDimmingChange(dimming.toInt())
            }
        )
    }

    ToggleSetting(
        title = stringResource(R.string.esde_settings_power_events),
        description = stringResource(R.string.esde_settings_power_events_description),
        checked = prefsState.powerEventsEnabled,
        onCheckedChange = { enabled ->
            onPowerEventsEnabledChange(enabled)
        }
    )
}
