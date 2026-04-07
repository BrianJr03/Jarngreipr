package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.PageType
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.settings.AppDrawerFabSettingsItem
import jr.brian.home.ui.components.settings.BackButtonShortcutItem
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.DockSettingsItem
import jr.brian.home.ui.components.settings.GridColumnSelectorItem
import jr.brian.home.ui.components.settings.ThorSettingsItem
import jr.brian.home.ui.components.settings.VisibilitySettingsItem
import jr.brian.home.esde.ui.components.ToggleSetting
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
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

    val pageTypeManager = LocalPageTypeManager.current
    val appPositionManager = LocalAppPositionManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val scrollDisabledByPage by appPositionManager.isScrollDisabledByPage.collectAsStateWithLifecycle()
    val appDrawerFilterByPage by powerSettingsManager.appDrawerFilterByPage.collectAsStateWithLifecycle()

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

        val appsTabs = pageTypes.mapIndexedNotNull { index, type ->
            if (type == PageType.APPS_TAB) index else null
        }
        if (appsTabs.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_layout_scroll_per_tab),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                appsTabs.forEachIndexed { tabNumber, pageIndex ->
                    val isDisabled = scrollDisabledByPage[pageIndex] ?: false
                    ToggleSetting(
                        title = "Tab ${tabNumber + 1}",
                        description = stringResource(R.string.settings_layout_scroll_description),
                        checked = isDisabled,
                        onCheckedChange = { appPositionManager.setScrollDisabled(pageIndex, it) }
                    )
                }
            }
        }

        val bottomFlingDisabledByPage by appPositionManager.isBottomFlingDisabledByPage.collectAsStateWithLifecycle()
        val flingTabs = pageTypes.mapIndexedNotNull { index, type ->
            if (type == PageType.APPS_TAB || type == PageType.APPS_AND_WIDGETS_TAB) index else null
        }
        if (flingTabs.isNotEmpty()) {
            Text(
                text = stringResource(R.string.settings_layout_bottom_fling_per_tab),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                flingTabs.forEachIndexed { tabNumber, pageIndex ->
                    val isDisabled = bottomFlingDisabledByPage[pageIndex] ?: false
                    ToggleSetting(
                        title = "Tab ${tabNumber + 1}",
                        description = stringResource(R.string.settings_layout_bottom_fling_description),
                        checked = isDisabled,
                        onCheckedChange = { appPositionManager.setBottomFlingDisabled(pageIndex, it) }
                    )
                }
            }
        }

        ToggleSetting(
            title = stringResource(R.string.settings_layout_app_drawer_filter_by_page),
            description = stringResource(R.string.settings_layout_app_drawer_filter_by_page_description),
            checked = appDrawerFilterByPage,
            onCheckedChange = { powerSettingsManager.setAppDrawerFilterByPage(it) }
        )
    }
}
