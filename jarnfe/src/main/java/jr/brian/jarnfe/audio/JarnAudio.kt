package jr.brian.jarnfe.audio

import android.app.Activity
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.session.JarnSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val HOLD_SETTLE_MS = 250L
private const val JINGLE_DWELL_MS = 200L

/** Long enough for the Now scene to settle and find the game's video, still before it starts. */
private const val JINGLE_PREVIEW_DWELL_MS = 450L

/** Covers the next game's art lookup, so moving between previews never lets music back in. */
private const val PREVIEW_RELEASE_GRACE_MS = 400L

/**
 * The only place Jarn FE starts or stops audio. Stage and Deck both report here, so a selection
 * or a focus change produces one action however many windows are open.
 */
internal object JarnAudio {
    private val gate = MutableStateFlow(JarnAudioGate())
    private val previewSoundOn = MutableStateFlow(false)
    private val windows = LinkedHashSet<Int>()
    private var scope: CoroutineScope? = null
    private var data: JarnAudioData? = null
    private var previewRelease: Job? = null

    val gateState: StateFlow<JarnAudioGate> = gate.asStateFlow()

    fun attach(activity: ComponentActivity) {
        val host = activity.jarnAudioDataOrNull() ?: return
        val id = System.identityHashCode(activity)
        windows += id
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> gate.update { it.withCovered(id, false) }
                    Lifecycle.Event.ON_STOP -> if (id in windows) {
                        gate.update { it.withCovered(id, activity.isOnLitDisplay()) }
                    }
                    else -> Unit
                }
            },
        )
        if (scope != null) return
        data = host
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { start(it, host) }
    }

    fun detach(activity: Activity) {
        val id = System.identityHashCode(activity)
        if (!windows.remove(id)) return
        gate.update { it.withTop(id, false).withCovered(id, false) }
        if (windows.isNotEmpty()) return
        scope?.cancel()
        scope = null
        data?.stopJingle()
        data?.holdPlayback()
        data = null
        previewRelease = null
        gate.value = JarnAudioGate()
    }

    fun onTopResumed(activity: Activity, isTop: Boolean) {
        val id = System.identityHashCode(activity)
        if (id in windows) gate.update { it.withTop(id, isTop) }
    }

    fun setVideoOpen(open: Boolean) = gate.update { it.copy(videoOpen = open) }

    /** While the Deck shows previews with sound, jingles wait a little to see if one will play. */
    fun setPreviewSoundOn(on: Boolean) {
        previewSoundOn.value = on
    }

    fun claimPreview(gameId: String) {
        previewRelease?.cancel()
        gate.update { it.copy(previewGameId = gameId) }
    }

    /** Lets go after [PREVIEW_RELEASE_GRACE_MS] unless the next preview claims first. */
    fun releasePreview(gameId: String) {
        if (gate.value.previewGameId != gameId) return
        previewRelease?.cancel()
        val scope = scope ?: return clearPreview(gameId)
        previewRelease = scope.launch {
            delay(PREVIEW_RELEASE_GRACE_MS)
            clearPreview(gameId)
        }
    }

    @VisibleForTesting
    internal fun resetForTest(value: JarnAudioGate = JarnAudioGate()) {
        previewRelease?.cancel()
        previewSoundOn.value = false
        gate.value = value
    }

    /** After a settings change, so music that was silent starts once it is allowed to. */
    fun syncMusic() {
        if (gate.value.allowed) data?.allowPlayback()
    }

    private fun start(scope: CoroutineScope, host: JarnAudioData) {
        val allowed = gate.map { it.allowed }.distinctUntilChanged()
        scope.launch {
            host.otherMediaPlaying.collect { playing ->
                gate.update { it.copy(otherMediaPlaying = playing) }
            }
        }
        scope.launch {
            allowed.collectLatest { canPlay ->
                if (canPlay) {
                    host.allowPlayback()
                } else {
                    delay(HOLD_SETTLE_MS)
                    host.holdPlayback()
                }
            }
        }
        scope.launch { runJingles(host) }
    }

    private suspend fun runJingles(host: JarnAudioData) {
        val trigger = JarnJingleTrigger()
        val selected = snapshotFlow { selectedGameId() }.distinctUntilChanged()
        val gates = gate.map { it.allowed to it.previewGameId }.distinctUntilChanged()
        combine(selected, gates, previewSoundOn) { gameId, (canPlay, previewId), waits ->
            JingleInput(gameId, canPlay, previewId, waits)
        }.collectLatest { (gameId, canPlay, previewId, waits) ->
            if (gameId != null && canPlay) delay(if (waits) JINGLE_PREVIEW_DWELL_MS else JINGLE_DWELL_MS)
            when (val step = trigger.next(gameId, canPlay, previewId)) {
                JarnJingleStep.None -> Unit
                JarnJingleStep.Stop -> host.stopJingle()
                is JarnJingleStep.Play -> host.gameKey(step.gameId)?.let { host.playJingle(it) }
            }
        }
    }

    private fun clearPreview(gameId: String) =
        gate.update { if (it.previewGameId == gameId) it.copy(previewGameId = null) else it }

    private data class JingleInput(
        val gameId: String?,
        val canPlay: Boolean,
        val previewId: String?,
        val waits: Boolean,
    )

    /** A window stopped because its screen went dark is not covered by anything. */
    private fun Activity.isOnLitDisplay(): Boolean = display?.state == Display.STATE_ON

    private fun selectedGameId(): String? {
        val session = JarnSession.state
        val selection = JarnLibrary.state.selection(session.stageRoute, session.librarySection)
        return (selection as? LibrarySelection.Game)?.game?.id
    }
}
