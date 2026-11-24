package jr.brian.jarnfe.canvas

import androidx.compose.ui.geometry.Offset
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import kotlin.math.floor

/** The spot a hold on an empty cell chose, kept with its page so it can't land on another one. */
internal data class CanvasAddTarget(val pageIndex: Int, val cell: GridCell)

/** A tile that was just added and is waiting for its grab, at [cell] when one was chosen. */
internal data class CanvasPendingGrab(val itemId: String, val cell: GridCell? = null)

/** The free cell under [point], or null over a tile, a gap, the menu cell, or outside the grid. */
internal fun canvasEmptyCell(
    board: CanvasBoardGeometry,
    layout: CanvasLayout,
    placements: Map<String, GridRect>,
    point: Offset,
): GridCell? {
    if (board.pitch <= 0f) return null
    val col = floor((point.x - board.paddingPx) / board.pitch).toInt()
    val row = floor((point.y - board.paddingPx) / board.pitch).toInt()
    if (col < 0 || row < 0) return null
    val cross = layout.activeCrossAxis.coerceAtLeast(1)
    val crossIndex = if (PushDirection.from(layout.activeOrientation) == PushDirection.DOWN) col else row
    if (crossIndex >= cross) return null
    val rect = GridRect(col, row, 1, 1)
    if (!board.contains(rect, point)) return null
    val cell = GridCell(col, row)
    val taken = placements.values.any { it.contains(cell) } ||
        reservedRectsForActive(layout).any { it.contains(cell) }
    return cell.takeUnless { taken }
}
