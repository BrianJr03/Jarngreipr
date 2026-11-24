package jr.brian.jarnfe.firstrun

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import jr.brian.frontend.arrival.JarnArrival
import jr.brian.frontend.look.JarnLookStore
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibraryHero
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.look.JarnLookPreview
import jr.brian.jarnfe.look.jarnLookStoreOrNull
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.session.JarnSessionStore
import jr.brian.jarnfe.session.LibrarySection
import jr.brian.jarnfe.session.SettingsSection
import jr.brian.jarnfe.session.StageRoute

/** Where the tour keeps its place, so it survives the process being killed. */
internal interface JarnFirstRunProgress {
    val isDone: Boolean
    val savedStep: String?
    fun saveStep(step: String)
    fun markDone()
}

private class JarnLookStoreProgress(private val store: JarnLookStore) : JarnFirstRunProgress {
    override val isDone get() = store.firstRunDone
    override val savedStep get() = store.firstRunStep
    override fun saveStep(step: String) = store.saveFirstRunStep(step)
    override fun markDone() = store.markFirstRunDone()
}

/** A replay never touches saved progress, so interrupting it cannot bring the tour back. */
internal class JarnFirstRunController(private val progress: JarnFirstRunProgress) {
    var state by mutableStateOf(
        if (progress.isDone) null else JarnFirstRunState(JarnFirstRunStep.fromName(progress.savedStep)),
    )
        private set
    var isArtDetour by mutableStateOf(false)
        private set
    private var isReplay = false

    fun coversShell(route: StageRoute): Boolean =
        state != null && !(isArtDetour && route == StageRoute.Settings)

    fun settle(twoScreens: Boolean) = move { it.settled(twoScreens) }

    fun back(twoScreens: Boolean) = move { it.back(twoScreens) }

    fun practice(gesture: JarnFirstRunGesture, tabDelta: Int = 0) = move { it.practice(gesture, tabDelta) }

    fun next(twoScreens: Boolean) {
        val current = state ?: return
        val following = current.next(twoScreens)
        if (following == null) finish() else move { following }
    }

    fun nextFrom(step: JarnFirstRunStep, twoScreens: Boolean) {
        if (state?.step == step) next(twoScreens)
    }

    fun finish() {
        state = null
        isArtDetour = false
        if (isReplay) isReplay = false else progress.markDone()
    }

    /** Only a tour that hasn't started yet becomes the short one. Replays always stay full. */
    fun startArrival() {
        val current = state ?: return
        if (isReplay || current.step != JarnFirstRunStep.Welcome || current.confirmingSkip) return
        move { JarnFirstRunState(JarnFirstRunStep.Ready, path = JarnFirstRunPath.Arrival) }
    }

    fun showLessons(twoScreens: Boolean) = move { it.withLessons(twoScreens) }

    fun replay() {
        isReplay = true
        isArtDetour = false
        state = JarnFirstRunState()
    }

    /** A restored backup that already finished the tour ends a first run still on screen. */
    fun onDoneElsewhere() {
        if (isReplay || state == null) return
        state = null
        isArtDetour = false
    }

    fun startArtDetour() {
        if (state != null) isArtDetour = true
    }

    fun endArtDetour() {
        isArtDetour = false
    }

    private fun move(transform: (JarnFirstRunState) -> JarnFirstRunState) {
        val current = state ?: return
        val next = transform(current)
        state = next
        if (!isReplay && next.step != current.step) progress.saveStep(next.step.name)
    }
}

internal object JarnFirstRun {
    var controller by mutableStateOf<JarnFirstRunController?>(null)
        private set

    val isArtDetour: Boolean get() = controller?.isArtDetour == true

    fun connect(context: Context): JarnFirstRunController? {
        controller?.let { return it }
        val store = context.jarnLookStoreOrNull() ?: return null
        return JarnFirstRunController(JarnLookStoreProgress(store)).also { controller = it }
    }

    fun replay(session: JarnSessionStore = JarnSession) {
        controller?.replay()
        session.update { landedOnLibrary() }
    }
}

internal fun JarnSessionState.landedOnLibrary(): JarnSessionState =
    openRoute(StageRoute.Library).copy(
        activeScreen = JarnScreen.Stage,
        deckPanelOpen = false,
        settingsReturnScreen = null,
    )

/** What the tour's buttons do, in one place so controller and touch can never disagree. */
internal class JarnFirstRunActions(
    private val controller: JarnFirstRunController,
    private val session: JarnSessionStore,
) {
    private val twoScreens get() = session.state.settingsOnDeck

    fun primary() {
        val run = controller.state ?: return
        val gesture = run.step.gesture
        if (!run.confirmingSkip && gesture != null && !run.practiced) {
            perform(gesture, tabDelta = 1)
        } else {
            advance()
        }
    }

    fun perform(gesture: JarnFirstRunGesture, tabDelta: Int = 0) {
        val run = controller.state ?: return
        if (run.step.gesture != gesture || run.confirmingSkip) return
        if (gesture == JarnFirstRunGesture.SwitchScreen && !run.practiced) switchScreen()
        controller.practice(gesture, tabDelta)
    }

    /** With one display this would open the Deck panel over the tour, so it does nothing. */
    fun switchScreen() {
        if (twoScreens) session.update { switchActiveScreen() }
    }

    fun back() = controller.back(twoScreens)

    fun showLessons() = controller.showLessons(twoScreens)

    /** The short tour's link for people with no games: straight to Look. */
    fun skipToLook() = advanceFrom(JarnFirstRunStep.Ready)

    fun skip() {
        controller.finish()
        land()
    }

    fun advanceFrom(step: JarnFirstRunStep) {
        controller.nextFrom(step, twoScreens)
        landIfFinished()
    }

    fun openArtSettings() {
        session.update { openSettings().copy(settingsSection = SettingsSection.Art) }
        controller.startArtDetour()
    }

    private fun advance() {
        controller.next(twoScreens)
        landIfFinished()
    }

    private fun landIfFinished() {
        if (controller.state == null) land()
    }

    private fun land() {
        JarnLookPreview.lookId = null
        session.update { landedOnLibrary().copy(librarySection = LibrarySection.Systems) }
        if (JarnLibrary.state.hero is LibraryHero.ContinuePlaying) JarnLibrary.requestHeroFocus()
    }
}

@Composable
internal fun rememberJarnFirstRun(route: StageRoute): JarnFirstRunController? {
    val context = LocalContext.current
    val controller = remember(context) { JarnFirstRun.connect(context) } ?: return null
    val store = remember(context) { context.jarnLookStoreOrNull() }
    val isDone = store?.firstRunDoneState?.collectAsState()?.value == true
    LaunchedEffect(isDone) { if (isDone) controller.onDoneElsewhere() }
    LaunchedEffect(controller) { if (JarnArrival.takeFromClassic(context)) controller.startArrival() }
    LaunchedEffect(route, controller.isArtDetour) {
        if (controller.isArtDetour && route != StageRoute.Settings) controller.endArtDetour()
    }
    return controller
}
