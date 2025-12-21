package jr.brian.home.model.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetConfig(
    val widgetId: Int,
    val providerClassName: String,
    val providerPackageName: String,
    val x: Int = 0,
    val y: Int = 0,
    val width: Int = 1,
    val height: Int = 1,
    val pageIndex: Int = 0,
    val order: Int = 0
)