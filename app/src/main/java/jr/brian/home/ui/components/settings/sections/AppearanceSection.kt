package jr.brian.home.ui.components.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.ui.components.settings.IconPackSelectorItem
import jr.brian.home.ui.components.settings.OledModeToggleItem
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.SettingsSectionHeader
import jr.brian.home.ui.components.settings.ThemeSelectorItem
import jr.brian.home.ui.components.settings.WallpaperSelectorItem
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_ICON_PACK
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_THEME
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_WALLPAPER
import jr.brian.home.util.SettingsTag

fun LazyListScope.appearanceSection(
    expandedItem: String?,
    onExpandedItemChange: (String?) -> Unit,
    isVisible: (String?) -> Boolean,
    onNavigateToCustomTheme: () -> Unit,
    onIconPackChanged: () -> Unit,
    onNavigateToEsdeSettings: () -> Unit = {}
) {
    item(key = "header_appearance") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_THEME) || isVisible(EXPANDED_ICON_PACK) || isVisible(EXPANDED_WALLPAPER),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_appearance)
            )
        }
    }

    item(key = "theme") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_THEME),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            ThemeSelectorItem(
                isExpanded = expandedItem == EXPANDED_THEME,
                onExpandChanged = {
                    onExpandedItemChange(if (it) EXPANDED_THEME else null)
                },
                onNavigateToCustomTheme = {
                    onExpandedItemChange(null)
                    onNavigateToCustomTheme()
                }
            )
        }
    }

    item(key = "icon_pack") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_ICON_PACK),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            IconPackSelectorItem(
                isExpanded = expandedItem == EXPANDED_ICON_PACK,
                onExpandChanged = {
                    onExpandedItemChange(if (it) EXPANDED_ICON_PACK else null)
                },
                onIconPackChanged = onIconPackChanged
            )
        }
    }

    item(key = "oled") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OledModeToggleItem(
                isExpanded = false
            )
        }
    }

    item(key = "wallpaper") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_WALLPAPER),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            WallpaperSelectorItem(
                isExpanded = expandedItem == EXPANDED_WALLPAPER,
                onExpandChanged = {
                    onExpandedItemChange(if (it) EXPANDED_WALLPAPER else null)
                }
            )
        }
    }

    item(key = "esde_settings") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingItem(
                tag = SettingsTag.EXPERIMENTAL,
                title = stringResource(R.string.esde_settings_title),
                description = stringResource(R.string.esde_settings_description),
                icon = Icons.Default.Gamepad,
                onClick = onNavigateToEsdeSettings
            )
        }
    }
}
