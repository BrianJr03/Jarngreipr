package jr.brian.jarnfe.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay

/**
 * Holds a newly switched tab's first focus until its content has slid open. Content calls
 * [whenOpen] with its own focus request, which runs at once when the gate is already open.
 * [preferFirst] asks a grid for its first item rather than the one it remembers.
 */
@Stable
internal class LibraryRevealGate(closed: Boolean) {
    var isOpen by mutableStateOf(!closed)
        private set
    val preferFirst: Boolean = closed
    private var pendingFocus: (() -> Boolean)? = null

    fun whenOpen(focus: () -> Boolean) {
        if (isOpen) focus() else pendingFocus = focus
    }

    /** Opens the gate and runs the waiting focus request. True when focus landed. */
    fun open(): Boolean {
        if (isOpen) return false
        isOpen = true
        val focus = pendingFocus ?: return false
        pendingFocus = null
        return focus()
    }
}

internal val LocalLibraryRevealGate = compositionLocalOf { LibraryRevealGate(closed = false) }

/**
 * Slides a newly switched Library tab open, then focuses its content once it has settled. Until
 * then a holder keeps focus on the new tab, and a press that arrives early opens the gate and
 * goes straight to the item it would have reached, so nothing is lost or lands on the old tab.
 * The first tab shown when Library opens appears as before, with no slide.
 */
@Composable
internal fun <S : Any> LibraryTabReveal(tab: S, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(tab) }
    val switched = tab != shown
    SideEffect { shown = tab }
    key(tab) {
        val animate = remember { switched }
        val gate = remember { LibraryRevealGate(closed = animate) }
        val holder = remember { FocusRequester() }
        LibraryRevealTiming(gate, holder, animate)
        CompositionLocalProvider(LocalLibraryRevealGate provides gate) {
            LibraryRevealMotion(animate) {
                LibraryRevealHolder(gate, holder, content)
            }
        }
    }
}

@Composable
private fun LibraryRevealTiming(gate: LibraryRevealGate, holder: FocusRequester, animate: Boolean) {
    val motion = JarnTheme.motion
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(Unit) {
        if (!animate) return@LaunchedEffect
        if (!tabsFocus.hasFocus) holder.requestFocus()
        if (motion.reduced) {
            withFrameNanos { }
        } else {
            delay(motion.tabReveal.durationMillis + motion.tabFocusSettleMillis)
        }
        gate.open()
    }
}

@Composable
private fun LibraryRevealMotion(animate: Boolean, content: @Composable () -> Unit) {
    val motion = JarnTheme.motion
    val visible = remember { MutableTransitionState(!animate).apply { targetState = true } }
    val enter = if (motion.reduced) {
        EnterTransition.None
    } else {
        fadeIn(motion.tabReveal.spec()) +
            expandVertically(motion.tabReveal.spec<IntSize>(), expandFrom = Alignment.Top)
    }
    AnimatedVisibility(visibleState = visible, enter = enter, exit = ExitTransition.None) { content() }
}

/** Focusable only while the gate is shut, so it never takes focus from the content afterwards. */
@Composable
private fun LibraryRevealHolder(gate: LibraryRevealGate, holder: FocusRequester, content: @Composable () -> Unit) {
    val view = LocalView.current
    Box(
        Modifier
            .fillMaxSize()
            .focusRequester(holder)
            .focusProperties { canFocus = !gate.isOpen }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || gate.isOpen) return@onKeyEvent false
                if (gate.open()) view.dispatchKeyEvent(event.nativeKeyEvent)
                true
            }
            .focusable(),
    ) { content() }
}
