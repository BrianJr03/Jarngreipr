package jr.brian.home.ui.meetjarn

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import jr.brian.frontend.arrival.JarnArrival
import jr.brian.home.data.JarnFeOffer
import jr.brian.home.data.preview.JarnPreviewGate
import jr.brian.home.ui.components.settings.rememberTryJarnFe
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.jarnfe.look.JarnLookStoreProvider

internal const val MEET_JARN_FADE_MS = 240
internal const val MEET_JARN_REDUCED_FADE_MS = 120

/** Where the screen was opened from. Only a launch counts a showing and leads into What's New. */
enum class MeetJarnSource { Launch, Settings }

/** The open Meet Jarn screen, if any, and whether it was turned down at this launch. */
@Stable
class MeetJarnState {
    var content by mutableStateOf<MeetJarnContent?>(null)
        private set
    var source by mutableStateOf(MeetJarnSource.Settings)
        private set

    /** True once Not now was pressed at launch, so What's New leaves out its Jarn card. */
    var declinedAtLaunch by mutableStateOf(false)
        private set

    val isOpen: Boolean get() = content != null

    fun open(content: MeetJarnContent, source: MeetJarnSource) {
        this.source = source
        this.content = content
    }

    fun close(declined: Boolean) {
        if (declined && source == MeetJarnSource.Launch) declinedAtLaunch = true
        content = null
    }
}

@Composable
fun rememberMeetJarnState(): MeetJarnState = remember { MeetJarnState() }

/** Opens Meet Jarn on its own at launch when the gate says it's due. Returns true when it opened. */
suspend fun MeetJarnState.openIfDue(context: Context, versionName: String): Boolean = openIfDue(
    versionName = versionName,
    gate = context.jarnPreviewGate(),
    offered = JarnFeOffer.isOffered(context),
    load = { loadMeetJarnContent(context) },
)

internal suspend fun MeetJarnState.openIfDue(
    versionName: String,
    gate: JarnPreviewGate,
    offered: Boolean,
    load: suspend () -> MeetJarnContent,
): Boolean {
    if (!offered || !gate.shouldShow(versionName)) return false
    open(load(), MeetJarnSource.Launch)
    gate.recordShown(versionName)
    return true
}

/** Opens Meet Jarn from Classic Settings. It never counts as a showing. */
suspend fun MeetJarnState.openFromSettings(context: Context) {
    open(loadMeetJarnContent(context), MeetJarnSource.Settings)
}

/**
 * The full-screen Meet Jarn screen on Classic's own window, fading in and out. Try Jarn switches
 * to Jarn and leaves the arrival flag for Jarn's short first run, Not now (or B) closes it.
 */
@Composable
fun MeetJarnDialog(state: MeetJarnState, onTried: () -> Unit = {}) {
    val content = state.content ?: return
    val context = LocalContext.current
    val fade = if (context.jarnReducedMotion()) MEET_JARN_REDUCED_FADE_MS else MEET_JARN_FADE_MS
    val visible = remember(content) { MutableTransitionState(false).apply { targetState = true } }
    var tried by remember(content) { mutableStateOf(false) }
    val tryJarn = rememberTryJarnFe {
        context.jarnPreviewGate().markTried()
        JarnArrival.markFromClassic(context)
        tried = true
        visible.targetState = false
        onTried()
    }
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) state.close(declined = !tried)
    }
    Dialog(
        onDismissRequest = { visible.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        AnimatedVisibility(visibleState = visible, enter = fadeIn(tween(fade)), exit = fadeOut(tween(fade))) {
            MeetJarnScreen(
                content = content,
                colors = MeetJarnColors(primary = ThemePrimaryColor, background = OledBackgroundColor),
                onTry = tryJarn,
                onNotNow = { visible.targetState = false },
            )
        }
    }
}

private fun Context.jarnReducedMotion(): Boolean =
    (applicationContext as? JarnLookStoreProvider)?.jarnLookStore?.reducedMotion == true

internal fun Context.jarnPreviewGate(): JarnPreviewGate =
    EntryPointAccessors.fromApplication(applicationContext, JarnPreviewGateEntryPoint::class.java).jarnPreviewGate()

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface JarnPreviewGateEntryPoint {
    fun jarnPreviewGate(): JarnPreviewGate
}
