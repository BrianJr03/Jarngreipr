package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect

/**
 * Pure, deterministic occupancy solver for the canvas grid. Every call returns
 * a complete new layout; nothing is mutated.
 *
 * Gestures should always pass the *committed* baseline snapshot and the current
 * cumulative target — never the result of the previous frame's solve. This
 * guarantees identical input → identical output and a clean return to baseline
 * if the user backs off mid-gesture.
 */
object GridSolver {

    /**
     * Move [itemId] so its top-left lands on [target]. Overlapped neighbors
     * are pushed along the push axis (cascading deterministically). The item's
     * size is preserved. The target origin is clamped so the rect stays within
     * the cross-axis bound.
     */
    fun solveMove(
        baseline: LayoutSnapshot,
        itemId: String,
        target: GridCell
    ): SolveResult {
        val current = baseline.placements[itemId] ?: return SolveResult(baseline, emptySet())
        val moved = clampOrigin(current.withOrigin(target.col, target.row), baseline.config)
        return resolve(baseline, itemId, moved, current)
    }

    /**
     * Resize [itemId] to [newRect], honoring [minColSpan]/[minRowSpan] and the
     * cross-axis bound. Overlapped neighbors are pushed along the push axis.
     */
    fun solveResize(
        baseline: LayoutSnapshot,
        itemId: String,
        newRect: GridRect,
        minColSpan: Int = 1,
        minRowSpan: Int = 1
    ): SolveResult {
        require(minColSpan >= 1 && minRowSpan >= 1) {
            "min span must be >= 1; got col=$minColSpan, row=$minRowSpan"
        }
        val current = baseline.placements[itemId]
        val resized = clampToBounds(newRect, baseline.config, minColSpan, minRowSpan)
        return resolve(baseline, itemId, resized, current)
    }

    /**
     * Pull items toward the grid origin along the push axis, preserving their
     * cross-axis column and relative order. The only operation allowed to
     * close gaps — invoke from explicit user "Tidy / Compact" action only.
     */
    fun compact(snapshot: LayoutSnapshot): LayoutSnapshot {
        val dir = snapshot.config.pushDirection
        val sorted = snapshot.placements.entries.sortedWith(
            compareBy(
                { it.value.pushAxis(dir) },
                { it.value.crossAxis(dir) },
                { it.key }
            )
        )
        val placed = LinkedHashMap<String, GridRect>()
        for ((id, rect) in sorted) {
            placed[id] = pullToOrigin(rect, placed.values, dir)
        }
        return snapshot.copy(placements = placed)
    }

    private fun resolve(
        baseline: LayoutSnapshot,
        anchorId: String,
        anchorRect: GridRect,
        originalRect: GridRect?
    ): SolveResult {
        val grid = OccupancyGrid(baseline.config, baseline.placements)
        grid.place(anchorId, anchorRect)

        val moved = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.addLast(anchorId)

        var safety = 0
        val maxIters = (grid.ids.size + 1) * (grid.ids.size + 1) + 1024

        while (queue.isNotEmpty()) {
            if (safety++ > maxIters) error("GridSolver failed to terminate after $safety iterations")
            val displacerId = queue.removeFirst()
            val displacerRect = grid.rectOf(displacerId) ?: continue

            for ((id, rect) in grid.overlapping(displacerRect, excludeId = displacerId)) {
                val pushed = pushPast(rect, displacerRect, baseline.config.pushDirection)
                grid.place(id, pushed)
                moved.add(id)
                queue.addLast(id)
            }
        }

        if (originalRect != null && anchorRect != originalRect) moved.add(anchorId)
        return SolveResult(baseline.copy(placements = grid.snapshot()), moved)
    }

    private fun clampOrigin(rect: GridRect, config: GridConfig): GridRect = when (config.pushDirection) {
        PushDirection.DOWN -> {
            val maxCol = (config.crossAxisCount - rect.colSpan).coerceAtLeast(0)
            rect.copy(
                col = rect.col.coerceIn(0, maxCol),
                row = rect.row.coerceAtLeast(0)
            )
        }
        PushDirection.RIGHT -> {
            val maxRow = (config.crossAxisCount - rect.rowSpan).coerceAtLeast(0)
            rect.copy(
                col = rect.col.coerceAtLeast(0),
                row = rect.row.coerceIn(0, maxRow)
            )
        }
    }

    private fun clampToBounds(
        rect: GridRect,
        config: GridConfig,
        minColSpan: Int,
        minRowSpan: Int
    ): GridRect = when (config.pushDirection) {
        PushDirection.DOWN -> {
            val cross = config.crossAxisCount
            val minCol = minColSpan.coerceAtMost(cross)
            val colSpan = rect.colSpan.coerceIn(minCol, cross)
            val maxCol = (cross - colSpan).coerceAtLeast(0)
            val col = rect.col.coerceIn(0, maxCol)
            val rowSpan = rect.rowSpan.coerceAtLeast(minRowSpan)
            val row = rect.row.coerceAtLeast(0)
            GridRect(col, row, colSpan, rowSpan)
        }
        PushDirection.RIGHT -> {
            val cross = config.crossAxisCount
            val minRow = minRowSpan.coerceAtMost(cross)
            val rowSpan = rect.rowSpan.coerceIn(minRow, cross)
            val maxRow = (cross - rowSpan).coerceAtLeast(0)
            val row = rect.row.coerceIn(0, maxRow)
            val colSpan = rect.colSpan.coerceAtLeast(minColSpan)
            val col = rect.col.coerceAtLeast(0)
            GridRect(col, row, colSpan, rowSpan)
        }
    }

    private fun pushPast(target: GridRect, displacer: GridRect, dir: PushDirection): GridRect =
        when (dir) {
            PushDirection.DOWN -> target.withOrigin(target.col, displacer.bottom)
            PushDirection.RIGHT -> target.withOrigin(displacer.right, target.row)
        }

    private fun pullToOrigin(
        rect: GridRect,
        placed: Collection<GridRect>,
        dir: PushDirection
    ): GridRect {
        var pos = 0
        val limit = 100_000
        while (pos <= limit) {
            val candidate = when (dir) {
                PushDirection.DOWN -> rect.withOrigin(rect.col, pos)
                PushDirection.RIGHT -> rect.withOrigin(pos, rect.row)
            }
            if (placed.none { it.overlaps(candidate) }) return candidate
            pos++
        }
        error("compact runaway for rect=$rect")
    }
}
