package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.ScreensaverBehavior
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ScreensaverBehaviorSelector

@Composable
fun ScreensaverSectionContent(
    prefsState: ESDEPrefsState,
    onScreensaverAppCountChange: (Int) -> Unit,
    onScreensaverBehaviorChange: (ScreensaverBehavior) -> Unit
) {
    ScreensaverBehaviorSelector(
        selectedBehavior = prefsState.screensaverBehavior,
        onBehaviorSelected = { behavior ->
            onScreensaverBehaviorChange(behavior)
        }
    )
    SliderSetting(
        title = stringResource(R.string.esde_settings_screensaver_app_count),
        value = prefsState.screensaverFloatyAppCount.toFloat(),
        valueRange = 0f..100f,
        steps = 99,
        valueText = if (prefsState.screensaverFloatyAppCount == 0) {
            stringResource(R.string.esde_settings_screensaver_app_count_all)
        } else {
            prefsState.screensaverFloatyAppCount.toString()
        },
        onValueChange = { onScreensaverAppCountChange(it.toInt()) },
        description = stringResource(R.string.esde_settings_screensaver_app_count_description)
    )
}
