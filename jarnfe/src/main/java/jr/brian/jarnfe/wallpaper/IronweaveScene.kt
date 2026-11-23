package jr.brian.jarnfe.wallpaper

import kotlin.math.max
import kotlin.math.roundToInt

internal object IronweaveScene {
    const val SHEEN_START_X = -620f
    const val SHEEN_START_Y = 659f
    const val SHEEN_END_X = 620f
    const val SHEEN_END_Y = 1341f
    const val SHEEN_DRIFT_Y = 0.35f
    const val RING_BASE_ALPHA = 0.46f
    const val RING_RADIUS_RATIO = 28f / 64f
    const val RING_WIDTH_RATIO = 5.2f / 64f
    val RingCentersX = floatArrayOf(0f, 1f, 0f, 1f, 0.5f)
    val RingCentersY = floatArrayOf(0f, 0f, 1f, 1f, 0.5f)
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val SHEEN_SPEED = 72.0
private const val SHEEN_SPAN = 4300.0
private const val SHEEN_ENTRY = 1800.0
private const val STILL_SHEEN_POSITION = -340.0
private const val TILE_SIZE = 58f
private const val TILE_MIN_DEVICE_SIZE = 11f
private const val TILE_MIN_PIXELS = 8

/** Starts live time at the parked sheen, so the first moving frame matches the still. */
internal const val IRONWEAVE_STILL_SECONDS = (STILL_SHEEN_POSITION + SHEEN_ENTRY) / SHEEN_SPEED

internal fun ironweaveSeconds(timeNanos: Long, isStill: Boolean): Double =
    if (isStill) {
        IRONWEAVE_STILL_SECONDS
    } else {
        IRONWEAVE_STILL_SECONDS + timeNanos / NANOS_PER_SECOND
    }

internal fun ironweaveSheenPosition(seconds: Double): Float =
    ((seconds * SHEEN_SPEED % SHEEN_SPAN) - SHEEN_ENTRY).toFloat()

internal fun ironweaveTileSize(scale: Float): Float = max(TILE_SIZE, TILE_MIN_DEVICE_SIZE / scale)

internal fun ironweaveTilePixels(scale: Float): Int =
    max(TILE_MIN_PIXELS, (ironweaveTileSize(scale) * scale).roundToInt())

/** The resting ring color: the weave tone at its designed strength over the background. */
internal fun ironweaveRingBase(palette: JarnWallpaperPalette): Int =
    jarnWallpaperMix(palette.bg, palette.midGlow, IronweaveScene.RING_BASE_ALPHA)
