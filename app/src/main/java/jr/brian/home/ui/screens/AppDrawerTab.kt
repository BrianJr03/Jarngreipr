package jr.brian.home.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
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
import kotlinx.coroutines.flow.collectLatest
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
    val verticalPagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 1 }
    )

    val animationScope = rememberCoroutineScope()

    BackHandler(
        enabled = verticalPagerState.currentPage == 1
    ) {
        animationScope.launch {
            verticalPagerState.animateScrollToPage(0)
        }
    }

    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    val density = LocalDensity.current
    val anchors = remember(density) {
        val offset = with(density) {
            200.dp.toPx()
        }
        DraggableAnchors {
            SheetValue.Hidden at 0f
            SheetValue.Expanded at offset
        }
    }

    val state = remember {
        AnchoredDraggableState(
            initialValue = SheetValue.Hidden,
            anchors = anchors
        )
    }

    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(state) {
        snapshotFlow { state.currentValue }
            .collectLatest { value ->
                Log.i("Dragging", "value = $value")

                if (value == SheetValue.Expanded) {
                    bottomSheetState.expand()
                }
            }
    }

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }
            .collectLatest { value ->
                if (value == SheetValue.Hidden) {
                    state.anchoredDrag(
                        targetValue = SheetValue.Hidden,
                        dragPriority = MutatePriority.Default,
                        block = { _, _ -> }
                    )
                }
            }
    }

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()

    BottomSheetScaffold(
        modifier = modifier.onSizeChanged { size ->
            state.updateAnchors(
                DraggableAnchors {
                    SheetValue.Hidden at 0f
                    SheetValue.Expanded at (size.height * 0.5f )
                }
            )
        },
        containerColor = Color.Transparent,
        sheetContainerColor = OledBackgroundColor,
        scaffoldState = scaffoldState,
        sheetDragHandle = {
            Box {}
        },
        sheetContent = {
            val gridSettingsManager = LocalGridSettingsManager.current
            val rows = gridSettingsManager.rowCount
            val unlimitedMode = gridSettingsManager.unlimitedMode
            val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows

            val filteredApps = remember(apps, maxAppsPerPage) {
                apps.sortedBy { it.label.uppercase() }
            }
            AppsModalContent(
                apps = filteredApps,
                appsUnfiltered = appsUnfiltered,
                isLoading = isLoading,
                allApps = allApps,
                pageIndex = pageIndex,
                isHeaderVisible = isHeaderVisible,
            )
        }
    ) {
        EmptyPage(
            modifier = Modifier
                .fillMaxSize()
                .anchoredDraggable(
                    state = state,
                    reverseDirection = true,
                    orientation = Orientation.Vertical,
                    interactionSource = interactionSource,
                    flingBehavior = AnchoredDraggableDefaults.flingBehavior(
                        state = state,
                        positionalThreshold = { distance: Float -> distance * 0.5f },
                        animationSpec = tween()
                    ),
                ),
            powerViewModel = powerViewModel,
            totalPages = totalPages,
            onShowBottomSheet = onShowBottomSheet,
            onSettingsClick = onSettingsClick,
            onDeletePage = onDeletePage,
            pageIndicatorBorderColor = pageIndicatorBorderColor,
            pagerState = pagerState,
            onNavigateToSearch = onNavigateToSearch,
            isHeaderVisible = isHeaderVisible,
            onNavigateToRecentApps = onNavigateToRecentApps
        )
    }
}


@Composable
private fun EmptyPage(
    modifier: Modifier = Modifier,
    powerViewModel: PowerViewModel = hiltViewModel(),
    totalPages: Int = 1,
    onShowBottomSheet: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    pagerState: PagerState? = null,
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
    isHeaderVisible: Boolean,
    onNavigateToRecentApps: () -> Unit
) {

    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showHomeTabDialog by remember { mutableStateOf(false) }
    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }

    BackHandler(enabled = isPoweredOff) {}

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .then(
                if (showDrawerOptionsDialog ||
                    showHomeTabDialog
                ) {
                    Modifier.blockAllNavigation()
                } else {
                    Modifier.blockHorizontalNavigation()
                }
            )
    ) {
        if (pagerState != null) {
            val settingsIconFocusRequester = remember { FocusRequester() }
            val menuIconFocusRequester = remember { FocusRequester() }

            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                ScreenHeaderRow(
                    totalPages = totalPages,
                    pagerState = pagerState,
                    leadingIcon = Icons.Default.Settings,
                    leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                    onLeadingIconClick = onSettingsClick,
                    leadingIconFocusRequester = settingsIconFocusRequester,
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

        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
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
            onMenuClick = null,
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = null,
            onRecentAppsClick = onNavigateToRecentApps
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
