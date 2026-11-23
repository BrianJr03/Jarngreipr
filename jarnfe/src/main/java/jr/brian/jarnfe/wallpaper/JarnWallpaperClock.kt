package jr.brian.jarnfe.wallpaper

import android.view.Choreographer
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.observeReads

private const val NANOS_PER_SECOND = 1_000_000_000L
private const val NANOS_PER_MILLI = 1_000_000L
private const val MAX_STEP_NANOS = 2 * NANOS_PER_SECOND
private const val WAKE_MARGIN_MILLIS = 8L
private const val MIN_FPS = 1
private const val MAX_FPS = 120

/** Wallpaper time, which only moves while frames run, so it never jumps after a pause. */
internal class JarnWallpaperTimeline {
    var elapsedNanos = 0L
        private set
    private var lastFrameNanos = 0L

    fun rejoin(frameNanos: Long) {
        lastFrameNanos = maxOf(lastFrameNanos, frameNanos)
    }

    /** Both windows report the same frame, and only the first one moves time. */
    fun advance(frameNanos: Long) {
        if (frameNanos <= lastFrameNanos) return
        elapsedNanos += minOf(frameNanos - lastFrameNanos, MAX_STEP_NANOS)
        lastFrameNanos = frameNanos
    }
}

private val SharedTimeline = JarnWallpaperTimeline()

internal fun jarnWallpaperFrameNanos(targetFps: Int): Long =
    NANOS_PER_SECOND / targetFps.coerceIn(MIN_FPS, MAX_FPS)

internal fun jarnWallpaperSlot(elapsedNanos: Long, targetFps: Int): Long =
    elapsedNanos / jarnWallpaperFrameNanos(targetFps)

/** How long to sleep before the vsync that lands on the next slot. */
internal fun jarnWallpaperWakeDelayMillis(elapsedNanos: Long, targetFps: Int): Long {
    val frameNanos = jarnWallpaperFrameNanos(targetFps)
    val remainingNanos = frameNanos - elapsedNanos % frameNanos
    return (remainingNanos / NANOS_PER_MILLI - WAKE_MARGIN_MILLIS).coerceAtLeast(0L)
}

/** Takes primitive time so a draw never boxes. */
internal fun interface JarnWallpaperPainter {
    fun DrawScope.paint(timeNanos: Long)
}

/**
 * Draws [painter] at up to [targetFps] on the shared wallpaper time, behind the content. While
 * [canAnimate] is false no frame is requested and the last frame stays on screen.
 */
internal fun Modifier.jarnWallpaperDraw(
    canAnimate: State<Boolean>,
    targetFps: Int,
    painter: JarnWallpaperPainter,
): Modifier = this then JarnWallpaperDrawElement(canAnimate, targetFps, painter)

private data class JarnWallpaperDrawElement(
    val canAnimate: State<Boolean>,
    val targetFps: Int,
    val painter: JarnWallpaperPainter,
) : ModifierNodeElement<JarnWallpaperDrawNode>() {
    override fun create() = JarnWallpaperDrawNode(canAnimate, targetFps, painter)

    override fun update(node: JarnWallpaperDrawNode) = node.update(canAnimate, targetFps, painter)
}

private class JarnWallpaperDrawNode(
    private var canAnimate: State<Boolean>,
    private var targetFps: Int,
    private var painter: JarnWallpaperPainter,
) : Modifier.Node(), DrawModifierNode, ObserverModifierNode, Choreographer.FrameCallback {
    private var isRunning = false
    private var isRejoining = false
    private var drawnSlot = 0L

    override fun onAttach() {
        drawnSlot = jarnWallpaperSlot(SharedTimeline.elapsedNanos, targetFps)
        syncWithGate()
    }

    override fun onDetach() = stop()

    override fun onObservedReadsChanged() = syncWithGate()

    fun update(canAnimate: State<Boolean>, targetFps: Int, painter: JarnWallpaperPainter) {
        val gateChanged = this.canAnimate !== canAnimate
        val fpsChanged = this.targetFps != targetFps
        this.canAnimate = canAnimate
        this.targetFps = targetFps
        this.painter = painter
        if (fpsChanged) drawnSlot = jarnWallpaperSlot(SharedTimeline.elapsedNanos, targetFps)
        if (gateChanged) syncWithGate()
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        with(painter) { paint(drawnSlot * jarnWallpaperFrameNanos(targetFps)) }
        drawContent()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isRunning) return
        if (isRejoining) {
            SharedTimeline.rejoin(frameTimeNanos)
            isRejoining = false
        } else {
            SharedTimeline.advance(frameTimeNanos)
        }
        val elapsed = SharedTimeline.elapsedNanos
        val slot = jarnWallpaperSlot(elapsed, targetFps)
        if (slot != drawnSlot) {
            drawnSlot = slot
            invalidateDraw()
        }
        Choreographer.getInstance()
            .postFrameCallbackDelayed(this, jarnWallpaperWakeDelayMillis(elapsed, targetFps))
    }

    private fun syncWithGate() {
        var isOpen = false
        observeReads { isOpen = canAnimate.value }
        if (isOpen) start() else stop()
    }

    private fun start() {
        if (isRunning) return
        isRunning = true
        isRejoining = true
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun stop() {
        if (!isRunning) return
        isRunning = false
        Choreographer.getInstance().removeFrameCallback(this)
    }
}
