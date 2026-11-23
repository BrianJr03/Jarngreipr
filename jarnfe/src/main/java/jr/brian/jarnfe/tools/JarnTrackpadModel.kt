package jr.brian.jarnfe.tools

import kotlin.math.abs
import kotlin.math.hypot

private const val FULL_TRAVERSE_INCHES = 2.5f
private const val SMOOTHING_ALPHA = 0.35f
private const val FIRST_SAMPLE_SECONDS = 1f / 120f
private const val MIN_SAMPLE_SECONDS = 0.001f
private const val MAX_SAMPLE_SECONDS = 0.1f
private const val NANOS_PER_SECOND = 1_000_000_000f
private const val NANOS_PER_MS = 1_000_000L
private const val MIN_SCROLL_PX = 0.5f
private const val SCROLL_SCALE = 10f
private const val DPAD_STEP = 0.02f
private const val SHOULDER_SCROLL = 1f

private val AccelCurve = listOf(0f to 0.5f, 0.3f to 1f, 1.2f to 2.8f, 3f to 5.5f)

internal enum class JarnPadDirection(val dx: Int, val dy: Int) {
    Up(0, -1),
    Down(0, 1),
    Left(-1, 0),
    Right(1, 0),
}

/** Turns raw finger movement into screen-independent, accelerated cursor movement. */
internal class JarnTrackpadProcessor(private val xDpi: Float, private val yDpi: Float) {
    private var smoothedSpeed = 0f
    private var lastTimestampNs = 0L

    fun process(rawDx: Float, rawDy: Float, timestampNs: Long): Pair<Float, Float> {
        val dxInches = rawDx / xDpi
        val dyInches = rawDy / yDpi
        val seconds = if (lastTimestampNs == 0L) {
            FIRST_SAMPLE_SECONDS
        } else {
            ((timestampNs - lastTimestampNs) / NANOS_PER_SECOND).coerceIn(MIN_SAMPLE_SECONDS, MAX_SAMPLE_SECONDS)
        }
        lastTimestampNs = timestampNs
        val speed = hypot(dxInches, dyInches) / seconds
        smoothedSpeed = SMOOTHING_ALPHA * speed + (1f - SMOOTHING_ALPHA) * smoothedSpeed
        val accel = acceleration(smoothedSpeed)
        return dxInches / FULL_TRAVERSE_INCHES * accel to dyInches / FULL_TRAVERSE_INCHES * accel
    }

    fun reset() {
        smoothedSpeed = 0f
        lastTimestampNs = 0L
    }

    private fun acceleration(speed: Float): Float {
        if (speed <= AccelCurve.first().first) return AccelCurve.first().second
        if (speed >= AccelCurve.last().first) return AccelCurve.last().second
        val upper = AccelCurve.indexOfFirst { speed <= it.first }
        val (s0, m0) = AccelCurve[upper - 1]
        val (s1, m1) = AccelCurve[upper]
        return m0 + (speed - s0) / (s1 - s0) * (m1 - m0)
    }
}

/** Holds the cursor and turns touch and pad input into pointer commands for [send]. */
internal class JarnTrackpadController(
    private val processor: JarnTrackpadProcessor,
    private val bounds: JarnPointerBounds,
    private val send: (JarnPointerCommand) -> Unit,
) {
    var x = 0.5f
        private set
    var y = 0.5f
        private set

    private val pixelX get() = x * bounds.width
    private val pixelY get() = y * bounds.height

    fun drag(rawDx: Float, rawDy: Float, timestampNs: Long) {
        val (dx, dy) = processor.process(rawDx, rawDy, timestampNs)
        moveBy(dx, dy)
    }

    fun endDrag() = processor.reset()

    fun step(direction: JarnPadDirection) = moveBy(direction.dx * DPAD_STEP, direction.dy * DPAD_STEP)

    fun click(secondary: Boolean) = send(JarnPointerCommand.Click(pixelX, pixelY, secondary))

    fun scrollBy(fractionDy: Float) = send(JarnPointerCommand.Scroll(pixelX, pixelY, -fractionDy * SCROLL_SCALE))

    fun scrollUp() = send(JarnPointerCommand.Scroll(pixelX, pixelY, SHOULDER_SCROLL))

    fun scrollDown() = send(JarnPointerCommand.Scroll(pixelX, pixelY, -SHOULDER_SCROLL))

    private fun moveBy(dx: Float, dy: Float) {
        x = (x + dx).coerceIn(0f, 1f)
        y = (y + dy).coerceIn(0f, 1f)
        send(JarnPointerCommand.Move(pixelX, pixelY))
    }
}

internal data class JarnTouchSample(val x: Float, val y: Float, val timeMs: Long)

internal data class JarnTouchPoint(
    val id: Long,
    val pressed: Boolean,
    val current: JarnTouchSample,
    val history: List<JarnTouchSample> = emptyList(),
)

internal sealed interface JarnTrackpadGestureEvent {
    data class Drag(val dx: Float, val dy: Float, val timestampNs: Long) : JarnTrackpadGestureEvent
    data class Scroll(val dyPx: Float) : JarnTrackpadGestureEvent
    data object Tap : JarnTrackpadGestureEvent
    data object TwoFingerTap : JarnTrackpadGestureEvent
    data object End : JarnTrackpadGestureEvent
}

/**
 * Reads one touch sequence at a time. Fingers rarely lift in the same frame, so a two-finger tap
 * is decided when the last finger lifts, using the most fingers seen during the sequence.
 */
internal class JarnTrackpadGesture(private val tapSlopPx: Float) {
    private val downAt = HashMap<Long, JarnTouchSample>()
    private val lastAt = HashMap<Long, JarnTouchSample>()
    private var peakFingers = 0
    private var travelled = false

    fun onFrame(points: List<JarnTouchPoint>): List<JarnTrackpadGestureEvent> {
        val active = points.filter { it.pressed }
        points.forEach(::trackTravel)
        active.forEach { point -> downAt.getOrPut(point.id) { point.current } }
        peakFingers = maxOf(peakFingers, active.size)
        val events = mutableListOf<JarnTrackpadGestureEvent>()
        when {
            active.size >= 2 -> twoFingerScroll(active, events)
            active.size == 1 && peakFingers == 1 -> oneFingerDrag(active.single(), events)
            active.size == 1 -> lastAt[active.single().id] = active.single().current
            downAt.isNotEmpty() -> finish(events)
        }
        return events
    }

    private fun trackTravel(point: JarnTouchPoint) {
        val down = downAt[point.id] ?: return
        if (hypot(point.current.x - down.x, point.current.y - down.y) >= tapSlopPx) travelled = true
    }

    private fun twoFingerScroll(active: List<JarnTouchPoint>, events: MutableList<JarnTrackpadGestureEvent>) {
        val deltas = active.mapNotNull { point -> lastAt[point.id]?.let { point.current.y - it.y } }
        active.forEach { lastAt[it.id] = it.current }
        if (!travelled || deltas.isEmpty()) return
        val average = deltas.average().toFloat()
        if (abs(average) > MIN_SCROLL_PX) events += JarnTrackpadGestureEvent.Scroll(average)
    }

    private fun oneFingerDrag(point: JarnTouchPoint, events: MutableList<JarnTrackpadGestureEvent>) {
        var previous = lastAt[point.id]
        if (previous != null) {
            (point.history + point.current).forEach { sample ->
                val from = previous ?: sample
                events += JarnTrackpadGestureEvent.Drag(sample.x - from.x, sample.y - from.y, sample.timeMs * NANOS_PER_MS)
                previous = sample
            }
        }
        lastAt[point.id] = point.current
    }

    private fun finish(events: MutableList<JarnTrackpadGestureEvent>) {
        if (!travelled) {
            events += if (peakFingers >= 2) JarnTrackpadGestureEvent.TwoFingerTap else JarnTrackpadGestureEvent.Tap
        }
        events += JarnTrackpadGestureEvent.End
        downAt.clear()
        lastAt.clear()
        peakFingers = 0
        travelled = false
    }
}
