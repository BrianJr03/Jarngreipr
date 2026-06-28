package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.withPlacement

/**
 * Grid dimensions and push axis for a single canvas page. The cross axis is
 * bounded by [crossAxisCount]; the push axis is unbounded.
 */
data class GridConfig(
    val crossAxisCount: Int,
    val pushDirection: PushDirection
) {
    init {
        require(crossAxisCount >= 1) { "crossAxisCount must be >= 1, was $crossAxisCount" }
    }
}

/**
 * Pure value snapshot of a layout — what the solver operates on. Carries grid
 * config plus a map of item id → placement.
 */
data class LayoutSnapshot(
    val config: GridConfig,
    val placements: Map<String, GridRect>
)

/**
 * Output of a single solve. [snapshot] is the new layout; [movedIds] is the
 * set of ids whose placement changed (anchor included if its rect moved).
 */
data class SolveResult(
    val snapshot: LayoutSnapshot,
    val movedIds: Set<String>
)

fun CanvasLayout.toSnapshot(): LayoutSnapshot {
    val direction = PushDirection.from(orientation)
    val crossAxis = when (direction) {
        PushDirection.DOWN -> columns
        PushDirection.RIGHT -> rows
    }
    val placements = items.associate {
        it.id to GridRect(it.col, it.row, it.colSpan, it.rowSpan)
    }
    return LayoutSnapshot(GridConfig(crossAxis, direction), placements)
}

fun CanvasLayout.withSnapshot(snapshot: LayoutSnapshot): CanvasLayout {
    val updated = items.map { item ->
        val rect = snapshot.placements[item.id] ?: return@map item
        item.withPlacement(rect.col, rect.row, rect.colSpan, rect.rowSpan)
    }
    return copy(items = updated)
}
