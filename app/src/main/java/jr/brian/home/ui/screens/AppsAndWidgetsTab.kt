package jr.brian.home.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.AppInfo
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.dialog.AppsAndWidgetsOptionsDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.components.widget.AppItem
import jr.brian.home.ui.components.widget.WidgetItem
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

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
    onNavigateToSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val columns = gridSettingsManager.columnCount
    val scope = rememberCoroutineScope()

    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()

    val addWidgetIconFocusRequester = remember { FocusRequester() }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var swapModeEnabled by remember { mutableStateOf(false) }
    var swapSourceWidgetId by remember { mutableStateOf<Int?>(null) }
    var showFolderOptionsDialog by remember { mutableStateOf(false) }
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showHomeTabDialog by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editModeEnabled = uiState.editModeByPage[pageIndex] ?: false

    val visibleApps by widgetPageAppManager.getVisibleApps(pageIndex)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val appsFirst by widgetPageAppManager.getAppsFirstOrder(pageIndex)
        .collectAsStateWithLifecycle(initialValue = false)

    val displayedApps = remember(allApps, visibleApps) {
        allApps.filter { it.packageName in visibleApps }
    }

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()

    val settingsIconFocusRequester = remember { FocusRequester() }

    val gridState = rememberLazyGridState()

    val widgetPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val appWidgetId = data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            -1
        ) ?: -1

        if (appWidgetId != -1) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

            if (appWidgetInfo != null) {
                val cellSize = 70
                val widgetWidth =
                    ceil(appWidgetInfo.minWidth.toFloat() / cellSize).toInt()
                        .coerceAtLeast(1)
                val widgetHeight =
                    ceil(appWidgetInfo.minHeight.toFloat() / cellSize).toInt()
                        .coerceAtLeast(1)

                val widgetInfo = WidgetInfo(
                    widgetId = appWidgetId,
                    providerInfo = appWidgetInfo,
                    pageIndex = pageIndex,
                    width = widgetWidth,
                    height = widgetHeight
                )

                viewModel.addWidgetToPage(widgetInfo, pageIndex)
                scope.launch {
                    gridState.animateScrollToItem(0)
                }
            }
        }
    }

    BackHandler(enabled = isPoweredOff) {}

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (showWidgetPicker || showAddOptionsDialog || showAppSelectionDialog || swapModeEnabled) {
                    Modifier.blockAllNavigation()
                } else {
                    Modifier.blockHorizontalNavigation()
                }
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        showDrawerOptionsDialog = true
                    }
                )
            }
    ) {
        if (showWidgetPicker) {
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (swapModeEnabled) {
                    WidgetSwapModeHeaderCard {
                        swapModeEnabled = false
                        swapSourceWidgetId = null
                    }
                } else if (editModeEnabled && pagerState != null) {
                    WidgetEditModeHeaderCard {
                        viewModel.toggleEditMode(pageIndex)
                    }
                } else if (pagerState != null) {
                    AnimatedVisibility(
                        visible = isHeaderVisible,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it })
                    ) {
                        ScreenHeaderRow(
                            totalPages = totalPages,
                            pagerState = pagerState,
                            powerViewModel = powerViewModel,
                            showPowerButton = isPowerButtonVisible,
                            leadingIcon = Icons.Default.Settings,
                            leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                            onLeadingIconClick = onSettingsClick,
                            leadingIconFocusRequester = settingsIconFocusRequester,
                            trailingIcon = Icons.Default.Menu,
                            trailingIconContentDescription = null,
                            onTrailingIconClick = { showAddOptionsDialog = true },
                            trailingIconFocusRequester = addWidgetIconFocusRequester,
                            onNavigateToGrid = {},
                            onNavigateFromGrid = {
                                addWidgetIconFocusRequester.requestFocus()
                            },
                            onFolderClick = onShowBottomSheet,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            onDeletePage = onDeletePage,
                            pageIndicatorBorderColor = pageIndicatorBorderColor,
                            allApps = allApps,
                            onNavigateToSearch = onNavigateToSearch
                        )
                    }
                }

                val isTabEmpty = widgets.isEmpty() && displayedApps.isEmpty()

                if (isTabEmpty && !editModeEnabled) {
                    EmptyWidgetsState(
                        onAddClick = { showAddOptionsDialog = true }
                    )
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, end = 8.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sections = if (appsFirst) {
                            listOf("apps" to displayedApps, "widgets" to widgets)
                        } else {
                            listOf("widgets" to widgets, "apps" to displayedApps)
                        }

                        sections.forEachIndexed { sectionIndex, (sectionType, items) ->
                            if (sectionType == "apps" && displayedApps.isNotEmpty() && !editModeEnabled) {
                                @Suppress("UNCHECKED_CAST")
                                (items as List<AppInfo>).forEach { app ->
                                    item(key = "app_${app.packageName}") {
                                        AppItem(
                                            app = app,
                                            pageIndex = pageIndex
                                        )
                                    }
                                }
                            } else if (sectionType == "widgets" && widgets.isNotEmpty()) {
                                @Suppress("UNCHECKED_CAST")
                                (items as List<WidgetInfo>).forEachIndexed { index, widget ->
                                    item(
                                        key = "widget_${widget.widgetId}_${pageIndex}_$index",
                                        span = { GridItemSpan(widget.width.coerceIn(1, columns)) }
                                    ) {
                                        WidgetItem(
                                            widgetInfo = widget,
                                            viewModel = viewModel,
                                            pageIndex = pageIndex,
                                            onNavigateToResize = onNavigateToResize,
                                            swapModeEnabled = swapModeEnabled,
                                            isSwapSource = swapSourceWidgetId == widget.widgetId,
                                            onSwapSelect = { selectedWidgetId ->
                                                if (swapSourceWidgetId != null && swapSourceWidgetId != selectedWidgetId) {
                                                    viewModel.swapWidgets(
                                                        swapSourceWidgetId!!,
                                                        selectedWidgetId,
                                                        pageIndex
                                                    )
                                                    swapModeEnabled = false
                                                    swapSourceWidgetId = null
                                                }
                                            },
                                            onEnableSwapMode = {
                                                swapModeEnabled = true
                                                swapSourceWidgetId = widget.widgetId
                                            },
                                            editModeEnabled = editModeEnabled
                                        )
                                    }
                                }
                            }


                        }
                    }
                }
            }
        }
    }

    if (showAddOptionsDialog || showFolderOptionsDialog) {
        val isTabEmpty = widgets.isEmpty() && displayedApps.isEmpty()

        AppsAndWidgetsOptionsDialog(
            onDismiss = {
                showAddOptionsDialog = false
                showFolderOptionsDialog = false
            },
            onAddWidget = { showWidgetPicker = true },
            onAddApp = { showAppSelectionDialog = true },
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

    if (showAppSelectionDialog) {
        AppVisibilityDialogForWidgetTab(
            apps = allApps,
            visibleApps = visibleApps,
            pageIndex = pageIndex,
            onDismiss = { showAppSelectionDialog = false },
            widgetPageAppManager = widgetPageAppManager
        )
    }

    if (showWidgetPicker) {
        LaunchedEffect(Unit) {
            val appWidgetId = viewModel.allocateAppWidgetId()
            if (appWidgetId != -1) {
                val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                widgetPickerLauncher.launch(pickIntent)
            }
            showWidgetPicker = false
        }
    }

    if (showHomeTabDialog) {
        val homeTabManager = jr.brian.home.ui.theme.managers.LocalHomeTabManager.current
        val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
        val pageCountManager = jr.brian.home.ui.theme.managers.LocalPageCountManager.current
        val pageTypeManager = jr.brian.home.ui.theme.managers.LocalPageTypeManager.current
        val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()

        HomeTabSelectionDialog(
            currentTabIndex = currentHomeTabIndex,
            totalPages = totalPages,
            allApps = allApps,
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

    if (showDrawerOptionsDialog) {
        DrawerOptionsDialog(
            onDismiss = { showDrawerOptionsDialog = false },
            onPowerClick = {
                powerViewModel?.togglePower()
            },
            onTabsClick = {
                showHomeTabDialog = true
            },
            onMenuClick = {
                showAddOptionsDialog = true
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet
        )
    }
}

@Composable
private fun WidgetSwapModeHeaderCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemePrimaryColor.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.widget_swap_mode_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = stringResource(R.string.widget_swap_mode_instructions),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.widget_swap_cancel))
            }
        }
    }
}

@Composable
private fun WidgetEditModeHeaderCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = ThemePrimaryColor.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.widget_page_edit_mode_active),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.widget_page_edit_mode_exit),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.widget_edit_tap_instruction),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun AppVisibilityDialogForWidgetTab(
    apps: List<AppInfo>,
    visibleApps: Set<String>,
    pageIndex: Int,
    onDismiss: () -> Unit,
    widgetPageAppManager: jr.brian.home.data.WidgetPageAppManager
) {
    val scope = rememberCoroutineScope()

    AppVisibilityDialog(
        apps = apps,
        onDismiss = onDismiss,
        pageIndex = pageIndex,
        isWidgetTabMode = true,
        visibleAppsOverride = visibleApps,
        onToggleAppOverride = { packageName ->
            scope.launch {
                if (packageName in visibleApps) {
                    widgetPageAppManager.removeVisibleApp(pageIndex, packageName)
                } else {
                    widgetPageAppManager.addVisibleApp(pageIndex, packageName)
                }
            }
        },
        onShowAllOverride = {
            scope.launch {
                apps.forEach { app ->
                    if (app.packageName !in visibleApps) {
                        widgetPageAppManager.addVisibleApp(pageIndex, app.packageName)
                    }
                }
            }
        },
        onHideAllOverride = {
            scope.launch {
                apps.forEach { app ->
                    if (app.packageName in visibleApps) {
                        widgetPageAppManager.removeVisibleApp(pageIndex, app.packageName)
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyWidgetsState(
    onAddClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = stringResource(R.string.widgets_tab_no_content_title),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.widgets_tab_no_content_description),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val cardGradient = Brush.linearGradient(
                colors = if (isFocused) {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.9f),
                        ThemeSecondaryColor.copy(alpha = 0.9f)
                    )
                } else {
                    listOf(
                        ThemePrimaryColor.copy(alpha = 0.4f),
                        ThemeSecondaryColor.copy(alpha = 0.3f)
                    )
                }
            )

            Box(
                modifier = Modifier
                    .scale(animatedFocusedScale(isFocused))
                    .onFocusChanged { isFocused = it.isFocused }
                    .background(
                        brush = cardGradient,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = if (isFocused) 3.dp else 2.dp,
                        brush = if (isFocused) {
                            borderBrush(
                                isFocused = true,
                                colors = listOf(
                                    ThemePrimaryColor.copy(alpha = 0.8f),
                                    ThemeSecondaryColor.copy(alpha = 0.6f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    ThemePrimaryColor.copy(alpha = 0.6f),
                                    ThemeSecondaryColor.copy(alpha = 0.4f)
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onAddClick() }
                    .focusable()
                    .padding(horizontal = 48.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(R.string.widgets_tab_add_button),
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
