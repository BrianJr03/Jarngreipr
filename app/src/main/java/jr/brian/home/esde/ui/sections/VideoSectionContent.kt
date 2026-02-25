package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.model.ESDEPrefsState
import jr.brian.home.esde.model.VideoScaleMode
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.esde.ui.components.VideoScaleModeSelector

@Composable
fun VideoSectionContent(
    prefsState: ESDEPrefsState,
    onVideoAudioEnabledChange: (Boolean) -> Unit,
    onVideoScaleModeChange: (VideoScaleMode) -> Unit,
    onVideoDelayChange: (Int) -> Unit,
    onVideoEnabledChange: (Boolean) -> Unit
) {
    ToggleSetting(
        title = stringResource(R.string.esde_settings_video_audio),
        description = stringResource(R.string.esde_settings_video_audio_description),
        checked = prefsState.videoAudioEnabled,
        onCheckedChange = { enabled ->
            onVideoAudioEnabledChange(enabled)
        }
    )

    VideoScaleModeSelector(
        selectedMode = prefsState.videoScaleMode,
        onModeSelected = { mode ->
            onVideoScaleModeChange(mode)
        }
    )

    SliderSetting(
        title = stringResource(R.string.esde_settings_video_delay),
        value = prefsState.videoDelaySeconds.toFloat(),
        valueRange = 0f..10f,
        steps = 9,
        valueText = "${prefsState.videoDelaySeconds}s",
        onValueChange = { delay ->
            onVideoDelayChange(delay.toInt())
        }
    )

    ToggleSetting(
        title = stringResource(R.string.esde_settings_video_enabled),
        description = stringResource(R.string.esde_settings_video_enabled_description),
        checked = prefsState.videoEnabled,
        onCheckedChange = { enabled ->
            onVideoEnabledChange(enabled)
        }
    )
}
