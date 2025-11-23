package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.ui.extensions.handleShoulderButtons
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherPagerScreen(
    homeViewModel: HomeViewModel,
    widgetViewModel: WidgetViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOverlayShown: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val widgetUiState by widgetViewModel.uiState.collectAsStateWithLifecycle()

    val totalPages = 1 + WidgetViewModel.MAX_WIDGET_PAGES
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { totalPages }
    )

    BackHandler(enabled = !isOverlayShown) {
        if (pagerState.currentPage != 0) {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

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
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isOverlayShown
        ) { page ->
            when (page) {
                0 -> {
                    AppDrawerScreen(
                        apps = homeUiState.allApps,
                        isLoading = homeUiState.isLoading,
                        onSettingsClick = onSettingsClick,
                        totalPages = totalPages,
                        pagerState = pagerState
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
                            totalPages = totalPages,
                            pagerState = pagerState
                        )
                    }
                }
            }
        }
    }
}

