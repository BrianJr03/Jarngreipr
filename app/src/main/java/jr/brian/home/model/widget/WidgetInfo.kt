package jr.brian.home.model.widget

import android.appwidget.AppWidgetProviderInfo

data class WidgetInfo(
    val widgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 1,
    val height: Int = 1,
    val pageIndex: Int = 0,
    val order: Int = 0
)

data class WidgetPage(
    val index: Int,
    val widgets: List<WidgetInfo> = emptyList()
)
