package jr.brian.home.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.model.AppInfo
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.components.dialog.AddToWidgetPageDialog
import jr.brian.home.ui.components.widget.AppItem
import jr.brian.home.ui.components.widget.SectionHeader
import jr.brian.home.ui.components.widget.WidgetItem
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.components.wallpaper.WallpaperDisplay
import jr.brian.home.ui.components.dialog.WidgetPageAppSelectionDialog
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.LocalWidgetPageAppManager
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetPageScreen(
    pageIndex: Int,
    widgets: List<WidgetInfo>,
    viewModel: WidgetViewModel,
    powerViewModel: PowerViewModel,
    allApps: List<AppInfo> = emptyList(),
    modifier: Modifier = Modifier,
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    onSettingsClick: () -> Unit = {},
    onNavigateToResize: (WidgetInfo, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val widgetPageAppManager = LocalWidgetPageAppManager.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val columns = gridSettingsManager.columnCount
    val scope = rememberCoroutineScope()

    val addWidgetIconFocusRequester = remember { FocusRequester() }
    var showAddOptionsDialog by remember { mutableStateOf(false) }
    var showAppSelectionDialog by remember { mutableStateOf(false) }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var swapModeEnabled by remember { mutableStateOf(false) }
    var swapSourceWidgetId by remember { mutableStateOf<Int?>(null) }

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

    val settingsIconFocusRequester = remember { FocusRequester() }

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
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (showWidgetPicker || showAddOptionsDialog || showAppSelectionDialog ||
                    (widgets.isEmpty() && displayedApps.isEmpty()) || swapModeEnabled
                ) {
                    Modifier.blockAllNavigation()
                } else {
                    Modifier.blockHorizontalNavigation()
                }
            )
    ) {
        WallpaperDisplay(
            wallpaperUri = wallpaperManager.getWallpaperUri(),
            wallpaperType = wallpaperManager.getWallpaperType(),
            modifier = Modifier.fillMaxSize()
        )

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
        } else if (widgets.isEmpty() && displayedApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.widget_no_widgets_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showAddOptionsDialog = true },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.widget_add_widget),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
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
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sections = if (appsFirst) {
                        listOf("apps" to displayedApps, "widgets" to widgets)
                    } else {
                        listOf("widgets" to widgets, "apps" to displayedApps)
                    }

                    sections.forEach { (sectionType, items) ->
                        if (sectionType == "apps" && displayedApps.isNotEmpty() && !editModeEnabled) {
                            item(span = { GridItemSpan(columns) }) {
                                SectionHeader(
                                    title = stringResource(R.string.widget_page_section_apps)
                                )
                            }

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
                            item(span = { GridItemSpan(columns) }) {
                                SectionHeader(
                                    title = stringResource(R.string.widget_page_section_widgets)
                                )
                            }

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

                    item {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    if (showAddOptionsDialog) {
        AddToWidgetPageDialog(
            onDismiss = { showAddOptionsDialog = false },
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
            isEditModeActive = editModeEnabled
        )
    }

    if (showAppSelectionDialog) {
        WidgetPageAppSelectionDialog(
            apps = allApps,
            visibleApps = visibleApps,
            onDismiss = { showAppSelectionDialog = false },
            onToggleApp = { packageName ->
                scope.launch {
                    if (packageName in visibleApps) {
                        widgetPageAppManager.removeVisibleApp(pageIndex, packageName)
                    } else {
                        widgetPageAppManager.addVisibleApp(pageIndex, packageName)
                    }
                }
            }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.widget_page_edit_mode_active),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text(stringResource(R.string.widget_page_edit_mode_exit))
            }
        }
    }
}
