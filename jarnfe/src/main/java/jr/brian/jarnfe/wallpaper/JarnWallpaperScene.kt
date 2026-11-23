package jr.brian.jarnfe.wallpaper

internal const val JARN_WALLPAPER_SCREEN_HEIGHT = 1080f

private const val LEHMER_MULTIPLIER = 16807L
private const val LEHMER_MODULUS = 2147483647L

/** The design's seeded generator, so scattered art lands exactly where it was approved. */
internal class JarnWallpaperRandom(private var seed: Long) {
    fun next(): Double {
        seed = seed * LEHMER_MULTIPLIER % LEHMER_MODULUS
        return (seed - 1).toDouble() / (LEHMER_MODULUS - 1)
    }
}

/**
 * True on the physical bottom screen. A swap trades the roles inside each window and never moves
 * a window, so the display id keeps the scene aligned across the hinge. One screen shows the top.
 */
internal fun jarnWallpaperIsBottomScreen(
    windowDisplayId: Int?,
    primaryDisplayId: Int,
    hasBothScreens: Boolean,
): Boolean = hasBothScreens && windowDisplayId != null && windowDisplayId != primaryDisplayId

/** Fits the window's height, so marks keep their size on either physical screen. */
internal fun jarnWallpaperScale(heightPx: Float): Float = heightPx / JARN_WALLPAPER_SCREEN_HEIGHT

internal fun jarnWallpaperHalfWidth(widthPx: Float, heightPx: Float): Float =
    widthPx / (2f * jarnWallpaperScale(heightPx))

internal fun jarnWallpaperOriginY(isBottomScreen: Boolean): Float =
    if (isBottomScreen) JARN_WALLPAPER_SCREEN_HEIGHT else 0f
