package jr.brian.home.canvas.grid

import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect

/**
 * Direction along which the solver displaces neighbors. Always the unbounded
 * scroll axis, so a valid push always exists.
 */
enum class PushDirection {
    /** Vertical scroll: push toward increasing row. */
    DOWN,

    /** Horizontal scroll: push toward increasing col. */
    RIGHT;

    companion object {
        fun from(orientation: CanvasScrollOrientation): PushDirection = when (orientation) {
            CanvasScrollOrientation.VERTICAL -> DOWN
            CanvasScrollOrientation.HORIZONTAL -> RIGHT
        }
    }
}

internal fun GridRect.pushAxis(dir: PushDirection): Int = when (dir) {
    PushDirection.DOWN -> row
    PushDirection.RIGHT -> col
}

internal fun GridRect.crossAxis(dir: PushDirection): Int = when (dir) {
    PushDirection.DOWN -> col
    PushDirection.RIGHT -> row
}

internal fun GridRect.pushEnd(dir: PushDirection): Int = when (dir) {
    PushDirection.DOWN -> bottom
    PushDirection.RIGHT -> right
}
