package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridRect

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
 * Pure value snapshot of one orientation's grid — what the solver operates on.
 * Carries grid config plus a map of item id → placement.
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

/**
 * Snapshot of the currently-rendered orientation. Solver gestures see only the
 * active arrangement — moves/resizes never perturb the inactive one.
 */
fun CanvasLayout.toSnapshot(): LayoutSnapshot {
    val direction = PushDirection.from(activeOrientation)
    return LayoutSnapshot(
        config = GridConfig(activeCrossAxis, direction),
        placements = activeArrangement
    )
}

/**
 * Write [snapshot]'s placements back into the active orientation's arrangement,
 * leaving the other orientation untouched. Items in this layout with no
 * placement in [snapshot] keep their existing rect (defensive — solver always
 * returns all placements, but this keeps invariant preservation explicit).
 */
fun CanvasLayout.withSnapshot(snapshot: LayoutSnapshot): CanvasLayout {
    val existing = activeArrangement
    val updated = existing.toMutableMap()
    snapshot.placements.forEach { (id, rect) -> updated[id] = rect }
    return withArrangement(activeOrientation, updated)
}
