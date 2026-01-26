package jr.brian.home.model.alignment

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.model.DistanceMeasurement
import jr.brian.home.model.GuideType
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class AlignmentState(
    val guides: List<AlignmentGuide> = emptyList(),
    val snappedX: Float? = null,
    val snappedY: Float? = null,
    val distances: List<DistanceMeasurement> = emptyList()
)

fun calculateAlignmentGuides(
    dragX: Float,
    dragY: Float,
    iconSize: Float,
    density: Density,
    draggingIndex: Int,
    apps: List<AppInfo>,
    snapThreshold: Float,
    containerSize: IntSize,
    positions: Map<String, AppPosition>,
    borderPadding: Float,
    folders: List<Folder> = emptyList(),
    draggingFolderId: String? = null
): AlignmentState {
    // Return early if nothing is being dragged (no app index and no folder id) or container not ready
    if ((draggingIndex < 0 && draggingFolderId == null) || containerSize.width == 0) {
        return AlignmentState()
    }

    val guides = mutableListOf<AlignmentGuide>()
    val distances = mutableListOf<DistanceMeasurement>()
    var snappedX: Float? = null
    var snappedY: Float? = null

    val iconSizePx = with(density) { iconSize.dp.toPx() }
    val draggingCenterX = dragX + iconSizePx / 2
    val draggingCenterY = dragY + iconSizePx / 2
    val draggingRight = dragX + iconSizePx
    val draggingBottom = dragY + iconSizePx

    val screenCenterX = containerSize.width / 2f
    val screenCenterY = containerSize.height / 2f

    // Distance threshold for showing measurements (in pixels)
    val distanceThreshold = with(density) { 200.dp.toPx() }

    // Check alignment with screen center
    if (abs(draggingCenterX - screenCenterX) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.VERTICAL, screenCenterX))
        snappedX = screenCenterX - iconSizePx / 2
    }

    if (abs(draggingCenterY - screenCenterY) < snapThreshold) {
        guides.add(AlignmentGuide(GuideType.HORIZONTAL, screenCenterY))
        snappedY = screenCenterY - iconSizePx / 2
    }

    // Check alignment with other apps and calculate distances
    apps.forEachIndexed { index, otherApp ->
        if (index != draggingIndex) {
            val otherPosition = positions[otherApp.packageName]

            // Calculate default position for apps without saved positions
            val otherX = otherPosition?.x ?: with(density) {
                val columns = 4
                val itemWidth = 80.dp.toPx()
                val spacing = 32.dp.toPx()
                val column = index % columns
                val startPadding = max(8.dp.toPx(), borderPadding)
                (startPadding + column * (itemWidth + spacing))
            }
            val otherY = otherPosition?.y ?: with(density) {
                val columns = 4
                val itemHeight = 100.dp.toPx()
                val spacing = 24.dp.toPx()
                val row = index / columns
                val topPadding = max(8.dp.toPx(), borderPadding)
                (topPadding + row * (itemHeight + spacing))
            }
            val otherIconSize = otherPosition?.iconSize ?: 64f
            val otherIconSizePx = with(density) { otherIconSize.dp.toPx() }
            val otherCenterX = otherX + otherIconSizePx / 2
            val otherCenterY = otherY + otherIconSizePx / 2
            val otherRight = otherX + otherIconSizePx
            val otherBottom = otherY + otherIconSizePx

            // Vertical alignment checks (center, left, right)
            // Center-to-center alignment
            if (abs(draggingCenterX - otherCenterX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherCenterX))
                // Position dragging app so its center aligns with other app's center
                if (snappedX == null) snappedX = otherCenterX - iconSizePx / 2
            }
            // Left edge alignment
            else if (abs(dragX - otherX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                if (snappedX == null) snappedX = otherX
            }
            // Right edge alignment - both right edges should align
            else if (abs(draggingRight - otherRight) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                // Position dragging app so its right edge aligns with other app's right edge
                if (snappedX == null) snappedX = otherRight - iconSizePx
            }
            // Left edge to right edge (for spacing next to each other)
            else if (abs(dragX - otherRight) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherRight))
                if (snappedX == null) snappedX = otherRight
            }
            // Right edge to left edge (for spacing next to each other)
            else if (abs(draggingRight - otherX) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.VERTICAL, otherX))
                if (snappedX == null) snappedX = otherX - iconSizePx
            }

            // Horizontal alignment checks (center, top, bottom)
            // Center-to-center alignment
            if (abs(draggingCenterY - otherCenterY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherCenterY))
                // Position dragging app so its center aligns with other app's center
                if (snappedY == null) snappedY = otherCenterY - iconSizePx / 2
            }
            // Top edge alignment
            else if (abs(dragY - otherY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                if (snappedY == null) snappedY = otherY
            }
            // Bottom edge alignment - both bottom edges should align
            else if (abs(draggingBottom - otherBottom) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                // Position dragging app so its bottom edge aligns with other app's bottom edge
                if (snappedY == null) snappedY = otherBottom - iconSizePx
            }
            // Top edge to bottom edge (for spacing above/below each other)
            else if (abs(dragY - otherBottom) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherBottom))
                if (snappedY == null) snappedY = otherBottom
            }
            // Bottom edge to top edge (for spacing above/below each other)
            else if (abs(draggingBottom - otherY) < snapThreshold) {
                guides.add(AlignmentGuide(GuideType.HORIZONTAL, otherY))
                if (snappedY == null) snappedY = otherY - iconSizePx
            }

            // Calculate distances to nearby apps
            // Horizontal distance (left/right)
            val horizontalDistance = if (draggingRight < otherX) {
                // Dragging app is to the left
                otherX - draggingRight
            } else if (dragX > otherRight) {
                // Dragging app is to the right
                dragX - otherRight
            } else {
                null // Apps overlap horizontally
            }

            // Vertical distance (top/bottom)
            val verticalDistance = if (draggingBottom < otherY) {
                // Dragging app is above
                otherY - draggingBottom
            } else if (dragY > otherBottom) {
                // Dragging app is below
                dragY - otherBottom
            } else {
                null // Apps overlap vertically
            }

            // Add horizontal distance measurement if within threshold and apps are roughly aligned vertically
            if (horizontalDistance != null && horizontalDistance > 0 && horizontalDistance < distanceThreshold) {
                val verticalOverlap = min(draggingBottom, otherBottom) - max(dragY, otherY)
                if (verticalOverlap > 0) {
                    val measurementY = max(dragY, otherY) + verticalOverlap / 2
                    val startX = if (draggingRight < otherX) draggingRight else dragX
                    val endX = if (draggingRight < otherX) otherX else otherRight

                    distances.add(
                        DistanceMeasurement(
                            startX = startX,
                            startY = measurementY,
                            endX = endX,
                            endY = measurementY,
                            distance = with(density) { horizontalDistance.toDp().value },
                            isHorizontal = true
                        )
                    )
                }
            }

            // Add vertical distance measurement if within threshold and apps are roughly aligned horizontally
            if (verticalDistance != null && verticalDistance > 0 && verticalDistance < distanceThreshold) {
                val horizontalOverlap = min(draggingRight, otherRight) - max(dragX, otherX)
                if (horizontalOverlap > 0) {
                    val measurementX = max(dragX, otherX) + horizontalOverlap / 2
                    val startY = if (draggingBottom < otherY) draggingBottom else dragY
                    val endY = if (draggingBottom < otherY) otherY else otherBottom

                    distances.add(
                        DistanceMeasurement(
                            startX = measurementX,
                            startY = startY,
                            endX = measurementX,
                            endY = endY,
                            distance = with(density) { verticalDistance.toDp().value },
                            isHorizontal = false
                        )
                    )
                }
            }
        }
    }

    // Check alignment with folders
    folders.forEach { folder ->
        // Skip the folder being dragged
        if (folder.id == draggingFolderId) return@forEach

        val folderPosition = folder.position
        val folderX = folderPosition.x
        val folderY = folderPosition.y
        val folderIconSize = folderPosition.iconSize
        val folderIconSizePx = with(density) { folderIconSize.dp.toPx() }
        val folderCenterX = folderX + folderIconSizePx / 2
        val folderCenterY = folderY + folderIconSizePx / 2
        val folderRight = folderX + folderIconSizePx
        val folderBottom = folderY + folderIconSizePx

        // Vertical alignment checks (center, left, right)
        // Center-to-center alignment
        if (abs(draggingCenterX - folderCenterX) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folderCenterX))
            if (snappedX == null) snappedX = folderCenterX - iconSizePx / 2
        }
        // Left edge alignment
        else if (abs(dragX - folderX) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folderX))
            if (snappedX == null) snappedX = folderX
        }
        // Right edge alignment
        else if (abs(draggingRight - folderRight) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folderRight))
            if (snappedX == null) snappedX = folderRight - iconSizePx
        }
        // Left edge to right edge
        else if (abs(dragX - folderRight) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folderRight))
            if (snappedX == null) snappedX = folderRight
        }
        // Right edge to left edge
        else if (abs(draggingRight - folderX) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.VERTICAL, folderX))
            if (snappedX == null) snappedX = folderX - iconSizePx
        }

        // Horizontal alignment checks (center, top, bottom)
        // Center-to-center alignment
        if (abs(draggingCenterY - folderCenterY) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folderCenterY))
            if (snappedY == null) snappedY = folderCenterY - iconSizePx / 2
        }
        // Top edge alignment
        else if (abs(dragY - folderY) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folderY))
            if (snappedY == null) snappedY = folderY
        }
        // Bottom edge alignment
        else if (abs(draggingBottom - folderBottom) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folderBottom))
            if (snappedY == null) snappedY = folderBottom - iconSizePx
        }
        // Top edge to bottom edge
        else if (abs(dragY - folderBottom) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folderBottom))
            if (snappedY == null) snappedY = folderBottom
        }
        // Bottom edge to top edge
        else if (abs(draggingBottom - folderY) < snapThreshold) {
            guides.add(AlignmentGuide(GuideType.HORIZONTAL, folderY))
            if (snappedY == null) snappedY = folderY - iconSizePx
        }

        // Calculate distances to folders
        val horizontalDistance = if (draggingRight < folderX) {
            folderX - draggingRight
        } else if (dragX > folderRight) {
            dragX - folderRight
        } else {
            null
        }

        val verticalDistance = if (draggingBottom < folderY) {
            folderY - draggingBottom
        } else if (dragY > folderBottom) {
            dragY - folderBottom
        } else {
            null
        }

        val distanceThreshold = with(density) { 200.dp.toPx() }

        if (horizontalDistance != null && horizontalDistance > 0 && horizontalDistance < distanceThreshold) {
            val verticalOverlap = min(draggingBottom, folderBottom) - max(dragY, folderY)
            if (verticalOverlap > 0) {
                val measurementY = max(dragY, folderY) + verticalOverlap / 2
                val startX = if (draggingRight < folderX) draggingRight else dragX
                val endX = if (draggingRight < folderX) folderX else folderRight

                distances.add(
                    DistanceMeasurement(
                        startX = startX,
                        startY = measurementY,
                        endX = endX,
                        endY = measurementY,
                        distance = with(density) { horizontalDistance.toDp().value },
                        isHorizontal = true
                    )
                )
            }
        }

        if (verticalDistance != null && verticalDistance > 0 && verticalDistance < distanceThreshold) {
            val horizontalOverlap = min(draggingRight, folderRight) - max(dragX, folderX)
            if (horizontalOverlap > 0) {
                val measurementX = max(dragX, folderX) + horizontalOverlap / 2
                val startY = if (draggingBottom < folderY) draggingBottom else dragY
                val endY = if (draggingBottom < folderY) folderY else folderBottom

                distances.add(
                    DistanceMeasurement(
                        startX = measurementX,
                        startY = startY,
                        endX = measurementX,
                        endY = endY,
                        distance = with(density) { verticalDistance.toDp().value },
                        isHorizontal = false
                    )
                )
            }
        }
    }

    return AlignmentState(
        guides = guides.distinctBy { it.position to it.type },
        snappedX = snappedX,
        snappedY = snappedY,
        distances = distances
    )
}
