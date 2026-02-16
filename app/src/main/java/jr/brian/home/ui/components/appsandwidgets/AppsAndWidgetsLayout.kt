package jr.brian.home.ui.components.appsandwidgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel

@Composable
fun TabContent(
    swapModeEnabled: Boolean,
    editModeEnabled: Boolean,
    pagerState: PagerState?,
    isHeaderVisible: Boolean,
    totalPages: Int,
    powerViewModel: PowerViewModel,
    isPowerButtonVisible: Boolean,
    onSettingsClick: () -> Unit,
    settingsIconFocusRequester: FocusRequester,
    onShowOptionsDialog: () -> Unit,
    addWidgetIconFocusRequester: FocusRequester,
    onShowBottomSheet: () -> Unit,
    onDeletePage: (Int) -> Unit,
    pageIndicatorBorderColor: Color,
    allApps: List<AppInfo>,
    onNavigateToSearch: () -> Unit,
    widgets: List<WidgetInfo>,
    displayedApps: List<AppInfo>,
    folders: List<Folder>,
    gridState: LazyGridState,
    columns: Int,
    appsFirst: Boolean,
    pageIndex: Int,
    viewModel: WidgetViewModel,
    onNavigateToResize: (WidgetInfo, Int) -> Unit,
    swapSourceWidgetId: Int?,
    onSwapModeDisabled: () -> Unit,
    onEditModeToggle: () -> Unit,
    onSwapModeEnabled: (Int) -> Unit,
    onFolderClick: (Folder) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TabHeader(
            swapModeEnabled = swapModeEnabled,
            editModeEnabled = editModeEnabled,
            pagerState = pagerState,
            isHeaderVisible = isHeaderVisible,
            totalPages = totalPages,
            powerViewModel = powerViewModel,
            isPowerButtonVisible = isPowerButtonVisible,
            onSettingsClick = onSettingsClick,
            settingsIconFocusRequester = settingsIconFocusRequester,
            onShowOptionsDialog = onShowOptionsDialog,
            addWidgetIconFocusRequester = addWidgetIconFocusRequester,
            onShowBottomSheet = onShowBottomSheet,
            onDeletePage = onDeletePage,
            pageIndicatorBorderColor = pageIndicatorBorderColor,
            onNavigateToSearch = onNavigateToSearch,
            onSwapModeDisabled = onSwapModeDisabled,
            onEditModeToggle = onEditModeToggle
        )

        WidgetsAndAppsGrid(
            gridState = gridState,
            columns = columns,
            appsFirst = appsFirst,
            displayedApps = displayedApps,
            folders = folders,
            allApps = allApps,
            widgets = widgets,
            editModeEnabled = editModeEnabled,
            pageIndex = pageIndex,
            viewModel = viewModel,
            onNavigateToResize = onNavigateToResize,
            swapModeEnabled = swapModeEnabled,
            swapSourceWidgetId = swapSourceWidgetId,
            onSwapComplete = onSwapModeDisabled,
            onSwapModeEnabled = onSwapModeEnabled,
            onFolderClick = onFolderClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabHeader(
    swapModeEnabled: Boolean,
    editModeEnabled: Boolean,
    pagerState: PagerState?,
    isHeaderVisible: Boolean,
    totalPages: Int,
    powerViewModel: PowerViewModel,
    isPowerButtonVisible: Boolean,
    onSettingsClick: () -> Unit,
    settingsIconFocusRequester: FocusRequester,
    onShowOptionsDialog: () -> Unit,
    addWidgetIconFocusRequester: FocusRequester,
    onShowBottomSheet: () -> Unit,
    onDeletePage: (Int) -> Unit,
    pageIndicatorBorderColor: Color,
    onNavigateToSearch: () -> Unit,
    onSwapModeDisabled: () -> Unit,
    onEditModeToggle: () -> Unit
) {
    when {
        swapModeEnabled -> {
            WidgetSwapModeHeaderCard(onClick = onSwapModeDisabled)
        }

        editModeEnabled && pagerState != null -> {
            WidgetEditModeHeaderCard(onClick = onEditModeToggle)
        }

        pagerState != null -> {
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                ScreenHeaderRow(
                    totalPages = totalPages,
                    pagerState = pagerState,
                    powerViewModel = powerViewModel,
                    showPowerButton = isPowerButtonVisible,
                    leadingIcon = Icons.Default.Settings,
                    leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                    onLeadingIconClick = onSettingsClick,
                    leadingIconFocusRequester = settingsIconFocusRequester,
                    trailingIcon = Icons.Default.Menu,
                    trailingIconContentDescription = null,
                    onTrailingIconClick = onShowOptionsDialog,
                    trailingIconFocusRequester = addWidgetIconFocusRequester,
                    onNavigateToGrid = {},
                    onNavigateFromGrid = {
                        addWidgetIconFocusRequester.requestFocus()
                    },
                    onFolderClick = onShowBottomSheet,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    onDeletePage = onDeletePage,
                    pageIndicatorBorderColor = pageIndicatorBorderColor,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
        }
    }
}

@Composable
fun WidgetsAndAppsGrid(
    gridState: LazyGridState,
    columns: Int,
    appsFirst: Boolean,
    displayedApps: List<AppInfo>,
    folders: List<Folder>,
    allApps: List<AppInfo>,
    widgets: List<WidgetInfo>,
    editModeEnabled: Boolean,
    pageIndex: Int,
    viewModel: WidgetViewModel,
    onNavigateToResize: (WidgetInfo, Int) -> Unit,
    swapModeEnabled: Boolean,
    swapSourceWidgetId: Int?,
    onSwapComplete: () -> Unit,
    onSwapModeEnabled: (Int) -> Unit,
    onFolderClick: (Folder) -> Unit
) {
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val sections = if (appsFirst) {
            listOf("apps" to displayedApps, "folders" to folders, "widgets" to widgets)
        } else {
            listOf("widgets" to widgets, "apps" to displayedApps, "folders" to folders)
        }

        sections.forEach { (sectionType, _) ->
            when (sectionType) {
                "apps" -> renderAppItems(
                    apps = displayedApps,
                    items = displayedApps,
                    editModeEnabled = editModeEnabled,
                    pageIndex = pageIndex
                )

                "folders" -> renderFolderItems(
                    folders = folders,
                    allApps = allApps,
                    editModeEnabled = editModeEnabled,
                    onClick = onFolderClick
                )

                "widgets" -> renderWidgetItems(
                    widgets = widgets,
                    items = widgets,
                    columns = columns,
                    pageIndex = pageIndex,
                    viewModel = viewModel,
                    onNavigateToResize = onNavigateToResize,
                    swapModeEnabled = swapModeEnabled,
                    swapSourceWidgetId = swapSourceWidgetId,
                    onSwapComplete = onSwapComplete,
                    onSwapModeEnabled = onSwapModeEnabled,
                    editModeEnabled = editModeEnabled
                )
            }
        }
    }
}
