package jr.brian.jarnfe.deck

import android.os.SystemClock
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.launch.JarnGameRef

/** A window that stops later than this after a launch was not covered by the game. */
internal const val NOW_PLAYING_COVER_GRACE_MS = 30_000L

/** The game a launch from Jarn started, as the launch knew it. */
@Immutable
internal data class NowPlayingGame(
    val systemId: String,
    val path: String,
    val title: String,
    val artworkPath: String? = null,
)

internal fun JarnGameRef.toNowPlayingGame() = NowPlayingGame(systemName, path, title, artworkPath)

/**
 * One game session as Jarn's free screen sees it. Android has no "game exited" signal, so the
 * session ends when the Jarn window the game covered comes back. [gameHost] is that window, by
 * the role it hosts unswapped (the Stage window sits on the top display). The other window
 * resuming, as a tap on it does, is never that signal, and neither is it being covered later,
 * for example by a companion.
 *
 * [expectedHost] is the window on the display the launcher sent the game to, when it knows. Then
 * only that window's stop counts, so a companion opening first on the other screen is never
 * mistaken for the game.
 *
 * [dismissed] is B or Show Deck: the page stays away for this session and a header pill brings
 * it back. [companions] are the launch's companion choices, kept so the page can offer them
 * again after the sheet closes.
 */
@Immutable
internal data class NowPlayingSession(
    val game: NowPlayingGame,
    val launchedAtUptimeMs: Long,
    val startedAtWallMs: Long,
    val gameHost: JarnScreen? = null,
    val expectedHost: JarnScreen? = null,
    val dismissed: Boolean = false,
    val companions: List<String> = emptyList(),
)

internal fun JarnScreen.otherScreen(): JarnScreen =
    if (this == JarnScreen.Stage) JarnScreen.Deck else JarnScreen.Stage

/** The role a window shows: swapped screens trade roles between the two windows. */
internal fun roleShownBy(host: JarnScreen, rolesSwapped: Boolean): JarnScreen =
    if (rolesSwapped) host.otherScreen() else host

/** The window on [displayId]: Stage on the primary display, Deck on the bottom one. */
internal fun gameHostOn(displayId: Int?, primaryDisplayId: Int, deckDisplayId: Int?): JarnScreen? = when (displayId) {
    null -> null
    primaryDisplayId -> JarnScreen.Stage
    deckDisplayId -> JarnScreen.Deck
    else -> null
}

/**
 * The first Jarn window to stop soon after a launch is the one the game covers, or the expected
 * one when the launcher said where the game went.
 */
internal fun NowPlayingSession.onWindowStopped(host: JarnScreen, nowUptimeMs: Long): NowPlayingSession =
    if (
        gameHost == null &&
        (expectedHost == null || expectedHost == host) &&
        nowUptimeMs - launchedAtUptimeMs in 0..NOW_PLAYING_COVER_GRACE_MS
    ) {
        copy(gameHost = host)
    } else {
        this
    }

/** Null once the session is over: the covered window is back, or the game never covered one. */
internal fun NowPlayingSession.onWindowResumed(host: JarnScreen, nowUptimeMs: Long): NowPlayingSession? = when {
    gameHost == host -> null
    gameHost == null && nowUptimeMs - launchedAtUptimeMs > NOW_PLAYING_COVER_GRACE_MS -> null
    else -> this
}

/** True while a game covers a Jarn window, whether or not the page is showing. */
internal val NowPlayingSession?.isGameInFront: Boolean get() = this?.gameHost != null

/**
 * The role shown by the window the game left free, on two screens only. Null on one screen or
 * with no game in front.
 */
internal fun nowPlayingFreeRole(
    session: NowPlayingSession?,
    deckDisplayAvailable: Boolean,
    rolesSwapped: Boolean,
): JarnScreen? {
    val covered = session?.gameHost ?: return null
    if (!deckDisplayAvailable) return null
    return roleShownBy(covered.otherScreen(), rolesSwapped)
}

/**
 * The role that shows Now Playing, or null. It needs the setting, two screens, a game in front
 * and the player not having left it. The covered window taking OS focus back means Jarn is in
 * front again there, even before its resume lands.
 */
internal fun nowPlayingRole(
    session: NowPlayingSession?,
    enabled: Boolean,
    deckDisplayAvailable: Boolean,
    rolesSwapped: Boolean,
    osFocusedScreen: JarnScreen?,
): JarnScreen? {
    if (!enabled || session == null || session.dismissed) return null
    val free = nowPlayingFreeRole(session, deckDisplayAvailable, rolesSwapped) ?: return null
    return free.takeIf { osFocusedScreen != free.otherScreen() }
}

/** The role whose header carries the way back to a page the player left, or null. */
internal fun nowPlayingPillRole(
    session: NowPlayingSession?,
    enabled: Boolean,
    deckDisplayAvailable: Boolean,
    rolesSwapped: Boolean,
): JarnScreen? {
    if (!enabled || session?.dismissed != true) return null
    return nowPlayingFreeRole(session, deckDisplayAvailable, rolesSwapped)
}

/** Main thread only. Kept apart from the page, so later bottom-screen work can read the same session. */
internal object JarnNowPlaying {
    var session by mutableStateOf<NowPlayingSession?>(null)
        private set

    fun onGameLaunched(
        game: NowPlayingGame,
        expectedHost: JarnScreen? = null,
        nowUptimeMs: Long = SystemClock.uptimeMillis(),
        nowWallMs: Long = System.currentTimeMillis(),
    ) {
        session = NowPlayingSession(game, nowUptimeMs, nowWallMs, expectedHost = expectedHost)
    }

    fun onCompanionsOffered(packages: List<String>) {
        session = session?.copy(companions = packages)
    }

    /** [host] is the reporting window's unswapped role, as the activities know it. */
    fun onWindowStopped(host: JarnScreen, nowUptimeMs: Long = SystemClock.uptimeMillis()) {
        session = session?.onWindowStopped(host, nowUptimeMs)
    }

    fun onWindowResumed(host: JarnScreen, nowUptimeMs: Long = SystemClock.uptimeMillis()) {
        session = session?.onWindowResumed(host, nowUptimeMs)
    }

    fun dismiss() {
        session = session?.copy(dismissed = true)
    }

    fun reopen() {
        session = session?.copy(dismissed = false)
    }

    internal fun reset() {
        session = null
    }
}
