package jr.brian.jarnfe.wallpaper

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Outlined shapes riding a swell that flows down the shared 1920 by 2160 scene. */
internal object TidemarkScene {
    const val KIND_CROSS = 0
    const val KIND_TRIANGLE = 1
    const val KIND_SQUARE = 2
    const val KIND_CIRCLE = 3
    const val KIND_COUNT = 4

    const val SHAPE_STRIDE = 5
    const val SHAPE_X = 0
    const val SHAPE_Y = 1
    const val SHAPE_SIZE = 2
    const val SHAPE_TURN = 3
    const val SHAPE_TONE = 4

    const val SCENE_HEIGHT = 2160f
    const val BOB = 12f
    const val SWAY_DEGREES = 6f
    const val SQUASH = 0.08f
    const val MAX_TURN_DEGREES = 18f
    const val MIN_SIZE = 30f
    const val SIZE_RANGE = 26f

    /** The first band is the swell the shapes ride, the others add depth. */
    val BandLength = floatArrayOf(620f, 410f, 900f)
    val BandPeriod = doubleArrayOf(8.0, 6.5, 10.0)
    val BandTiltDegrees = floatArrayOf(7f, 4f, 10f)
    val BandAlpha = floatArrayOf(0.38f, 0.55f, 0.35f)
    val BandUsesGlow = booleanArrayOf(true, false, false)

    private const val TOP_COLS = 4
    private const val TOP_ROWS = 3
    private const val BOTTOM_COLS = 4
    private const val BOTTOM_ROWS = 2
    const val SHAPE_COUNT = TOP_COLS * TOP_ROWS + BOTTOM_COLS * BOTTOM_ROWS

    val Kinds = IntArray(SHAPE_COUNT)
    val Shapes: FloatArray = seededShapes(Kinds)

    /** Fills [kinds] too. Each row holds one of every kind, so the mix stays even. */
    fun seededShapes(kinds: IntArray): FloatArray {
        val random = JarnWallpaperRandom(SHAPE_SEED)
        val shapes = FloatArray(SHAPE_COUNT * SHAPE_STRIDE)
        var index = 0
        fun place(cols: Int, rows: Int, width: Float, top: Float) {
            val cellWidth = width / cols
            val cellHeight = JARN_WALLPAPER_SCREEN_HEIGHT / rows
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val at = index * SHAPE_STRIDE
                    val jitterX = (random.next() * 2 - 1) * JITTER * cellWidth
                    val jitterY = (random.next() * 2 - 1) * JITTER * cellHeight
                    shapes[at + SHAPE_X] = (-width / 2 + (col + 0.5f) * cellWidth + jitterX).toFloat()
                    shapes[at + SHAPE_Y] = (top + (row + 0.5f) * cellHeight + jitterY).toFloat()
                    shapes[at + SHAPE_SIZE] = (MIN_SIZE + random.next() * SIZE_RANGE).toFloat()
                    shapes[at + SHAPE_TURN] = ((random.next() * 2 - 1) * MAX_TURN_DEGREES).toFloat()
                    shapes[at + SHAPE_TONE] = if (random.next() < NEAR_SHARE) 1f else 0f
                    kinds[index] = (col + row) % KIND_COUNT
                    index++
                }
            }
        }
        place(TOP_COLS, TOP_ROWS, TOP_WIDTH, 0f)
        place(BOTTOM_COLS, BOTTOM_ROWS, BOTTOM_WIDTH, JARN_WALLPAPER_SCREEN_HEIGHT)
        return shapes
    }

    private const val SHAPE_SEED = 5151L
    private const val TOP_WIDTH = 1760f
    // The bottom screen is narrower, so its shapes stay near the middle.
    private const val BOTTOM_WIDTH = 1040f
    private const val JITTER = 0.3
    private const val NEAR_SHARE = 0.5
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val FULL_TURN = 2 * PI
private const val DEGREES_TO_RADIANS = PI / 180
private val RideCos = cos(TidemarkScene.BandTiltDegrees[0] * DEGREES_TO_RADIANS).toFloat()
private val RideSin = sin(TidemarkScene.BandTiltDegrees[0] * DEGREES_TO_RADIANS).toFloat()

/** Starts live time at the still moment, so the first moving frame matches the still. */
internal const val TIDEMARK_STILL_SECONDS = 1.5

internal fun tidemarkSeconds(timeNanos: Long, isStill: Boolean): Double =
    if (isStill) {
        TIDEMARK_STILL_SECONDS
    } else {
        TIDEMARK_STILL_SECONDS + timeNanos / NANOS_PER_SECOND
    }

/** How far a band has flowed along its own direction, wrapping every swell length. */
internal fun tidemarkSwellOffset(seconds: Double, band: Int): Float {
    val period = TidemarkScene.BandPeriod[band]
    return ((seconds % period) / period * TidemarkScene.BandLength[band]).toFloat()
}

/**
 * Where a point sits on the ridden swell. Matches the band drawn rotated by its tilt and
 * shifted by [offset], so the crest (cosine at -1) lines up with the band's brightest line.
 */
internal fun tidemarkWaveAngle(x: Float, y: Float, offset: Float): Float {
    val along = y * RideCos - x * RideSin - offset
    return (FULL_TURN * along / TidemarkScene.BandLength[0]).toFloat()
}

/** Negative is up, so a shape rises as the crest passes under it. */
internal fun tidemarkLift(angle: Float): Float = TidemarkScene.BOB * cos(angle)

internal fun tidemarkTurn(angle: Float, baseTurn: Float): Float =
    baseTurn + TidemarkScene.SWAY_DEGREES * sin(angle)

/** Flattens a little on the crest, which reads as the outline bending with the water. */
internal fun tidemarkSquash(angle: Float): Float =
    1f - TidemarkScene.SQUASH * (1f - cos(angle)) / 2f
