package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.preferences.MusicVideoBehavior
import jr.brian.home.esde.ui.components.MusicVideoBehaviorSelector
import jr.brian.home.esde.ui.components.PathSetting
import jr.brian.home.esde.ui.components.SliderSetting
import jr.brian.home.esde.ui.components.ToggleSetting

@Composable
fun MusicSectionContent(
    prefsState: ESDEPrefsState,
    onMusicEnabledChange: (Boolean) -> Unit,
    onMusicVolumeChange: (Int) -> Unit,
    onSelectMusicPath: () -> Unit,
    onClearMusicPath: () -> Unit,
    onMusicGameEnabledChange: (Boolean) -> Unit,
    onMusicScreensaverEnabledChange: (Boolean) -> Unit,
    onMusicSystemEnabledChange: (Boolean) -> Unit,
    onMusicVideoBehaviorChange: (MusicVideoBehavior) -> Unit,
    onMusicUseSystemSpecificChange: (Boolean) -> Unit,
    onMusicLoopEnabledChange: (Boolean) -> Unit
) {
    ToggleSetting(
        title = stringResource(R.string.esde_settings_music_enabled),
        description = stringResource(R.string.esde_settings_music_enabled_description),
        checked = prefsState.musicEnabled,
        onCheckedChange = { enabled ->
            onMusicEnabledChange(enabled)
        }
    )

    if (prefsState.musicEnabled) {
        SliderSetting(
            title = stringResource(R.string.esde_settings_music_volume),
            value = prefsState.musicVolume.toFloat(),
            valueRange = 0f..100f,
            steps = 19,
            valueText = "${prefsState.musicVolume}%",
            onValueChange = { volume ->
                onMusicVolumeChange(volume.toInt())
            },
            enabled = false,
            description = stringResource(R.string.esde_settings_music_slider_description)
        )

        PathSetting(
            title = stringResource(R.string.esde_settings_music_path),
            description = stringResource(R.string.esde_settings_music_path_description),
            currentPath = prefsState.musicPath,
            defaultText = stringResource(R.string.esde_settings_path_not_set),
            onSelectPath = onSelectMusicPath,
            onClearPath = onClearMusicPath
        )

        ToggleSetting(
            title = stringResource(R.string.esde_settings_music_use_system_specific),
            description = stringResource(R.string.esde_settings_music_use_system_specific_description),
            checked = prefsState.musicUseSystemSpecific,
            onCheckedChange = { useSystemSpecific ->
                onMusicUseSystemSpecificChange(useSystemSpecific)
            }
        )

        ToggleSetting(
            title = stringResource(R.string.esde_settings_music_loop),
            description = stringResource(R.string.esde_settings_music_loop_description),
            checked = prefsState.musicLoopEnabled,
            onCheckedChange = { loopEnabled ->
                onMusicLoopEnabledChange(loopEnabled)
            }
        )

        ToggleSetting(
            title = stringResource(R.string.esde_settings_music_game),
            description = stringResource(R.string.esde_settings_music_game_description),
            checked = prefsState.musicGameEnabled,
            onCheckedChange = { enabled ->
                onMusicGameEnabledChange(enabled)
            }
        )

        ToggleSetting(
            title = stringResource(R.string.esde_settings_music_screensaver),
            description = stringResource(R.string.esde_settings_music_screensaver_description),
            checked = prefsState.musicScreensaverEnabled,
            onCheckedChange = { enabled ->
                onMusicScreensaverEnabledChange(enabled)
            }
        )

        ToggleSetting(
            title = stringResource(R.string.esde_settings_music_system),
            description = stringResource(R.string.esde_settings_music_system_description),
            checked = prefsState.musicSystemEnabled,
            onCheckedChange = { enabled ->
                onMusicSystemEnabledChange(enabled)
            }
        )

        MusicVideoBehaviorSelector(
            selectedBehavior = prefsState.musicVideoBehavior,
            onBehaviorSelected = { behavior ->
                onMusicVideoBehaviorChange(behavior)
            }
        )
    }
}
