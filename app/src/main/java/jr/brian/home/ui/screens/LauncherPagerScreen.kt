package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import jr.brian.home.data.FabPosition
import jr.brian.home.data.HomeTabManager
import jr.brian.home.data.PageCountManager
import jr.brian.home.data.PageTypeManager
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.viewmodels.ESDEViewModel
import jr.brian.home.model.PageType
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.ui.components.wallpaper.WallpaperDisplay
import jr.brian.home.ui.extensions.handleShoulderButtons
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppDrawerFabManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalGlobalIconRefreshManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.viewmodels.MainViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun LauncherPagerScreen(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel = hiltViewModel(),
    widgetViewModel: WidgetViewModel = hiltViewModel(),
    powerViewModel: PowerViewModel = hiltViewModel(),
    esdeViewModel: ESDEViewModel = hiltViewModel(),
    navController: NavHostController? = null,
    initialPage: Int = 0,
    onSettingsClick: () -> Unit,
    onShowBottomSheet: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onBackButtonShortcut: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {},
    onNavigateToSystemApps: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {},
    onPagerScrollProgressChanged: (Float) -> Unit = {},
    onCurrentPageChanged: (Int) -> Unit = {},
    onDockPositioned: (Float) -> Unit = {},
    hideLauncherUI: Boolean = false
) {
    val scope = rememberCoroutineScope()

    val homeTabManager = LocalHomeTabManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val pageTypeManager = LocalPageTypeManager.current
    val pageCountManager = LocalPageCountManager.current
    val wallpaperManager = LocalWallpaperManager.current
    val currentWallpaper = wallpaperManager.currentWallpaper
    val appDrawerFabManager = LocalAppDrawerFabManager.current
    val esdePrefsManager = LocalESDEPreferencesManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val powerSettingsManager = LocalPowerSettingsManager.current
    val globalIconRefreshManager = LocalGlobalIconRefreshManager.current

    val homeUiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    val widgetUiState by widgetViewModel.uiState.collectAsStateWithLifecycle()
    val fabColor by appDrawerFabManager.fabColor.collectAsStateWithLifecycle()
    val fabPosition by appDrawerFabManager.fabPosition.collectAsStateWithLifecycle()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
    val isBackButtonShortcutEnabled by powerSettingsManager.backButtonShortcutEnabled.collectAsStateWithLifecycle()

    var resizePageIndex by remember { mutableIntStateOf(0) }
    var showResizeScreen by remember { mutableStateOf(false) }
    var showAppDrawerSheet by remember { mutableStateOf(false) }
    var isAppDrawerGameInProgress by remember { mutableStateOf(false) }
    var currentTabIsScrolling by remember { mutableStateOf(false) }
    var resizeWidgetInfo by remember { mutableStateOf<WidgetInfo?>(null) }
    var currentTabHasScrollableContent by remember { mutableStateOf(false) }
    val appDrawerSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            !(isAppDrawerGameInProgress && targetValue == SheetValue.Hidden)
        }
    )
    val closeAppDrawerSheet: () -> Unit = {
        isAppDrawerGameInProgress = false
        showAppDrawerSheet = false
        scope.launch { runCatching { appDrawerSheetState.hide() } }
    }
    // Safety-net: whenever the sheet actually reaches Hidden (by any means),
    // guarantee the compose tree removes it and resets game state.
    LaunchedEffect(appDrawerSheetState) {
        var wasSheetExpanded = false
        snapshotFlow { appDrawerSheetState.currentValue }
            .collect { value ->
                if (value != SheetValue.Hidden) {
                    wasSheetExpanded = true
                }
                if (value == SheetValue.Hidden && wasSheetExpanded) {
                    isAppDrawerGameInProgress = false
                    showAppDrawerSheet = false
                }
            }
    }

    val totalPages = pageTypes.size

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceAtMost(totalPages - 1),
        pageCount = { totalPages }
    )

    val isAnyPageInEditMode = widgetUiState.editModeByPage.values.any { it }
    val shouldBlockBackButtonShortcut = showResizeScreen || isAnyPageInEditMode

    LaunchedEffect(totalPages, pageTypes) {
        if (pagerState.currentPage >= totalPages && totalPages > 0) {
            pagerState.scrollToPage((totalPages - 1).coerceAtLeast(0))
        }
        if (totalPages == 0) {
            homeTabManager.setHomeTabIndex(0)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow {
            abs(pagerState.currentPageOffsetFraction)
        }.collect { offsetFraction ->
            onPagerScrollProgressChanged(offsetFraction)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCurrentPageChanged(page)
            }
    }

    BackHandler(enabled = showResizeScreen) {
        showResizeScreen = false
        resizeWidgetInfo = null
    }

    BackHandler(enabled = !showResizeScreen && isAnyPageInEditMode) {
        widgetUiState.editModeByPage.forEach { (pageIndex, isEnabled) ->
            if (isEnabled) {
                widgetViewModel.toggleEditMode(pageIndex)
            }
        }
    }

    BackHandler(
        enabled = !isPoweredOff
                && !isBackButtonShortcutEnabled
                && !shouldBlockBackButtonShortcut
    ) {
        if (pagerState.currentPage > 0) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    BackHandler(
        enabled = !isPoweredOff
                && isBackButtonShortcutEnabled
                && !shouldBlockBackButtonShortcut
    ) {
        onBackButtonShortcut()
    }
    
    // While the game is active in the app drawer, consume system back/gesture.
    BackHandler(enabled = showAppDrawerSheet && isAppDrawerGameInProgress) {}

    val widgetToResize = resizeWidgetInfo
    if (showResizeScreen && widgetToResize != null) {
        WidgetResizeScreen(
            widgetInfo = widgetToResize,
            pageIndex = resizePageIndex,
            viewModel = widgetViewModel,
            onNavigateBack = {
                showResizeScreen = false
                resizeWidgetInfo = null
            }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .handleShoulderButtons(
                    onLeftShoulder = {
                        if (pagerState.currentPage > 0) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    onRightShoulder = {
                        if (pagerState.currentPage < totalPages - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
        ) {
            key(currentWallpaper) {
                WallpaperDisplay(
                    wallpaperUri = wallpaperManager.getWallpaperUri(),
                    wallpaperType = wallpaperManager.getWallpaperType(),
                    modifier = Modifier.fillMaxSize()
                )
            }

            val pagerVisible = !hideLauncherUI && !isPoweredOff
            val pagerAlpha by animateFloatAsState(
                targetValue = if (pagerVisible) 1f else 0f,
                label = "pagerAlpha"
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = pagerAlpha },
                beyondViewportPageCount = 1,
                userScrollEnabled = pagerVisible,
            ) { page ->
                    val pageType =
                        if (page < pageTypes.size) pageTypes[page] else PageType.APPS_TAB

                    when (pageType) {
                        PageType.APPS_TAB -> {
                            val hiddenApps = hiddenAppsByPage[page] ?: emptySet()
                            val visibleApps = remember(homeUiState.allApps, hiddenApps) {
                                homeUiState.allApps.filter { app ->
                                    app.packageName !in hiddenApps
                                }
                            }

                            key(globalIconRefreshManager?.refreshCounter) {
                                AppsTab(
                                    apps = visibleApps,
                                    appsUnfiltered = homeUiState.allAppsUnfiltered,
                                    isLoading = homeUiState.isLoading,
                                    onSettingsClick = onSettingsClick,
                                    powerViewModel = powerViewModel,
                                    totalPages = totalPages,
                                    pagerState = pagerState,
                                    onShowBottomSheet = onShowBottomSheet,
                                    pageIndex = page,
                                    onDockPositioned = onDockPositioned,
                                    onDeletePage = { pagerPageIndex ->
                                        scope.launch {
                                            deleteTab(
                                                totalPages = totalPages,
                                                pagerPageIndex = pagerPageIndex,
                                                pagerState = pagerState,
                                                pageTypeManager = pageTypeManager,
                                                pageCountManager = pageCountManager,
                                                homeTabManager = homeTabManager,
                                                onDeleteWidgetPage = { widgetPageIndex ->
                                                    widgetViewModel.deletePage(widgetPageIndex)
                                                }
                                            )
                                        }
                                    },
                                    pageIndicatorBorderColor = ThemePrimaryColor,
                                    allApps = homeUiState.allAppsUnfiltered,
                                    onNavigateToSearch = onNavigateToSearch,
                                    onNavigateToDockSettings = onNavigateToDockSettings,
                                    onNavigateToSystemApps = onNavigateToSystemApps,
                                    onNavigateToRomSearch = onNavigateToRomSearch,
                                    onShowAppDrawer = { showAppDrawerSheet = true },
                                    onScrollStateChanged = { isScrolling, hasScrollableContent ->
                                        currentTabIsScrolling = isScrolling
                                        currentTabHasScrollableContent = hasScrollableContent
                                    }
                                )
                            }
                        }

                        PageType.APPS_AND_WIDGETS_TAB -> {
                            val widgetPageIndex = getAppAndWidgetTabIndex(page, pageTypes)
                            val widgetPage = widgetUiState.widgetPages.getOrNull(widgetPageIndex)

                            if (widgetPage != null) {
                                key(globalIconRefreshManager?.refreshCounter) {
                                    AppsAndWidgetsTab(
                                        pageIndex = widgetPageIndex,
                                        widgets = widgetPage.widgets,
                                        viewModel = widgetViewModel,
                                        powerViewModel = powerViewModel,
                                        allApps = homeUiState.allAppsUnfiltered,
                                        totalPages = totalPages,
                                        pagerState = pagerState,
                                        onShowBottomSheet = onShowBottomSheet,
                                        onSettingsClick = onSettingsClick,
                                        onDockPositioned = onDockPositioned,
                                        onNavigateToResize = { widgetInfo, pageIdx ->
                                            resizeWidgetInfo = widgetInfo
                                            resizePageIndex = pageIdx
                                            showResizeScreen = true
                                        },
                                        onDeletePage = { pagerPageIndex ->
                                            scope.launch {
                                                deleteTab(
                                                    totalPages = totalPages,
                                                    pagerPageIndex = pagerPageIndex,
                                                    pagerState = pagerState,
                                                    pageTypeManager = pageTypeManager,
                                                    pageCountManager = pageCountManager,
                                                    homeTabManager = homeTabManager,
                                                    onDeleteWidgetPage = { widgetPageIndex ->
                                                        widgetViewModel.deletePage(
                                                            widgetPageIndex
                                                        )
                                                    }
                                                )
                                            }
                                        },
                                        pageIndicatorBorderColor = ThemeSecondaryColor,
                                        onNavigateToSearch = onNavigateToSearch,
                                        onNavigateToDockSettings = onNavigateToDockSettings,
                                        onNavigateToSystemApps = onNavigateToSystemApps,
                                        onNavigateToRomSearch = onNavigateToRomSearch,
                                        navController = navController,
                                        onShowAppDrawer = { showAppDrawerSheet = true },
                                        onScrollStateChanged = { isScrolling, hasScrollableContent ->
                                            currentTabIsScrolling = isScrolling
                                            currentTabHasScrollableContent = hasScrollableContent
                                        }
                                    )
                                }
                            }
                        }

                        PageType.APP_DRAWER_TAB -> {
                            val widgetPageIndex = getAppAndWidgetTabIndex(page, pageTypes)
                            val widgetPage = widgetUiState.widgetPages.getOrNull(widgetPageIndex)


                            if (widgetPage != null) {
                                val hiddenApps = hiddenAppsByPage[page] ?: emptySet()
                                val visibleApps = remember(homeUiState.allApps, hiddenApps) {
                                    homeUiState.allApps.filter { app ->
                                        app.packageName !in hiddenApps
                                    }
                                }

                                key(globalIconRefreshManager?.refreshCounter) {
                                    AppDrawerTab(
                                        powerViewModel = powerViewModel,
                                        totalPages = totalPages,
                                        onShowBottomSheet = onShowBottomSheet,
                                        onSettingsClick = onSettingsClick,
                                        apps = visibleApps,
                                        appsUnfiltered = homeUiState.allAppsUnfiltered,
                                        allApps = homeUiState.allAppsUnfiltered,
                                        onDockPositioned = onDockPositioned,
                                        onDeletePage = { pagerPageIndex ->
                                            scope.launch {
                                                deleteTab(
                                                    totalPages = totalPages,
                                                    pagerPageIndex = pagerPageIndex,
                                                    pagerState = pagerState,
                                                    pageTypeManager = pageTypeManager,
                                                    pageCountManager = pageCountManager,
                                                    homeTabManager = homeTabManager,
                                                    onDeleteWidgetPage = { widgetPageIndex ->
                                                        widgetViewModel.deletePage(
                                                            widgetPageIndex
                                                        )
                                                    }
                                                )
                                            }
                                        },
                                        pageIndicatorBorderColor = ThemeSecondaryColor,
                                        pagerState = pagerState,
                                        isLoading = homeUiState.isLoading,
                                        pageIndex = page,
                                        onNavigateToDockSettings = onNavigateToDockSettings,
                                        onNavigateToSystemApps = onNavigateToSystemApps,
                                        onNavigateToRomSearch = onNavigateToRomSearch,
                                    )
                                }
                            }
                        }
                    }
                }

            AnimatedVisibility(
                visible = isPoweredOff,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PoweredOffScreen(
                    onPowerOn = {
                        powerViewModel.powerOn()
                    },
                    musicVolume = esdePrefsState.musicVolume,
                    onMusicVolumeChange = { volume ->
                        esdePrefsManager.setMusicVolume(volume)
                        esdeViewModel.musicController.setVolume(volume / 100f)
                    }
                )
            }

            val isFabVisibleOnCurrentPage =
                appDrawerFabManager.isFabVisibleOnPage(pagerState.currentPage)
            AnimatedVisibility(
                visible = !hideLauncherUI && !isPoweredOff && !showAppDrawerSheet
                        && isFabVisibleOnCurrentPage /*&& currentTabHasScrollableContent && !currentTabIsScrolling*/,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(if (fabPosition == FabPosition.LEFT) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(
                        start = if (fabPosition == FabPosition.LEFT) 16.dp else 0.dp,
                        end = if (fabPosition == FabPosition.RIGHT) 16.dp else 0.dp,
                        bottom = 16.dp
                    )
            ) {
                SmallFloatingActionButton(
                    onClick = { showAppDrawerSheet = true },
                    shape = CircleShape,
                    containerColor = fabColor.copy(alpha = 0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = "Open App Drawer",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (showAppDrawerSheet) {
                val currentPage = pagerState.currentPage
                ModalBottomSheet(
                    onDismissRequest = { closeAppDrawerSheet() },
                    sheetState = appDrawerSheetState,
                    properties = ModalBottomSheetProperties(
                        shouldDismissOnBackPress = !isAppDrawerGameInProgress
                    ),
                    containerColor = OledBackgroundColor.copy(alpha = esdePrefsState.appDrawerOpacityFloat),
                    dragHandle = {
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
                    }
                ) {
                    AppsModalContent(
                        modifier = Modifier.fillMaxSize(),
                        apps = homeUiState.allApps,
                        appsUnfiltered = homeUiState.allAppsUnfiltered,
                        isLoading = homeUiState.isLoading,
                        showHideAppButton = false,
                        allApps = homeUiState.allAppsUnfiltered,
                        pageIndex = currentPage,
                        isHeaderVisible = isHeaderVisible,
                        onGameInProgressChanged = { isAppDrawerGameInProgress = it },
                        onCloseRequested = closeAppDrawerSheet,
                        onAppOpened = closeAppDrawerSheet
                    )
                }
            }
        }
    }
}

private fun getAppAndWidgetTabIndex(
    currentPageIndex: Int,
    pageTypes: List<PageType>
): Int {
    var widgetPageCount = 0
    for (i in 0 until currentPageIndex) {
        if (pageTypes.getOrNull(i) != PageType.APPS_TAB) {
            widgetPageCount++
        }
    }
    return widgetPageCount
}

private suspend fun deleteTab(
    totalPages: Int,
    pagerPageIndex: Int,
    pagerState: PagerState,
    pageTypeManager: PageTypeManager,
    pageCountManager: PageCountManager,
    homeTabManager: HomeTabManager,
    onDeleteWidgetPage: (Int) -> Unit
) {
    val pageTypes = pageTypeManager.pageTypes.value
    val pageType = pageTypes.getOrNull(pagerPageIndex)
    val currentHomeTabIndex = homeTabManager.homeTabIndex.value

    if (pageType != PageType.APPS_TAB) {
        val widgetPageIndex = getAppAndWidgetTabIndex(
            pagerPageIndex,
            pageTypes
        )
        onDeleteWidgetPage(widgetPageIndex)
    }

    pageTypeManager.removePage(pagerPageIndex)
    pageCountManager.removePage()

    if (pagerPageIndex == currentHomeTabIndex) {
        homeTabManager.setHomeTabIndex(0)
    } else if (pagerPageIndex < currentHomeTabIndex) {
        homeTabManager.setHomeTabIndex(currentHomeTabIndex - 1)
    }

    val newTotalPages = totalPages - 1
    if (newTotalPages > 0 && pagerState.currentPage >= newTotalPages) {
        pagerState.animateScrollToPage(
            (newTotalPages - 1).coerceAtLeast(0)
        )
    }
}
