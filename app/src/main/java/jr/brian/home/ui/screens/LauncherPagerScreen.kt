package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.HomeTabManager
import jr.brian.home.data.PageCountManager
import jr.brian.home.data.PageType
import jr.brian.home.data.PageTypeManager
import jr.brian.home.ui.components.wallpaper.WallpaperDisplay
import jr.brian.home.ui.extensions.handleShoulderButtons
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

@Composable
fun LauncherPagerScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel(),
    widgetViewModel: WidgetViewModel = hiltViewModel(),
    powerViewModel: PowerViewModel = hiltViewModel(),
    initialPage: Int = 0,
    onSettingsClick: () -> Unit,
    onShowBottomSheet: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val widgetUiState by widgetViewModel.uiState.collectAsStateWithLifecycle()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val wallpaperManager = LocalWallpaperManager.current
    val currentWallpaper = wallpaperManager.currentWallpaper
    val pageCountManager = LocalPageCountManager.current
    val homeTabManager = LocalHomeTabManager.current
    val pageTypeManager = LocalPageTypeManager.current
    val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()
    val appVisibilityManager = LocalAppVisibilityManager.current

    var showResizeScreen by remember { mutableStateOf(false) }
    var resizeWidgetInfo by remember { mutableStateOf<jr.brian.home.model.WidgetInfo?>(null) }
    var resizePageIndex by remember { mutableStateOf(0) }

    val totalPages = pageTypes.size

    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceAtMost(totalPages - 1),
        pageCount = { totalPages }
    )

    LaunchedEffect(totalPages, pageTypes) {
        if (pagerState.currentPage >= totalPages && totalPages > 0) {
            pagerState.scrollToPage((totalPages - 1).coerceAtLeast(0))
        }
        if (totalPages == 0) {
            homeTabManager.setHomeTabIndex(0)
        }
    }

    BackHandler(enabled = !isPoweredOff) {
        if (pagerState.currentPage > 0) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    if (showResizeScreen && resizeWidgetInfo != null) {
        WidgetResizeScreen(
            widgetInfo = resizeWidgetInfo!!,
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
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val pageType =
                    if (page < pageTypes.size) pageTypes[page] else PageType.APPS_TAB

                when (pageType) {
                    PageType.APPS_TAB -> {
                        val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
                        val hiddenApps = hiddenAppsByPage[page] ?: emptySet()
                        val visibleApps = remember(homeUiState.allApps, hiddenApps) {
                            homeUiState.allApps.filter { app ->
                                app.packageName !in hiddenApps
                            }
                        }

                            // Get widgets for this specific AppsTab page
                            val appsTabWidgetPageIndex = getAppsTabWidgetPageIndex(page, pageTypes)
                            val appsTabWidgetPage =
                                widgetUiState.widgetPages.getOrNull(appsTabWidgetPageIndex)
                            val appsTabWidgets = appsTabWidgetPage?.widgets ?: emptyList()

           
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
                            onNavigateToSearch = onNavigateToSearch
                        )
                    }

                    PageType.APPS_AND_WIDGETS_TAB -> {
                        val widgetPageIndex = getAppAndWidgetTabIndex(page, pageTypes)
                        val widgetPage = widgetUiState.widgetPages.getOrNull(widgetPageIndex)

                        if (widgetPage != null) {
                            AppsAndWidgetsTab(
                                pageIndex = widgetPageIndex,
                                widgets = widgetPage.widgets,
                                viewModel = widgetViewModel,
                                powerViewModel = powerViewModel,
                                allApps = homeUiState.allAppsUnfiltered,
                                totalPages = totalPages,
                                pagerState = pagerState,
                                onShowBottomSheet = onShowBottomSheet,
                                pageIndex = appsTabWidgetPageIndex,
                                onSettingsClick = onSettingsClick,
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
                                                widgetViewModel.deletePage(widgetPageIndex)
                                            }
                                        )
                                    }
                                },
                                pageIndicatorBorderColor = ThemePrimaryColor,
                                allApps = homeUiState.allAppsUnfiltered,
                                onNavigateToSearch = onNavigateToSearch,
                                widgets = appsTabWidgets,
                                widgetViewModel = widgetViewModel,
                                onNavigateToWidgetResize = { widgetInfo, pageIdx ->
                                    resizeWidgetInfo = widgetInfo
                                    resizePageIndex = pageIdx
                                    showResizeScreen = true
                                }
                            )
                        }
                    }
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
        if (pageTypes.getOrNull(i) == PageType.APPS_AND_WIDGETS_TAB) {
            widgetPageCount++
        }
    }
    return widgetPageCount
}

private fun getAppsTabWidgetPageIndex(
    currentPageIndex: Int,
    pageTypes: List<PageType>
): Int {
    // Each AppsTab and AppsAndWidgetsTab gets its own widget page index
    // Count all widget-supporting pages (both types) from the beginning up to current page
    var widgetPageIndex = 0
    for (i in 0..currentPageIndex) {
        when (pageTypes.getOrNull(i)) {
            PageType.APPS_TAB, PageType.APPS_AND_WIDGETS_TAB -> {
                if (i == currentPageIndex) {
                    return widgetPageIndex
                }
                widgetPageIndex++
            }

            else -> {}
        }
    }
    return widgetPageIndex
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

    // Delete widget page for both APPS_TAB and APPS_AND_WIDGETS_TAB
    if (pageType == PageType.APPS_TAB || pageType == PageType.APPS_AND_WIDGETS_TAB) {
        val widgetPageIndex = getAppsTabWidgetPageIndex(
            pagerPageIndex,
            pageTypes
        )
        onDeleteWidgetPage(widgetPageIndex)
    }

    pageTypeManager.removePage(pagerPageIndex)
    pageCountManager.removePage()

    // If the deleted tab was the current home tab, set the first tab as home
    if (pagerPageIndex == currentHomeTabIndex) {
        homeTabManager.setHomeTabIndex(0)
    } else if (pagerPageIndex < currentHomeTabIndex) {
        // If a tab before the home tab was deleted, adjust the home tab index
        homeTabManager.setHomeTabIndex(currentHomeTabIndex - 1)
    }

    val newTotalPages = totalPages - 1
    if (newTotalPages > 0 && pagerState.currentPage >= newTotalPages) {
        pagerState.animateScrollToPage(
            (newTotalPages - 1).coerceAtLeast(0)
        )
    }
}


