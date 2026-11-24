package jr.brian.jarnfe.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

internal const val NOW_REVEAL_MIN_HOLD_MILLIS = 60L
internal const val NOW_REVEAL_MAX_WAIT_MILLIS = 280L

/** What the incoming view is still waiting on before it may be seen. */
internal class NowRevealGate {
    private val parts = mutableStateMapOf<Any, Boolean>()

    val isReady: Boolean get() = parts.values.all { it }

    fun report(part: Any, ready: Boolean) {
        parts[part] = ready
    }

    fun forget(part: Any) {
        parts.remove(part)
    }

    suspend fun awaitReady(minHoldMillis: Long = NOW_REVEAL_MIN_HOLD_MILLIS) {
        delay(minHoldMillis)
        withFrameNanos { }
        withTimeoutOrNull(NOW_REVEAL_MAX_WAIT_MILLIS) { snapshotFlow { isReady }.first { it } }
    }
}

internal val LocalNowRevealGate = staticCompositionLocalOf<NowRevealGate?> { null }

/** Keeps the new view hidden until [ready], so its parts appear together instead of popping in. */
@Composable
internal fun NowRevealWhen(ready: Boolean) {
    val gate = LocalNowRevealGate.current ?: return
    val part = remember { Any() }
    SideEffect { gate.report(part, ready) }
    DisposableEffect(gate, part) { onDispose { gate.forget(part) } }
}

private class NowRevealState<T>(initial: T) {
    var held by mutableStateOf(initial)
    val alpha = Animatable(1f)
    val gate = NowRevealGate()
}

/** The value to draw, and the fade around each change to it. */
internal class NowReveal<T>(val shown: T, val gate: NowRevealGate, private val alpha: Animatable<Float, *>) {
    val modifier: Modifier = Modifier.fillMaxSize().graphicsLayer { this.alpha = this@NowReveal.alpha.value }
}

/**
 * Fades the old value out, swaps it while hidden, and fades in once the new one is ready. A change
 * [followsRoute] (the Stage moving between systems and games) trails the Stage by the Deck follow
 * timing instead, so the two screens never move at once.
 */
@Composable
internal fun <T> rememberNowReveal(
    target: T,
    followsRoute: (from: T, to: T) -> Boolean = { _, _ -> false },
    keyOf: (T) -> Any?,
): NowReveal<T> {
    val state = remember { NowRevealState(target) }
    val motion = JarnTheme.motion
    val latest by rememberUpdatedState(target)
    val key = keyOf(target)
    val isSame = key == keyOf(state.held)
    if (isSame) SideEffect { state.held = target }
    LaunchedEffect(key) {
        if (key == keyOf(state.held)) {
            state.alpha.animateTo(1f, motion.small.spec())
        } else if (followsRoute(state.held, latest)) {
            val follow = motion.deckFollow
            delay(follow.delayMillis.toLong())
            state.alpha.animateTo(0f, tween(follow.outMillis, easing = LinearEasing))
            state.held = latest
            state.gate.awaitReady(minHoldMillis = 0L)
            state.alpha.animateTo(1f, tween(follow.inMillis, easing = LinearEasing))
        } else {
            state.alpha.animateTo(0f, motion.focusOut.spec())
            state.held = latest
            state.gate.awaitReady()
            state.alpha.animateTo(1f, motion.small.spec())
        }
    }
    return NowReveal(if (isSame) target else state.held, state.gate, state.alpha)
}

@Composable
internal fun NowRevealBox(reveal: NowReveal<*>, content: @Composable () -> Unit) {
    Box(reveal.modifier, contentAlignment = Alignment.Center) { content() }
}

/** The shelf for the view on screen, held while the scene has already moved to the next system. */
@Composable
internal fun rememberShownSystemNow(sceneNow: JarnSystemNow?, shownSystemId: String?): JarnSystemNow? {
    var held by remember { mutableStateOf<JarnSystemNow?>(null) }
    val matching = sceneNow?.takeIf { it.systemId == shownSystemId }
    SideEffect { if (matching != null) held = matching }
    return matching ?: held?.takeIf { it.systemId == shownSystemId }
}
