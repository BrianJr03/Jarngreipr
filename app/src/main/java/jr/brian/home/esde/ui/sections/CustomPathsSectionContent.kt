package jr.brian.home.esde.ui.sections

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.esde.preferences.ESDEPrefsState
import jr.brian.home.esde.ui.components.PathSetting

@Composable
fun CustomPathsSectionContent(
    prefsState: ESDEPrefsState,
    onSelectSystemImagesPath: () -> Unit,
    onClearSystemImagesPath: () -> Unit,
    onSelectSystemLogosPath: () -> Unit,
    onClearSystemLogosPath: () -> Unit,
    onSelectMediaPath: () -> Unit,
    onClearMediaPath: () -> Unit
) {
    PathSetting(
        title = stringResource(R.string.esde_settings_custom_system_images_path),
        description = stringResource(R.string.esde_settings_custom_system_images_path_description),
        currentPath = prefsState.customSystemImagesPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSystemImagesPath,
        onClearPath = onClearSystemImagesPath
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_custom_system_logos_path),
        description = stringResource(R.string.esde_settings_custom_system_logos_path_description),
        currentPath = prefsState.customSystemLogosPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSystemLogosPath,
        onClearPath = onClearSystemLogosPath
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_custom_media_path),
        description = stringResource(R.string.esde_settings_custom_media_path_description),
        currentPath = prefsState.customMediaPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectMediaPath,
        onClearPath = onClearMediaPath
    )
}
