package jr.brian.home.ui.screens

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jr.brian.home.data.WidgetProviderRepository
import jr.brian.home.model.widget.WidgetCategory
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.ui.components.QwertyKeyboard
import jr.brian.home.ui.components.widgetpicker.EmptyStateView
import jr.brian.home.ui.components.widgetpicker.HorizontalWidgetList
import jr.brian.home.ui.components.widgetpicker.LoadingView
import jr.brian.home.ui.theme.OledBackgroundColor
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
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

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

    val filteredCategories = remember(widgetCategories, searchQuery) {
        if (searchQuery.isBlank()) {
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

    LaunchedEffect(Unit) {
        isLoading = true
        widgetCategories = repository.getWidgetCategories()
        isLoading = false
    }

    BackHandler {
        onNavigateBack()
    }

    val keyboardFocusRequesters = remember { SnapshotStateMap<Int, FocusRequester>() }
    var focusedKeyIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OledBackgroundColor)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Widget horizontal list (top section)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                isLoading -> {
                    LoadingView()
                }

                filteredCategories.isEmpty() -> {
                    EmptyStateView(searchQuery)
                }

                else -> {
                    HorizontalWidgetList(
                        categories = filteredCategories,
                        repository = repository,
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(OledBackgroundColor)
        ) {
            QwertyKeyboard(
                searchQuery = searchQuery,
                showFlipLayoutButton = false,
                onQueryChange = { searchQuery = it },
                keyboardFocusRequesters = keyboardFocusRequesters,
                onFocusChanged = { focusedKeyIndex = it },
                onFlipLayout = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
