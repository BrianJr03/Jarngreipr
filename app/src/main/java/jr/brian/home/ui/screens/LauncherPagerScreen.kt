package jr.brian.home.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.ui.extensions.handleShoulderButtons
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

@Composable
fun LauncherPagerScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    widgetViewModel: WidgetViewModel = hiltViewModel(),
    powerViewModel: PowerViewModel = hiltViewModel(),
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onShowBottomSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val widgetUiState by widgetViewModel.uiState.collectAsStateWithLifecycle()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val wallpaperManager = LocalWallpaperManager.current
    val currentWallpaper = wallpaperManager.currentWallpaper

    var showResizeScreen by remember { mutableStateOf(false) }
    var resizeWidgetInfo by remember { mutableStateOf<jr.brian.home.model.WidgetInfo?>(null) }
    var resizePageIndex by remember { mutableStateOf(0) }

    val prefs = remember {
        context.getSharedPreferences("gaming_launcher_prefs", Context.MODE_PRIVATE)
    }
    var keyboardVisible by remember {
        mutableStateOf(prefs.getBoolean("keyboard_visible", false))
    }

    LaunchedEffect(keyboardVisible) {
        prefs.edit {
            putBoolean("keyboard_visible", keyboardVisible)
        }
    }

    val totalPages = 1 + WidgetViewModel.MAX_WIDGET_PAGES

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { totalPages }
    )

    BackHandler(enabled = !isPoweredOff) {
        if (pagerState.currentPage > 0) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        } else {
            keyboardVisible = !keyboardVisible
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
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    when (page) {
                        0 -> {
                            AppDrawerScreen(
                                apps = homeUiState.allApps,
                                appsUnfiltered = homeUiState.allAppsUnfiltered,
                                isLoading = homeUiState.isLoading,
                                onSettingsClick = onSettingsClick,
                                powerViewModel = powerViewModel,
                                totalPages = totalPages,
                                pagerState = pagerState,
                                keyboardVisible = keyboardVisible,
                                onShowBottomSheet = onShowBottomSheet
                            )
                        }

                        else -> {
                            val widgetPageIndex = page - 1
                            val widgetPage = widgetUiState.widgetPages.getOrNull(widgetPageIndex)

                            if (widgetPage != null) {
                                WidgetPageScreen(
                                    pageIndex = widgetPageIndex,
                                    widgets = widgetPage.widgets,
                                    viewModel = widgetViewModel,
                                    powerViewModel = powerViewModel,
                                    allApps = homeUiState.allAppsUnfiltered,
                                    totalPages = totalPages,
                                    pagerState = pagerState,
                                    onSettingsClick = onSettingsClick,
                                    onNavigateToResize = { widgetInfo, pageIdx ->
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
}


