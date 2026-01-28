package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import jr.brian.home.data.FolderManager.Companion.TAB_TYPE_WIDGETS
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.ui.components.appsandwidgets.AppVisibilityDialogForWidgetTab
import jr.brian.home.ui.components.appsandwidgets.TabContent
import jr.brian.home.ui.components.dialog.AppsAndWidgetsOptionsDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.util.Routes
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import jr.brian.home.ui.animations.onPressScaleAndOffset

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsAndWidgetsTab(
    pageIndex: Int,
    widgets: List<WidgetInfo>,
    viewModel: WidgetViewModel,
    modifier: Modifier = Modifier,
    powerViewModel: PowerViewModel = hiltViewModel(),
    allApps: List<AppInfo> = emptyList(),
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    onShowBottomSheet: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNavigateToResize: (WidgetInfo, Int) -> Unit = { _, _ -> },
    onDeletePage: (Int) -> Unit = {},
    pageIndicatorBorderColor: Color = ThemeSecondaryColor,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToRecentApps: () -> Unit = {},
    navController: NavHostController? = null
) {
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val folderManager = LocalFolderManager.current
    val columns = gridSettingsManager.columnCount
    val scope = rememberCoroutineScope()

    val folders by folderManager.getFolders(pageIndex, TAB_TYPE_WIDGETS)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

    val addWidgetIconFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var swapModeEnabled by remember { mutableStateOf(false) }
    var swapSourceWidgetId by remember { mutableStateOf<Int?>(null) }
    var showFolderOptionsDialog by remember { mutableStateOf(false) }
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showHomeTabDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showFolderContentsDialog by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editModeEnabled = uiState.editModeByPage[pageIndex] ?: false

    val visibleApps by widgetPageAppManager.getVisibleApps(pageIndex)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val appsFirst by widgetPageAppManager.getAppsFirstOrder(pageIndex)
        .collectAsStateWithLifecycle(initialValue = false)

    val displayedApps = remember(allApps, visibleApps) {
        allApps.filter { it.packageName in visibleApps }
    }

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()

    val settingsIconFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val gridState = rememberLazyGridState()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed && !showDrawerOptionsDialog)

    BackHandler(enabled = isPoweredOff) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(pressScale)
            .offset(y = offsetY)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
                if (showWidgetPicker ||
                    showAddOptionsDialog ||
                    showAppSelectionDialog ||
                    swapModeEnabled
                ) {
                    Modifier.blockAllNavigation()
                } else {
                    Modifier.blockHorizontalNavigation()
                }
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onDoubleClick = {
                    powerViewModel.togglePower()
                },
                onLongClick = {
                    showDrawerOptionsDialog = true
                }
            )
    ) {
        if (showWidgetPicker) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = ThemePrimaryColor,
                    strokeWidth = 4.dp
                )
            }
        } else {
            TabContent(
                swapModeEnabled = swapModeEnabled,
                editModeEnabled = editModeEnabled,
                pagerState = pagerState,
                isHeaderVisible = isHeaderVisible,
                totalPages = totalPages,
                powerViewModel = powerViewModel,
                isPowerButtonVisible = isPowerButtonVisible,
                onSettingsClick = onSettingsClick,
                settingsIconFocusRequester = settingsIconFocusRequester,
                onShowOptionsDialog = { showAddOptionsDialog = true },
                addWidgetIconFocusRequester = addWidgetIconFocusRequester,
                onShowBottomSheet = onShowBottomSheet,
                onDeletePage = onDeletePage,
                pageIndicatorBorderColor = pageIndicatorBorderColor,
                allApps = allApps,
                onNavigateToSearch = onNavigateToSearch,
                widgets = widgets,
                displayedApps = displayedApps,
                folders = folders,
                gridState = gridState,
                columns = columns,
                appsFirst = appsFirst,
                pageIndex = pageIndex,
                viewModel = viewModel,
                onNavigateToResize = onNavigateToResize,
                swapSourceWidgetId = swapSourceWidgetId,
                onSwapModeDisabled = {
                    swapModeEnabled = false
                    swapSourceWidgetId = null
                },
                onEditModeToggle = { viewModel.toggleEditMode(pageIndex) },
                onSwapModeEnabled = { widgetId ->
                    swapModeEnabled = true
                    swapSourceWidgetId = widgetId
                },
                onFolderClick = { folder ->
                    selectedFolder = folder
                    showFolderContentsDialog = true
                }
            )
        }
    }

    if (showAddOptionsDialog || showFolderOptionsDialog) {
        val isTabEmpty = widgets.isEmpty() && displayedApps.isEmpty()

        AppsAndWidgetsOptionsDialog(
            onDismiss = {
                showAddOptionsDialog = false
                showFolderOptionsDialog = false
            },
            onAddWidget = { showWidgetPicker = true },
            onAddApp = { showAppSelectionDialog = true },
            onSwapSections = {
                scope.launch {
                    widgetPageAppManager.toggleSectionOrder(pageIndex)
                }
            },
            onToggleEditMode = {
                viewModel.toggleEditMode(pageIndex)
            },
            isEditModeActive = editModeEnabled,
            isEmpty = isTabEmpty
        )
    }

    if (showAppSelectionDialog) {
        AppVisibilityDialogForWidgetTab(
            apps = allApps,
            visibleApps = visibleApps,
            pageIndex = pageIndex,
            onDismiss = { showAppSelectionDialog = false },
            widgetPageAppManager = widgetPageAppManager
        )
    }

    if (showWidgetPicker) {
        LaunchedEffect(Unit) {
            navController?.navigate(Routes.widgetPicker(pageIndex))
            showWidgetPicker = false
        }
    }

    if (showHomeTabDialog) {
        val homeTabManager = LocalHomeTabManager.current
        val pageTypeManager = LocalPageTypeManager.current
        val pageCountManager = LocalPageCountManager.current
        val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
        val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()

        HomeTabSelectionDialog(
            currentTabIndex = currentHomeTabIndex,
            totalPages = totalPages,
            onTabSelected = { index ->
                homeTabManager.setHomeTabIndex(index)
            },
            onDismiss = { showHomeTabDialog = false },
            onDeletePage = { pageIndex ->
                onDeletePage(pageIndex)
            },
            onAddPage = { pageType ->
                pageTypeManager.addPage(pageType)
                pageCountManager.addPage()
            },
            pageTypes = pageTypes,
            onNavigateToSearch = onNavigateToSearch
        )
    }

    if (showDrawerOptionsDialog) {
        DrawerOptionsDialog(
            onDismiss = { showDrawerOptionsDialog = false },
            onPowerClick = {
                powerViewModel.togglePower()
            },
            onTabsClick = {
                showHomeTabDialog = true
            },
            onMenuClick = {
                showAddOptionsDialog = true
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = {
                showCreateFolderDialog = true
            },
            onRecentAppsClick = onNavigateToRecentApps
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            apps = displayedApps,
            onDismiss = { showCreateFolderDialog = false },
            pageIndex = pageIndex,
            allApps = allApps,
            tabType = jr.brian.home.data.FolderManager.TAB_TYPE_WIDGETS
        )
    }

    if (showFolderContentsDialog && selectedFolder != null) {
        val folderApps = allApps.filter { it.packageName in selectedFolder!!.appPackageNames }
        FolderContentsDialog(
            folderName = selectedFolder!!.name,
            apps = folderApps,
            folderId = selectedFolder!!.id,
            pageIndex = pageIndex,
            allApps = allApps,
            tabType = jr.brian.home.data.FolderManager.TAB_TYPE_WIDGETS,
            onDismiss = {
                showFolderContentsDialog = false
                selectedFolder = null
            }
        )
    }
}
