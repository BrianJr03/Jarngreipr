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
    onSelectSingleSystemImage: () -> Unit,
    onClearSingleSystemImage: () -> Unit,
    onSelectSingleSystemLogo: () -> Unit,
    onClearSingleSystemLogo: () -> Unit,
    onSelectSingleGameImage: () -> Unit,
    onClearSingleGameImage: () -> Unit,
    onSelectSingleGameLogo: () -> Unit,
    onClearSingleGameLogo: () -> Unit,
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
        title = stringResource(R.string.esde_settings_single_system_image),
        description = stringResource(R.string.esde_settings_single_system_image_description),
        currentPath = prefsState.singleSystemImagePath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleSystemImage,
        onClearPath = onClearSingleSystemImage
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
        title = stringResource(R.string.esde_settings_single_system_logo),
        description = stringResource(R.string.esde_settings_single_system_logo_description),
        currentPath = prefsState.singleSystemLogoPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleSystemLogo,
        onClearPath = onClearSingleSystemLogo
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_single_game_image),
        description = stringResource(R.string.esde_settings_single_game_image_description),
        currentPath = prefsState.singleGameImagePath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleGameImage,
        onClearPath = onClearSingleGameImage
    )

    PathSetting(
        title = stringResource(R.string.esde_settings_single_game_logo),
        description = stringResource(R.string.esde_settings_single_game_logo_description),
        currentPath = prefsState.singleGameLogoPath,
        defaultText = stringResource(R.string.esde_settings_path_not_set),
        onSelectPath = onSelectSingleGameLogo,
        onClearPath = onClearSingleGameLogo
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
