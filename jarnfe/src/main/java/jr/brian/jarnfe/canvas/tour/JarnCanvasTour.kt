package jr.brian.jarnfe.canvas.tour

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.StageRoute
import java.io.File

private const val SEEN_MARKER = "jarnfe_canvas_tour_seen"

internal interface CanvasTourMemory {
    val seen: Boolean
    fun markSeen()
}

/** Lives in the no-backup folder, so neither app backups nor Android's own restore carry it over. */
private class NoBackupTourMemory(context: Context) : CanvasTourMemory {
    private val marker = File(context.noBackupFilesDir, SEEN_MARKER)
    override val seen: Boolean get() = marker.exists()

    override fun markSeen() {
        runCatching { marker.createNewFile() }
    }
}

internal class CanvasTourController(private val memory: CanvasTourMemory) {
    var state by mutableStateOf<CanvasTourState?>(null)
        private set

    /** Marks it seen as it appears, so closing the app on this card can never ask a second time. */
    fun offerOnce() {
        if (state != null || memory.seen) return
        start()
    }

    fun start() {
        memory.markSeen()
        state = CanvasTourState()
    }

    fun press(input: CanvasTourInput, repeat: Boolean = false) {
        state = state?.press(input, repeat)
    }

    fun primary() {
        val current = state ?: return
        when {
            current.step == CanvasTourStep.Done -> finish()
            current.step == CanvasTourStep.Intro || current.practiced -> state = current.next()
            else -> press(CanvasTourInput.Confirm)
        }
    }

    fun back() {
        state = state?.back()
    }

    fun doItForMe() {
        val current = state ?: return
        if (current.step.isPractice && !current.practiced) state = current.doneForMe()
    }

    fun advanceFrom(step: CanvasTourStep) {
        val current = state ?: return
        if (current.step == step && current.practiced) state = current.next()
    }

    fun finish() {
        state = null
    }
}

internal object JarnCanvasTour {
    var controller by mutableStateOf<CanvasTourController?>(null)
        private set

    fun connect(context: Context): CanvasTourController =
        controller ?: CanvasTourController(NoBackupTourMemory(context.applicationContext)).also { controller = it }

    /** Leaves Settings for Canvas with control on it, as a panel when there is no second display. */
    fun replay(context: Context, session: JarnSessionStore = JarnSession) {
        connect(context).start()
        session.update { openingCanvas() }
    }
}

internal fun JarnSessionState.openingCanvas(): JarnSessionState =
    openRoute(StageRoute.Library).copy(
        deckTab = DeckTab.Canvas,
        activeScreen = JarnScreen.Deck,
        deckPanelOpen = !deckDisplayAvailable,
        settingsReturnScreen = null,
    )

/** Stands in for Canvas during the tour, so no real tile or input layer exists to be changed. */
@Composable
internal fun CanvasTourGate(canvas: @Composable () -> Unit) {
    val context = LocalContext.current
    val tour = remember(context) { JarnCanvasTour.connect(context) }
    val isActive = LocalJarnInputRouter.current.activeScreen == LocalJarnScreen.current
    LaunchedEffect(isActive) { if (isActive) tour.offerOnce() }
    if (tour.state != null) CanvasTourScreen(tour, isActive) else canvas()
}
