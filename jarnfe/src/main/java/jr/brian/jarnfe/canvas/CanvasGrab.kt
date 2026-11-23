package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.grid.GridSolver
import jr.brian.home.canvas.grid.LayoutSnapshot
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.grid.toSnapshot
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect

internal const val MAX_PUSH_AXIS_SPAN = 12

internal enum class CanvasResizeAxis { Width, Height }

internal enum class CanvasGrabDriver { Controller, Touch }

/** The page a grab started on, frozen so a cancel can always return to exactly this layout. */
internal data class CanvasGrabHome(
    val pageIndex: Int,
    val baseline: LayoutSnapshot,
    val reserved: List<GridRect>,
    val rect: GridRect,
    val visiblePushCells: Int,
    val orientation: CanvasScrollOrientation = CanvasScrollOrientation.HORIZONTAL,
)

internal data class CanvasPageWrite(val pageIndex: Int, val placements: Map<String, GridRect>)

internal sealed interface CanvasGrabDrop {
    data object Unchanged : CanvasGrabDrop

    data class SamePage(val write: CanvasPageWrite) : CanvasGrabDrop

    /** [writes] puts the destination first, so a failed save can duplicate a tile but never lose one. */
    data class BetweenPages(
        val itemId: String,
        val destination: CanvasPageWrite,
        val source: CanvasPageWrite,
        val isNewTile: Boolean = false,
    ) : CanvasGrabDrop {
        val writes: List<CanvasPageWrite> get() = listOf(destination, source)
    }
}

/**
 * One grab, driven by touch or controller. Every step re-solves from the frozen [baseline], so
 * backing off a move restores the neighbors, and cancelling only has to drop this object.
 */
internal data class CanvasGrab(
    val itemId: String,
    val baseline: LayoutSnapshot,
    val reserved: List<GridRect>,
    val minColSpan: Int,
    val minRowSpan: Int,
    val cursor: GridRect,
    val home: CanvasGrabHome,
    val pageIndex: Int = home.pageIndex,
    val driver: CanvasGrabDriver = CanvasGrabDriver.Controller,
    val axis: CanvasResizeAxis = CanvasResizeAxis.Width,
    val preview: LayoutSnapshot = baseline,
    val visiblePushCells: Int = 0,
    val isNewTile: Boolean = false,
    val isFolder: Boolean = false,
    val leaveEditOnEnd: Boolean = false,
    val resizeCorner: CanvasResizeCorner? = null,
    val refusal: CanvasMoveRefusal? = null,
) {
    val rect: GridRect get() = preview.placements[itemId] ?: cursor

    val hasChanges: Boolean get() = preview.placements != baseline.placements

    val isAwayFromHome: Boolean get() = pageIndex != home.pageIndex

    /** Steps from where the tile sits, so a cell the solver refused never swallows a press. */
    fun moved(deltaCol: Int, deltaRow: Int): CanvasGrab =
        steppedTo(rect.col + deltaCol, rect.row + deltaRow)

    fun movedTo(col: Int, row: Int): CanvasGrab {
        val solvedMove = solved(fit(rect.withOrigin(col, row)))
        return solvedMove.copy(cursor = solvedMove.rect)
    }

    /** A move the player asked for, which remembers why the tile didn't get there. */
    fun steppedTo(col: Int, row: Int): CanvasGrab {
        val requested = rect.withOrigin(col, row)
        val placed = movedTo(col, row)
        return placed.copy(refusal = canvasMoveRefusal(requested, fit(requested), placed.rect, reserved))
    }

    fun resized(delta: Int): CanvasGrab {
        val next = when (axis) {
            CanvasResizeAxis.Width -> cursor.withSize(spanAfter(cursor.colSpan, delta, minColSpan), cursor.rowSpan)
            CanvasResizeAxis.Height -> cursor.withSize(cursor.colSpan, spanAfter(cursor.rowSpan, delta, minRowSpan))
        }
        val result = solved(fit(next))
        val refusal = when (axis) {
            CanvasResizeAxis.Width -> canvasSpanRefusal(cursor.colSpan + delta, result.cursor.colSpan)
            CanvasResizeAxis.Height -> canvasSpanRefusal(cursor.rowSpan + delta, result.cursor.rowSpan)
        }
        return result.copy(refusal = refusal)
    }

    fun resizedTo(target: GridRect, refusal: CanvasMoveRefusal? = null): CanvasGrab = solved(
        fit(
            target.withSize(
                spanAfter(target.colSpan, 0, minColSpan),
                spanAfter(target.rowSpan, 0, minRowSpan),
            ),
        ),
    ).copy(refusal = refusal)

    /** Puts every neighbor back while the tile hovers over the remove bar. */
    fun previewCleared(): CanvasGrab =
        copy(preview = baseline, cursor = baseline.placements[itemId] ?: cursor, refusal = null)

    fun axisToggled(): CanvasGrab = copy(
        axis = if (axis == CanvasResizeAxis.Width) CanvasResizeAxis.Height else CanvasResizeAxis.Width,
        refusal = null,
    )

    fun takenOverBy(next: CanvasGrabDriver): CanvasGrab = copy(driver = next)

    /**
     * Carries the grab to [target], or null when it can't go there. Nothing is saved, and the
     * neighbors only move once the tile is placed. Going back home restores the frozen layout.
     */
    fun changedPage(target: JarnCanvasPage): CanvasGrab? = when {
        target.index == pageIndex -> this
        isFolder -> null
        target.index == home.pageIndex -> copy(
            pageIndex = home.pageIndex,
            baseline = home.baseline,
            reserved = home.reserved,
            cursor = home.rect,
            preview = home.baseline,
            visiblePushCells = home.visiblePushCells,
            refusal = null,
        )
        !target.canReceive(itemId) -> null
        else -> arrivedOn(target)
    }

    fun dropResult(): CanvasGrabDrop = when {
        !isAwayFromHome ->
            if (preview.placements == home.baseline.placements) {
                CanvasGrabDrop.Unchanged
            } else {
                CanvasGrabDrop.SamePage(CanvasPageWrite(pageIndex, preview.placements))
            }
        itemId !in preview.placements -> CanvasGrabDrop.Unchanged
        else -> CanvasGrabDrop.BetweenPages(
            itemId = itemId,
            destination = CanvasPageWrite(pageIndex, preview.placements),
            source = CanvasPageWrite(home.pageIndex, home.baseline.placements - itemId),
            isNewTile = isNewTile,
        )
    }

    private fun arrivedOn(target: JarnCanvasPage): CanvasGrab {
        val carried = rect
        val arriving = target.layout.toSnapshot()
        val moved = copy(
            pageIndex = target.index,
            baseline = arriving,
            reserved = reservedRectsForActive(target.layout),
            preview = arriving,
            visiblePushCells = 0,
            refusal = null,
        )
        return moved.copy(cursor = moved.fit(carried))
    }

    private fun spanAfter(span: Int, delta: Int, min: Int): Int =
        (span + delta).coerceIn(min.coerceAtMost(MAX_PUSH_AXIS_SPAN), MAX_PUSH_AXIS_SPAN)

    private fun fit(target: GridRect): GridRect {
        val cross = baseline.config.crossAxisCount
        return when (baseline.config.pushDirection) {
            PushDirection.DOWN -> {
                val colSpan = target.colSpan.coerceAtMost(cross)
                target.copy(
                    col = target.col.coerceIn(0, cross - colSpan),
                    row = target.row.coerceIn(0, pushLimit(target.rowSpan)),
                    colSpan = colSpan,
                )
            }
            PushDirection.RIGHT -> {
                val rowSpan = target.rowSpan.coerceAtMost(cross)
                target.copy(
                    col = target.col.coerceIn(0, pushLimit(target.colSpan)),
                    row = target.row.coerceIn(0, cross - rowSpan),
                    rowSpan = rowSpan,
                )
            }
        }
    }

    /**
     * The last visible cell or one past the furthest neighbor, whichever is greater, so every empty
     * cell on screen is reachable while a held direction still can't run off into empty space.
     */
    private fun pushLimit(span: Int): Int {
        val down = baseline.config.pushDirection == PushDirection.DOWN
        val furthest = baseline.placements
            .filterKeys { it != itemId }
            .values
            .maxOfOrNull { if (down) it.bottom else it.right } ?: 0
        val own = baseline.placements[itemId]?.let { if (down) it.row else it.col } ?: 0
        return maxOf(visiblePushCells - span, furthest, own + 1)
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
            refusal = null,
        )
    }
}

private fun JarnCanvasPage.canReceive(itemId: String): Boolean =
    kind == JarnCanvasPageKind.Canvas &&
        layout.items.none { it.id == itemId } &&
        itemId !in layout.activeArrangement

internal fun beginCanvasGrab(
    layout: CanvasLayout,
    itemId: String,
    minColSpan: Int = 1,
    minRowSpan: Int = 1,
    visiblePushCells: Int = 0,
    pageIndex: Int = 0,
    driver: CanvasGrabDriver = CanvasGrabDriver.Controller,
): CanvasGrab? {
    val baseline = layout.toSnapshot()
    val rect = baseline.placements[itemId] ?: return null
    val reserved = reservedRectsForActive(layout)
    return CanvasGrab(
        itemId = itemId,
        baseline = baseline,
        reserved = reserved,
        minColSpan = minColSpan.coerceAtLeast(1),
        minRowSpan = minRowSpan.coerceAtLeast(1),
        cursor = rect,
        home = CanvasGrabHome(pageIndex, baseline, reserved, rect, visiblePushCells, layout.activeOrientation),
        driver = driver,
        visiblePushCells = visiblePushCells,
        isFolder = layout.items.any { it.id == itemId && it is CanvasItem.FolderItem },
    )
}
