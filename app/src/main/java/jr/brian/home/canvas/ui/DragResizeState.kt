package jr.brian.home.canvas.ui

import jr.brian.home.canvas.grid.LayoutSnapshot
import jr.brian.home.canvas.model.GridRect

/**
 * Live state of an active canvas gesture. The [baseline] snapshot is captured
 * at gesture start and is **immutable for the gesture's lifetime** — every
 * preview re-solves from baseline plus the cumulative cell delta, never from
 * the previous frame's solved result. That property is what makes back-off
 * mid-gesture return neighbors to their starting cells.
 *
 * This struct updates only on **cell-boundary crossings** (start, target
 * change, end). The raw per-frame pointer offset for the dragged tile lives
 * in a separate `mutableStateOf` so 60fps pointer events don't recompose the
 * whole grid — see `CanvasGridLayout`'s `pointerOffsetState`.
 *
 * [Mode.Move] uses the same struct: [baselineRect] holds the item's starting
 * rect, [targetRect] gets a new (col, row) on each cell crossing, and the
 * solver call is [jr.brian.home.canvas.grid.GridSolver.solveMove].
 *
 * [Mode.Resize] reuses [baselineRect] as the start; [targetRect] grows/shrinks
 * its colSpan/rowSpan as the user drags the corner handle, and the solver
 * call is [jr.brian.home.canvas.grid.GridSolver.solveResize] with widget
 * minimum spans applied.
 */
internal data class CanvasGestureState(
    val mode: Mode,
    val draggedId: String,
    val baseline: LayoutSnapshot,
    val baselineRect: GridRect,
    val minColSpan: Int = 1,
    val minRowSpan: Int = 1,
    val previewSnapshot: LayoutSnapshot = baseline,
    val targetRect: GridRect = baselineRect
) {
    enum class Mode { Move, Resize }
}
