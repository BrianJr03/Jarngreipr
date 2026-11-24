package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.grid.GridSolver
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.firstFreeCell
import jr.brian.home.canvas.grid.reservedRectsFor
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.grid.toSnapshot
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect

sealed interface JarnCanvasSetupChange {
    /** Rows on a sideways page, columns on a page that scrolls down. */
    data class CrossAxis(val orientation: CanvasScrollOrientation, val count: Int) : JarnCanvasSetupChange

}

enum class JarnCanvasSetupRefusal { TilesPastEdge, MenuCellTaken, NotCanvas }

sealed interface JarnCanvasSetupResult {
    data class Applied(val layout: CanvasLayout) : JarnCanvasSetupResult

    data class Refused(val reason: JarnCanvasSetupRefusal) : JarnCanvasSetupResult
}

/** Never hides or drops a tile. A change that would is refused and the layout stays as it was. */
fun canvasLayoutAfterSetup(layout: CanvasLayout, change: JarnCanvasSetupChange): JarnCanvasSetupResult =
    when (change) {
        is JarnCanvasSetupChange.CrossAxis -> layout.withCrossAxis(change.orientation, change.count)
    }

private fun CanvasLayout.withCrossAxis(orientation: CanvasScrollOrientation, requested: Int): JarnCanvasSetupResult {
    val count = requested.coerceIn(CanvasLayout.MIN_AXIS, CanvasLayout.MAX_AXIS)
    val current = crossAxisFor(orientation)
    if (count == current) return JarnCanvasSetupResult.Applied(this)
    val placed = arrangementFor(orientation).values
    if (count < current && placed.any { it.crossEnd(orientation) > count }) {
        return JarnCanvasSetupResult.Refused(JarnCanvasSetupRefusal.TilesPastEdge)
    }
    val next = when (orientation) {
        CanvasScrollOrientation.VERTICAL -> copy(verticalColumns = count)
        CanvasScrollOrientation.HORIZONTAL -> copy(horizontalRows = count)
    }
    val reserved = reservedRectsFor(next, orientation)
    val menuMoved = reserved != reservedRectsFor(this, orientation)
    if (menuMoved && placed.any { rect -> reserved.any(rect::overlaps) }) {
        return JarnCanvasSetupResult.Refused(JarnCanvasSetupRefusal.MenuCellTaken)
    }
    return JarnCanvasSetupResult.Applied(next)
}

/** A tile in the way on screen is the user's to move. One in the unseen orientation is moved for them. */
private fun GridRect.crossEnd(orientation: CanvasScrollOrientation): Int = when (orientation) {
    CanvasScrollOrientation.VERTICAL -> right
    CanvasScrollOrientation.HORIZONTAL -> bottom
}

/** Classic's Tidy for the active orientation, or null when there are no gaps to close. */
internal fun canvasTidied(layout: CanvasLayout): Map<String, GridRect>? {
    val before = layout.activeArrangement
    val after = GridSolver.compact(layout.toSnapshot(), reservedRectsForActive(layout)).placements
    return after.takeIf { it.keys == before.keys && it != before }
}

/** Saves the tidied page and opens the normal undo window. False when it was already tidy. */
internal fun tidyCanvasPage(undo: CanvasUndoController, writes: CanvasUndoWrites, page: JarnCanvasPage): Boolean {
    val after = canvasTidied(page.layout) ?: return false
    val orientation = page.layout.activeOrientation
    writes.commit(page.index, orientation, after)
    undo.recordLayout(
        writes,
        CanvasLayoutChange(page.index, orientation, page.layout.activeArrangement, after, CanvasUndoKind.Tidied),
    )
    return true
}

/** Where the first tile of an empty page lands, clear of the menu tile. */
internal fun canvasFirstOpenCell(layout: CanvasLayout): GridCell = firstFreeCell(
    occupied = layout.activeArrangement.values + reservedRectsForActive(layout),
    crossAxisCount = layout.activeCrossAxis.coerceAtLeast(1),
    pushDirection = PushDirection.from(layout.activeOrientation),
)
