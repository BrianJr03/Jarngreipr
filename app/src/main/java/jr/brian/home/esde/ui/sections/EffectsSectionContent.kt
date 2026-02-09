package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.preferences.GameImageType
import jr.brian.home.esde.preferences.SystemImageType
import jr.brian.home.esde.ui.components.BackgroundColorSelector
import jr.brian.home.esde.ui.components.GameImageTypeSelector
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.SystemImageTypeSelector
import jr.brian.home.esde.ui.components.ToggleSetting

@Composable
fun EffectsSectionContent(
    prefsState: ESDEPrefsState,
    onBackgroundColorChange: (Int) -> Unit,
    onBlurLevelChange: (Int) -> Unit,
    onDimmingLevelChange: (Int) -> Unit,
    onExcludeEffectsFromHomeChange: (Boolean) -> Unit,
    onGameImageTypeChange: (GameImageType) -> Unit,
    onRandomSystemImageChange: (Boolean) -> Unit,
    onSystemImageTypeChange: (SystemImageType) -> Unit
) {
    BackgroundColorSelector(
        selectedColor = Color(prefsState.backgroundColor),
        onColorSelected = { color ->
            onBackgroundColorChange(color.toArgb())
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_blur_level),
        value = prefsState.blurLevel.toFloat(),
        valueRange = 0f..25f,
        steps = 24,
        valueText = if (prefsState.blurLevel == 0)
            stringResource(R.string.esde_settings_off)
        else
            "${prefsState.blurLevel}",
        onValueChange = { blur ->
            onBlurLevelChange(blur.toInt())
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_dimming_level),
        value = prefsState.dimmingLevel.toFloat(),
        valueRange = 0f..70f,
        steps = 13,
        valueText = "${prefsState.dimmingLevel}%",
        onValueChange = { dimming ->
            onDimmingLevelChange(dimming.toInt())
        }
    )

    ToggleSetting(
        title = stringResource(R.string.esde_settings_exclude_effects_from_home),
        description = stringResource(R.string.esde_settings_exclude_effects_from_home_description),
        checked = prefsState.excludeEffectsFromHome,
        onCheckedChange = { exclude ->
            onExcludeEffectsFromHomeChange(exclude)
        }
    )

    GameImageTypeSelector(
        selectedType = prefsState.gameImageType,
        onTypeSelected = { type ->
            onGameImageTypeChange(type)
        }
    )

    ToggleSetting(
        title = stringResource(R.string.esde_settings_random_system_image),
        description = stringResource(R.string.esde_settings_random_system_image_description),
        checked = prefsState.randomSystemImage,
        onCheckedChange = { random ->
            onRandomSystemImageChange(random)
        }
    )

    SystemImageTypeSelector(
        selectedType = prefsState.systemImageType,
        onTypeSelected = { type ->
            onSystemImageTypeChange(type)
        }
    )
}
