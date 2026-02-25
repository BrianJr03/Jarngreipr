package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.ui.components.SliderSetting

@Composable
fun AppDrawerSectionContent(
    prefsState: ESDEPrefsState,
    onAppDrawerOpacityChange: (Int) -> Unit
) {
    SliderSetting(
        title = stringResource(R.string.esde_settings_app_drawer_opacity),
        value = prefsState.appDrawerOpacity.toFloat(),
        valueRange = 0f..100f,
        steps = 19,
        valueText = "${prefsState.appDrawerOpacity}%",
        onValueChange = { opacity ->
            onAppDrawerOpacityChange(opacity.toInt())
        }
    )
}
