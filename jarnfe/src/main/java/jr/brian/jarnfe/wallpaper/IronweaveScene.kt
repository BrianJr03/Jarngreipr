package jr.brian.jarnfe.wallpaper

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

internal object IronweaveScene {
    const val WAVELENGTH = 480f
    const val WAVE_STOP_COUNT = 9
    const val CREST_ALPHA = 0.75f
    const val RING_BASE_ALPHA = 0.46f
    const val RING_RADIUS_RATIO = 28f / 64f
    const val RING_WIDTH_RATIO = 5.2f / 64f
    val RingCentersX = floatArrayOf(0f, 1f, 0f, 1f, 0.5f)
    val RingCentersY = floatArrayOf(0f, 0f, 1f, 1f, 0.5f)

    /** The wave travels along the old sheen's diagonal, about 29 degrees below horizontal. */
    val WaveDirectionX = (WAVE_RUN / hypot(WAVE_RUN, WAVE_RISE)).toFloat()
    val WaveDirectionY = (WAVE_RISE / hypot(WAVE_RUN, WAVE_RISE)).toFloat()

    /** Sampled from a cosine, so linear steps between them read as a sine. Mirrored exactly. */
    val WaveStops = FloatArray(WAVE_STOP_COUNT) { it / (WAVE_STOP_COUNT - 1f) }
    val WaveLevels = FloatArray(WAVE_STOP_COUNT) {
        ironweaveWaveLevel(WaveStops[minOf(it, WAVE_STOP_COUNT - 1 - it)])
    }
}

private const val NANOS_PER_SECOND = 1_000_000_000.0
private const val WAVE_RUN = 1240.0
private const val WAVE_RISE = 682.0
private const val WAVE_SPEED = 80.0
private const val STILL_CREST_X = 0f
private const val STILL_CREST_Y = 400f
private const val TILE_SIZE = 58f
private const val TILE_MIN_DEVICE_SIZE = 11f
private const val TILE_MIN_PIXELS = 8

/** Starts live time at the parked crest, so the first moving frame matches the still. */
internal val IRONWEAVE_STILL_SECONDS: Double = ironweaveStillPhase() / WAVE_SPEED

internal fun ironweaveSeconds(timeNanos: Long, isStill: Boolean): Double =
    if (isStill) {
        IRONWEAVE_STILL_SECONDS
    } else {
        IRONWEAVE_STILL_SECONDS + timeNanos / NANOS_PER_SECOND
    }

/** How far the wave has travelled along its direction, within one wavelength. */
internal fun ironweaveWavePhase(seconds: Double): Float =
    (seconds * WAVE_SPEED % IronweaveScene.WAVELENGTH).toFloat()

/** Brightness 0 (trough) to 1 (crest) at [cycle], the fraction of one wavelength. */
internal fun ironweaveWaveLevel(cycle: Float): Float = (0.5 - 0.5 * cos(2 * PI * cycle)).toFloat()

/** The wave's brightness at a scene point, matching what the repeating gradient paints. */
internal fun ironweaveWaveLevelAt(sceneX: Float, sceneY: Float, phase: Float): Float {
    val along = sceneX * IronweaveScene.WaveDirectionX + sceneY * IronweaveScene.WaveDirectionY - phase
    return ironweaveWaveLevel(along / IronweaveScene.WAVELENGTH)
}

/** Parks a crest in the upper middle of the top screen, below the header band. */
private fun ironweaveStillPhase(): Double {
    val along = STILL_CREST_X * IronweaveScene.WaveDirectionX + STILL_CREST_Y * IronweaveScene.WaveDirectionY
    val wavelength = IronweaveScene.WAVELENGTH.toDouble()
    return ((along - wavelength / 2) % wavelength + wavelength) % wavelength
}

internal fun ironweaveTileSize(scale: Float): Float = max(TILE_SIZE, TILE_MIN_DEVICE_SIZE / scale)

internal fun ironweaveTilePixels(scale: Float): Int =
    max(TILE_MIN_PIXELS, (ironweaveTileSize(scale) * scale).roundToInt())

/** The resting ring color: the weave tone at its designed strength over the background. */
internal fun ironweaveRingBase(palette: JarnWallpaperPalette): Int =
    jarnWallpaperMix(palette.bg, palette.midGlow, IronweaveScene.RING_BASE_ALPHA)

/** One color per wave stop, from the resting rings in the trough to the glow at the crest. */
internal fun ironweaveWaveColors(palette: JarnWallpaperPalette): IntArray {
    val trough = ironweaveRingBase(palette)
    val crest = jarnWallpaperMix(trough, palette.glow, IronweaveScene.CREST_ALPHA)
    return IntArray(IronweaveScene.WAVE_STOP_COUNT) {
        jarnWallpaperMix(trough, crest, IronweaveScene.WaveLevels[it])
    }
}
