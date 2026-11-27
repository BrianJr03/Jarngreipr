package jr.brian.jarnfe.input

import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.hypot

const val JARN_STICK_DEAD_ZONE = 0.2f
const val JARN_STICK_SMOOTHING = 0.5f
private const val SNAP = 0.01f

/** One stick, after the dead zone and smoothing. Both axes run from -1 to 1; +x is right, +y is down. */
@Immutable
data class JarnStickVector(val x: Float = 0f, val y: Float = 0f) {
    val isCentered: Boolean get() = x == 0f && y == 0f

    companion object {
        val Centered = JarnStickVector()
    }
}

/**
 * Both sticks at [eventTimeMs]. Delivered only when something changed, so a layer moving things
 * while a stick is held keeps the last value and applies it each frame until it is centered.
 */
@Immutable
data class JarnSticks(
    val left: JarnStickVector = JarnStickVector.Centered,
    val right: JarnStickVector = JarnStickVector.Centered,
    val eventTimeMs: Long = 0L,
) {
    val isCentered: Boolean get() = left.isCentered && right.isCentered
}

/** Raw axis values, separated from [MotionEvent] so routing stays testable on the JVM. */
data class JarnStickRaw(
    val leftX: Float,
    val leftY: Float,
    val rightX: Float,
    val rightY: Float,
    val eventTimeMs: Long,
    /** The D-pad as a hat. Android turns it into D-pad keys, so an event carrying it is never consumed. */
    val hatPressed: Boolean = false,
)

/**
 * A radial dead zone, rescaled so movement starts at 0 just past it, then exponential smoothing.
 * Centering and full deflection snap, so a released stick reports exactly zero.
 */
class JarnStickFilter(
    private val deadZone: Float = JARN_STICK_DEAD_ZONE,
    private val smoothing: Float = JARN_STICK_SMOOTHING,
) {
    var value = JarnStickVector.Centered
        private set

    fun update(rawX: Float, rawY: Float): JarnStickVector {
        val target = applyDeadZone(rawX, rawY)
        value = if (target.isCentered) {
            JarnStickVector.Centered
        } else {
            JarnStickVector(smooth(value.x, target.x), smooth(value.y, target.y))
        }
        return value
    }

    fun reset() {
        value = JarnStickVector.Centered
    }

    private fun applyDeadZone(x: Float, y: Float): JarnStickVector {
        val magnitude = hypot(x, y)
        if (magnitude <= deadZone || magnitude.isNaN()) return JarnStickVector.Centered
        val scale = ((magnitude - deadZone) / (1f - deadZone)).coerceAtMost(1f) / magnitude
        return JarnStickVector((x * scale).coerceIn(-1f, 1f), (y * scale).coerceIn(-1f, 1f))
    }

    private fun smooth(from: Float, to: Float): Float {
        val next = from + (to - from) * smoothing
        return if (abs(next - to) < SNAP) to else next
    }
}

/**
 * The stick axes of a joystick move, or null for anything else. The right stick is AXIS_Z / AXIS_RZ
 * on most pads and AXIS_RX / AXIS_RY on some; the device's motion ranges decide, and without a
 * device the pair that moved is used.
 */
internal fun MotionEvent.jarnStickRaw(): JarnStickRaw? {
    if (!isFromSource(InputDevice.SOURCE_JOYSTICK) || actionMasked != MotionEvent.ACTION_MOVE) return null
    val useRx = device?.let { it.getMotionRange(MotionEvent.AXIS_Z) == null && it.getMotionRange(MotionEvent.AXIS_RX) != null }
        ?: (getAxisValue(MotionEvent.AXIS_Z) == 0f && getAxisValue(MotionEvent.AXIS_RZ) == 0f)
    val (rx, ry) = if (useRx) MotionEvent.AXIS_RX to MotionEvent.AXIS_RY else MotionEvent.AXIS_Z to MotionEvent.AXIS_RZ
    return JarnStickRaw(
        leftX = getAxisValue(MotionEvent.AXIS_X),
        leftY = getAxisValue(MotionEvent.AXIS_Y),
        rightX = getAxisValue(rx),
        rightY = getAxisValue(ry),
        eventTimeMs = eventTime,
        hatPressed = getAxisValue(MotionEvent.AXIS_HAT_X) != 0f || getAxisValue(MotionEvent.AXIS_HAT_Y) != 0f,
    )
}
