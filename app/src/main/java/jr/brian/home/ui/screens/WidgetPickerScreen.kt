package jr.brian.home.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.home.R
import jr.brian.home.data.WidgetProviderRepository
import jr.brian.home.model.widget.WidgetCategory
import jr.brian.home.model.widget.WidgetProviderInfo
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.model.widget.WidgetWithCategory
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.components.OnScreenKeyboard
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.OledCardColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.ThemeSecondaryColor
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

@Composable
fun WidgetPickerScreen(
    pageIndex: Int,
    onNavigateBack: () -> Unit,
    onWidgetAdded: () -> Unit = {},
    widgetViewModel: WidgetViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { WidgetProviderRepository(context) }

    var widgetCategories by remember { mutableStateOf<List<WidgetCategory>>(emptyList()) }
    var filteredCategories by remember { mutableStateOf<List<WidgetCategory>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var showKeyboard by remember { mutableStateOf(false) }

    val bindWidgetLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        val appWidgetId = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1

        if (appWidgetId != -1) {
            scope.launch {
                try {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

                    if (appWidgetInfo != null) {
                        val sizeInfo = repository.getWidgetSizeInfo(appWidgetInfo)
                        val widgetInfo = WidgetInfo(
                            widgetId = appWidgetId,
                            providerInfo = appWidgetInfo,
                            pageIndex = pageIndex,
                            width = sizeInfo.targetWidthCells,
                            height = sizeInfo.targetHeightCells
                        )

                        widgetViewModel.addWidgetToPage(widgetInfo, pageIndex)
                        onWidgetAdded()
                        onNavigateBack()
                    }
                } catch (_: Exception) {
                    // Handle error
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        widgetCategories = repository.getWidgetCategories()
        filteredCategories = widgetCategories
        isLoading = false
    }

    LaunchedEffect(searchQuery, widgetCategories) {
        filteredCategories = if (searchQuery.isBlank()) {
            widgetCategories
        } else {
            widgetCategories.mapNotNull { category ->
                val matchingWidgets = category.widgets.filter { widget ->
                    widget.label.contains(searchQuery, ignoreCase = true) ||
                            widget.appName.contains(searchQuery, ignoreCase = true)
                }
                if (matchingWidgets.isNotEmpty()) {
                    category.copy(widgets = matchingWidgets)
                } else {
                    null
                }
            }
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(if (showKeyboard) 1f else 2f)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    WidgetPickerHeader(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onNavigateBack = onNavigateBack,
                        onSearchBarClick = { showKeyboard = !showKeyboard },
                        showKeyboard = showKeyboard
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    when {
                        isLoading -> {
                            LoadingView()
                        }

                        filteredCategories.isEmpty() -> {
                            EmptyStateView(searchQuery)
                        }

                        else -> {
                            WidgetCategoryList(
                                categories = filteredCategories,
                                repository = repository,
                                showKeyboard = showKeyboard,
                                onWidgetSelected = { widgetProviderInfo ->
                                    scope.launch {
                                        val appWidgetId = widgetViewModel.allocateAppWidgetId()
                                        if (appWidgetId != -1) {
                                            val appWidgetManager =
                                                AppWidgetManager.getInstance(context)

                                            val canBind = appWidgetManager.bindAppWidgetIdIfAllowed(
                                                appWidgetId,
                                                widgetProviderInfo.providerInfo.provider
                                            )

                                            if (canBind) {
                                                val sizeInfo =
                                                    repository.getWidgetSizeInfo(widgetProviderInfo.providerInfo)
                                                val widgetInfo = WidgetInfo(
                                                    widgetId = appWidgetId,
                                                    providerInfo = widgetProviderInfo.providerInfo,
                                                    pageIndex = pageIndex,
                                                    width = sizeInfo.targetWidthCells,
                                                    height = sizeInfo.targetHeightCells
                                                )

                                                widgetViewModel.addWidgetToPage(
                                                    widgetInfo,
                                                    pageIndex
                                                )
                                                onWidgetAdded()
                                                onNavigateBack()
                                            } else {
                                                val bindIntent =
                                                    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                                                        putExtra(
                                                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                                                            appWidgetId
                                                        )
                                                        putExtra(
                                                            AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
                                                            widgetProviderInfo.providerInfo.provider
                                                        )
                                                    }
                                                bindWidgetLauncher.launch(bindIntent)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            if (showKeyboard) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .background(OledBackgroundColor)
                ) {
                    val keyboardFocusRequesters =
                        remember { SnapshotStateMap<Int, FocusRequester>() }
                    var focusedKeyIndex by remember { mutableIntStateOf(0) }

                    OnScreenKeyboard(
                        searchQuery = searchQuery,
                        showQueryText = false,
                        onQueryChange = { searchQuery = it },
                        keyboardFocusRequesters = keyboardFocusRequesters,
                        onFocusChanged = { focusedKeyIndex = it },
                        onNavigateRight = {},
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPickerHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onSearchBarClick: () -> Unit,
    showKeyboard: Boolean
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.dialog_cancel),
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.widget_picker_title),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        WidgetSearchBar(
            query = searchQuery,
            onClick = onSearchBarClick,
            showKeyboard = showKeyboard
        )
    }
}

@Composable
private fun WidgetSearchBar(
    query: String,
    showKeyboard: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                color = OledCardColor,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) ThemePrimaryColor else ThemePrimaryColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = query.ifEmpty { stringResource(R.string.widget_picker_search_hint) },
            color = if (query.isEmpty()) Color.White.copy(alpha = 0.5f) else Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Icon(
            imageVector = if (showKeyboard) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (showKeyboard) "Hide keyboard" else "Show keyboard",
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun WidgetCategoryList(
    categories: List<WidgetCategory>,
    repository: WidgetProviderRepository,
    showKeyboard: Boolean,
    onWidgetSelected: (WidgetProviderInfo) -> Unit
) {
    val allWidgets = remember(categories) {
        categories.flatMap { category ->
            category.widgets.map { widget ->
                WidgetWithCategory(
                    widget = widget,
                    categoryName = category.appName,
                    categoryIcon = category.appIcon
                )
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (showKeyboard) 1 else 2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = allWidgets,
            key = { "${it.category.packageName}_${it.widget.providerInfo.provider.className}" }
        ) { widgetWithCategory ->
            WidgetPreviewCard(
                widget = widgetWithCategory.widget,
                repository = repository,
                onClick = { onWidgetSelected(widgetWithCategory.widget) }
            )
        }
    }
}

@Composable
private fun WidgetPreviewCard(
    widget: WidgetProviderInfo,
    repository: WidgetProviderRepository,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val sizeInfo = remember(widget) { repository.getWidgetSizeInfo(widget.providerInfo) }

    val cardGradient = Brush.linearGradient(
        colors = if (isFocused) {
            listOf(
                ThemePrimaryColor.copy(alpha = 0.3f),
                ThemeSecondaryColor.copy(alpha = 0.3f)
            )
        } else {
            listOf(
                OledCardColor,
                OledCardColor
            )
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .background(
                brush = cardGradient,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) {
                    borderBrush(
                        isFocused = true,
                        colors = listOf(
                            ThemePrimaryColor,
                            ThemeSecondaryColor
                        )
                    )
                } else {
                    Brush.linearGradient(
                        colors = listOf(
                            ThemePrimaryColor.copy(alpha = 0.2f),
                            ThemeSecondaryColor.copy(alpha = 0.2f)
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .focusable()
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Preview image - larger size
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                widget.previewImage?.let { preview ->
                    androidx.compose.foundation.Image(
                        bitmap = preview.toBitmap(400, 400).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                } ?: run {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Widget name
            Text(
                text = widget.label,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // App name (smaller)
            Text(
                text = widget.appName,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Size and resizable info in a row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        R.string.widget_picker_size_info,
                        sizeInfo.targetWidthCells,
                        sizeInfo.targetHeightCells
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )

                if (sizeInfo.isResizable) {
                    Text(
                        text = " | ",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 15.sp
                    )
                    Text(
                        text = stringResource(R.string.widget_picker_resizable),
                        color = ThemePrimaryColor.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Description (if available)
            if (widget.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = widget.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = ThemePrimaryColor,
                strokeWidth = 4.dp
            )
            Text(
                text = stringResource(R.string.widget_picker_loading),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun EmptyStateView(searchQuery: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (searchQuery.isBlank()) {
                    stringResource(R.string.widget_picker_no_widgets)
                } else {
                    stringResource(R.string.widget_picker_no_results)
                },
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (searchQuery.isBlank()) {
                    stringResource(R.string.widget_picker_no_widgets_description)
                } else {
                    stringResource(R.string.widget_picker_try_different_search)
                },
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
