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
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import kotlinx.coroutines.delay

/**
 * Holds a newly switched tab's first focus until its content has slid open. Content calls
 * [whenOpen] with its own focus request, which runs at once when the gate is already open.
 * [preferFirst] asks a grid for its first item rather than the one it remembers. [takesFocus]
 * means the switch was made on the screen with the controller, so the content takes focus even
 * from the tab row.
 */
@Stable
internal class LibraryRevealGate(closed: Boolean, val takesFocus: Boolean = false) {
    var isOpen by mutableStateOf(!closed)
        private set
    val preferFirst: Boolean = closed
    private var pendingFocus: (() -> Boolean)? = null

    fun whenOpen(focus: () -> Boolean) {
        if (isOpen) focus() else pendingFocus = focus
    }

    /** True only while the gate's own opening runs a request that may take focus from the tab row. */
    var forcing = false
        private set

    /** True while a switch that should take focus is still waiting to open. */
    val willForce: Boolean get() = takesFocus && !isOpen

    /** Opens the gate and runs the waiting focus request. True when focus landed. */
    fun open(): Boolean {
        if (isOpen) return false
        isOpen = true
        val focus = pendingFocus ?: return false
        pendingFocus = null
        forcing = takesFocus
        return try {
            focus()
        } finally {
            forcing = false
        }
    }
}

internal val LocalLibraryRevealGate = compositionLocalOf { LibraryRevealGate(closed = false) }

/**
 * Slides a newly switched Library tab open, then focuses its content once it has settled, from
 * the content or the tab row alike. A switch made from the other screen changes the tab but never
 * takes focus. When focus was in the old content, a holder keeps it on the new tab until then, and
 * a press that arrives early opens the gate and goes straight to the item it would have reached.
 * On the tab row focus stays put until the gate opens, so a held D-pad keeps stepping tabs, and
 * each newer switch drops the older one's pending focus. The first tab shown when Library opens
 * appears as before, with no slide.
 */
@Composable
internal fun <S : Any> LibraryTabReveal(tab: S, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(tab) }
    val switched = tab != shown
    SideEffect { shown = tab }
    key(tab) {
        val animate = remember { switched }
        val router = LocalJarnInputRouter.current
        val screen = LocalJarnScreen.current
        val gate = remember { LibraryRevealGate(closed = animate, takesFocus = animate && router.activeScreen == screen) }
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
