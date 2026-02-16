package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.ui.components.AnimationStyleSelector
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.animation.AnimationStyle

@Composable
fun AnimationSectionContent(
    prefsState: ESDEPrefsState,
    onAnimationDurationChange: (Int) -> Unit,
    onAnimationScaleChange: (Float) -> Unit,
    onAnimationStyleChange: (AnimationStyle) -> Unit
) {
    SliderSetting(
        title = stringResource(R.string.esde_settings_animation_duration),
        value = prefsState.animationDuration.toFloat(),
        valueRange = 100f..1000f,
        steps = 8,
        valueText = "${prefsState.animationDuration}ms",
        onValueChange = { duration ->
            onAnimationDurationChange(duration.toInt())
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_animation_scale),
        value = prefsState.animationScale,
        valueRange = 0.5f..1.0f,
        steps = 9,
        valueText = "${(prefsState.animationScale * 100).toInt()}%",
        onValueChange = { scale ->
            onAnimationScaleChange(scale)
        }
    )

    AnimationStyleSelector(
        selectedStyle = prefsState.animationStyle,
        onStyleSelected = { style ->
            onAnimationStyleChange(style)
        }
    )
}
