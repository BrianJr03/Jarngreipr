package jr.brian.home.ui.components.settings.sections

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.IconPackSelectorItem
import jr.brian.home.ui.components.settings.IconShapeToggleItem
import jr.brian.home.ui.components.settings.OledModeToggleItem
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.ThemeSelectorItem
import jr.brian.home.ui.components.settings.WallpaperSelectorItem
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_ICON_PACK
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_THEME
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_WALLPAPER
import jr.brian.home.util.SettingsTag

@Composable
fun AppearanceSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onNavigateToCustomTheme: () -> Unit,
    onIconPackChanged: () -> Unit,
    onNavigateToEsdeSettings: () -> Unit = {}
) {
    var expandedItem by remember { mutableStateOf<String?>(null) }
    
    CollapsibleSettingsSection(
        title = stringResource(id = R.string.settings_section_appearance),
        icon = Icons.Default.Palette,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        ThemeSelectorItem(
            isExpanded = expandedItem == EXPANDED_THEME,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_THEME else null
            },
            onNavigateToCustomTheme = {
                expandedItem = null
                onNavigateToCustomTheme()
            }
        )

        SettingItem(
            tag = SettingsTag.EXPERIMENTAL,
            title = stringResource(R.string.esde_settings_title),
            description = stringResource(R.string.esde_settings_description),
            icon = Icons.Default.Gamepad,
            onClick = onNavigateToEsdeSettings
        )

        IconPackSelectorItem(
            isExpanded = expandedItem == EXPANDED_ICON_PACK,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_ICON_PACK else null
            },
            onIconPackChanged = onIconPackChanged
        )

        IconShapeToggleItem(
            isExpanded = false
        )

        OledModeToggleItem(
            isExpanded = false
        )

        WallpaperSelectorItem(
            isExpanded = expandedItem == EXPANDED_WALLPAPER,
            onExpandChanged = {
                expandedItem = if (it) EXPANDED_WALLPAPER else null
            },
            onESDESetupClick = onNavigateToEsdeSettings
        )
    }
}
