package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal const val CANVAS_LIFT_SCALE = 1.04f

/** Reduced motion keeps the tile at its real size, so nothing scales in or out. */
internal fun canvasLiftScale(lift: Float, reduced: Boolean): Float =
    if (reduced) 1f else 1f + (CANVAS_LIFT_SCALE - 1f) * lift

/**
 * The one tile riding above the board, in board pixels. [x], [y] and [lift] are read only inside a
 * graphics layer, so following the finger redraws that layer without recomposing or remeasuring.
 */
@Stable
internal class CanvasDragVisual {
    var tileId by mutableStateOf<String?>(null)
        private set

    var x by mutableFloatStateOf(0f)
        private set

    var y by mutableFloatStateOf(0f)
        private set

    var lift by mutableFloatStateOf(0f)
        private set

    private var settleFromX = 0f
    private var settleFromY = 0f
    private var settleFromLift = 0f

    fun hold(id: String, originX: Float, originY: Float) {
        tileId = id
        x = originX
        y = originY
        lift = 1f
    }

    fun follow(tileX: Float, tileY: Float) {
        x = tileX
        y = tileY
    }

    fun beginSettle() {
        settleFromX = x
        settleFromY = y
        settleFromLift = lift
    }

    fun settleStep(progress: Float, targetX: Float, targetY: Float) {
        x = settleFromX + (targetX - settleFromX) * progress
        y = settleFromY + (targetY - settleFromY) * progress
        lift = settleFromLift * (1f - progress)
    }

    fun finish() {
        tileId = null
        lift = 0f
    }
}

/**
 * Moves a tile to its new cell in one layout pass, then eases the leftover distance away in its
 * layer. [place] runs before layout and draw, so the tile never flashes at its destination.
 */
@Stable
internal class CanvasSlotGlide {
    private var placed = false
    private var placedX = 0
    private var placedY = 0
    private var fromX = 0f
    private var fromY = 0f

    var dx by mutableFloatStateOf(0f)
        private set

    var dy by mutableFloatStateOf(0f)
        private set

    var generation by mutableIntStateOf(0)
        private set

    val isGliding: Boolean get() = fromX != 0f || fromY != 0f

    fun place(x: Int, y: Int, glides: Boolean) {
        if (placed && x == placedX && y == placedY) return
        fromX = if (placed && glides) placedX + dx - x else 0f
        fromY = if (placed && glides) placedY + dy - y else 0f
        dx = fromX
        dy = fromY
        placed = true
        placedX = x
        placedY = y
        generation += 1
    }

    fun step(progress: Float) {
        dx = fromX * (1f - progress)
        dy = fromY * (1f - progress)
    }
}
