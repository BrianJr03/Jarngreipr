package jr.brian.home.ui.components.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppPositionManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.components.widget.AppGridLayout
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.viewmodels.PowerViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsTabContent(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onAppFocusChanged: (Int) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppDoubleClick: (AppInfo) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    powerViewModel: PowerViewModel? = null,
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    onMenuClick: () -> Unit = {},
    onShowBottomSheet: () -> Unit = {},
    isFreeModeEnabled: Boolean = false,
    appPositionManager: AppPositionManager? = null,
    onDeletePage: (Int) -> Unit = {},
    isDragLocked: Boolean = false,
    pageIndex: Int = 0,
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
    allApps: List<AppInfo> = emptyList(),
    onNavigateToSearch: () -> Unit = {},
    folders: List<Folder> = emptyList(),
    onFolderClick: (Folder) -> Unit = {},
    gridState: LazyGridState = rememberLazyGridState()
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    val rows = gridSettingsManager.rowCount
    val unlimitedMode = gridSettingsManager.unlimitedMode
    val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows

    val filteredApps = remember(apps, maxAppsPerPage) {
        apps.sortedBy { it.label.uppercase() }
    }

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (pagerState != null) {
            val settingsIconFocusRequester = remember { FocusRequester() }
            val menuIconFocusRequester = remember { FocusRequester() }

            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                ScreenHeaderRow(
                    totalPages = totalPages,
                    pagerState = pagerState,
                    leadingIcon = Icons.Default.Settings,
                    leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                    onLeadingIconClick = onSettingsClick,
                    leadingIconFocusRequester = settingsIconFocusRequester,
                    trailingIcon = Icons.Default.Menu,
                    trailingIconContentDescription = null,
                    onTrailingIconClick = onMenuClick,
                    trailingIconFocusRequester = menuIconFocusRequester,
                    onNavigateToGrid = {
                        appFocusRequesters[0]?.requestFocus()
                    },
                    onNavigateFromGrid = {
                        menuIconFocusRequester.requestFocus()
                    },
                    powerViewModel = powerViewModel,
                    showPowerButton = isPowerButtonVisible,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    onFolderClick = onShowBottomSheet,
                    onDeletePage = onDeletePage,
                    pageIndicatorBorderColor = pageIndicatorBorderColor,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
        }

        if (isFreeModeEnabled && appPositionManager != null) {
            FreePositionedAppsLayout(
                apps = filteredApps,
                appPositionManager = appPositionManager,
                keyboardVisible = false,
                onAppClick = onAppClick,
                isDragLocked = isDragLocked,
                pageIndex = pageIndex,
                allApps = allApps,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        } else {
            AppGridLayout(
                apps = filteredApps,
                columns = columns,
                maxAppsPerPage = maxAppsPerPage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                appFocusRequesters = appFocusRequesters,
                onFocusChanged = onAppFocusChanged,
                onNavigateLeft = {},
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                onAppDoubleClick = onAppDoubleClick,
                folders = folders,
                allApps = allApps,
                onFolderClick = onFolderClick,
                isHeaderVisible = isHeaderVisible,
                equalizeMargins = true,
                isHomeScreen = true,
                gridState = gridState
            )
        }
    }
}
