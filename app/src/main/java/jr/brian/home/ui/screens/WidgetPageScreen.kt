package jr.brian.home.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import jr.brian.home.R
import jr.brian.home.model.WidgetInfo
import jr.brian.home.ui.components.ScreenHeaderRow
import jr.brian.home.ui.components.WallpaperDisplay
import jr.brian.home.ui.extensions.blockAllNavigation
import jr.brian.home.ui.extensions.blockHorizontalNavigation
import jr.brian.home.ui.theme.LocalWallpaperManager
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.viewmodels.WidgetViewModel
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WidgetPageScreen(
    pageIndex: Int,
    widgets: List<WidgetInfo>,
    viewModel: WidgetViewModel,
    modifier: Modifier = Modifier,
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    onNavigateToResize: (WidgetInfo, Int) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val wallpaperManager = LocalWallpaperManager.current
    val gridSettingsManager = jr.brian.home.ui.theme.LocalGridSettingsManager.current
    val columns = gridSettingsManager.columnCount
    val addWidgetIconFocusRequester = remember { FocusRequester() }
    var showWidgetPicker by remember { mutableStateOf(false) }
    var swapModeEnabled by remember { mutableStateOf(false) }
    var swapSourceWidgetId by remember { mutableStateOf<Int?>(null) }

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
                if (showWidgetPicker || widgets.isEmpty() || swapModeEnabled) {
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
        } else if (widgets.isEmpty()) {
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.widget_no_widgets_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showWidgetPicker = true },
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
                                onClick = {
                                    swapModeEnabled = false
                                    swapSourceWidgetId = null
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color.White
                                )
                            ) {
                                Text(stringResource(R.string.widget_swap_cancel))
                            }
                        }
                    }
                } else if (pagerState != null) {
                    ScreenHeaderRow(
                        totalPages = totalPages,
                        pagerState = pagerState,
                        trailingIcon = Icons.Default.Add,
                        trailingIconContentDescription = "Add Widget",
                        onTrailingIconClick = { showWidgetPicker = true },
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
                    if (showWidgetPicker) {
                        item(span = { GridItemSpan(columns) }) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = ThemePrimaryColor.copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = ThemePrimaryColor,
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(R.string.widget_loading),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    widgets.forEachIndexed { index, widget ->
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
                                }
                            )
                        }
                    }
                }
            }
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetItem(
    widgetInfo: WidgetInfo,
    viewModel: WidgetViewModel,
    pageIndex: Int,
    modifier: Modifier = Modifier,
    onNavigateToResize: (WidgetInfo, Int) -> Unit = { _, _ -> },
    swapModeEnabled: Boolean = false,
    isSwapSource: Boolean = false,
    onSwapSelect: (Int) -> Unit = {},
    onEnableSwapMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var showOptionsDialog by remember { mutableStateOf(false) }

    val currentWidgetId by rememberUpdatedState(widgetInfo.widgetId)
    val currentProviderInfo by rememberUpdatedState(widgetInfo.providerInfo)

    // Use widgetInfo directly for height calculation so it updates when widget changes
    val widgetHeightDp = remember(widgetInfo.height) {
        val cellHeight = 80.dp
        val calculatedHeight = (widgetInfo.height * cellHeight.value).dp
        calculatedHeight.coerceAtLeast(80.dp)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (swapModeEnabled && !isSwapSource) Color(0xFF3A3A3A) else Color(
                    0xFF2A2A2A
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isSwapSource) 4.dp else 2.dp,
                color = if (isSwapSource) Color.Yellow else ThemePrimaryColor,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        key("${currentWidgetId}_${widgetInfo.width}x${widgetInfo.height}") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(widgetHeightDp)
                    .background(Color(0xFF2A2A2A))
            ) {
                AndroidView(
                    factory = { ctx ->
                        val host = viewModel.getAppWidgetHost()
                        val widgetView = host?.createView(
                            ctx,
                            currentWidgetId,
                            currentProviderInfo
                        )
                        widgetView ?: ComposeView(ctx)
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        // Force the widget to update its layout when dimensions change
                        view.requestLayout()
                    }
                )
            }
        }

        Card(
            onClick = {
                if (swapModeEnabled && !isSwapSource) {
                    onSwapSelect(widgetInfo.widgetId)
                } else if (!swapModeEnabled) {
                    showOptionsDialog = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            shape = RoundedCornerShape(
                topStart = 0.dp,
                topEnd = 0.dp,
                bottomStart = 10.dp,
                bottomEnd = 10.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSwapSource) Color.Yellow else ThemePrimaryColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (swapModeEnabled) {
                    if (isSwapSource) {
                        Text(
                            text = "Selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Black
                        )
                    } else {
                        Text(
                            text = "Tap to Swap",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.widget_edit_description),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (showOptionsDialog && !swapModeEnabled) {
        WidgetOptionsDialog(
            widgetInfo = widgetInfo,
            currentPageIndex = pageIndex,
            onDismiss = { showOptionsDialog = false },
            onDelete = {
                viewModel.removeWidgetFromPage(widgetInfo.widgetId, pageIndex)
                showOptionsDialog = false
            },
            onMove = { targetPage ->
                viewModel.moveWidgetToPage(
                    widgetId = widgetInfo.widgetId,
                    fromPageIndex = pageIndex,
                    toPageIndex = targetPage
                )
                showOptionsDialog = false
            },
            onResize = {
                showOptionsDialog = false
                onNavigateToResize(widgetInfo, pageIndex)
            },
            onSwap = {
                showOptionsDialog = false
                onEnableSwapMode()
            }
        )
    }
}

@Composable
private fun WidgetOptionsDialog(
    widgetInfo: WidgetInfo,
    currentPageIndex: Int,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onResize: () -> Unit,
    onSwap: () -> Unit
) {
    AlertDialog(

        modifier = Modifier.fillMaxSize(),
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E2E),
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.widget_options_title),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = widgetInfo.providerInfo.loadLabel(LocalContext.current.packageManager),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = onResize,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ThemePrimaryColor.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = null,
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.widget_resize_title),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.widget_resize_description),
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Card(
                    onClick = onSwap,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ThemePrimaryColor.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = ThemePrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.widget_swap_title),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.widget_swap_description),
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Card(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.widget_delete_title),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(R.string.widget_delete_description),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.widget_move_section_title),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                repeat(WidgetViewModel.MAX_WIDGET_PAGES) { index ->
                    if (index != currentPageIndex) {
                        Card(
                            onClick = { onMove(index) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ThemePrimaryColor.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoveDown,
                                    contentDescription = null,
                                    tint = ThemePrimaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = stringResource(
                                            R.string.widget_move_page_title,
                                            index + 1
                                        ),
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.widget_move_description),
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ThemePrimaryColor
                )
            ) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}