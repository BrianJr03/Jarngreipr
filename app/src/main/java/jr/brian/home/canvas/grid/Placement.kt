package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect

/**
 * Lowest cross-axis-first free cell that can hold a [colSpan] × [rowSpan]
 * rect, scanning along the push axis from the origin. Used to anchor new
 * items into an arrangement deterministically.
 *
 * [reservedRects] are treated exactly like [occupied]: candidates that
 * overlap them are skipped, so auto-placement never lands on a UI-reserved
 * cell (e.g. the floating add-icon corner).
 */
fun firstFreeRect(
    occupied: Collection<GridRect>,
    crossAxisCount: Int,
    pushDirection: PushDirection,
    colSpan: Int = 1,
    rowSpan: Int = 1,
    reservedRects: Collection<GridRect> = emptyList()
): GridRect {
    val cellSpan = when (pushDirection) {
        PushDirection.DOWN -> colSpan
        PushDirection.RIGHT -> rowSpan
    }
    val maxCross = (crossAxisCount - cellSpan).coerceAtLeast(0)
    val occupiedList = occupied.toList()
    val reservedList = reservedRects.toList()
    var push = 0
    val limit = 100_000
    while (push <= limit) {
        for (cross in 0..maxCross) {
            val rect = when (pushDirection) {
                PushDirection.DOWN -> GridRect(cross, push, colSpan, rowSpan)
                PushDirection.RIGHT -> GridRect(push, cross, colSpan, rowSpan)
            }
            if (occupiedList.none { it.overlaps(rect) } &&
                reservedList.none { it.overlaps(rect) }
            ) return rect
        }
        push++
    }
    return GridRect(0, 0, colSpan, rowSpan)
}

/**
 * Lowest cross-axis-first 1×1 free cell, scanning along the push axis from
 * the origin. Convenience over [firstFreeRect] for single-cell anchors.
 */
fun firstFreeCell(
    occupied: Collection<GridRect>,
    crossAxisCount: Int,
    pushDirection: PushDirection
): GridCell {
    val rect = firstFreeRect(occupied, crossAxisCount, pushDirection, colSpan = 1, rowSpan = 1)
    return GridCell(rect.col, rect.row)
}
