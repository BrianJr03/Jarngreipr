package jr.brian.jarnfe.wallpaper

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Seeded exactly like the design, in the shared 1920 by 2160 scene. */
internal object HearthriseScene {
    const val EMBER_COUNT = 38
    const val EMBER_STRIDE = 4
    const val EMBER_X = 0
    const val EMBER_SPEED = 1
    const val EMBER_PHASE = 2
    const val EMBER_SIZE = 3
    const val LEVEL_COUNT = 3
    const val EMBER_START_Y = 2150f
    const val EMBER_RANGE = 1940f
    const val EMBER_CULL_MARGIN = 8f

    const val SEAM_COUNT = 7
    const val SEAM_POINTS = 7
    const val SEAM_MIN_WIDTH = 2.4f

    const val COAL_COUNT = 3
    const val COAL_CENTER_Y = 2260f
    const val COAL_INNER_RADIUS = 30f
    const val COAL_OUTER_RADIUS = 760f
    const val COAL_MID_STOP = 0.46f
    const val COAL_TOP = 1200f
    const val SCENE_BOTTOM = 2160f

    val LevelAlphas = floatArrayOf(0.55f, 0.8f, 1f)
    val CoalX = floatArrayOf(-360f, 60f, 430f)
    val CoalPhase = floatArrayOf(0f, 2.1f, 4.2f)
    val Embers: FloatArray = seededEmbers()
    val Seams: FloatArray = seededSeams()

    private fun seededEmbers(): FloatArray {
        val random = JarnWallpaperRandom(EMBER_SEED)
        val embers = FloatArray(EMBER_COUNT * EMBER_STRIDE)
        for (i in 0 until EMBER_COUNT) {
            val at = i * EMBER_STRIDE
            val sizeRoll = random.next()
            val spreadA = random.next()
            val spreadB = random.next()
            embers[at + EMBER_X] = ((spreadA + spreadB - 1) * EMBER_SPREAD).toFloat()
            embers[at + EMBER_SPEED] = (MIN_SPEED + random.next() * SPEED_RANGE).toFloat()
            embers[at + EMBER_PHASE] = random.next().toFloat()
            embers[at + EMBER_SIZE] = (MIN_SIZE + sizeRoll * sizeRoll * SIZE_RANGE).toFloat()
        }
        return embers
    }

    private fun seededSeams(): FloatArray {
        val random = JarnWallpaperRandom(SEAM_SEED)
        val seams = FloatArray(SEAM_COUNT * SEAM_POINTS * 2)
        var at = 0
        for (seam in 0 until SEAM_COUNT) {
            var x = SEAM_START_X + random.next() * SEAM_START_X_RANGE + seam * SEAM_STEP_X
            var y = SEAM_START_Y + random.next() * SEAM_START_Y_RANGE
            seams[at++] = x.toFloat()
            seams[at++] = y.toFloat()
            repeat(SEAM_POINTS - 1) {
                x += SEAM_MIN_RUN + random.next() * SEAM_RUN_RANGE
                y += (random.next() - SEAM_RISE_BIAS) * SEAM_RISE_RANGE
                seams[at++] = x.toFloat()
                seams[at++] = y.toFloat()
            }
        }
        return seams
    }

    private const val EMBER_SEED = 9187L
    private const val EMBER_SPREAD = 760.0
    private const val MIN_SPEED = 15.0
    private const val SPEED_RANGE = 22.0
    private const val MIN_SIZE = 4.2
    private const val SIZE_RANGE = 7.0
    private const val SEAM_SEED = 771L
    private const val SEAM_START_X = -620.0
    private const val SEAM_START_X_RANGE = 220.0
    private const val SEAM_STEP_X = 170
    private const val SEAM_START_Y = 1850.0
    private const val SEAM_START_Y_RANGE = 250.0
    private const val SEAM_MIN_RUN = 35.0
    private const val SEAM_RUN_RANGE = 65.0
    private const val SEAM_RISE_BIAS = 0.45
    private const val SEAM_RISE_RANGE = 72.0
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val COAL_BASE_ALPHA = 0.62
private const val COAL_ALPHA_SWING = 0.16
private const val COAL_SPEED = 0.22
private const val SEAM_BASE_ALPHA = 0.42
private const val SEAM_ALPHA_SWING = 0.14
private const val SEAM_SPEED = 0.3
private const val SWAY_SPEED = 0.16
private const val SWAY_PHASE = 18.0
private const val SWAY_REACH = 34.0
private const val EMBER_MIN_SHRINK = 0.6f
private const val EMBER_SHRINK_RANGE = 0.4f
private const val EMBER_MIN_PX = 0.9f

internal const val HEARTHRISE_STILL_SECONDS = 64.0

internal fun hearthriseSeconds(timeNanos: Long, isStill: Boolean): Double =
    if (isStill) {
        HEARTHRISE_STILL_SECONDS
    } else {
        HEARTHRISE_STILL_SECONDS + timeNanos / NANOS_PER_SECOND
    }

internal fun hearthriseCoalAlpha(seconds: Double, phase: Float): Float =
    (COAL_BASE_ALPHA + COAL_ALPHA_SWING * sin(seconds * COAL_SPEED + phase)).toFloat()

internal fun hearthriseSeamAlpha(seconds: Double): Float =
    (SEAM_BASE_ALPHA + SEAM_ALPHA_SWING * sin(seconds * SEAM_SPEED)).toFloat()

internal fun hearthriseTravel(seconds: Double, speed: Float, phase: Float): Float {
    val range = HearthriseScene.EMBER_RANGE.toDouble()
    val travel = ((seconds * speed + phase * range) % range).toFloat()
    return if (travel >= HearthriseScene.EMBER_RANGE) 0f else travel
}

internal fun hearthriseLife(travel: Float): Float = travel / HearthriseScene.EMBER_RANGE

internal fun hearthriseEmberY(travel: Float): Float = HearthriseScene.EMBER_START_Y - travel

internal fun hearthriseEmberX(seconds: Double, baseX: Float, phase: Float): Float =
    (baseX + sin(seconds * SWAY_SPEED + phase * SWAY_PHASE) * SWAY_REACH).toFloat()

internal fun hearthriseEmberLevel(life: Float): Int =
    min(HearthriseScene.LEVEL_COUNT - 1, floor(sin(PI * life) * HearthriseScene.LEVEL_COUNT).toInt())
        .coerceAtLeast(0)

internal fun hearthriseEmberRadius(size: Float, life: Float, pixel: Float): Float =
    max(size * (EMBER_MIN_SHRINK + EMBER_SHRINK_RANGE * (1f - life)), EMBER_MIN_PX * pixel)

internal fun hearthriseEmberIsOnScreen(y: Float, top: Float): Boolean =
    y >= top - HearthriseScene.EMBER_CULL_MARGIN &&
        y <= top + JARN_WALLPAPER_SCREEN_HEIGHT + HearthriseScene.EMBER_CULL_MARGIN

/** Canvas gradients start at an inner radius. Android's start at zero, so the stops shift out. */
internal fun hearthriseCoalStops(): FloatArray {
    val inner = HearthriseScene.COAL_INNER_RADIUS
    val outer = HearthriseScene.COAL_OUTER_RADIUS
    val middle = inner + HearthriseScene.COAL_MID_STOP * (outer - inner)
    return floatArrayOf(inner / outer, middle / outer, 1f)
}
