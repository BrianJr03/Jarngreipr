package jr.brian.jarnfe.canvas

import androidx.annotation.StringRes
import androidx.compose.ui.geometry.Offset
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R

/** Why the held tile didn't go where it was asked to. */
internal enum class CanvasMoveRefusal(@param:StringRes @get:StringRes val label: Int) {
    BoardEdge(R.string.jarnfe_canvas_refused_edge),
    MenuCell(R.string.jarnfe_canvas_cell_locked),
    SmallestSize(R.string.jarnfe_canvas_refused_smallest),
    LargestSize(R.string.jarnfe_canvas_refused_largest),
}

/** [fitted] is [requested] kept on the board, and [placed] is where the solver put it. */
internal fun canvasMoveRefusal(
    requested: GridRect,
    fitted: GridRect,
    placed: GridRect,
    reserved: List<GridRect>,
): CanvasMoveRefusal? = when {
    requested.col != fitted.col || requested.row != fitted.row -> CanvasMoveRefusal.BoardEdge
    (placed.col != fitted.col || placed.row != fitted.row) && reserved.any { it.overlaps(fitted) } ->
        CanvasMoveRefusal.MenuCell
    else -> null
}

internal fun canvasSpanRefusal(requested: Int, actual: Int): CanvasMoveRefusal? = when {
    actual > requested -> CanvasMoveRefusal.SmallestSize
    actual < requested -> CanvasMoveRefusal.LargestSize
    else -> null
}

/** Compares the span a corner drag asked for with the [target] it was limited to. */
internal fun canvasResizeRefusal(
    from: GridRect,
    corner: CanvasResizeCorner,
    deltaCol: Int,
    deltaRow: Int,
    target: GridRect,
): CanvasMoveRefusal? {
    val wantedCols = from.colSpan + if (corner.west) -deltaCol else deltaCol
    val wantedRows = from.rowSpan + if (corner.north) -deltaRow else deltaRow
    return canvasSpanRefusal(wantedCols, target.colSpan) ?: canvasSpanRefusal(wantedRows, target.rowSpan)
}

/** Centers the chip on the tile's lower edge and keeps it fully on the board. */
internal fun canvasChipOrigin(
    tile: Offset,
    tileWidth: Float,
    tileHeight: Float,
    chipWidth: Float,
    chipHeight: Float,
    boardWidth: Float,
    boardHeight: Float,
    inset: Float,
): Offset = Offset(
    x = (tile.x + (tileWidth - chipWidth) / 2f).coerceIn(0f, (boardWidth - chipWidth).coerceAtLeast(0f)),
    y = (tile.y + tileHeight - chipHeight - inset).coerceIn(0f, (boardHeight - chipHeight).coerceAtLeast(0f)),
)
