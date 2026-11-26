package jr.brian.jarnfe.wallpaper

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** An even grid of small outlines in the shared scene, swelling as a slow diagonal wave passes. */
internal object TidemarkScene {
    const val KIND_CROSS = 0
    const val KIND_TRIANGLE = 1
    const val KIND_SQUARE = 2
    const val KIND_CIRCLE = 3
    const val KIND_COUNT = 4

    const val CELL = 45f
    const val PATTERN_SIZE = 4
    const val TILE_SIZE = CELL * PATTERN_SIZE

    const val BASE_HALF = 7f
    const val CREST_HALF = 15f
    const val STROKE = 2.2f
    const val BASE_ALPHA = 0.5f

    /** Seconds for one crest to pass a point. */
    const val SWELL_PERIOD_SECONDS = 8.0
    const val WAVELENGTH = 420f

    /** Only the top of each swell lifts out of the base grid, which keeps crest cells few. */
    const val CREST_START = 0.88f

    const val CREST_STRIDE = 4
    const val CREST_X = 0
    const val CREST_Y = 1
    const val CREST_AMOUNT = 2
    const val CREST_KIND = 3

    // A Latin square: every row and column holds each kind once, so the mix stays even.
    val Pattern = intArrayOf(
        KIND_CROSS, KIND_CIRCLE, KIND_SQUARE, KIND_TRIANGLE,
        KIND_SQUARE, KIND_TRIANGLE, KIND_CROSS, KIND_CIRCLE,
        KIND_CIRCLE, KIND_CROSS, KIND_TRIANGLE, KIND_SQUARE,
        KIND_TRIANGLE, KIND_SQUARE, KIND_CIRCLE, KIND_CROSS,
    )

    /** The wave travels down and right, so crests run from the top screen into the bottom. */
    val DIR_X = (1 / sqrt(2.0)).toFloat()
    val DIR_Y = (1 / sqrt(2.0)).toFloat()

    /** Where the still frame parks a crest: the upper middle of the top screen. */
    const val STILL_CREST_X = 0f
    const val STILL_CREST_Y = 330f
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val FULL_TURN = 2 * PI
private const val CHANNEL_MASK = 0xFF

private fun floorMod(value: Int, mod: Int): Int = ((value % mod) + mod) % mod

/** Seconds at which a crest sits on the still point, so the first live frame matches the still. */
internal val TIDEMARK_STILL_SECONDS: Double = run {
    val along = TidemarkScene.STILL_CREST_X * TidemarkScene.DIR_X +
        TidemarkScene.STILL_CREST_Y * TidemarkScene.DIR_Y
    val wrapped = ((along % TidemarkScene.WAVELENGTH) + TidemarkScene.WAVELENGTH) % TidemarkScene.WAVELENGTH
    wrapped / TidemarkScene.WAVELENGTH * TidemarkScene.SWELL_PERIOD_SECONDS
}

internal fun tidemarkSeconds(timeNanos: Long, isStill: Boolean): Double =
    if (isStill) {
        TIDEMARK_STILL_SECONDS
    } else {
        TIDEMARK_STILL_SECONDS + timeNanos / NANOS_PER_SECOND
    }

/** How far the crests have travelled along the wave direction, wrapping every wavelength. */
internal fun tidemarkPhase(seconds: Double): Float {
    val period = TidemarkScene.SWELL_PERIOD_SECONDS
    return ((seconds % period) / period * TidemarkScene.WAVELENGTH).toFloat()
}

/** 1 on a crest, 0 in a trough, for a point in scene space. */
internal fun tidemarkLevel(x: Float, y: Float, phase: Float): Float {
    val along = x * TidemarkScene.DIR_X + y * TidemarkScene.DIR_Y - phase
    return ((1 + cos(FULL_TURN * along / TidemarkScene.WAVELENGTH)) / 2).toFloat()
}

/** How far a cell has lifted out of the base grid, eased so it swells in and out smoothly. */
internal fun tidemarkCrestAmount(level: Float): Float {
    val t = ((level - TidemarkScene.CREST_START) / (1f - TidemarkScene.CREST_START)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

internal fun tidemarkHalfSize(amount: Float): Float =
    TidemarkScene.BASE_HALF + (TidemarkScene.CREST_HALF - TidemarkScene.BASE_HALF) * amount

/** Blends two opaque colors without allocating, for use inside a frame. */
internal fun tidemarkMix(from: Int, to: Int, amount: Float): Int {
    var result = from and (CHANNEL_MASK shl 24)
    for (shift in ColorShifts) {
        val start = from shr shift and CHANNEL_MASK
        val end = to shr shift and CHANNEL_MASK
        result = result or ((start + (end - start) * amount).roundToInt().coerceIn(0, CHANNEL_MASK) shl shift)
    }
    return result
}

private val ColorShifts = intArrayOf(16, 8, 0)

internal fun tidemarkKind(col: Int, row: Int): Int {
    val size = TidemarkScene.PATTERN_SIZE
    return TidemarkScene.Pattern[floorMod(row, size) * size + floorMod(col, size)]
}

internal fun tidemarkCellCenter(index: Int): Float = (index + 0.5f) * TidemarkScene.CELL

internal fun tidemarkFirstRow(top: Float): Int = floor(top / TidemarkScene.CELL).toInt()

internal fun tidemarkLastRow(top: Float): Int =
    ceil((top + JARN_WALLPAPER_SCREEN_HEIGHT) / TidemarkScene.CELL).toInt() - 1

internal fun tidemarkFirstCol(halfWidth: Float): Int = floor(-halfWidth / TidemarkScene.CELL).toInt()

internal fun tidemarkLastCol(halfWidth: Float): Int = ceil(halfWidth / TidemarkScene.CELL).toInt() - 1

/** Room for every visible cell, so [tidemarkFillCrest] never runs out. */
internal fun tidemarkCrestCapacity(halfWidth: Float, top: Float): Int {
    val cols = tidemarkLastCol(halfWidth) - tidemarkFirstCol(halfWidth) + 1
    val rows = tidemarkLastRow(top) - tidemarkFirstRow(top) + 1
    return cols * rows * TidemarkScene.CREST_STRIDE
}

/** Writes each visible crest cell into [out] (stride [TidemarkScene.CREST_STRIDE]) and returns the count. */
internal fun tidemarkFillCrest(phase: Float, halfWidth: Float, top: Float, out: FloatArray): Int {
    var count = 0
    val capacity = out.size / TidemarkScene.CREST_STRIDE
    for (row in tidemarkFirstRow(top)..tidemarkLastRow(top)) {
        val y = tidemarkCellCenter(row)
        for (col in tidemarkFirstCol(halfWidth)..tidemarkLastCol(halfWidth)) {
            val x = tidemarkCellCenter(col)
            val amount = tidemarkCrestAmount(tidemarkLevel(x, y, phase))
            if (amount <= 0f || count == capacity) continue
            val at = count * TidemarkScene.CREST_STRIDE
            out[at + TidemarkScene.CREST_X] = x
            out[at + TidemarkScene.CREST_Y] = y
            out[at + TidemarkScene.CREST_AMOUNT] = amount
            out[at + TidemarkScene.CREST_KIND] = tidemarkKind(col, row).toFloat()
            count++
        }
    }
    return count
}
