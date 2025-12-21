package jr.brian.home.model.alignment

import jr.brian.home.model.DistanceMeasurement

data class AlignmentState(
    val guides: List<AlignmentGuide> = emptyList(),
    val snappedX: Float? = null,
    val snappedY: Float? = null,
    val distances: List<DistanceMeasurement> = emptyList()
)
