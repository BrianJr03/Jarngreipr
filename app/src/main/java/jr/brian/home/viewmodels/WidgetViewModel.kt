package jr.brian.home.viewmodels

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.data.WidgetPreferences
import jr.brian.home.model.WidgetConfig
import jr.brian.home.model.WidgetInfo
import jr.brian.home.model.WidgetPage
import jr.brian.home.model.state.WidgetUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetViewModel @Inject constructor(
    private val widgetPreferences: WidgetPreferences
) : ViewModel() {
    private val _uiState = MutableStateFlow(WidgetUIState())
    val uiState = _uiState.asStateFlow()

    private var appWidgetHost: AppWidgetHost? = null
    private var appWidgetManager: AppWidgetManager? = null
    private var isLoadingFromPreferences = false

    companion object {
        private const val APPWIDGET_HOST_ID = 1024
        const val MAX_WIDGET_PAGES = 2
        private const val TAG = "WidgetViewModel"
    }

    fun initializeWidgetHost(context: Context) {
        appWidgetHost = AppWidgetHost(context, APPWIDGET_HOST_ID)
        appWidgetManager = AppWidgetManager.getInstance(context)

        appWidgetHost?.startListening()

        val pages = (0 until MAX_WIDGET_PAGES).map { index ->
            WidgetPage(index = index)
        }

        _uiState.value = _uiState.value.copy(
            widgetPages = pages,
            isInitialized = true
        )

        loadSavedWidgets()
    }

    private fun loadSavedWidgets() {
        viewModelScope.launch {
            widgetPreferences.widgetConfigs.collect { configs ->
                if (isLoadingFromPreferences) {
                    return@collect
                }

                isLoadingFromPreferences = true

                val currentPages = _uiState.value.widgetPages.toMutableList()

                currentPages.forEachIndexed { index, page ->
                    currentPages[index] = page.copy(widgets = emptyList())
                }

                configs.forEach { config ->
                    try {
                        val appWidgetInfo = appWidgetManager?.getAppWidgetInfo(config.widgetId)

                        if (appWidgetInfo != null) {
                            val widgetInfo = WidgetInfo(
                                widgetId = config.widgetId,
                                providerInfo = appWidgetInfo,
                                x = config.x,
                                y = config.y,
                                width = config.width,
                                height = config.height,
                                pageIndex = config.pageIndex
                            )

                            val pageIndex = config.pageIndex.coerceIn(0, MAX_WIDGET_PAGES - 1)
                            val page = currentPages[pageIndex]
                            currentPages[pageIndex] = page.copy(widgets = page.widgets + widgetInfo)
                        } else {
                            appWidgetHost?.deleteAppWidgetId(config.widgetId)
                            widgetPreferences.removeWidgetConfig(config.widgetId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error restoring widget ${config.widgetId}", e)
                    }
                }

                _uiState.value = _uiState.value.copy(widgetPages = currentPages)
                isLoadingFromPreferences = false
            }
        }
    }

    fun addWidgetToPage(widgetInfo: WidgetInfo, pageIndex: Int) {
        viewModelScope.launch {
            val currentPages = _uiState.value.widgetPages.toMutableList()
            val pageToUpdate = currentPages.getOrNull(pageIndex) ?: return@launch
            val updatedWidget = widgetInfo.copy(pageIndex = pageIndex)
            val updatedWidgets = pageToUpdate.widgets + updatedWidget
            currentPages[pageIndex] = pageToUpdate.copy(widgets = updatedWidgets)
            _uiState.value = _uiState.value.copy(widgetPages = currentPages)
            saveWidgetConfig(updatedWidget)
        }
    }

    private fun saveWidgetConfig(widgetInfo: WidgetInfo) {
        viewModelScope.launch {
            try {
                isLoadingFromPreferences = true
                val config = WidgetConfig(
                    widgetId = widgetInfo.widgetId,
                    providerClassName = widgetInfo.providerInfo.provider.className,
                    providerPackageName = widgetInfo.providerInfo.provider.packageName,
                    x = widgetInfo.x,
                    y = widgetInfo.y,
                    width = widgetInfo.width,
                    height = widgetInfo.height,
                    pageIndex = widgetInfo.pageIndex
                )
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
                val updatedWidgets = pageToUpdate.widgets.filter { it.widgetId != widgetId }
                currentPages[pageIndex] = pageToUpdate.copy(widgets = updatedWidgets)
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

            val updatedWidget = widgetToMove.copy(pageIndex = toPageIndex)
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
                            pageIndex = widget.pageIndex
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
                            pageIndex = widget.pageIndex
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

    fun updateWidgetSize(widgetId: Int, pageIndex: Int, newWidth: Int, newHeight: Int) {
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
                        pageIndex = updatedWidget.pageIndex
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

    override fun onCleared() {
        super.onCleared()
        appWidgetHost?.stopListening()
    }
}
