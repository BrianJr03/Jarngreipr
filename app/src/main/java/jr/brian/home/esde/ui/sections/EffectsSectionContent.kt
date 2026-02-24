package jr.brian.home.esde.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jr.brian.home.R
import jr.brian.home.esde.preferences.BackgroundScaleMode
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.preferences.GameImageType
import jr.brian.home.esde.preferences.SystemImageType
import jr.brian.home.esde.ui.components.BackgroundColorSelector
import jr.brian.home.esde.ui.components.EffectsExcludedPageOption
import jr.brian.home.esde.ui.components.GameBackgroundScaleModeSelector
import jr.brian.home.esde.ui.components.GameImageTypeSelector
import jr.brian.home.esde.ui.components.SystemBackgroundScaleModeSelector
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.SystemImageTypeSelector
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.model.PageType

@Composable
fun EffectsSectionContent(
    prefsState: ESDEPrefsState,
    pageTypes: List<PageType>,
    onBackgroundColorChange: (Int) -> Unit,
    onSystemBackgroundScaleModeChange: (BackgroundScaleMode) -> Unit,
    onGameBackgroundScaleModeChange: (BackgroundScaleMode) -> Unit,
    onSystemBlurLevelChange: (Int) -> Unit,
    onGameBlurLevelChange: (Int) -> Unit,
    onSystemBackgroundDimmingChange: (Int) -> Unit,
    onGameBackgroundDimmingChange: (Int) -> Unit,
    onToggleEffectsExcludedPage: (Int) -> Unit,
    onGameImageTypeChange: (GameImageType) -> Unit,
    onRandomSystemImageChange: (Boolean) -> Unit,
    onSystemImageTypeChange: (SystemImageType) -> Unit,
    onAndroidGamesBackgroundScaleChange: (Float) -> Unit
) {
    BackgroundColorSelector(
        selectedColor = Color(prefsState.backgroundColor),
        onColorSelected = { color ->
            onBackgroundColorChange(color.toArgb())
        }
    )

    SystemBackgroundScaleModeSelector(
        selectedMode = prefsState.systemBackgroundScaleMode,
        onModeSelected = { mode ->
            onSystemBackgroundScaleModeChange(mode)
        }
    )

    GameBackgroundScaleModeSelector(
        selectedMode = prefsState.gameBackgroundScaleMode,
        onModeSelected = { mode ->
            onGameBackgroundScaleModeChange(mode)
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_android_games_bg_scale),
        value = prefsState.androidGamesBackgroundScale,
        valueRange = 0.2f..1.0f,
        steps = 7,
        valueText = "${(prefsState.androidGamesBackgroundScale * 100).toInt()}%",
        onValueChange = { scale ->
            onAndroidGamesBackgroundScaleChange(scale)
        },
        description = stringResource(R.string.esde_settings_android_games_bg_scale_description)
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_system_blur_level),
        value = prefsState.systemBlurLevel.toFloat(),
        valueRange = 0f..25f,
        steps = 24,
        valueText = if (prefsState.systemBlurLevel == 0)
            stringResource(R.string.esde_settings_off)
        else
            "${prefsState.systemBlurLevel}",
        onValueChange = { blur ->
            onSystemBlurLevelChange(blur.toInt())
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_game_blur_level),
        value = prefsState.gameBlurLevel.toFloat(),
        valueRange = 0f..25f,
        steps = 24,
        valueText = if (prefsState.gameBlurLevel == 0)
            stringResource(R.string.esde_settings_off)
        else
            "${prefsState.gameBlurLevel}",
        onValueChange = { blur ->
            onGameBlurLevelChange(blur.toInt())
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_system_background_dimming),
        value = prefsState.systemBackgroundDimming.toFloat(),
        valueRange = 0f..70f,
        steps = 13,
        valueText = "${prefsState.systemBackgroundDimming}%",
        onValueChange = { dimming ->
            onSystemBackgroundDimmingChange(dimming.toInt())
        }
    )

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

    if (pageTypes.size > 1) {
        Column {
            Text(
                text = stringResource(R.string.esde_settings_effects_excluded_pages_title),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stringResource(R.string.esde_settings_effects_excluded_pages_description),
                color = Color.Gray.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (pageIndex in pageTypes.indices) {
                    val isExcluded = prefsState.isEffectsExcludedOnPage(pageIndex)
                    EffectsExcludedPageOption(
                        pageIndex = pageIndex,
                        isExcluded = isExcluded,
                        onToggle = { onToggleEffectsExcludedPage(pageIndex) }
                    )
                }
            }
        }
    }

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
