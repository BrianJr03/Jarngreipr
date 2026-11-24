package jr.brian.jarnfe.shell

import android.os.Handler
import android.os.SystemClock
import android.view.KeyEvent
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnButtonMap
import jr.brian.jarnfe.session.DECK_FOCUS_SETTLE_MS
import jr.brian.jarnfe.session.JarnDeckClose
import jr.brian.jarnfe.session.JarnDeckRecovery
import jr.brian.jarnfe.session.JarnDeckSelectAction
import jr.brian.jarnfe.session.JarnDeckSurroundings
import jr.brian.jarnfe.session.JarnSession

/** Outlives both windows, so a recreated Stage still knows the display may be owned. */
internal object JarnDeckWatch {
    var recovery = JarnDeckRecovery()
        private set
    private var controller: JarnDeckRecoveryController? = null
    private var isSelectConsumed = false

    fun attach(next: JarnDeckRecoveryController) {
        controller = next
    }

    fun detach(previous: JarnDeckRecoveryController) {
        if (controller === previous) controller = null
    }

    fun update(transform: JarnDeckRecovery.() -> JarnDeckRecovery) {
        recovery = recovery.transform()
    }

    fun onDeckUp() {
        update { onDeckUp(SystemClock.elapsedRealtime()) }
        JarnSession.deckHostCreated = true
        JarnSession.update { onDeckWindowBack().withDeckHost(true) }
        controller?.evaluate()
    }

    fun onDeckClosed(close: JarnDeckClose?) {
        JarnSession.deckHostCreated = false
        JarnSession.update { withDeckHost(false) }
        if (close == null) return
        val state = JarnSession.state
        val hasDisplay = state.deckDisplayAvailable || state.deckWindowLost
        val effective = if (hasDisplay) close else JarnDeckClose.Deliberate
        update { onDeckClosed(effective, SystemClock.elapsedRealtime()) }
        if (effective != JarnDeckClose.Deliberate) JarnSession.update { onDeckWindowLost() }
        controller?.evaluate()
    }

    fun onDisplayGone() {
        recovery = JarnDeckRecovery()
        JarnSession.update { withDeckDisplay(false) }
    }

    fun onLeavingJarn() {
        recovery = JarnDeckRecovery()
        JarnSession.update { onDeckWindowAbandoned() }
        controller?.cancel()
    }

    /**
     * Claims the switch screen button (X) while the Deck is missing, so it brings the Deck back. A
     * claimed press also owns its repeats and key-up.
     */
    fun consumeSwitchScreen(event: KeyEvent): Boolean {
        if (JarnButtonMap.actionFor(event.keyCode) != JarnAction.SwitchScreen) return false
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            isSelectConsumed = JarnSession.state.deckWindowLost && controller != null
            if (isSelectConsumed) controller?.onSelectPressed()
        }
        val consumed = isSelectConsumed
        if (event.action == KeyEvent.ACTION_UP) isSelectConsumed = false
        return consumed
    }
}

internal class JarnDeckRecoveryController(
    private val handler: Handler,
    private val deckDisplayId: () -> Int?,
    private val isFinishing: () -> Boolean,
    private val launch: (Int) -> Unit,
) {
    private var isStageResumed = false
    private var wasToldToPressSelect = false
    private val retry = Runnable(::launchIfStillAllowed)
    private val focusSettled = Runnable {
        JarnDeckWatch.update { onFocusSettled() }
        evaluate()
    }

    fun onStageResumed() {
        isStageResumed = true
        evaluate()
    }

    fun onStagePaused() {
        isStageResumed = false
        handler.removeCallbacks(focusSettled)
        JarnDeckWatch.update { onStagePaused() }
        evaluate()
    }

    fun onStageFocusChanged(isTop: Boolean) {
        handler.removeCallbacks(focusSettled)
        if (isTop) {
            JarnDeckWatch.update { onStageFocusBack() }
        } else if (!JarnSession.state.awaitingDeckLaunch) {
            JarnDeckWatch.update { onStageLostFocus() }
            handler.postDelayed(focusSettled, DECK_FOCUS_SETTLE_MS)
        }
        evaluate()
    }

    fun onOtherAppOpening() {
        JarnDeckWatch.update { onOtherAppOpened() }
        evaluate()
    }

    fun evaluate() {
        handler.removeCallbacks(retry)
        val recovery = JarnDeckWatch.recovery
        if (!recovery.isMissing) wasToldToPressSelect = false
        recovery.nextRetryDelayMs(surroundings())?.let { handler.postDelayed(retry, it) }
        tellToPressSelectOnce()
    }

    fun onSelectPressed() {
        val result = JarnDeckWatch.recovery.onSelect(surroundings(), SystemClock.elapsedRealtime())
        JarnDeckWatch.update { result.next }
        when (result.action) {
            JarnDeckSelectAction.Relaunch -> {
                wasToldToPressSelect = false
                handler.removeCallbacks(retry)
                JarnNotices.show(R.string.jarnfe_deck_recovery_restoring)
                deckDisplayId()?.let(launch)
            }
            JarnDeckSelectAction.AskConfirm -> JarnNotices.show(R.string.jarnfe_deck_recovery_confirm)
            JarnDeckSelectAction.Waiting -> JarnNotices.show(R.string.jarnfe_deck_recovery_restoring)
            JarnDeckSelectAction.NotMissing -> Unit
        }
    }

    fun cancel() {
        handler.removeCallbacks(retry)
        handler.removeCallbacks(focusSettled)
    }

    private fun launchIfStillAllowed() {
        if (JarnDeckWatch.recovery.nextRetryDelayMs(surroundings()) == null) return
        val displayId = deckDisplayId() ?: return
        JarnDeckWatch.update { onRetryLaunched() }
        launch(displayId)
    }

    private fun tellToPressSelectOnce() {
        val canAct = isStageResumed && !JarnSession.state.awaitingDeckLaunch
        if (!canAct || wasToldToPressSelect || !JarnDeckWatch.recovery.needsSelect) return
        wasToldToPressSelect = true
        JarnNotices.show(R.string.jarnfe_deck_recovery_press_select)
    }

    private fun surroundings() = JarnDeckSurroundings(
        hasDeckDisplay = deckDisplayId() != null,
        isStageResumed = isStageResumed,
        isStageFinishing = isFinishing(),
        isDeckHosted = JarnSession.deckHostCreated,
        isLaunchPending = JarnSession.state.awaitingDeckLaunch,
    )
}
