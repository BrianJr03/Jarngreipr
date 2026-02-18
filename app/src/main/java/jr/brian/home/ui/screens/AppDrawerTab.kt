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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.SyncLogoPositionLock
import jr.brian.home.esde.setup.SetupStep
import jr.brian.home.esde.ui.ESDESetupScreen
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppOptionsMenu
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.DockAppSelectionDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.dock.AppDock
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.components.settings.displayName
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalDockManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberFocusRequesterMap
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.util.openAppInfo
import jr.brian.home.viewmodels.PowerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val DOCK_ITEM_HEIGHT = 100.dp

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
    onNavigateToDockSettings: () -> Unit = {},
    onDockPositioned: (Float) -> Unit = {}
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
    val dockAppOptionsDialogState = rememberDialogState<AppInfo>()

    val filteredApps = remember(apps, maxAppsPerPage) {
        apps.sortedBy { it.label.uppercase() }
    }

    val animationScope = rememberCoroutineScope()

    BackHandler(enabled = isPoweredOff) {}

    val viewHeight = remember { mutableIntStateOf(0) }
    val headerHeight = remember { mutableIntStateOf(0) }
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

    val isDockEnabled by dockManager.isDockVisible.collectAsStateWithLifecycle()
    val isDockVisibleOnPage = dockManager.isDockVisibleOnPage(pageIndex)


    LaunchedEffect(
        scrollState,
        viewHeight.intValue
    ) {
        if (viewHeight.intValue == 0) return@LaunchedEffect
        val halfHeight = viewHeight.intValue / 2

        snapshotFlow {
            Triple(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset,
                scrollState.isScrollInProgress
            )
        }.collect { (index, offset, isScrolling) ->
            if (!isScrolling) {
                when (index) {
                    0, 1 -> {
                        if (offset > halfHeight) {
                            scrollState.animateScrollToItem(2) // Snap to drawer
                        } else {
                            scrollState.animateScrollToItem(0) // Snap to top (dock visible)
                        }
                    }

                    2 -> {
                        scrollState.animateScrollToItem(2)
                    }
                }
            }
        }
    }

    val esdePrefsManager = LocalESDEPreferencesManager.current
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    SyncLogoPositionLock(esdePrefsState, esdePrefsManager)
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
                        modifier = Modifier
                            .statusBarsPadding()
                            .onSizeChanged { size ->
                                headerHeight.intValue = size.height
                            }
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
            item {
                if (viewHeight.intValue > 0) {
                    val isDockEnabledOnPage = isDockEnabled && isDockVisibleOnPage
                    val effectiveHeaderHeight = if (isHeaderVisible) headerHeight.intValue else 0
                    val touchAreaHeight = with(LocalDensity.current) {
                        if (isDockEnabledOnPage) {
                            (viewHeight.intValue - DOCK_ITEM_HEIGHT.roundToPx() - effectiveHeaderHeight).toDp()
                        } else {
                            (viewHeight.intValue - effectiveHeaderHeight).toDp()
                        }
                    }
                    DrawerTouchArea(
                        height = touchAreaHeight,
                        showDrawerOptionsDialog = drawerOptionsDialogState.isVisible,
                        showHomeTabDialog = homeTabDialogState.isVisible,
                        onDoubleTap = { powerViewModel.togglePower() },
                        onLongPress = { drawerOptionsDialogState.show() }
                    )
                }
            }

            item {
                val isDockEnabledOnPage = isDockEnabled && isDockVisibleOnPage
                if (isDockEnabledOnPage) {
                    Box(modifier = Modifier.requiredHeight(DOCK_ITEM_HEIGHT)) {
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
                                    currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                                        app.packageName
                                    )
                                )
                            },
                            onAppLongClick = { app ->
                                dockAppOptionsDialogState.show(app)
                            },
                            onEmptySlotClick = { position ->
                                dockAppSelectionDialogState.show(position)
                            },
                            onEmptySlotLongClick = { position ->
                                dockManager.removeEmptySlot(position)
                            },
                            onDockPositioned = onDockPositioned
                        )
                    }
                }
            }

            item {
                if (viewHeight.intValue > 0) {
                    val height = with(LocalDensity.current) {
                        viewHeight.intValue.toDp()
                    }
                    val alpha = if (lastVisibleIndex == 0) {
                        0f
                    } else {
                        (1f * ((viewHeight.intValue - lastOffset).toFloat() / viewHeight.intValue.toFloat()))
                            .coerceIn(0f, 1f)
                    }
                    DrawerContent(
                        height = height,
                        alpha = alpha,
                        drawerOpacity = drawerOpacity,
                        showAppDrawer = showAppDrawer.value,
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
                title = stringResource(R.string.app_drawer_tab_options_title),
                isLogoPositionLocked = esdePrefsState.marqueePositionLocked,
                onToggleMarqueePositionLock = { esdePrefsManager.toggleLogoPositionLocked() }
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

    dockAppOptionsDialogState.item?.let { appInfo ->
        if (dockAppOptionsDialogState.isVisible) {
            AppOptionsMenu(
                appLabel = appInfo.displayName(),
                currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                    appInfo.packageName
                ),
                onDismiss = dockAppOptionsDialogState::dismiss,
                onAppInfoClick = {
                    openAppInfo(context, appInfo.packageName)
                },
                onDisplayPreferenceChange = { preference ->
                    appDisplayPreferenceManager.setAppDisplayPreference(
                        appInfo.packageName,
                        preference
                    )
                },
                isInDock = true,
                onRemoveFromDock = {
                    dockManager.removeAppFromDock(appInfo.packageName)
                    dockAppOptionsDialogState.dismiss()
                }
            )
        }
    }
}

@Composable
private fun DrawerTouchArea(
    height: Dp,
    showDrawerOptionsDialog: Boolean,
    showHomeTabDialog: Boolean,
    onDoubleTap: () -> Unit,
    onLongPress: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = 16.dp)
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
    height: Dp,
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