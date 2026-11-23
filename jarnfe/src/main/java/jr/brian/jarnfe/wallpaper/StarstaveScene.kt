package jr.brian.jarnfe.wallpaper

import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** One virtual space, 1920 by 2160, with x centered and the hinge at y 1080. */
internal object StarstaveScene {
    const val SCREEN_HEIGHT = 1080f
    const val STAR_COUNT = 150
    const val STAR_STRIDE = 4
    const val STAR_X = 0
    const val STAR_Y = 1
    const val STAR_PHASE = 2
    const val STAR_SIZE = 3
    const val LEVEL_COUNT = 4

    const val DIAL_CENTER_X = 0f
    const val DIAL_CENTER_Y = 1610f
    const val DIAL_INNER_RADIUS = 190f
    const val DIAL_MIDDLE_RADIUS = 330f
    const val DIAL_OUTER_RADIUS = 455f
    const val DIAL_TICK_COUNT = 48
    const val DIAL_TICK_STEP = 0.1309
    const val DIAL_TICK_SPIN = 0.012
    const val DIAL_TICK_SHORT = 10f
    const val DIAL_TICK_LONG = 24f
    const val DIAL_LONG_TICK_EVERY = 4

    val LevelAlphas = floatArrayOf(0.26f, 0.46f, 0.7f, 1f)
    val NodeX = floatArrayOf(-560f, -390f, -470f, -205f, -40f, 175f, 370f, 585f)
    val NodeY = floatArrayOf(250f, 395f, 610f, 690f, 455f, 575f, 345f, 470f)
    val NodeReach: FloatArray = cumulativeLengths(NodeX, NodeY)
    val TotalReach: Float = NodeReach.last()
    val Stars: FloatArray = seededStars()

    private fun cumulativeLengths(xs: FloatArray, ys: FloatArray): FloatArray {
        val lengths = DoubleArray(xs.size)
        for (i in 1 until xs.size) {
            lengths[i] = lengths[i - 1] +
                hypot((xs[i] - xs[i - 1]).toDouble(), (ys[i] - ys[i - 1]).toDouble())
        }
        return FloatArray(xs.size) { lengths[it].toFloat() }
    }

    private fun seededStars(): FloatArray {
        var seed = STAR_SEED
        val next = {
            seed = seed * LEHMER_MULTIPLIER % LEHMER_MODULUS
            (seed - 1).toDouble() / (LEHMER_MODULUS - 1)
        }
        val stars = FloatArray(STAR_COUNT * STAR_STRIDE)
        for (i in 0 until STAR_COUNT) {
            val at = i * STAR_STRIDE
            stars[at + STAR_X] = (next() * FIELD_WIDTH - FIELD_WIDTH / 2).toFloat()
            stars[at + STAR_Y] = (next() * FIELD_HEIGHT).toFloat()
            stars[at + STAR_PHASE] = (next() * FULL_TURN).toFloat()
            val spread = next()
            stars[at + STAR_SIZE] = (MIN_STAR_SIZE + spread * next() * STAR_SIZE_RANGE).toFloat()
        }
        return stars
    }

    private const val STAR_SEED = 4242L
    private const val LEHMER_MULTIPLIER = 16807L
    private const val LEHMER_MODULUS = 2147483647L
    private const val FIELD_WIDTH = 1920.0
    private const val FIELD_HEIGHT = 2160.0
    private const val FULL_TURN = 6.283
    private const val MIN_STAR_SIZE = 2.4
    private const val STAR_SIZE_RANGE = 4.6
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val CYCLE_SECONDS = 60.0
private const val DRAW_END = 0.62
private const val FADE_START = 0.86
private const val STAR_BREATH_SPEED = 0.32

/** The approved still moment. */
internal const val STARSTAVE_STILL_SECONDS = 37.0

/** Just past the moment the sign completes, so the first live frame matches the still. */
internal const val STARSTAVE_LIVE_START_SECONDS = 37.25

internal fun starstaveSeconds(timeNanos: Long, isStill: Boolean): Double =
    if (isStill) {
        STARSTAVE_STILL_SECONDS
    } else {
        STARSTAVE_LIVE_START_SECONDS + timeNanos / NANOS_PER_SECOND
    }

internal fun starstaveCycle(seconds: Double): Double = (seconds % CYCLE_SECONDS) / CYCLE_SECONDS

internal fun starstaveDrawProgress(cycle: Double, isStill: Boolean): Double =
    if (isStill) 1.0 else min(1.0, cycle / DRAW_END)

internal fun starstaveFade(cycle: Double, isStill: Boolean): Double =
    if (isStill || cycle <= FADE_START) 1.0 else 1.0 - (cycle - FADE_START) / (1.0 - FADE_START)

internal fun starstaveStarLevel(seconds: Double, phase: Float): Int {
    val breath = 0.5 + 0.5 * sin(seconds * STAR_BREATH_SPEED + phase)
    return min(StarstaveScene.LEVEL_COUNT - 1, floor(breath * StarstaveScene.LEVEL_COUNT).toInt())
}

private fun starstaveSegment(reach: Float): Int {
    val lengths = StarstaveScene.NodeReach
    for (i in 1 until lengths.size) {
        if (reach <= lengths[i]) return i
    }
    return lengths.lastIndex
}

private fun starstaveSegmentProgress(reach: Float, segment: Int): Float {
    val lengths = StarstaveScene.NodeReach
    val start = lengths[segment - 1]
    return ((reach - start) / (lengths[segment] - start)).coerceIn(0f, 1f)
}

private fun lerp(from: Float, to: Float, amount: Float): Float = from + (to - from) * amount

internal fun starstaveHeadX(reach: Float): Float {
    val segment = starstaveSegment(reach)
    val xs = StarstaveScene.NodeX
    return lerp(xs[segment - 1], xs[segment], starstaveSegmentProgress(reach, segment))
}

internal fun starstaveHeadY(reach: Float): Float {
    val segment = starstaveSegment(reach)
    val ys = StarstaveScene.NodeY
    return lerp(ys[segment - 1], ys[segment], starstaveSegmentProgress(reach, segment))
}

/**
 * True on the physical bottom screen. A swap trades the roles inside each window and never moves
 * a window, so the display id keeps the scene aligned across the hinge. One screen shows the sky.
 */
internal fun starstaveShowsDial(
    windowDisplayId: Int?,
    primaryDisplayId: Int,
    hasBothScreens: Boolean,
): Boolean = hasBothScreens && windowDisplayId != null && windowDisplayId != primaryDisplayId

/** Fits the window's height, so marks keep their size on either physical screen. */
internal fun starstaveScale(heightPx: Float): Float = heightPx / StarstaveScene.SCREEN_HEIGHT

internal fun starstaveHalfWidth(widthPx: Float, heightPx: Float): Float =
    widthPx / (2f * starstaveScale(heightPx))

internal fun starstaveOriginY(showsDial: Boolean): Float =
    if (showsDial) StarstaveScene.SCREEN_HEIGHT else 0f
