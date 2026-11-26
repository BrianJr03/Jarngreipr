package jr.brian.jarnfe.deck

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import jr.brian.frontend.look.JarnPlayingDim
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.JarnScreen
import kotlinx.coroutines.delay

internal const val NOW_PLAYING_BLACKOUT_TAG = "jarn_now_playing_blackout"

/**
 * The role whose screen goes black while a game runs, or null. It needs the setting, two
 * screens and a game in front, and never covers the companion offer. The covered window taking
 * OS focus back means Jarn is in front again there.
 */
internal fun nowPlayingBlackoutRole(
    session: NowPlayingSession?,
    screenOff: Boolean,
    deckDisplayAvailable: Boolean,
    rolesSwapped: Boolean,
    osFocusedScreen: JarnScreen?,
    offerShowing: Boolean,
): JarnScreen? {
    if (!screenOff || offerShowing) return null
    val free = nowPlayingFreeRole(session, deckDisplayAvailable, rolesSwapped) ?: return null
    return free.takeIf { osFocusedScreen != free.otherScreen() }
}

/**
 * Black until the first wake, then black again once [dim]'s delay has passed since the last one.
 * With [JarnPlayingDim.Never] a woken screen stays on for the rest of the session.
 */
internal fun nowPlayingBlackoutDue(dim: JarnPlayingDim, lastWakeMs: Long?, nowMs: Long): Boolean =
    lastWakeMs == null || nowPlayingDimmed(dim, lastWakeMs, nowMs)

/** One session's black screen. [wake] lights it up and restarts the go-black delay. */
internal class NowPlayingBlackoutState {
    var lastWakeMs by mutableStateOf<Long?>(null)
        private set
    var checkedAtMs by mutableLongStateOf(System.currentTimeMillis())
        private set

    fun isBlack(dim: JarnPlayingDim): Boolean = nowPlayingBlackoutDue(dim, lastWakeMs, checkedAtMs)

    fun wake(nowMs: Long = System.currentTimeMillis()) {
        lastWakeMs = nowMs
        checkedAtMs = nowMs
    }

    fun recheck(nowMs: Long = System.currentTimeMillis()) {
        checkedAtMs = nowMs
    }
}

/** A fresh state per session, re-checked when the go-black delay runs out after a wake. */
@Composable
internal fun rememberNowPlayingBlackout(sessionKey: Long?, dim: JarnPlayingDim): NowPlayingBlackoutState {
    val state = remember(sessionKey) { NowPlayingBlackoutState() }
    val woke = state.lastWakeMs
    LaunchedEffect(state, woke, dim) {
        val wokeAt = woke ?: return@LaunchedEffect
        val delayMs = dim.delayMs() ?: return@LaunchedEffect
        delay((wokeAt + delayMs - System.currentTimeMillis()).coerceAtLeast(0L))
        state.recheck()
    }
    return state
}

/**
 * The black screen. It replaces the role's content, so nothing under it can be pressed: the
 * waking tap or button only wakes. Swap and the L3/R3 mutes are left to the app, as everywhere.
 */
@Composable
internal fun NowPlayingBlackout(onWake: () -> Unit) {
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        bindings = {
            JarnAction.entries.filter { !it.isAppWide }.forEach { action ->
                on(action, repeats = false) { onWake() }
            }
        },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag(NOW_PLAYING_BLACKOUT_TAG)
                .pointerInput(onWake) {
                    awaitEachGesture {
                        awaitFirstDown().consume()
                        onWake()
                    }
                },
        )
    }
}
