package jr.brian.home.model.widget

data class WidgetSizeInfo(
    val minWidthCells: Int,
    val minHeightCells: Int,
    val targetWidthCells: Int,
    val targetHeightCells: Int,
    val maxWidthCells: Int,
    val maxHeightCells: Int,
    val isResizable: Boolean
)