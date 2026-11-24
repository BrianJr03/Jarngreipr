package jr.brian.jarnfe.canvas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.core.ui.components.JarnSurface
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R
import kotlin.math.max
import kotlin.math.min

private const val HANDLE_HIT_DP = 48f
private const val HANDLE_MIN_INWARD_DP = 12f
private const val HANDLE_MAX_INWARD_DP = 24f
private const val HANDLE_INWARD_SHARE = 0.25f
private const val HANDLES_Z_INDEX = 2f
private val HandleSize = 18.dp
private val HandleBorder = 3.dp

internal enum class CanvasResizeCorner(val west: Boolean, val north: Boolean) {
    NorthWest(west = true, north = true),
    NorthEast(west = false, north = true),
    SouthWest(west = true, north = false),
    SouthEast(west = false, north = false),
}

/** The inward reach stays small on a small tile, so its center can still start a move. */
internal fun canvasHandleAt(
    board: CanvasBoardGeometry,
    rect: GridRect,
    point: Offset,
    density: Float,
): CanvasResizeCorner? {
    val origin = board.originOf(rect)
    val width = board.extentOf(rect.colSpan)
    val height = board.extentOf(rect.rowSpan)
    val inwardDp = (HANDLE_INWARD_SHARE * min(width, height) / density)
        .coerceIn(HANDLE_MIN_INWARD_DP, HANDLE_MAX_INWARD_DP)
    val inward = inwardDp * density
    val outward = (HANDLE_HIT_DP - inwardDp) * density
    return CanvasResizeCorner.entries.firstOrNull { corner ->
        val edgeX = if (corner.west) origin.x else origin.x + width
        val edgeY = if (corner.north) origin.y else origin.y + height
        val inX = if (corner.west) {
            point.x >= edgeX - outward && point.x <= edgeX + inward
        } else {
            point.x >= edgeX - inward && point.x <= edgeX + outward
        }
        val inY = if (corner.north) {
            point.y >= edgeY - outward && point.y <= edgeY + inward
        } else {
            point.y >= edgeY - inward && point.y <= edgeY + outward
        }
        inX && inY
    }
}

/** The rect after dragging [corner] by whole cells. The opposite corner never moves. */
internal fun canvasResizeTarget(
    from: GridRect,
    corner: CanvasResizeCorner,
    deltaCol: Int,
    deltaRow: Int,
    minColSpan: Int,
    minRowSpan: Int,
    crossAxisCount: Int,
    direction: PushDirection,
): GridRect {
    val down = direction == PushDirection.DOWN
    val (col, colSpan) = resizedAxis(
        start = from.col,
        span = from.colSpan,
        delta = deltaCol,
        movesStart = corner.west,
        minSpan = minColSpan,
        maxSpan = if (down) crossAxisCount else MAX_PUSH_AXIS_SPAN,
        limit = if (down) crossAxisCount else null,
    )
    val (row, rowSpan) = resizedAxis(
        start = from.row,
        span = from.rowSpan,
        delta = deltaRow,
        movesStart = corner.north,
        minSpan = minRowSpan,
        maxSpan = if (down) MAX_PUSH_AXIS_SPAN else crossAxisCount,
        limit = if (down) null else crossAxisCount,
    )
    return GridRect(col, row, colSpan, rowSpan)
}

private fun resizedAxis(
    start: Int,
    span: Int,
    delta: Int,
    movesStart: Boolean,
    minSpan: Int,
    maxSpan: Int,
    limit: Int?,
): Pair<Int, Int> {
    val end = start + span
    if (movesStart) {
        val lowest = max(0, end - maxSpan)
        val highest = max(lowest, end - max(minSpan, 1))
        val next = (start + delta).coerceIn(lowest, highest)
        return next to end - next
    }
    val largest = max(1, min(maxSpan, limit?.minus(start) ?: maxSpan))
    return start to (span + delta).coerceIn(min(max(minSpan, 1), largest), largest)
}

@Composable
internal fun CanvasResizeHandles(rect: GridRect, x: Dp, y: Dp, width: Dp, height: Dp, showSize: Boolean) {
    Box(
        Modifier
            .zIndex(HANDLES_Z_INDEX)
            .offset(x, y)
            .size(width, height),
    ) {
        CanvasResizeCorner.entries.forEach { CanvasResizeHandle(it) }
        if (showSize) CanvasSizeReadout(rect, Modifier.align(Alignment.Center))
    }
}

@Composable
private fun BoxScope.CanvasResizeHandle(corner: CanvasResizeCorner) {
    val colors = JarnTheme.colors
    val half = HandleSize / 2
    Box(
        Modifier
            .align(corner.alignment)
            .offset(if (corner.west) -half else half, if (corner.north) -half else half)
            .size(HandleSize)
            .background(colors.accent, CircleShape)
            .border(HandleBorder, colors.surface, CircleShape),
    )
}

private val CanvasResizeCorner.alignment: Alignment
    get() = when (this) {
        CanvasResizeCorner.NorthWest -> Alignment.TopStart
        CanvasResizeCorner.NorthEast -> Alignment.TopEnd
        CanvasResizeCorner.SouthWest -> Alignment.BottomStart
        CanvasResizeCorner.SouthEast -> Alignment.BottomEnd
    }

@Composable
private fun CanvasSizeReadout(rect: GridRect, modifier: Modifier) {
    JarnSurface(modifier = modifier, color = JarnTheme.colors.accent) {
        JarnText(
            text = stringResource(R.string.jarnfe_canvas_size_readout, rect.colSpan, rect.rowSpan),
            style = JarnTheme.type.labelStrong,
            color = JarnTheme.colors.onAccent,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = JarnTheme.space.sm, vertical = JarnTheme.space.xs),
        )
    }
}
