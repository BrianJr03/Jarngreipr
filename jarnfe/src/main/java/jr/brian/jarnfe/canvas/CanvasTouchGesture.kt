package jr.brian.jarnfe.canvas

import kotlin.math.abs
import kotlin.math.hypot

/** Starting values from the approved prototype. They still need tuning on a real device. */
internal data class CanvasTouchThresholds(
    val liftMs: Long = 360L,
    val widgetLiftMs: Long = 500L,
    val slopDp: Float = 8f,
    val dragStartDp: Float = 10f,
    val widgetClaimDp: Float = 3f,
    val cellHysteresis: Float = 0.25f,
    val palmContactDp: Float = 32f,
    val palmEdgeDp: Float = 16f,
)

internal enum class CanvasTouchStart { Browse, BrowseWidget, Edit, TakeOver, Blocked }

internal enum class CanvasTouchPhase { Idle, Pressing, Lifted, Dragging, PassedThrough, Blocked }

internal enum class CanvasTouchEffect { None, Tap, Lift, OpenMenu, BeginDrag, TakeOver, Move, Drop, Unlift, CancelDrag }

/**
 * Decides what one finger means and touches nothing else, so every path can be unit tested.
 * [CanvasTouchPhase.PassedThrough] leaves the gesture to the Canvas scroll or a widget's own view.
 */
internal class CanvasTouchGesture(
    density: Float,
    private val thresholds: CanvasTouchThresholds = CanvasTouchThresholds(),
) {
    private val slopPx = thresholds.slopDp * density
    private val dragStartPx = thresholds.dragStartDp * density
    private var startX = 0f
    private var startY = 0f
    private var dragsOnSlop = false

    var phase = CanvasTouchPhase.Idle
        private set

    var liftAtMs: Long? = null
        private set

    /** True while this gesture must keep the finger away from taps and scrolling. */
    val ownsPointer: Boolean
        get() = phase == CanvasTouchPhase.Lifted ||
            phase == CanvasTouchPhase.Dragging ||
            phase == CanvasTouchPhase.Blocked

    fun down(start: CanvasTouchStart, x: Float, y: Float, timeMs: Long): CanvasTouchEffect {
        startX = x
        startY = y
        liftAtMs = null
        dragsOnSlop = false
        return when (start) {
            CanvasTouchStart.Browse -> press(timeMs + thresholds.liftMs)
            CanvasTouchStart.BrowseWidget -> press(timeMs + thresholds.widgetLiftMs)
            CanvasTouchStart.Edit -> {
                dragsOnSlop = true
                phase = CanvasTouchPhase.Pressing
                CanvasTouchEffect.None
            }
            CanvasTouchStart.TakeOver -> {
                phase = CanvasTouchPhase.Dragging
                CanvasTouchEffect.TakeOver
            }
            CanvasTouchStart.Blocked -> {
                phase = CanvasTouchPhase.Blocked
                CanvasTouchEffect.None
            }
        }
    }

    fun timeReached(timeMs: Long): CanvasTouchEffect {
        val liftAt = liftAtMs ?: return CanvasTouchEffect.None
        if (phase != CanvasTouchPhase.Pressing || timeMs < liftAt) return CanvasTouchEffect.None
        liftAtMs = null
        phase = CanvasTouchPhase.Lifted
        return CanvasTouchEffect.Lift
    }

    fun moved(x: Float, y: Float): CanvasTouchEffect {
        val distance = hypot(x - startX, y - startY)
        return when (phase) {
            CanvasTouchPhase.Pressing -> when {
                distance <= slopPx -> CanvasTouchEffect.None
                dragsOnSlop -> beginDrag()
                else -> {
                    passThrough()
                    CanvasTouchEffect.None
                }
            }
            CanvasTouchPhase.Lifted -> if (distance > dragStartPx) beginDrag() else CanvasTouchEffect.None
            CanvasTouchPhase.Dragging -> CanvasTouchEffect.Move
            else -> CanvasTouchEffect.None
        }
    }

    fun up(): CanvasTouchEffect {
        val effect = when (phase) {
            CanvasTouchPhase.Pressing -> CanvasTouchEffect.Tap
            CanvasTouchPhase.Lifted -> CanvasTouchEffect.OpenMenu
            CanvasTouchPhase.Dragging -> CanvasTouchEffect.Drop
            else -> CanvasTouchEffect.None
        }
        finish()
        return effect
    }

    fun cancelled(): CanvasTouchEffect {
        val effect = restoreEffect()
        finish()
        return effect
    }

    /** A palm-like contact changes nothing. A real second finger ends the gesture and restores. */
    fun otherFinger(palmLike: Boolean): CanvasTouchEffect {
        if (palmLike || phase == CanvasTouchPhase.Idle || phase == CanvasTouchPhase.PassedThrough) {
            return CanvasTouchEffect.None
        }
        val effect = restoreEffect()
        abandon()
        return effect
    }

    /** A widget's child or the Canvas scroll took the finger before the hold finished. */
    fun claimedElsewhere() {
        if (phase == CanvasTouchPhase.Pressing) passThrough()
    }

    /** Keeps the finger but stops acting on it, such as when a grab could not start. */
    fun abandon() {
        liftAtMs = null
        phase = CanvasTouchPhase.Blocked
    }

    private fun press(liftAt: Long): CanvasTouchEffect {
        phase = CanvasTouchPhase.Pressing
        liftAtMs = liftAt
        return CanvasTouchEffect.None
    }

    private fun beginDrag(): CanvasTouchEffect {
        liftAtMs = null
        phase = CanvasTouchPhase.Dragging
        return CanvasTouchEffect.BeginDrag
    }

    private fun passThrough() {
        liftAtMs = null
        phase = CanvasTouchPhase.PassedThrough
    }

    private fun restoreEffect(): CanvasTouchEffect = when (phase) {
        CanvasTouchPhase.Lifted -> CanvasTouchEffect.Unlift
        CanvasTouchPhase.Dragging -> CanvasTouchEffect.CancelDrag
        else -> CanvasTouchEffect.None
    }

    private fun finish() {
        liftAtMs = null
        phase = CanvasTouchPhase.Idle
    }
}

/** Only ever applied to an extra contact. The top edge stays live, as in the prototype. */
internal fun canvasTouchIsPalm(
    contactDp: Float,
    xDp: Float,
    yDp: Float,
    windowWidthDp: Float,
    windowHeightDp: Float,
    thresholds: CanvasTouchThresholds = CanvasTouchThresholds(),
): Boolean =
    contactDp >= thresholds.palmContactDp ||
        xDp < thresholds.palmEdgeDp ||
        xDp > windowWidthDp - thresholds.palmEdgeDp ||
        yDp > windowHeightDp - thresholds.palmEdgeDp

internal data class CanvasScrollAxes(val horizontal: Boolean, val vertical: Boolean) {
    val any: Boolean get() = horizontal || vertical
}

/**
 * Whether a widget should claim this finger, or null until it has moved enough to tell. Deciding
 * before either slop is reached stops the Canvas from taking a list's scroll, while a swipe along
 * an axis the widget can't scroll is never claimed and still moves the Canvas.
 */
internal fun canvasWidgetClaim(deltaX: Float, deltaY: Float, axes: CanvasScrollAxes, minPx: Float): Boolean? = when {
    !axes.any || hypot(deltaX, deltaY) < minPx -> null
    abs(deltaY) >= abs(deltaX) -> axes.vertical
    else -> axes.horizontal
}
