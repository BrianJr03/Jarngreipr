package jr.brian.jarnfe.canvas

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameMillis

/** Starting values from the approved prototype. They still need tuning on a real device. */
internal data class CanvasEdgeThresholds(
    val edgeDp: Float = 36f,
    val dwellMs: Long = 550L,
    val scrollDpPerMs: Float = 0.64f,
    val minDepth: Float = 0.15f,
)

/** The board's scroll, as the drag needs it. Its deltas are in content pixels. */
internal class CanvasScrollPort(private val state: ScrollState, val horizontal: Boolean) {
    val value: Float get() = state.value.toFloat()
    val atStart: Boolean get() = state.value <= 0
    val atEnd: Boolean get() = state.value >= state.maxValue

    fun scrollBy(px: Float): Float = state.dispatchRawDelta(px)
}

/** Signed depth into the edge zone: negative at the start, positive at the end, 0 outside it. */
internal fun canvasEdgeDepth(
    position: Float,
    start: Float,
    end: Float,
    edgePx: Float,
    minDepth: Float = CanvasEdgeThresholds().minDepth,
): Float = when {
    edgePx <= 0f || end - start <= edgePx * 2 -> 0f
    position < start + edgePx -> -((start + edgePx - position) / edgePx).coerceIn(minDepth, 1f)
    position > end - edgePx -> ((position - (end - edgePx)) / edgePx).coerceIn(minDepth, 1f)
    else -> 0f
}

/** Times a hold at the scroll end. It latches, so one dwell moves one page until the finger leaves. */
internal class CanvasEdgeDwell(private val dwellMs: Long = CanvasEdgeThresholds().dwellMs) {
    private var side = 0
    private var since: Long? = null
    private var latched = false

    /** Returns the direction to flip, once per dwell. */
    fun step(direction: Int, atScrollEnd: Boolean, canFlip: Boolean, nowMs: Long): Int {
        if (direction == 0 || direction != side) {
            side = direction
            since = null
            latched = false
        }
        if (direction == 0 || latched || !atScrollEnd || !canFlip) {
            since = null
            return 0
        }
        val started = since ?: nowMs.also { since = it }
        if (nowMs - started < dwellMs) return 0
        since = null
        latched = true
        return direction
    }

    fun reset() {
        side = 0
        since = null
        latched = false
    }
}

@Composable
internal fun CanvasEdgeDriveEffect(driver: CanvasTouchDriver, active: Boolean) {
    LaunchedEffect(driver, active) {
        if (!active) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                driver.edgeFrame(now, (now - last).coerceAtMost(MAX_FRAME_MS))
                last = now
            }
        }
    }
}

private const val MAX_FRAME_MS = 32L
