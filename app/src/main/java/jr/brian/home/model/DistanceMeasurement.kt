package jr.brian.home.model

data class DistanceMeasurement(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val distance: Float,
    val isHorizontal: Boolean
)
