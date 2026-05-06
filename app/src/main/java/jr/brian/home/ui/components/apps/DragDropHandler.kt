package jr.brian.home.ui.components.apps

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.model.ItemRect
import jr.brian.home.model.alignment.AlignmentState
import jr.brian.home.model.alignment.calculateAlignmentGuides
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.getAllItemRects
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.util.findNonOverlappingPosition

/**
 * Handles drag and drop position calculations including alignment, snapping,
 * constraints, and collision detection.
 */
class DragDropHandler(
    private val density: Density,
    private val snapThreshold: Float,
    private val borderPadding: Float
) {
    /**
     * Result of processing a drag operation.
     */
    data class DragResult(
        val finalX: Float,
        val finalY: Float,
        val alignmentState: AlignmentState
    )

    /**
     * Process a drag operation for an app, calculating the final position
     * with alignment, snapping, constraints, and collision detection.
     */
    fun processDragForApp(
        dragX: Float,
        dragY: Float,
        draggingIndex: Int,
        iconSize: Float,
        containerSize: IntSize,
        contentHeight: Float,
        apps: List<AppInfo>,
        positions: Map<String, AppPosition>,
        folders: List<Folder>,
        excludePackageName: String,
        snapEnabled: Boolean = true
    ): DragResult {
        val iconSizePx = with(density) { iconSize.dp.toPx() }

        // Always calculate alignment guides for visual display
        val alignment = calculateAlignmentGuides(
            draggingIndex = draggingIndex,
            dragX = dragX,
            dragY = dragY,
            iconSize = iconSize,
            containerSize = containerSize,
            snapThreshold = snapThreshold,
            apps = apps,
            positions = positions,
            density = density,
            borderPadding = borderPadding,
            folders = folders
        )

        // Only apply snapping to position when snapEnabled
        val snappedX = if (snapEnabled) alignment.snappedX ?: dragX else dragX
        val snappedY = if (snapEnabled) alignment.snappedY ?: dragY else dragY

        // Apply container constraints
        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
        val maxY = contentHeight - iconSizePx - borderPadding

        val constrainedX = snappedX.coerceIn(borderPadding, maxX)
        val constrainedY = snappedY.coerceIn(borderPadding, maxY)

        // Check for collisions and adjust position
        val (finalX, finalY) = if (snapEnabled) {
            val allItems = getAllItemRects(apps, positions, folders, density)
            findNonOverlappingPosition(
                targetX = constrainedX,
                targetY = constrainedY,
                targetSize = iconSizePx,
                excludeId = excludePackageName,
                allItems = allItems,
                containerWidth = containerSize.width.toFloat(),
                containerHeight = contentHeight,
                borderPadding = borderPadding
            )
        } else {
            constrainedX to constrainedY
        }

        return DragResult(finalX, finalY, alignment)
    }

    /**
     * Process a drag operation for a folder, calculating the final position
     * with alignment, snapping, constraints, and collision detection.
     */
    fun processDragForFolder(
        dragX: Float,
        dragY: Float,
        iconSize: Float,
        containerSize: IntSize,
        contentHeight: Float,
        apps: List<AppInfo>,
        positions: Map<String, AppPosition>,
        folders: List<Folder>,
        draggingFolderId: String,
        snapEnabled: Boolean = true
    ): DragResult {
        val iconSizePx = with(density) { iconSize.dp.toPx() }

        // Always calculate alignment guides for visual display
        val alignment = calculateAlignmentGuides(
            draggingIndex = -1,
            dragX = dragX,
            dragY = dragY,
            iconSize = iconSize,
            containerSize = containerSize,
            snapThreshold = snapThreshold,
            apps = apps,
            positions = positions,
            density = density,
            borderPadding = borderPadding,
            folders = folders,
            draggingFolderId = draggingFolderId
        )

        // Only apply snapping to position when snapEnabled
        val snappedX = if (snapEnabled) alignment.snappedX ?: dragX else dragX
        val snappedY = if (snapEnabled) alignment.snappedY ?: dragY else dragY

        // Apply container constraints
        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
        val maxY = contentHeight - iconSizePx - borderPadding

        val constrainedX = snappedX.coerceIn(borderPadding, maxX)
        val constrainedY = snappedY.coerceIn(borderPadding, maxY)

        // Check for collisions and adjust position
        val (finalX, finalY) = if (snapEnabled) {
            val allItems = getAllItemRects(apps, positions, folders, density)
            findNonOverlappingPosition(
                targetX = constrainedX,
                targetY = constrainedY,
                targetSize = iconSizePx,
                excludeId = draggingFolderId,
                allItems = allItems,
                containerWidth = containerSize.width.toFloat(),
                containerHeight = contentHeight,
                borderPadding = borderPadding
            )
        } else {
            constrainedX to constrainedY
        }

        return DragResult(finalX, finalY, alignment)
    }

    fun processDragForRom(
        dragX: Float,
        dragY: Float,
        draggingRomKey: String,
        iconSize: Float,
        containerSize: IntSize,
        contentHeight: Float,
        apps: List<AppInfo>,
        positions: Map<String, AppPosition>,
        folders: List<Folder>,
        pinnedRoms: List<PinnedRomInfo>,
        snapEnabled: Boolean = true
    ): DragResult {
        val iconSizePx = with(density) { iconSize.dp.toPx() }

        val alignment = calculateAlignmentGuides(
            draggingIndex = -1,
            dragX = dragX,
            dragY = dragY,
            iconSize = iconSize,
            containerSize = containerSize,
            snapThreshold = snapThreshold,
            apps = apps,
            positions = positions,
            density = density,
            borderPadding = borderPadding,
            folders = folders,
            draggingRomKey = draggingRomKey,
            pinnedRoms = pinnedRoms
        )

        val snappedX = if (snapEnabled) alignment.snappedX ?: dragX else dragX
        val snappedY = if (snapEnabled) alignment.snappedY ?: dragY else dragY

        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
        val maxY = contentHeight - iconSizePx - borderPadding

        val constrainedX = snappedX.coerceIn(borderPadding, maxX)
        val constrainedY = snappedY.coerceIn(borderPadding, maxY)

        val (finalX, finalY) = if (snapEnabled) {
            val allItems = buildList {
                addAll(getAllItemRects(apps, positions, folders, density))
                pinnedRoms.forEach { rom ->
                    if (rom.key == draggingRomKey) return@forEach
                    val pos = positions[rom.key] ?: return@forEach
                    val romIconSizePx = with(density) { pos.iconSize.dp.toPx() }
                    add(ItemRect(rom.key, pos.x, pos.y, romIconSizePx, romIconSizePx))
                }
            }
            findNonOverlappingPosition(
                targetX = constrainedX,
                targetY = constrainedY,
                targetSize = iconSizePx,
                excludeId = draggingRomKey,
                allItems = allItems,
                containerWidth = containerSize.width.toFloat(),
                containerHeight = contentHeight,
                borderPadding = borderPadding
            )
        } else {
            constrainedX to constrainedY
        }

        return DragResult(finalX, finalY, alignment)
    }
}
