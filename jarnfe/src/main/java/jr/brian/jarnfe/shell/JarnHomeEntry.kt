package jr.brian.jarnfe.shell

import android.os.SystemClock
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import jr.brian.core.model.JarnGameCover
import jr.brian.core.model.JarnGameCoverTracker
import jr.brian.core.model.JarnHomeWindow
import jr.brian.jarnfe.firstrun.JarnFirstRun
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession

internal val JarnScreen.homeWindow: JarnHomeWindow
    get() = if (this == JarnScreen.Stage) JarnHomeWindow.Stage else JarnHomeWindow.Deck

private val JarnHomeWindow.screen: JarnScreen
    get() = if (this == JarnHomeWindow.Stage) JarnScreen.Stage else JarnScreen.Deck

/**
 * What the host's Home routing needs from the shell. While a game stays on one screen, the Jarn
 * window beside it holds input until the user leaves it, then stops being focusable so Android
 * hands the controller back to the game without anything being relaunched over it.
 */
object JarnHomeEntry {
    private val tracker = JarnGameCoverTracker()

    internal var heldBy by mutableStateOf<JarnScreen?>(null)
        private set
    internal var yieldedBy by mutableStateOf<JarnScreen?>(null)
        private set

    private val isTourRunning: Boolean get() = JarnFirstRun.controller?.state != null

    val gameCover: JarnGameCover
        get() = if (isTourRunning) JarnGameCover.None else tracker.cover

    val shouldRaiseDeck: Boolean
        get() = JarnSession.deckHostCreated || JarnSession.state.deckWindowLost

    val isDeckUp: Boolean get() = JarnSession.deckHostCreated

    /** The tour keeps control where it is. Otherwise Home lands on Stage. */
    val focusWindow: JarnHomeWindow
        get() {
            val state = JarnSession.state
            val isDeckWindowActive = state.roleOn(JarnScreen.Deck) == state.activeScreen
            return if (isTourRunning && isDeckWindowActive) JarnHomeWindow.Deck else JarnHomeWindow.Stage
        }

    fun onShowingBesideGame(window: JarnHomeWindow) {
        val screen = window.screen
        JarnSession.update {
            val restored = onDeckWindowBack().deckLaunchFailed()
            restored.activate(restored.roleOn(screen))
        }
        yieldedBy = null
        heldBy = screen
    }

    fun onShowFailed() = clearGameFocus()

    internal fun onGameLaunched() = tracker.onGameLaunched(SystemClock.uptimeMillis())

    internal fun onWindowCovered(screen: JarnScreen) =
        tracker.onWindowStopped(screen.homeWindow, SystemClock.uptimeMillis())

    internal fun onWindowResumed(screen: JarnScreen) {
        tracker.onWindowResumed(screen.homeWindow)
        if (tracker.cover == JarnGameCover.None) clearGameFocus()
    }

    internal fun isCoveredByGame(screen: JarnScreen): Boolean = tracker.isCovered(screen.homeWindow)

    internal fun yieldToGame() {
        yieldedBy = heldBy ?: return
        heldBy = null
    }

    /** True when a touch took a yielded window back from the game. */
    internal fun reclaim(screen: JarnScreen): Boolean {
        if (yieldedBy != screen) return false
        yieldedBy = null
        heldBy = screen
        return true
    }

    private fun clearGameFocus() {
        heldBy = null
        yieldedBy = null
    }
}

@Composable
internal fun JarnGameFocusEffect(window: Window, screen: JarnScreen, isActive: Boolean) {
    val isHeld = JarnHomeEntry.heldBy == screen
    var hasHadControl by remember(isHeld) { mutableStateOf(false) }
    LaunchedEffect(isHeld, isActive) {
        when {
            !isHeld -> Unit
            isActive -> hasHadControl = true
            hasHadControl -> JarnHomeEntry.yieldToGame()
        }
    }
    val isYielded = JarnHomeEntry.yieldedBy == screen
    LaunchedEffect(isYielded) { window.setFocusable(!isYielded) }
}

internal fun Window.setFocusable(focusable: Boolean) {
    val flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    if (focusable) clearFlags(flag) else addFlags(flag)
}
