package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeRect
import kotlin.math.abs
import kotlin.math.roundToInt

/** One nudge with snapping on: the 1% grid. */
internal const val SNAP_STEP = 0.01f

/** One nudge with snapping off. */
internal const val FINE_STEP = 0.0025f

/** An edge this close past a target is pulled onto it. */
internal const val SNAP_RANGE = 0.015f

/** The margin Jarn keeps from the screen edges, which pieces snap to. */
internal const val SAFE_MARGIN = 0.02f

/** Nothing gets smaller than this on either axis. */
internal const val MIN_PIECE_SIZE = 0.02f

private const val CENTER = 0.5f
private const val HOLD_FAST_AFTER = 5
private const val HOLD_FASTEST_AFTER = 15
private const val FAST_STEPS = 2
private const val FASTEST_STEPS = 4

/** Grid steps per nudge for the [repeat]-th repeat of a held direction: it speeds up as it's held. */
internal fun holdSteps(repeat: Int): Int = when {
    repeat >= HOLD_FASTEST_AFTER -> FASTEST_STEPS
    repeat >= HOLD_FAST_AFTER -> FAST_STEPS
    else -> 1
}

/** A rect by its edges, in screen fractions, ignoring its origin. */
internal data class PieceEdges(val left: Float, val top: Float, val width: Float, val height: Float) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val centerX: Float get() = left + width / 2
    val centerY: Float get() = top + height / 2

    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

internal val ThemeRect.edges: PieceEdges
    get() = PieceEdges(x - originX * width, y - originY * height, width, height)

/** Back to a rect with this rect's origin. */
internal fun ThemeRect.withEdges(edges: PieceEdges): ThemeRect = copy(
    x = edges.left + originX * edges.width,
    y = edges.top + originY * edges.height,
    width = edges.width,
    height = edges.height,
)

/** Lines pieces snap to on one axis: the screen edges, its center, the safe margins and [others]. */
internal fun snapLines(others: List<PieceEdges>, horizontal: Boolean): List<Float> {
    val screen = listOf(0f, SAFE_MARGIN, CENTER, 1f - SAFE_MARGIN, 1f)
    val pieces = others.flatMap { if (horizontal) listOf(it.left, it.centerX, it.right) else listOf(it.top, it.centerY, it.bottom) }
    return screen + pieces
}

/**
 * Moves [start] (the leading edge, center and trailing edge on one axis) by [delta]. With [snap],
 * a line the move crosses, or one within [SNAP_RANGE] past it, stops the move there; otherwise it
 * lands on the 1% grid. The result is the new leading edge.
 */
internal fun snappedMove(start: Float, size: Float, delta: Float, snap: Boolean, lines: List<Float>): Float {
    if (!snap || delta == 0f) return start + delta
    val sign = if (delta > 0) 1 else -1
    val edges = listOf(start, start + size / 2, start + size)
    val pull = edges.flatMap { edge ->
        lines.map { line -> line - edge }
            .filter { shift -> shift * sign > 0 && abs(shift) <= abs(delta) + SNAP_RANGE }
    }.minByOrNull { abs(it) }
    return if (pull != null) start + pull else onGrid(start + delta)
}

internal fun onGrid(value: Float): Float = (value / SNAP_STEP).roundToInt() * SNAP_STEP

/** Keeps a piece on screen: it may be as big as the screen, but no part of a smaller one leaves it. */
internal fun clampedStart(start: Float, size: Float): Float {
    val limit = 1f - size
    return start.coerceIn(minOf(0f, limit), maxOf(0f, limit))
}
