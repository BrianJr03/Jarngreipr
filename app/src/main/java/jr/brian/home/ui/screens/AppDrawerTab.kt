package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.viewmodels.PowerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerTab(
    modifier: Modifier = Modifier,
    powerViewModel: PowerViewModel = hiltViewModel(),
    totalPages: Int = 1,
    onShowBottomSheet: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    pagerState: PagerState? = null,
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    allApps: List<AppInfo> = emptyList(),
    columns: Int = 4,
    isLoading: Boolean = false,
    pageIndex: Int,
    onNavigateToRecentApps: () -> Unit = {}
) {
    val powerSettingsManager = LocalPowerSettingsManager.current
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()
    val gridSettingsManager = LocalGridSettingsManager.current
    val rows = gridSettingsManager.rowCount
    val unlimitedMode = gridSettingsManager.unlimitedMode
    val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows
    val showAppDrawer = remember { mutableStateOf(false) }
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showHomeTabDialog by remember { mutableStateOf(false) }
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

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
    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var showAppDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }

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
                            showAppDrawerOptionsDialog = true
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
                if (index == 0) {
                    Box(
                        modifier = Modifier
                            .requiredHeight(height)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        powerViewModel.togglePower()
                                    },
                                    onLongPress = {
                                        showDrawerOptionsDialog = true
                                    }
                                )
                            }
                            .then(
                                if (showDrawerOptionsDialog ||
                                    showHomeTabDialog
                                ) {
                                    Modifier.blockAllNavigation()
                                } else {
                                    Modifier.blockHorizontalNavigation()
                                }
                            ),
                    )
                } else {
                    Column(
                        Modifier
                            .requiredHeight(height)
                            .padding(top = 24.dp)
                            .background(
                                OledBackgroundColor, shape = RoundedCornerShape(
                                    topStart = 20.dp,
                                    topEnd = 20.dp
                                )
                            )
                            .alpha(if (showAppDrawer.value) alpha else 0f)
                            .zIndex(10f),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = 36.dp, height = 6.dp
                                    )
                                    .background(
                                        ThemePrimaryColor,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                        AppsModalContent(
                            modifier = Modifier.fillMaxSize(),
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
        }

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
                showAppDrawerOptionsDialog = true
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = null,
            onRecentAppsClick = onNavigateToRecentApps
        )
    }

    if (showAppDrawerOptionsDialog) {
        AppsTabOptionsDialog(
            onDismiss = { showAppDrawerOptionsDialog = false },
            onShowAppVisibility = { showAppVisibilityDialog = true },
            onResetPositions = {},
            isDragLocked = true,
            onToggleDragLock = { },
            title = stringResource(R.string.app_drawer_tab_options_title)
        )
    }

    if (showAppVisibilityDialog) {
        AppVisibilityDialog(
            apps = appsUnfiltered,
            onDismiss = { showAppVisibilityDialog = false },
            pageIndex = pageIndex
        )
    }

    if (showHomeTabDialog) {
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
}