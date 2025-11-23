package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.viewmodels.HomeViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import jr.brian.home.ui.extensions.handleShoulderButtons
import jr.brian.home.ui.theme.ThemePrimaryColor
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
                        onSettingsClick = onSettingsClick
                    )
                }

                else -> {
                    val widgetPageIndex = page - 1
                    val widgetPage = widgetUiState.widgetPages.getOrNull(widgetPageIndex)

                    if (widgetPage != null) {
                        WidgetPageScreen(
                            pageIndex = widgetPageIndex,
                            widgets = widgetPage.widgets,
                            viewModel = widgetViewModel
                        )
                    }
                }
            }
        }

        if (pagerState.currentPage != 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(bottom = 8.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    val isSelected = pagerState.currentPage == index
                    if (index == 0) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier
                                .size(if (isSelected) 16.dp else 12.dp),
                            tint = if (isSelected) {
                                ThemePrimaryColor
                            } else {
                                Color.White.copy(alpha = 0.4f)
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) {
                                        ThemePrimaryColor
                                    } else {
                                        Color.White.copy(alpha = 0.4f)
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}