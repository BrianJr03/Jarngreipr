package jr.brian.home.esde.ui.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting

@Composable
fun PowerSectionContent(
    prefsState: ESDEPrefsState,
    onPersistOnGameLaunchChange: (Boolean) -> Unit,
    onPersistLogoBrightnessChange: (Int) -> Unit,
    onPersistBackgroundBrightnessChange: (Int) -> Unit,
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
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SliderSetting(
                title = stringResource(R.string.esde_settings_persist_logo_brightness),
                value = prefsState.persistLogoBrightness.toFloat(),
                valueRange = 30f..100f,
                steps = 13,
                valueText = "${prefsState.persistLogoBrightness}%",
                onValueChange = { brightness ->
                    onPersistLogoBrightnessChange(brightness.toInt())
                }
            )

            SliderSetting(
                title = stringResource(R.string.esde_settings_persist_background_brightness),
                value = prefsState.persistBackgroundBrightness.toFloat(),
                valueRange = 30f..100f,
                steps = 13,
                valueText = "${prefsState.persistBackgroundBrightness}%",
                onValueChange = { brightness ->
                    onPersistBackgroundBrightnessChange(brightness.toInt())
                }
            )
        }
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
