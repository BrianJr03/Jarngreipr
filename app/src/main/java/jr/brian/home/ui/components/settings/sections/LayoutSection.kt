package jr.brian.home.ui.components.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.settings.AppDrawerFabSettingsItem
import jr.brian.home.ui.components.settings.BackButtonShortcutItem
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.DockSettingsItem
import jr.brian.home.ui.components.settings.GridColumnSelectorItem
import jr.brian.home.ui.components.settings.ThorSettingsItem
import jr.brian.home.ui.components.settings.VisibilitySettingsItem
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_APP_DRAWER_FAB
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_BACK_BUTTON
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_DOCK
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_GRID
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_THOR
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_VISIBILITY

@Composable
fun LayoutSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isThorDevice: Boolean,
    allAppsUnfiltered: List<AppInfo>,
    onNavigateToBackButtonShortcut: () -> Unit,
    onNavigateToDockSettings: () -> Unit = {}
) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    
    CollapsibleSettingsSection(
        title = stringResource(id = R.string.settings_section_layout),
        icon = Icons.Default.ViewModule,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        BackButtonShortcutItem(
            isExpanded = expandedItem == EXPANDED_BACK_BUTTON,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_BACK_BUTTON else null
            },
            onConfigureClick = {
                expandedItem = null
                onNavigateToBackButtonShortcut()
            }
        )

        DockSettingsItem(
            onClick = {
                expandedItem = null
                onNavigateToDockSettings()
            }
        )

        AppDrawerFabSettingsItem(
            isExpanded = expandedItem == EXPANDED_APP_DRAWER_FAB,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_APP_DRAWER_FAB else null
            }
        )

        GridColumnSelectorItem(
            isExpanded = expandedItem == EXPANDED_GRID,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_GRID else null
            },
            totalAppsCount = allAppsUnfiltered.size
        )

        if (isThorDevice) {
            ThorSettingsItem(
                isExpanded = expandedItem == EXPANDED_THOR,
                onExpandChanged = {
                    expandedItem = if (it) EXPANDED_THOR else null
                }
            )
        }

        VisibilitySettingsItem(
            isExpanded = expandedItem == EXPANDED_VISIBILITY,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_VISIBILITY else null
            }
        )
    }
}
