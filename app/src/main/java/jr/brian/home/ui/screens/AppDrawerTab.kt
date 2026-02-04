package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.dock.AppDock
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.DockAppSelectionDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.esde.ui.ESDESetupScreen
import jr.brian.home.esde.setup.SetupStep
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalDockManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberFocusRequesterMap
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.viewmodels.PowerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun AppDrawerTab(
    pageIndex: Int,
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    totalPages: Int = 1,
    isLoading: Boolean = false,
    pagerState: PagerState? = null,
    allApps: List<AppInfo> = emptyList(),
    powerViewModel: PowerViewModel = hiltViewModel(),
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
    onShowBottomSheet: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val dockManager = LocalDockManager.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val rows = gridSettingsManager.rowCount
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val unlimitedMode = gridSettingsManager.unlimitedMode
    val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows
    val showAppDrawer = remember { mutableStateOf(false) }
    val homeTabDialogState = rememberDialogState<Unit>()
    val drawerOptionsDialogState = rememberDialogState<Unit>()
    val dockAppSelectionDialogState = rememberDialogState<Int>()

    val filteredApps = remember(apps, maxAppsPerPage) {
        apps.sortedBy { it.label.uppercase() }
    }

    val animationScope = rememberCoroutineScope()

    BackHandler(enabled = isPoweredOff) {}

    val viewHeight = remember { mutableIntStateOf(0) }
    val scrollState = rememberLazyListState()
    val snappingLayout = remember(scrollState) { SnapLayoutInfoProvider(scrollState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)
    val lastVisibleIndex = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
    val lastOffset = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.offset ?: 0
    val settingsIconFocusRequester = remember { FocusRequester() }
    val menuIconFocusRequester = remember { FocusRequester() }
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val appFocusRequesters = rememberFocusRequesterMap()
    val appDrawerOptionsDialogState = rememberDialogState<Unit>()
    val appVisibilityDialogState = rememberDialogState<Unit>()
    val esdeSetupDialogState = rememberDialogState<SetupStep>()
    val wallpaperManager = LocalWallpaperManager.current

    val isDockVisible by dockManager.isDockVisible.collectAsStateWithLifecycle()
    val isDockVisibleOnPage = dockManager.isDockVisibleOnPage(pageIndex)

    val esdePrefsManager = LocalESDEPreferencesManager.current
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    val drawerOpacity = esdePrefsState.appDrawerOpacityFloat

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            flingBehavior = flingBehavior,
            modifier = modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    viewHeight.intValue = size.height

                    if (!showAppDrawer.value) {
                        animationScope.launch {
                            delay(300)
                            scrollState.scrollToItem(0)
                            showAppDrawer.value = true
                        }
                    }
                },
        ) {
            stickyHeader {
                if (pagerState != null) {
                    AnimatedVisibility(
                        visible = isHeaderVisible,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier.statusBarsPadding()
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
                            onTrailingIconClick = {
                                appDrawerOptionsDialogState.show()
                            },
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
            }
            items(2) { index ->
                DrawerItem(
                    index = index,
                    viewHeight = viewHeight.intValue,
                    lastVisibleIndex = lastVisibleIndex,
                    lastOffset = lastOffset,
                    showAppDrawer = showAppDrawer.value,
                    showDrawerOptionsDialog = drawerOptionsDialogState.isVisible,
                    showHomeTabDialog = homeTabDialogState.isVisible,
                    drawerOpacity = drawerOpacity,
                    onDoubleTap = { powerViewModel.togglePower() },
                    onLongPress = { drawerOptionsDialogState.show() },
                    apps = filteredApps,
                    appsUnfiltered = appsUnfiltered,
                    isLoading = isLoading,
                    allApps = allApps,
                    pageIndex = pageIndex,
                    isHeaderVisible = isHeaderVisible,
                    onAppOpened = {
                        animationScope.launch {
                            scrollState.scrollToItem(0)
                        }
                    }
                )
            }
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
                    appDrawerOptionsDialogState.show()
                },
                onSettingsClick = onSettingsClick,
                onQuickDeleteClick = onShowBottomSheet,
                onCreateFolderClick = null,
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

        if (appDrawerOptionsDialogState.isVisible) {
            AppsTabOptionsDialog(
                onDismiss = appDrawerOptionsDialogState::dismiss,
                onShowAppVisibility = { appVisibilityDialogState.show() },
                onResetPositions = {},
                isDragLocked = true,
                onToggleDragLock = { },
                title = stringResource(R.string.app_drawer_tab_options_title)
            )
        }

        if (appVisibilityDialogState.isVisible) {
            AppVisibilityDialog(
                apps = appsUnfiltered,
                onDismiss = appVisibilityDialogState::dismiss,
                pageIndex = pageIndex
            )
        }

        if (homeTabDialogState.isVisible) {
            val homeTabManager = LocalHomeTabManager.current
            val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
            val pageCountManager = LocalPageCountManager.current
            val pageTypeManager = LocalPageTypeManager.current
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

        AnimatedVisibility(
            visible = isDockVisible && isDockVisibleOnPage && 
                     scrollState.firstVisibleItemIndex == 0 && 
                     scrollState.firstVisibleItemScrollOffset == 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppDock(
                apps = appsUnfiltered,
                onAppClick = { app ->
                    val displayPreference =
                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
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
            val availableApps = appsUnfiltered.filter { app ->
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
}

@Composable
private fun DrawerItem(
    index: Int,
    viewHeight: Int,
    lastVisibleIndex: Int,
    lastOffset: Int,
    showAppDrawer: Boolean,
    showDrawerOptionsDialog: Boolean,
    showHomeTabDialog: Boolean,
    drawerOpacity: Float,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit,
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean,
    allApps: List<AppInfo>,
    pageIndex: Int,
    isHeaderVisible: Boolean,
    onAppOpened: () -> Unit
) {
    if (viewHeight > 0) {
        val height = with(LocalDensity.current) {
            viewHeight.toDp()
        }
        val alpha = if (lastVisibleIndex == 0) {
            0f
        } else {
            (1f * ((viewHeight - lastOffset).toFloat() / viewHeight.toFloat()))
                .coerceIn(0f, 1f)
        }

        if (index == 0) {
            DrawerTouchArea(
                height = height,
                showDrawerOptionsDialog = showDrawerOptionsDialog,
                showHomeTabDialog = showHomeTabDialog,
                onDoubleTap = onDoubleTap,
                onLongPress = onLongPress
            )
        } else {
            DrawerContent(
                height = height,
                alpha = alpha,
                drawerOpacity = drawerOpacity,
                showAppDrawer = showAppDrawer,
                apps = apps,
                appsUnfiltered = appsUnfiltered,
                isLoading = isLoading,
                allApps = allApps,
                pageIndex = pageIndex,
                isHeaderVisible = isHeaderVisible,
                onAppOpened = onAppOpened
            )
        }
    }
}

@Composable
private fun DrawerTouchArea(
    height: androidx.compose.ui.unit.Dp,
    showDrawerOptionsDialog: Boolean,
    showHomeTabDialog: Boolean,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .requiredHeight(height)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() }
                )
            }
            .then(
                if (showDrawerOptionsDialog || showHomeTabDialog) {
                    Modifier.blockAllNavigation()
                } else {
                    Modifier.blockHorizontalNavigation()
                }
            )
    )
}

@Composable
private fun DrawerContent(
    height: androidx.compose.ui.unit.Dp,
    alpha: Float,
    drawerOpacity: Float,
    showAppDrawer: Boolean,
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean,
    allApps: List<AppInfo>,
    pageIndex: Int,
    isHeaderVisible: Boolean,
    onAppOpened: () -> Unit
) {
    Column(
        Modifier
            .requiredHeight(height)
            .padding(top = 24.dp)
            .background(
                OledBackgroundColor.copy(alpha = drawerOpacity),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .alpha(if (showAppDrawer) alpha else 0f)
            .zIndex(10f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 6.dp)
                    .background(
                        ThemePrimaryColor,
                        shape = RoundedCornerShape(3.dp)
                    )
            )
        }

        AppsModalContent(
            modifier = Modifier.fillMaxSize(),
            apps = apps,
            appsUnfiltered = appsUnfiltered,
            isLoading = isLoading,
            allApps = allApps,
            pageIndex = pageIndex,
            isHeaderVisible = isHeaderVisible,
            onAppOpened = onAppOpened
        )
    }
}