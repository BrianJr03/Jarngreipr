package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect

/**
 * Cells the canvas treats as **always occupied** for [orientation], reserved for
 * UI affordances that live inside the grid (today: the inline "+" add tile).
 *
 * The reservation is purely solver-level: it shapes auto-placement, drag, and
 * resize identically without persisting any phantom item. Reserving in *both*
 * arrangements means the corner is protected whether the user is in vertical
 * or horizontal scroll mode and prevents the inactive arrangement from
 * silently growing a collision that surfaces on the next orientation switch.
 *
 * - Vertical scroll: top-right cell `(col = columns-1, row = 0)`.
 * - Horizontal scroll: top-left cell `(col = 0, row = 0)` — that's the corner
 *   that's always visible at scroll-origin in horizontal mode.
 */
fun reservedRectsFor(
    layout: CanvasLayout,
    orientation: CanvasScrollOrientation
): List<GridRect> = when (orientation) {
    CanvasScrollOrientation.VERTICAL -> {
        val col = (layout.verticalColumns - 1).coerceAtLeast(0)
        listOf(GridRect(col, 0, 1, 1))
    }
    CanvasScrollOrientation.HORIZONTAL -> {
        listOf(GridRect(0, 0, 1, 1))
    }
}

/** Convenience overload for the layout's active orientation. */
fun reservedRectsForActive(layout: CanvasLayout): List<GridRect> =
    reservedRectsFor(layout, layout.activeOrientation)
