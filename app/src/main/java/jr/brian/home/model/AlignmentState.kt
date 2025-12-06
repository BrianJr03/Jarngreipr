package jr.brian.home.model

data class AlignmentState(
    val guides: List<AlignmentGuide> = emptyList(),
    val snappedX: Float? = null,
    val snappedY: Float? = null
)
