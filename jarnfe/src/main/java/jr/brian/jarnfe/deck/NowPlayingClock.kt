package jr.brian.jarnfe.deck

import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.library.JarnLibraryGame
import java.util.Locale

private const val MS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60L
private const val MS_PER_SECOND = 1_000L

/** After this long dimmed, the readout drops to the lowest text tone. */
internal const val NOW_PLAYING_LONG_DIM_MS = 30 * MS_PER_MINUTE

/** How often the dimmed readout moves, and how far it may go, against burn-in. */
internal const val NOW_PLAYING_DRIFT_EVERY_MS = MS_PER_MINUTE
internal const val NOW_PLAYING_DRIFT_MAX_DP = 8

/** A slow walk around an 8 dp square, so no pixel stays lit for long. */
private val DriftSteps = listOf(0 to 0, 4 to 0, 8 to 0, 8 to 4, 8 to 8, 4 to 8, 0 to 8, 0 to 4, 4 to 4)

/** Hours and minutes since the session started, as HH:MM. Never negative. */
internal fun nowPlayingSessionText(elapsedMs: Long): String {
    val minutes = elapsedMs.coerceAtLeast(0L) / MS_PER_MINUTE
    return String.format(Locale.ROOT, "%02d:%02d", minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}

/** Time until the next whole minute since [anchorMs]. Always at least 1 ms, so a loop never spins. */
internal fun nextMinuteDelayMs(anchorMs: Long, nowMs: Long): Long {
    val into = Math.floorMod(nowMs - anchorMs, MS_PER_MINUTE)
    return (MS_PER_MINUTE - into).coerceAtLeast(1L)
}

/** Delay before the page dims after the last wake, or null when it never dims. */
internal fun JarnPlayingDim.delayMs(): Long? = seconds?.let { it * MS_PER_SECOND }

internal fun nowPlayingDimmed(dim: JarnPlayingDim, lastWakeMs: Long, nowMs: Long): Boolean {
    val delay = dim.delayMs() ?: return false
    return nowMs - lastWakeMs >= delay
}

/** True once the page has been dimmed for half an hour. */
internal fun nowPlayingLongDim(dimmedSinceMs: Long?, nowMs: Long): Boolean =
    dimmedSinceMs != null && nowMs - dimmedSinceMs >= NOW_PLAYING_LONG_DIM_MS

/** The dimmed readout's offset in dp for the [step]th minute dimmed. Inside the drift square. */
internal fun nowPlayingDrift(step: Long): Pair<Int, Int> =
    DriftSteps[(step.coerceAtLeast(0L) % DriftSteps.size).toInt()]

/**
 * The Library entry for the launched game. Library ids end in the relative path, and the launch
 * path is that path or an absolute path ending in it. The title is the fallback.
 */
internal fun matchesNowPlaying(game: JarnLibraryGame, playing: NowPlayingGame): Boolean {
    if (game.systemId != playing.systemId) return false
    val relative = game.id.split(":", limit = 3).getOrNull(2)
    if (!relative.isNullOrBlank()) {
        return playing.path == relative || playing.path.endsWith("/$relative")
    }
    return game.title == playing.title
}
