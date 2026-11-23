package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.home.canvas.grid.GridSolver
import jr.brian.home.canvas.grid.LayoutSnapshot
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.grid.toSnapshot
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridRect

private const val MAX_PUSH_AXIS_SPAN = 12

internal enum class CanvasResizeAxis { Width, Height }

/**
 * One pad grab. Every step re-solves from the frozen [baseline], so backing off a move restores
 * the neighbors, and cancelling only has to drop this object.
 */
internal data class CanvasGrab(
    val itemId: String,
    val baseline: LayoutSnapshot,
    val reserved: List<GridRect>,
    val minColSpan: Int,
    val minRowSpan: Int,
    val cursor: GridRect,
    val axis: CanvasResizeAxis = CanvasResizeAxis.Width,
    val preview: LayoutSnapshot = baseline,
) {
    val rect: GridRect get() = preview.placements[itemId] ?: cursor

    val hasChanges: Boolean get() = preview.placements != baseline.placements

    fun moved(deltaCol: Int, deltaRow: Int): CanvasGrab =
        solved(fit(cursor.withOrigin(cursor.col + deltaCol, cursor.row + deltaRow)))

    fun resized(delta: Int): CanvasGrab {
        val next = when (axis) {
            CanvasResizeAxis.Width -> cursor.withSize(spanAfter(cursor.colSpan, delta, minColSpan), cursor.rowSpan)
            CanvasResizeAxis.Height -> cursor.withSize(cursor.colSpan, spanAfter(cursor.rowSpan, delta, minRowSpan))
        }
        return solved(fit(next))
    }

    fun axisToggled(): CanvasGrab = copy(
        axis = if (axis == CanvasResizeAxis.Width) CanvasResizeAxis.Height else CanvasResizeAxis.Width,
    )

    private fun spanAfter(span: Int, delta: Int, min: Int): Int =
        (span + delta).coerceIn(min.coerceAtMost(MAX_PUSH_AXIS_SPAN), MAX_PUSH_AXIS_SPAN)

    private fun fit(target: GridRect): GridRect {
        val cross = baseline.config.crossAxisCount
        return when (baseline.config.pushDirection) {
            PushDirection.DOWN -> {
                val colSpan = target.colSpan.coerceAtMost(cross)
                target.copy(
                    col = target.col.coerceIn(0, cross - colSpan),
                    row = target.row.coerceIn(0, pushLimit()),
                    colSpan = colSpan,
                )
            }
            PushDirection.RIGHT -> {
                val rowSpan = target.rowSpan.coerceAtMost(cross)
                target.copy(
                    col = target.col.coerceIn(0, pushLimit()),
                    row = target.row.coerceIn(0, cross - rowSpan),
                    rowSpan = rowSpan,
                )
            }
        }
    }

    /** One step past the furthest neighbor, so a held direction can't run off into empty space. */
    private fun pushLimit(): Int {
        val down = baseline.config.pushDirection == PushDirection.DOWN
        val furthest = baseline.placements
            .filterKeys { it != itemId }
            .values
            .maxOfOrNull { if (down) it.bottom else it.right } ?: 0
        val own = baseline.placements[itemId]?.let { if (down) it.row else it.col } ?: 0
        return maxOf(furthest, own)
    }

    private fun solved(target: GridRect): CanvasGrab {
        val result = GridSolver.solveResize(
            baseline = baseline,
            itemId = itemId,
            newRect = target,
            minColSpan = minColSpan,
            minRowSpan = minRowSpan,
            reservedRects = reserved,
        ).snapshot
        val placed = result.placements[itemId] ?: target
        return copy(
            cursor = target.withSize(placed.colSpan, placed.rowSpan),
            preview = result,
        )
    }
}

internal fun beginCanvasGrab(
    layout: CanvasLayout,
    itemId: String,
    minColSpan: Int = 1,
    minRowSpan: Int = 1,
): CanvasGrab? {
    val baseline = layout.toSnapshot()
    val rect = baseline.placements[itemId] ?: return null
    return CanvasGrab(
        itemId = itemId,
        baseline = baseline,
        reserved = reservedRectsForActive(layout),
        minColSpan = minColSpan.coerceAtLeast(1),
        minRowSpan = minRowSpan.coerceAtLeast(1),
        cursor = rect,
    )
}

/** Edit state for the page on screen. It is never saved, so Classic's edit flag stays untouched. */
@Stable
internal class CanvasEditor {
    var editing by mutableStateOf(false)
        private set

    var grab by mutableStateOf<CanvasGrab?>(null)
        private set

    /** Holds a dropped layout on screen until the saved one arrives, so tiles never jump back. */
    var settled by mutableStateOf<Map<String, GridRect>?>(null)

    fun placements(layout: CanvasLayout): Map<String, GridRect> =
        grab?.preview?.placements ?: settled ?: layout.activeArrangement

    fun enter() {
        editing = true
    }

    fun exit() {
        grab = null
        editing = false
    }

    fun begin(layout: CanvasLayout, itemId: String, minColSpan: Int, minRowSpan: Int) {
        if (editing && settled == null) grab = beginCanvasGrab(layout, itemId, minColSpan, minRowSpan)
    }

    fun step(transform: CanvasGrab.() -> CanvasGrab) {
        grab = grab?.transform()
    }

    fun cancelGrab() {
        grab = null
    }

    fun drop(commit: (Map<String, GridRect>) -> Unit) {
        val finished = grab ?: return
        grab = null
        if (finished.hasChanges) {
            settled = finished.preview.placements
            commit(finished.preview.placements)
        }
    }
}
