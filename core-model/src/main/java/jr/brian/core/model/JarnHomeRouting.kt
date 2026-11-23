package jr.brian.core.model

enum class JarnHomeWindow { Stage, Deck }

data class JarnHomeLaunch(val window: JarnHomeWindow, val displayId: Int)

/** [Single] leaves a running game in place. [Open] always brings up all of Jarn FE. */
enum class JarnHomePress { Single, Open }

/** Which physical screens a game launched from Jarn FE is covering. */
data class JarnGameCover(val top: Boolean = false, val bottom: Boolean = false) {
    companion object {
        val None = JarnGameCover()
    }
}

enum class JarnHomeAction { OpenJarn, ShowDeck, ShowStage, AskAgain }

/**
 * A single press never covers a game. With no free screen it asks for a second press, so Home
 * cannot trap the user in a game on paths that have no double or long press.
 */
fun resolveJarnHomeAction(
    press: JarnHomePress,
    cover: JarnGameCover,
    hasBottomDisplay: Boolean,
    isRepeatPress: () -> Boolean,
): JarnHomeAction {
    val coversBottom = cover.bottom && hasBottomDisplay
    return when {
        press == JarnHomePress.Open || (!cover.top && !coversBottom) -> JarnHomeAction.OpenJarn
        cover.top && (coversBottom || !hasBottomDisplay) ->
            if (isRepeatPress()) JarnHomeAction.OpenJarn else JarnHomeAction.AskAgain
        cover.top -> JarnHomeAction.ShowDeck
        else -> JarnHomeAction.ShowStage
    }
}

/**
 * What a Home press fires for Jarn FE. [focus] starts last so it takes input focus. A Deck that
 * does not exist yet is left to Stage, which owns its first launch.
 */
fun planJarnHomeLaunches(
    bottomDisplayId: Int?,
    shouldRaiseDeck: Boolean,
    focus: JarnHomeWindow = JarnHomeWindow.Stage,
): List<JarnHomeLaunch> {
    val stage = JarnHomeLaunch(JarnHomeWindow.Stage, PRIMARY_DISPLAY_ID)
    if (bottomDisplayId == null || !shouldRaiseDeck) return listOf(stage)
    val deck = JarnHomeLaunch(JarnHomeWindow.Deck, bottomDisplayId)
    return if (focus == JarnHomeWindow.Deck) listOf(stage, deck) else listOf(deck, stage)
}

/** True for a press that follows an unanswered one inside the window. */
class JarnHomeRepeat(private val windowMs: Long = DEFAULT_WINDOW_MS) {
    private var lastPressMs: Long? = null

    fun isRepeat(nowMs: Long): Boolean {
        val last = lastPressMs
        if (last != null && nowMs - last < windowMs) {
            lastPressMs = null
            return true
        }
        lastPressMs = nowMs
        return false
    }

    private companion object {
        const val DEFAULT_WINDOW_MS = 3_000L
    }
}

/**
 * Infers what a game covers, since Android does not report other apps' displays. A stop only
 * counts soon after a launch or while a game is already covering a window, and the game is over
 * once every window it covered has come back.
 */
class JarnGameCoverTracker(private val launchGraceMs: Long = DEFAULT_LAUNCH_GRACE_MS) {
    private var launchedAtMs: Long? = null
    private val covered = mutableSetOf<JarnHomeWindow>()

    val cover: JarnGameCover
        get() = JarnGameCover(
            top = JarnHomeWindow.Stage in covered,
            bottom = JarnHomeWindow.Deck in covered,
        )

    fun isCovered(window: JarnHomeWindow): Boolean = window in covered

    fun onGameLaunched(nowMs: Long) {
        launchedAtMs = nowMs
        covered.clear()
    }

    fun onWindowStopped(window: JarnHomeWindow, nowMs: Long) {
        val launchedAt = launchedAtMs ?: return
        if (covered.isEmpty() && nowMs - launchedAt > launchGraceMs) {
            launchedAtMs = null
            return
        }
        covered += window
    }

    fun onWindowResumed(window: JarnHomeWindow) {
        if (covered.remove(window) && covered.isEmpty()) launchedAtMs = null
    }

    private companion object {
        const val DEFAULT_LAUNCH_GRACE_MS = 15_000L
    }
}

/**
 * Android re-sends SECONDARY_HOME while the bottom display has no window, so a Deck that keeps
 * failing to open would bounce through the router forever. Repeated entries without a Deck hand
 * that press to Classic, which fills the display.
 */
class JarnHomeLoopGuard(
    private val limit: Int = DEFAULT_LIMIT,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
) {
    private val entries = ArrayDeque<Long>()

    fun allowsJarn(nowMs: Long, isDeckUp: Boolean): Boolean {
        if (isDeckUp) {
            entries.clear()
            return true
        }
        entries.removeAll { nowMs - it >= windowMs }
        entries.addLast(nowMs)
        if (entries.size < limit) return true
        entries.clear()
        return false
    }

    private companion object {
        const val DEFAULT_LIMIT = 4
        const val DEFAULT_WINDOW_MS = 3_000L
    }
}
