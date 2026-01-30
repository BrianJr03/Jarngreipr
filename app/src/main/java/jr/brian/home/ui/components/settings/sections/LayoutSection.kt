package jr.brian.home.ui.components.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.res.stringResource
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.settings.BackButtonShortcutItem
import jr.brian.home.ui.components.settings.DockSettingsItem
import jr.brian.home.ui.components.settings.GridColumnSelectorItem
import jr.brian.home.ui.components.settings.SettingsSectionHeader
import jr.brian.home.ui.components.settings.ThorSettingsItem
import jr.brian.home.ui.components.settings.VisibilitySettingsItem
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_BACK_BUTTON
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_DOCK
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_GRID
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_THOR
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_VISIBILITY

fun LazyListScope.layoutSection(
    expandedItem: String?,
    onExpandedItemChange: (String?) -> Unit,
    isVisible: (String?) -> Boolean,
    isThorDevice: Boolean,
    allAppsUnfiltered: List<AppInfo>,
    onNavigateToBackButtonShortcut: () -> Unit,
    onNavigateToDockSettings: () -> Unit = {}
) {
    item(key = "header_layout") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_BACK_BUTTON)
                    || isVisible(EXPANDED_GRID)
                    || isVisible(EXPANDED_THOR)
                    || isVisible(EXPANDED_VISIBILITY)
                    || isVisible(EXPANDED_DOCK),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_layout)
            )
        }
    }

    item(key = "back_button") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_BACK_BUTTON),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BackButtonShortcutItem(
                isExpanded = expandedItem == EXPANDED_BACK_BUTTON,
                onExpandChanged = {
                    onExpandedItemChange(if (it) EXPANDED_BACK_BUTTON else null)
                },
                onConfigureClick = {
                    onExpandedItemChange(null)
                    onNavigateToBackButtonShortcut()
                }
            )
        }
    }

    item(key = "dock") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_DOCK),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            DockSettingsItem(
                onClick = {
                    onExpandedItemChange(null)
                    onNavigateToDockSettings()
                }
            )
        }
    }

    item(key = "grid") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_GRID),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            GridColumnSelectorItem(
                isExpanded = expandedItem == EXPANDED_GRID,
                onExpandChanged = {
                    onExpandedItemChange(if (it) EXPANDED_GRID else null)
                },
                totalAppsCount = allAppsUnfiltered.size
            )
        }
    }

    if (isThorDevice) {
        item(key = "thor") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_THOR),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ThorSettingsItem(
                    isExpanded = expandedItem == EXPANDED_THOR,
                    onExpandChanged = {
                        onExpandedItemChange(if (it) EXPANDED_THOR else null)
                    }
                )
            }
        }
    }

    item(key = "visibility") {
        AnimatedVisibility(
            visible = isVisible(EXPANDED_VISIBILITY),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            VisibilitySettingsItem(
                isExpanded = expandedItem == EXPANDED_VISIBILITY,
                onExpandChanged = {
                    onExpandedItemChange(if (it) EXPANDED_VISIBILITY else null)
                }
            )
        }
    }
}
