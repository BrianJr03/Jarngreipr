package jr.brian.jarnfe.shell

import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.session.JarnSession

private const val DECK_LAUNCH_TIMEOUT_MS = 5_000L

class JarnStageActivity : JarnScreenActivity() {
    override val screen = JarnScreen.Stage

    private val mainHandler = Handler(Looper.getMainLooper())
    private val deckLaunchTimeout = Runnable { JarnSession.update { deckLaunchFailed() } }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = syncDeck()
        override fun onDisplayRemoved(displayId: Int) = syncDeck()
        override fun onDisplayChanged(displayId: Int) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSystemService(DisplayManager::class.java)
            .registerDisplayListener(displayListener, mainHandler)
    }

    /** Not onStart: the lifecycle only reports STARTED after onStart returns. */
    override fun onResume() {
        super.onResume()
        syncDeck()
    }

    override fun onDestroy() {
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        mainHandler.removeCallbacks(deckLaunchTimeout)
        JarnSession.update { deckLaunchFailed() }
        super.onDestroy()
    }

    private fun syncDeck() {
        val deckDisplayId = resolveDeckDisplayId(this)
        JarnSession.update { withDeckDisplay(deckDisplayId != null) }
        val isStarted = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (deckDisplayId != null &&
            JarnSession.state.shouldLaunchDeck(isStarted, JarnSession.deckHostCreated)
        ) {
            launchDeck(deckDisplayId)
        }
    }

    private fun launchDeck(displayId: Int) {
        JarnSession.update { deckLaunchStarted() }
        mainHandler.removeCallbacks(deckLaunchTimeout)
        mainHandler.postDelayed(deckLaunchTimeout, DECK_LAUNCH_TIMEOUT_MS)
        val intent = Intent(this, JarnDeckActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        val options = ActivityOptions.makeBasic().setLaunchDisplayId(displayId)
        runCatching { startActivity(intent, options.toBundle()) }
            .onFailure {
                mainHandler.removeCallbacks(deckLaunchTimeout)
                JarnSession.update { deckLaunchFailed() }
            }
    }
}
