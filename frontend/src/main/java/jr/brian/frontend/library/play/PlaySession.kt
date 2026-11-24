package jr.brian.frontend.library.play

/** Shorter sessions are treated as failed launches and never counted. */
internal const val MIN_COUNTED_SESSION_SECONDS = 30

/** Longest time one session can add, for a game left running and forgotten. */
internal const val MAX_COUNTED_SESSION_SECONDS = 8 * 60 * 60

/** A window that stops later than this after a launch was not covered by the game. */
internal const val LAUNCH_COVER_WINDOW_MS = 30_000L

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

/**
 * The session in flight, saved so it outlives the process.
 *
 * Times use the uptime clock, which stops while the device sleeps, so sleep is
 * never counted as play. That clock restarts on boot, hence [bootCount].
 *
 * @param coveredScreen the Jarn window the game covered. Its return ends the session.
 */
internal data class PlayAnchor(
    val sessionId: Long,
    val startedUptimeMs: Long,
    val bootCount: Int?,
    val coveredScreen: String? = null,
)

internal sealed interface PlayStep {
    data object Wait : PlayStep

    /** Too short, never covered, or unmeasurable after a reboot. Adds nothing. */
    data object Discard : PlayStep

    data class Credit(val seconds: Int) : PlayStep
}

internal fun PlayAnchor.isSameBoot(nowUptimeMs: Long, nowBootCount: Int?): Boolean =
    nowUptimeMs >= startedUptimeMs && bootCount == nowBootCount

/** Marks [screen] as covered if it is the first window to stop soon after the launch. */
internal fun PlayAnchor.onWindowStopped(
    screen: String,
    nowUptimeMs: Long,
    nowBootCount: Int?,
): PlayAnchor? = takeIf {
    coveredScreen == null &&
        isSameBoot(nowUptimeMs, nowBootCount) &&
        nowUptimeMs - startedUptimeMs <= LAUNCH_COVER_WINDOW_MS
}?.copy(coveredScreen = screen)

internal fun PlayAnchor.onWindowResumed(
    screen: String,
    nowUptimeMs: Long,
    nowBootCount: Int?,
): PlayStep = when {
    !isSameBoot(nowUptimeMs, nowBootCount) -> PlayStep.Discard
    coveredScreen == null ->
        if (nowUptimeMs - startedUptimeMs > LAUNCH_COVER_WINDOW_MS) PlayStep.Discard else PlayStep.Wait
    coveredScreen != screen -> PlayStep.Wait
    else -> finished(nowUptimeMs)
}

/** What to do with this session when another game is launched before it ended. */
internal fun PlayAnchor.onNextLaunch(nowUptimeMs: Long, nowBootCount: Int?): PlayStep = when {
    coveredScreen == null || !isSameBoot(nowUptimeMs, nowBootCount) -> PlayStep.Discard
    else -> finished(nowUptimeMs)
}

private fun PlayAnchor.finished(nowUptimeMs: Long): PlayStep {
    val seconds = (nowUptimeMs - startedUptimeMs) / MILLIS_PER_SECOND
    return if (seconds < MIN_COUNTED_SESSION_SECONDS) {
        PlayStep.Discard
    } else {
        PlayStep.Credit(seconds.coerceAtMost(MAX_COUNTED_SESSION_SECONDS.toLong()).toInt())
    }
}

/** Whole minutes this session adds, carrying leftover seconds from the game's earlier sessions. */
internal fun creditedMinutes(priorSeconds: Long, sessionSeconds: Int): Int {
    val before = priorSeconds / SECONDS_PER_MINUTE
    val after = (priorSeconds + sessionSeconds) / SECONDS_PER_MINUTE
    return (after - before).toInt()
}
