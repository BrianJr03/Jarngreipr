package jr.brian.jarnfe.shell

import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import jr.brian.jarnfe.firstrun.JarnEsdeRomsReadyRow
import jr.brian.jarnfe.firstrun.JarnReadyRows
import jr.brian.jarnfe.firstrun.READY_ROW_GAMES_FOLDER
import jr.brian.jarnfe.home.JarnHomeReadyRowSource
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.launch.JarnGameActions
import jr.brian.jarnfe.launch.JarnGameActionsController
import jr.brian.jarnfe.launch.JarnGameFileStatus
import jr.brian.jarnfe.launch.JarnPlayController
import jr.brian.jarnfe.launch.jarnGameOptionsOrNull
import jr.brian.jarnfe.launch.jarnGameLauncherOrNull
import jr.brian.jarnfe.library.JarnFolderController
import jr.brian.jarnfe.library.JarnFolderPicker
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.library.jarnArtDetectorOrNull
import jr.brian.jarnfe.library.jarnLibraryDataOrNull
import jr.brian.jarnfe.library.jarnLibrarySetupOrNull
import jr.brian.jarnfe.session.JarnSession

private const val DECK_LAUNCH_TIMEOUT_MS = 5_000L

class JarnStageActivity : JarnScreenActivity() {
    override val screen = JarnScreen.Stage

    private val mainHandler = Handler(Looper.getMainLooper())
    private val deckLaunchTimeout = Runnable { onDeckLaunchFailed() }
    private val deckRecovery = JarnDeckRecoveryController(
        handler = mainHandler,
        deckDisplayId = { resolveDeckDisplayId(this) },
        isFinishing = { isFinishing },
        launch = ::launchDeck,
    )
    private var playHandler: ((JarnLibraryGame) -> Unit)? = null
    private var fileCheck: (suspend (JarnLibraryGame) -> JarnGameFileStatus)? = null
    private var folderHandler: ((Uri?) -> Unit)? = null
    private var actionsController: JarnGameActionsController? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = syncDeck()
        override fun onDisplayRemoved(displayId: Int) = syncDeck()
        override fun onDisplayChanged(displayId: Int) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JarnDeckWatch.attach(deckRecovery)
        connectPlay()
        connectFolderPicker()
        JarnHomeReadyRowSource.register()
        JarnReadyRows.register(READY_ROW_GAMES_FOLDER, JarnEsdeRomsReadyRow)
        getSystemService(DisplayManager::class.java)
            .registerDisplayListener(displayListener, mainHandler)
    }

    private fun connectFolderPicker() {
        val setup = jarnLibrarySetupOrNull() ?: return
        val controller = JarnFolderController(this, setup)
        folderHandler = controller::open
        JarnFolderPicker.open = folderHandler
    }

    /** Leaves onPlay null when launching is unavailable, so no Play hint is shown. */
    private fun connectPlay() {
        val data = jarnLibraryDataOrNull() ?: return
        val launcher = jarnGameLauncherOrNull() ?: return
        val options = jarnGameOptionsOrNull()
        val actions = options?.let { JarnGameActionsController(this, data, it) }
        actionsController = actions
        JarnGameActions.controller = actions
        val controller = JarnPlayController(this, data, launcher, options, actions)
        playHandler = controller::play
        JarnLibrary.onPlay = playHandler
        fileCheck = controller::fileStatus
        JarnLibrary.checkFile = fileCheck
    }

    /** Not onStart: the lifecycle only reports STARTED after onStart returns. */
    override fun onResume() {
        super.onResume()
        jarnLibrarySetupOrNull()?.recheck()
        jarnArtDetectorOrNull()?.check()
        syncDeck()
        deckRecovery.onStageResumed()
    }

    override fun onPause() {
        deckRecovery.onStagePaused()
        super.onPause()
    }

    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        deckRecovery.onStageFocusChanged(isTopResumedActivity)
    }

    /** Anything Jarn opens outside itself may land on the Deck display while the Deck is gone. */
    @Suppress("OVERRIDE_DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        if (intent.component?.packageName != packageName) deckRecovery.onOtherAppOpening()
        @Suppress("DEPRECATION")
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onDestroy() {
        deckRecovery.cancel()
        JarnDeckWatch.detach(deckRecovery)
        if (JarnLibrary.onPlay === playHandler) JarnLibrary.onPlay = null
        if (JarnLibrary.checkFile === fileCheck) JarnLibrary.checkFile = null
        if (JarnFolderPicker.open === folderHandler) JarnFolderPicker.open = null
        if (JarnGameActions.controller === actionsController) JarnGameActions.controller = null
        getSystemService(DisplayManager::class.java).unregisterDisplayListener(displayListener)
        mainHandler.removeCallbacks(deckLaunchTimeout)
        JarnSession.update { deckLaunchFailed() }
        super.onDestroy()
    }

    private fun syncDeck() {
        val deckDisplayId = resolveDeckDisplayId(this)
        if (deckDisplayId == null) JarnDeckWatch.onDisplayGone()
        if (!JarnSession.state.deckWindowLost) {
            JarnSession.update { withDeckDisplay(deckDisplayId != null) }
        }
        val isStarted = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        if (deckDisplayId != null &&
            JarnSession.state.shouldLaunchDeck(isStarted, JarnSession.deckHostCreated)
        ) {
            launchDeck(deckDisplayId)
        }
        deckRecovery.evaluate()
    }

    private fun onDeckLaunchFailed() {
        JarnSession.update { deckLaunchFailed() }
        deckRecovery.evaluate()
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
                onDeckLaunchFailed()
            }
    }
}
