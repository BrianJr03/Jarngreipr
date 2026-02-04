package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.navigation.NavHostController
import jr.brian.home.data.FolderManager.Companion.TAB_TYPE_WIDGETS
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.widget.WidgetInfo
import androidx.compose.ui.platform.LocalContext
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.components.dock.AppDock
import jr.brian.home.ui.components.appsandwidgets.AppVisibilityDialogForWidgetTab
import jr.brian.home.ui.components.appsandwidgets.TabContent
import jr.brian.home.ui.components.dialog.AppsAndWidgetsOptionsDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.DockAppSelectionDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.esde.ui.ESDESetupScreen
import jr.brian.home.esde.setup.SetupStep
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalDockManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.Routes
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

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
    onNavigateToDockSettings: () -> Unit = {},
    navController: NavHostController? = null
) {
    val context = LocalContext.current
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val folderManager = LocalFolderManager.current
    val dockManager = LocalDockManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val columns = gridSettingsManager.columnCount
    val scope = rememberCoroutineScope()

    val folders by folderManager.getFolders(pageIndex, TAB_TYPE_WIDGETS)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

    val addWidgetIconFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    val addOptionsDialogState = rememberDialogState<Unit>()
    val appSelectionDialogState = rememberDialogState<Unit>()
    val widgetPickerDialogState = rememberDialogState<Unit>()
    val folderOptionsDialogState = rememberDialogState<Unit>()
    val drawerOptionsDialogState = rememberDialogState<Unit>()
    val homeTabDialogState = rememberDialogState<Unit>()
    val createFolderDialogState = rememberDialogState<Unit>()
    val dockAppSelectionDialogState = rememberDialogState<Int>()
    val folderContentsDialogState = rememberDialogState<Folder>()
    val esdeSetupDialogState = rememberDialogState<SetupStep>()
    val wallpaperManager = LocalWallpaperManager.current
    var swapModeEnabled by remember { mutableStateOf(false) }
    var swapSourceWidgetId by remember { mutableStateOf<Int?>(null) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editModeEnabled = uiState.editModeByPage[pageIndex] ?: false

    val visibleApps by widgetPageAppManager.getVisibleApps(pageIndex)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val appsFirst by widgetPageAppManager.getAppsFirstOrder(pageIndex)
        .collectAsStateWithLifecycle(initialValue = false)

    val displayedApps = remember(allApps, visibleApps) {
        allApps.filter { it.packageName in visibleApps }
    }

    val isDockVisible by dockManager.isDockVisible.collectAsStateWithLifecycle()
    val isDockVisibleOnPage = dockManager.isDockVisibleOnPage(pageIndex)

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()

    val settingsIconFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    val gridState = rememberLazyGridState()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed && !drawerOptionsDialogState.isVisible)

    var isScrolling by remember { mutableStateOf(false) }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    isScrolling = true
                } else {
                    delay(300) // Wait 300ms after scrolling stops
                    isScrolling = false
                }
            }
    }

    BackHandler(enabled = isPoweredOff) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .scale(pressScale)
            .offset(y = offsetY)
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
                if (widgetPickerDialogState.isVisible ||
                    addOptionsDialogState.isVisible ||
                    appSelectionDialogState.isVisible ||
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
                    drawerOptionsDialogState.show()
                }
            )
    ) {
        if (widgetPickerDialogState.isVisible) {
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
                onShowOptionsDialog = { addOptionsDialogState.show() },
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
                onFolderClick = folderContentsDialogState::show
            )
        }

        AnimatedVisibility(
            visible = isDockVisible && isDockVisibleOnPage && !isScrolling,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppDock(
                apps = allApps,
                onAppClick = { app ->
                    val displayPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    launchApp(
                        context = context,
                        packageName = app.packageName,
                        displayPreference = displayPreference
                    )
                },
                onAppDoubleClick = { app ->
                    launchAppOnOppositeDisplay(
                        context = context,
                        packageName = app.packageName,
                        currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    )
                },
                onAppLongClick = { _ -> },
                onEmptySlotClick = { position ->
                    dockAppSelectionDialogState.show(position)
                },
                onEmptySlotLongClick = { position ->
                    dockManager.removeEmptySlot(position)
                }
            )
        }
    }

    dockAppSelectionDialogState.item?.let { position ->
        if (dockAppSelectionDialogState.isVisible) {
            val availableApps = allApps.filter { app ->
                !dockManager.isAppInDock(app.packageName)
            }
            DockAppSelectionDialog(
                apps = availableApps,
                onAppSelected = { app ->
                    dockManager.addAppToDock(
                        position = position,
                        packageName = app.packageName,
                    )
                    dockAppSelectionDialogState.dismiss()
                },
                onDismiss = dockAppSelectionDialogState::dismiss
            )
        }
    }

    if (addOptionsDialogState.isVisible || folderOptionsDialogState.isVisible) {
        val isTabEmpty = widgets.isEmpty() && displayedApps.isEmpty()

        AppsAndWidgetsOptionsDialog(
            onDismiss = {
                addOptionsDialogState.dismiss()
                folderOptionsDialogState.dismiss()
            },
            onAddWidget = { widgetPickerDialogState.show() },
            onAddApp = { appSelectionDialogState.show() },
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

    if (appSelectionDialogState.isVisible) {
        AppVisibilityDialogForWidgetTab(
            apps = allApps,
            visibleApps = visibleApps,
            pageIndex = pageIndex,
            onDismiss = appSelectionDialogState::dismiss,
            widgetPageAppManager = widgetPageAppManager
        )
    }

    if (widgetPickerDialogState.isVisible) {
        LaunchedEffect(Unit) {
            navController?.navigate(Routes.widgetPicker(pageIndex))
            widgetPickerDialogState.dismiss()
        }
    }

    if (homeTabDialogState.isVisible) {
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
            onDismiss = homeTabDialogState::dismiss,
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

    if (drawerOptionsDialogState.isVisible) {
        DrawerOptionsDialog(
            onDismiss = drawerOptionsDialogState::dismiss,
            onPowerClick = {
                powerViewModel.togglePower()
            },
            onTabsClick = {
                homeTabDialogState.show()
            },
            onMenuClick = {
                addOptionsDialogState.show()
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = {
                createFolderDialogState.show()
            },
            onDockSettingsClick = onNavigateToDockSettings,
            onESDESetupClick = {
                esdeSetupDialogState.show(SetupStep.Welcome)
            }
        )
    }

    ESDESetupScreen(
        dialogState = esdeSetupDialogState,
        onDismiss = { },
        onSetupComplete = {
            wallpaperManager.setESDE()
        }
    )

    if (createFolderDialogState.isVisible) {
        CreateFolderDialog(
            apps = displayedApps,
            onDismiss = createFolderDialogState::dismiss,
            pageIndex = pageIndex,
            allApps = allApps,
            tabType = TAB_TYPE_WIDGETS
        )
    }

    folderContentsDialogState.item?.let { folder ->
        if (folderContentsDialogState.isVisible) {
            val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
            FolderContentsDialog(
                folderName = folder.name,
                apps = folderApps,
                folderId = folder.id,
                pageIndex = pageIndex,
                allApps = allApps,
                tabType = TAB_TYPE_WIDGETS,
                onDismiss = folderContentsDialogState::dismiss
            )
        }
    }
}
