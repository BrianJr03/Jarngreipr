package jr.brian.home.ui.components.apps

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import jr.brian.home.data.SNAP_GRID_STEP
import jr.brian.home.data.SnapMode
import jr.brian.home.model.ItemRect
import jr.brian.home.model.alignment.AlignmentState
import jr.brian.home.model.alignment.calculateAlignmentGuides
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.model.getAllItemRects
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.util.findNonOverlappingPosition
import kotlin.math.roundToInt

/**
 * Handles drag and drop position calculations including alignment, snapping,
 * constraints, and collision detection.
 */
class DragDropHandler(
    private val density: Density,
    private val snapThreshold: Float,
    private val borderPadding: Float
) {
    private val gridStepPx: Float = with(density) { SNAP_GRID_STEP.toPx() }

    data class DragResult(
        val finalX: Float,
        val finalY: Float,
        val alignmentState: AlignmentState
    )

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
        snapMode: SnapMode = SnapMode.ICON
    ): DragResult {
        val iconSizePx = with(density) { iconSize.dp.toPx() }

        val alignment = if (snapMode == SnapMode.ICON) {
            calculateAlignmentGuides(
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
        } else {
            AlignmentState()
        }

        val snappedX = computeSnappedX(snapMode, alignment, dragX, iconSizePx)
        val snappedY = computeSnappedY(snapMode, alignment, dragY, iconSizePx)

        val (constrainedX, constrainedY) = applyContainerConstraints(
            snappedX, snappedY, iconSizePx, containerSize, contentHeight
        )

        val (finalX, finalY) = if (snapMode != SnapMode.OFF) {
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
        snapMode: SnapMode = SnapMode.ICON
    ): DragResult {
        val iconSizePx = with(density) { iconSize.dp.toPx() }

        val alignment = if (snapMode == SnapMode.ICON) {
            calculateAlignmentGuides(
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
        } else {
            AlignmentState()
        }

        val snappedX = computeSnappedX(snapMode, alignment, dragX, iconSizePx)
        val snappedY = computeSnappedY(snapMode, alignment, dragY, iconSizePx)

        val (constrainedX, constrainedY) = applyContainerConstraints(
            snappedX, snappedY, iconSizePx, containerSize, contentHeight
        )

        val (finalX, finalY) = if (snapMode != SnapMode.OFF) {
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
        snapMode: SnapMode = SnapMode.ICON
    ): DragResult {
        val iconSizePx = with(density) { iconSize.dp.toPx() }

        val alignment = if (snapMode == SnapMode.ICON) {
            calculateAlignmentGuides(
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
        } else {
            AlignmentState()
        }

        val snappedX = computeSnappedX(snapMode, alignment, dragX, iconSizePx)
        val snappedY = computeSnappedY(snapMode, alignment, dragY, iconSizePx)

        val (constrainedX, constrainedY) = applyContainerConstraints(
            snappedX, snappedY, iconSizePx, containerSize, contentHeight
        )

        val (finalX, finalY) = if (snapMode != SnapMode.OFF) {
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

    private fun computeSnappedX(snapMode: SnapMode, alignment: AlignmentState, dragX: Float, iconSizePx: Float): Float =
        when (snapMode) {
            SnapMode.ICON -> alignment.snappedX ?: dragX
            SnapMode.GRID -> snapToGridCentered(dragX, iconSizePx)
            SnapMode.OFF -> dragX
        }

    private fun computeSnappedY(snapMode: SnapMode, alignment: AlignmentState, dragY: Float, iconSizePx: Float): Float =
        when (snapMode) {
            SnapMode.ICON -> alignment.snappedY ?: dragY
            SnapMode.GRID -> snapToGridCentered(dragY, iconSizePx)
            SnapMode.OFF -> dragY
        }

    private fun snapToGridCentered(coord: Float, iconSizePx: Float): Float {
        val center = coord + iconSizePx / 2f
        val centerOffset = center - borderPadding
        val cellIndex = (centerOffset / gridStepPx - 0.5f).roundToInt()
        val snappedCenter = borderPadding + (cellIndex + 0.5f) * gridStepPx
        return snappedCenter - iconSizePx / 2f
    }

    private fun applyContainerConstraints(
        x: Float,
        y: Float,
        iconSizePx: Float,
        containerSize: IntSize,
        contentHeight: Float
    ): Pair<Float, Float> {
        val maxX = containerSize.width.toFloat() - iconSizePx - borderPadding
        val maxY = contentHeight - iconSizePx - borderPadding
        return x.coerceIn(borderPadding, maxX) to y.coerceIn(borderPadding, maxY)
    }

}
