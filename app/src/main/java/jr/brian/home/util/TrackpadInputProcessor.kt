package jr.brian.home.util

import android.util.DisplayMetrics
import kotlin.math.sqrt

/**
 * Stateful processor that converts raw touch deltas into
 * accurate, DPI-normalised, accelerated cursor movements.
 *
 * Call [process] for every input sample (including historical ones).
 * Call [reset] on drag end/cancel.
 */
class TrackpadInputProcessor(
    private val surfaceMetrics: DisplayMetrics,
    private val outputMetrics: DisplayMetrics,
) {
    private val FULL_TRAVERSE_INCHES = 2.5f

    private val ACCEL_CURVE = listOf(
        0.00f to 0.5f,
        0.30f to 1.0f,
        1.20f to 2.8f,
        3.00f to 5.5f,
    )

    private val SMOOTHING_ALPHA = 0.35f

    private var smoothedSpeedInches = 0f
    private var lastTimestampNs = 0L

    data class ProcessedDelta(val dx: Float, val dy: Float)

    fun process(rawDxPx: Float, rawDyPx: Float, timestampNs: Long): ProcessedDelta {
        val dxInches = rawDxPx / surfaceMetrics.xdpi
        val dyInches = rawDyPx / surfaceMetrics.ydpi

        val dtSec = if (lastTimestampNs == 0L) (1f / 120f)
        else ((timestampNs - lastTimestampNs) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastTimestampNs = timestampNs

        val distInches = sqrt(dxInches * dxInches + dyInches * dyInches)
        val instantSpeedIps = distInches / dtSec

        smoothedSpeedInches = SMOOTHING_ALPHA * instantSpeedIps +
                (1f - SMOOTHING_ALPHA) * smoothedSpeedInches

        val accelMultiplier = interpolateAccel(smoothedSpeedInches)

        val cursorDx = (dxInches / FULL_TRAVERSE_INCHES) * accelMultiplier
        val cursorDy = (dyInches / FULL_TRAVERSE_INCHES) * accelMultiplier

        return ProcessedDelta(cursorDx, cursorDy)
    }

    fun reset() {
        smoothedSpeedInches = 0f
        lastTimestampNs = 0L
    }

    private fun interpolateAccel(speedIps: Float): Float {
        if (speedIps <= ACCEL_CURVE.first().first) return ACCEL_CURVE.first().second
        if (speedIps >= ACCEL_CURVE.last().first) return ACCEL_CURVE.last().second

        for (i in 0 until ACCEL_CURVE.size - 1) {
            val (s0, m0) = ACCEL_CURVE[i]
            val (s1, m1) = ACCEL_CURVE[i + 1]
            if (speedIps in s0..s1) {
                val t = (speedIps - s0) / (s1 - s0)
                return m0 + t * (m1 - m0)
            }
        }
        return 1f
    }
}
