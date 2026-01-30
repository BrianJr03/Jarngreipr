package jr.brian.home.ui.components.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dock
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.home.R

@Composable
fun DockSettingsItem(
    onClick: () -> Unit
) {
    SettingItem(
        title = stringResource(R.string.dock_settings_title),
        description = stringResource(R.string.dock_settings_description),
        icon = Icons.Default.Dock,
        onClick = onClick
    )
}
