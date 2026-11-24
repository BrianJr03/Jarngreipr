package jr.brian.home.viewmodels

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.canvas.data.isWidgetIdReferenced
import jr.brian.home.canvas.jarn.JarnWidgetHostHandoff
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.data.PageTypeManager
import jr.brian.home.data.WidgetPageAppManager
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.PageType
import jr.brian.home.model.widget.WidgetConfig
import jr.brian.home.model.widget.WidgetInfo
import jr.brian.home.model.widget.WidgetPage
import jr.brian.home.model.state.WidgetUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetPreferences: WidgetPreferences,
    private val pageTypeManager: PageTypeManager,
    private val widgetPageAppManager: WidgetPageAppManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(WidgetUIState())
    val uiState = _uiState.asStateFlow()

    private var appWidgetHost: AppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null
    private var isLoadingFromPreferences = false
    private var isDeletingPage = false

    companion object {
        const val APPWIDGET_HOST_ID = 1024
        const val MAX_WIDGET_PAGES = 2
        private const val TAG = "WidgetViewModel"
    }

    fun initializeWidgetHost(context: Context) {
        appWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(context)

        appWidgetHost?.startListening()
        appWidgetHost?.let(JarnWidgetHostHandoff::classicStarted)

        val pageTypes = pageTypeManager.pageTypes.value
        val widgetPageCount = pageTypes.count { it != PageType.APPS_TAB }

        val pages = (0 until widgetPageCount).map { index ->
            WidgetPage(index = index)
        }

        _uiState.value = _uiState.value.copy(
            widgetPages = pages,
            isInitialized = true
        )

        loadSavedWidgets()
        observePageTypes()
    }

    private fun observePageTypes() {
        viewModelScope.launch {
            pageTypeManager.pageTypes.collect { pageTypes ->
                if (isDeletingPage) {
                    return@collect
                }

                val widgetPageCount = pageTypes.count { it != PageType.APPS_TAB }
                val currentPages = _uiState.value.widgetPages

                when {
                    widgetPageCount > currentPages.size -> {
                        val additionalPages =
                            (currentPages.size until widgetPageCount).map { index ->
                                WidgetPage(index = index)
                            }
                        _uiState.value = _uiState.value.copy(
                            widgetPages = currentPages + additionalPages
                        )
                    }

                    widgetPageCount < currentPages.size -> {
                        val savedPages = widgetPreferences.widgetConfigs.first()
                            .associate { it.widgetId to it.pageIndex }
                        val latestPages = _uiState.value.widgetPages
                        val trimmedPages = latestPages.take(widgetPageCount)
                        val removedPages = latestPages.drop(widgetPageCount)

                        removedPages.forEach { page ->
                            page.allWidgetIds().forEach { widgetId ->
                                // A page removed outside this ViewModel has already moved its
                                // neighbors down in storage, so those widgets must survive.
                                val savedPage = savedPages[widgetId]
                                if (savedPage != null && savedPage < widgetPageCount) return@forEach
                                appWidgetHost?.deleteAppWidgetId(widgetId)
                                viewModelScope.launch {
                                    widgetPreferences.removeWidgetConfig(widgetId)
                                }
                            }
                        }

                        _uiState.value = _uiState.value.copy(
                            widgetPages = trimmedPages
                        )
                    }
                }
            }
        }
    }

    private fun loadSavedWidgets() {
        viewModelScope.launch {
            widgetPreferences.widgetConfigs.collect { configs ->
                if (isLoadingFromPreferences) {
                    return@collect
                }

                val currentPages = _uiState.value.widgetPages.toMutableList()

                currentPages.forEachIndexed { index, page ->
                    currentPages[index] = page.copy(
                        widgets = emptyList(),
                        unavailableWidgets = emptyList()
                    )
                }

                // Detect if we need to migrate old data (all order values are 0)
                val needsMigration = configs.isNotEmpty() && configs.all { it.order == 0 }

                // Sort configs by order to maintain consistent positioning
                val sortedConfigs = if (needsMigration) {
                    // For migration, group by page and assign sequential order values
                    configs.groupBy { it.pageIndex }
                        .flatMap { (_, pageConfigs) ->
                            pageConfigs.mapIndexed { index, config ->
                                config.copy(order = index)
                            }
                        }
                } else {
                    configs.sortedBy { it.order }
                }

                sortedConfigs.forEach { config ->
                    try {
                        val appWidgetInfo = appWidgetManager?.getAppWidgetInfo(config.widgetId)
                        val pageIndex = config.pageIndex.coerceIn(0, MAX_WIDGET_PAGES - 1)
                        val page = currentPages[pageIndex]

                        if (appWidgetInfo != null) {
                            val widgetInfo = config.toWidgetInfo(appWidgetInfo)
                            currentPages[pageIndex] = page.copy(widgets = page.widgets + widgetInfo)

                            if (needsMigration) {
                                saveWidgetConfig(widgetInfo)
                            }
                        } else {
                            currentPages[pageIndex] = page.copy(
                                unavailableWidgets = page.unavailableWidgets + config
                            )

                            if (needsMigration) {
                                persistWidgetConfig(config)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring widget ${config.widgetId}", e)
                    }
                }

                _uiState.value = _uiState.value.copy(widgetPages = currentPages)
            }
        }
    }

    /** Providers can vanish during an update or storage unmount, then return with the same id. */
    fun refreshUnavailableWidgets() {
        val manager = appWidgetManager ?: return
        var anyRestored = false
        val pages = _uiState.value.widgetPages.map { page ->
            val restored = page.unavailableWidgets.mapNotNull { config ->
                runCatching { manager.getAppWidgetInfo(config.widgetId) }.getOrNull()
                    ?.let { config.toWidgetInfo(it) }
            }
            if (restored.isEmpty()) return@map page
            anyRestored = true
            val restoredIds = restored.map { it.widgetId }.toSet()
            page.copy(
                widgets = (page.widgets + restored).sortedBy { it.order },
                unavailableWidgets = page.unavailableWidgets.filter { it.widgetId !in restoredIds }
            )
        }
        if (anyRestored) _uiState.value = _uiState.value.copy(widgetPages = pages)
    }

    /** Keeps the id when storage can't confirm nothing else uses it. */
    fun releaseWidgetIdIfUnreferenced(widgetId: Int, layouts: () -> Collection<CanvasLayout>) {
        viewModelScope.launch {
            val widgetPageIds = runCatching {
                widgetPreferences.widgetConfigs.first().map { it.widgetId }
            }.getOrNull() ?: return@launch
            if (!isWidgetIdReferenced(layouts(), widgetPageIds, widgetId)) {
                runCatching { appWidgetHost?.deleteAppWidgetId(widgetId) }
            }
        }
    }

    private fun WidgetPage.allWidgetIds(): List<Int> =
        widgets.map { it.widgetId } + unavailableWidgets.map { it.widgetId }

    private fun WidgetPage.nextOrder(): Int =
        (widgets.map { it.order } + unavailableWidgets.map { it.order }).maxOrNull()?.plus(1) ?: 0

    private fun WidgetConfig.toWidgetInfo(providerInfo: AppWidgetProviderInfo) = WidgetInfo(
        widgetId = widgetId,
        providerInfo = providerInfo,
        x = x,
        y = y,
        width = width,
        height = height,
        pageIndex = pageIndex,
        order = order
    )

    fun addWidgetToPage(widgetInfo: WidgetInfo, pageIndex: Int) {
        viewModelScope.launch {
            val currentPages = _uiState.value.widgetPages.toMutableList()
            val pageToUpdate = currentPages.getOrNull(pageIndex) ?: return@launch
            val updatedWidget = widgetInfo.copy(
                pageIndex = pageIndex,
                order = pageToUpdate.nextOrder()
            )
            val updatedWidgets = pageToUpdate.widgets + updatedWidget
            currentPages[pageIndex] = pageToUpdate.copy(widgets = updatedWidgets)
            _uiState.value = _uiState.value.copy(widgetPages = currentPages)
            saveWidgetConfig(updatedWidget)
        }
    }

    private fun saveWidgetConfig(widgetInfo: WidgetInfo) {
        persistWidgetConfig(
            WidgetConfig(
                widgetId = widgetInfo.widgetId,
                providerClassName = widgetInfo.providerInfo.provider.className,
                providerPackageName = widgetInfo.providerInfo.provider.packageName,
                x = widgetInfo.x,
                y = widgetInfo.y,
                width = widgetInfo.width,
                height = widgetInfo.height,
                pageIndex = widgetInfo.pageIndex,
                order = widgetInfo.order
            )
        )
    }

    private fun persistWidgetConfig(config: WidgetConfig) {
        viewModelScope.launch {
            try {
                isLoadingFromPreferences = true
                widgetPreferences.addWidgetConfig(config)
                Log.d(TAG, "Saved widget config: ${config.providerClassName}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving widget config", e)
            } finally {
                isLoadingFromPreferences = false
            }
        }
    }

    fun removeWidgetFromPage(widgetId: Int, pageIndex: Int) {
        viewModelScope.launch {
            isLoadingFromPreferences = true
            try {
                val currentPages = _uiState.value.widgetPages.toMutableList()
                val pageToUpdate = currentPages.getOrNull(pageIndex) ?: return@launch
                currentPages[pageIndex] = pageToUpdate.copy(
                    widgets = pageToUpdate.widgets.filter { it.widgetId != widgetId },
                    unavailableWidgets = pageToUpdate.unavailableWidgets
                        .filter { it.widgetId != widgetId }
                )
                _uiState.value = _uiState.value.copy(widgetPages = currentPages)
                widgetPreferences.removeWidgetConfig(widgetId)
                appWidgetHost?.deleteAppWidgetId(widgetId)
            } finally {
                isLoadingFromPreferences = false
            }
        }
    }


    fun moveWidgetToPage(widgetId: Int, fromPageIndex: Int, toPageIndex: Int) {
        viewModelScope.launch {
            val currentPages = _uiState.value.widgetPages.toMutableList()
            val fromPage = currentPages.getOrNull(fromPageIndex) ?: return@launch
            val toPage = currentPages.getOrNull(toPageIndex) ?: return@launch

            val widgetToMove = fromPage.widgets.find { it.widgetId == widgetId } ?: return@launch

            val updatedFromWidgets = fromPage.widgets.filter { it.widgetId != widgetId }
            currentPages[fromPageIndex] = fromPage.copy(widgets = updatedFromWidgets)

            val updatedWidget = widgetToMove.copy(
                pageIndex = toPageIndex,
                order = toPage.nextOrder()
            )
            val updatedToWidgets = toPage.widgets + updatedWidget
            currentPages[toPageIndex] = toPage.copy(widgets = updatedToWidgets)

            _uiState.value = _uiState.value.copy(widgetPages = currentPages)

            saveWidgetConfig(updatedWidget)
        }
    }

    fun swapWidgets(
        widget1Id: Int,
        widget2Id: Int,
        pageIndex: Int
    ) {
        viewModelScope.launch {
            isLoadingFromPreferences = true
            try {
                val currentPages = _uiState.value.widgetPages.toMutableList()
                val page = currentPages.getOrNull(pageIndex) ?: return@launch

                val widget1Index = page.widgets.indexOfFirst { it.widgetId == widget1Id }
                val widget2Index = page.widgets.indexOfFirst { it.widgetId == widget2Id }

                if (widget1Index != -1 && widget2Index != -1) {
                    val updatedWidgets = page.widgets.toMutableList()

                    val widget1 = updatedWidgets[widget1Index]
                    val widget2 = updatedWidgets[widget2Index]
                    val tempOrder = widget1.order

                    updatedWidgets[widget1Index] = widget1.copy(order = widget2.order)
                    updatedWidgets[widget2Index] = widget2.copy(order = tempOrder)

                    val temp = updatedWidgets[widget1Index]
                    updatedWidgets[widget1Index] = updatedWidgets[widget2Index]
                    updatedWidgets[widget2Index] = temp

                    currentPages[pageIndex] = page.copy(widgets = updatedWidgets)
                    _uiState.value = _uiState.value.copy(widgetPages = currentPages)

                    updatedWidgets[widget1Index].let { widget ->
                        val config = WidgetConfig(
                            widgetId = widget.widgetId,
                            providerClassName = widget.providerInfo.provider.className,
                            providerPackageName = widget.providerInfo.provider.packageName,
                            x = widget.x,
                            y = widget.y,
                            width = widget.width,
                            height = widget.height,
                            pageIndex = widget.pageIndex,
                            order = widget.order
                        )
                        widgetPreferences.addWidgetConfig(config)
                    }
                    updatedWidgets[widget2Index].let { widget ->
                        val config = WidgetConfig(
                            widgetId = widget.widgetId,
                            providerClassName = widget.providerInfo.provider.className,
                            providerPackageName = widget.providerInfo.provider.packageName,
                            x = widget.x,
                            y = widget.y,
                            width = widget.width,
                            height = widget.height,
                            pageIndex = widget.pageIndex,
                            order = widget.order
                        )
                        widgetPreferences.addWidgetConfig(config)
                    }

                    Log.d(TAG, "Swapped widgets: $widget1Id <-> $widget2Id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error swapping widgets", e)
            } finally {
                isLoadingFromPreferences = false
            }
        }
    }

    fun updateWidgetSize(
        widgetId: Int,
        pageIndex: Int,
        newWidth: Int,
        newHeight: Int
    ) {
        viewModelScope.launch {
            isLoadingFromPreferences = true
            try {
                val currentPages = _uiState.value.widgetPages.toMutableList()
                val page = currentPages.getOrNull(pageIndex) ?: return@launch

                val widgetIndex = page.widgets.indexOfFirst { it.widgetId == widgetId }
                if (widgetIndex != -1) {
                    val updatedWidget = page.widgets[widgetIndex].copy(
                        width = newWidth,
                        height = newHeight
                    )

                    val updatedWidgets = page.widgets.toMutableList()
                    updatedWidgets[widgetIndex] = updatedWidget

                    currentPages[pageIndex] = page.copy(widgets = updatedWidgets)
                    _uiState.value = _uiState.value.copy(widgetPages = currentPages)

                    val config = WidgetConfig(
                        widgetId = updatedWidget.widgetId,
                        providerClassName = updatedWidget.providerInfo.provider.className,
                        providerPackageName = updatedWidget.providerInfo.provider.packageName,
                        x = updatedWidget.x,
                        y = updatedWidget.y,
                        width = updatedWidget.width,
                        height = updatedWidget.height,
                        pageIndex = updatedWidget.pageIndex,
                        order = updatedWidget.order
                    )
                    widgetPreferences.addWidgetConfig(config)
                    Log.d(TAG, "Updated widget size: $widgetId to ${newWidth}x${newHeight}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget size", e)
            } finally {
                isLoadingFromPreferences = false
            }
        }
    }

    @Suppress("unused")
    fun reorderWidgets(pageIndex: Int, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentPages = _uiState.value.widgetPages.toMutableList()
            val pageToUpdate = currentPages.getOrNull(pageIndex) ?: return@launch

            val updatedWidgets = pageToUpdate.widgets.toMutableList()
            val temp = updatedWidgets[fromIndex]
            updatedWidgets[fromIndex] = updatedWidgets[toIndex]
            updatedWidgets[toIndex] = temp

            currentPages[pageIndex] = pageToUpdate.copy(widgets = updatedWidgets.toList())
            _uiState.value = _uiState.value.copy(widgetPages = currentPages.toList())

            updatedWidgets.forEach { widget ->
                saveWidgetConfig(widget)
            }
        }
    }

    fun allocateAppWidgetId(): Int {
        return appWidgetHost?.allocateAppWidgetId() ?: -1
    }

    fun getAppWidgetHost(): AppWidgetHost? = appWidgetHost

    fun toggleEditMode(pageIndex: Int) {
        val currentEditModeMap = _uiState.value.editModeByPage.toMutableMap()
        val currentMode = currentEditModeMap[pageIndex] ?: false
        currentEditModeMap[pageIndex] = !currentMode
        _uiState.value = _uiState.value.copy(editModeByPage = currentEditModeMap)
    }

    fun deletePage(widgetPageIndexToDelete: Int) {
        viewModelScope.launch {
            isDeletingPage = true
            try {
                val currentPages = _uiState.value.widgetPages.toMutableList()

                Log.d(TAG, "Attempting to delete widget page at index: $widgetPageIndexToDelete")
                Log.d(TAG, "Current widget pages count: ${currentPages.size}")

                if (widgetPageIndexToDelete < 0 || widgetPageIndexToDelete >= currentPages.size) {
                    Log.d(TAG, "Invalid widget page index, aborting deletion")
                    return@launch
                }

                val pageToDelete = currentPages[widgetPageIndexToDelete]
                Log.d(TAG, "Deleting page with ${pageToDelete.widgets.size} widgets")

                pageToDelete.allWidgetIds().forEach { widgetId ->
                    appWidgetHost?.deleteAppWidgetId(widgetId)
                    widgetPreferences.removeWidgetConfig(widgetId)
                }

                widgetPageAppManager.reindexPages(widgetPageIndexToDelete, currentPages.size - 1)

                currentPages.removeAt(widgetPageIndexToDelete)

                val reindexedPages = currentPages.mapIndexed { newIndex, page ->
                    val updatedWidgets = page.widgets.map { widget ->
                        if (widget.pageIndex != newIndex) {
                            val updatedWidget = widget.copy(pageIndex = newIndex)
                            saveWidgetConfig(updatedWidget)
                            updatedWidget
                        } else {
                            widget
                        }
                    }
                    val updatedUnavailable = page.unavailableWidgets.map { config ->
                        if (config.pageIndex != newIndex) {
                            config.copy(pageIndex = newIndex).also(::persistWidgetConfig)
                        } else {
                            config
                        }
                    }
                    page.copy(
                        index = newIndex,
                        widgets = updatedWidgets,
                        unavailableWidgets = updatedUnavailable
                    )
                }

                _uiState.value = _uiState.value.copy(widgetPages = reindexedPages)

                Log.d(TAG, "Widget page deleted. New widget pages count: ${reindexedPages.size}")
            } finally {
                isDeletingPage = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        appWidgetHost?.stopListening()
        appWidgetHost?.let(JarnWidgetHostHandoff::classicStopped)
    }
}
