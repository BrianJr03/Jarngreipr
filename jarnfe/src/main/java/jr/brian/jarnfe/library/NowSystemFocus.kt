package jr.brian.jarnfe.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.LocalJarnScreenActive

private const val FRAMES_AFTER_SCREEN_FOCUS = 3

internal sealed interface NowFocusTarget {
    data object Browse : NowFocusTarget
    data object Manage : NowFocusTarget
    data class Cover(val game: JarnLibraryGame) : NowFocusTarget
}

/** The remembered cover when it is still on the shelf, otherwise the first one. */
internal fun nowShelfEntryId(shelfIds: List<String>, lastId: String?): String? =
    lastId?.takeIf { it in shelfIds } ?: shelfIds.firstOrNull()

/** Activation lands on the tabs by default. Only a deliberate stay on the tabs is left alone. */
internal fun nowTakesEntryFocus(activated: Boolean, holdsFocus: Boolean, tabsHaveFocus: Boolean): Boolean =
    !holdsFocus && (activated || !tabsHaveFocus)

@Stable
internal class NowSystemFocus {
    val browse = FocusRequester()
    val manage = FocusRequester()
    private val covers = HashMap<String, FocusRequester>()
    var shelfIds by mutableStateOf(emptyList<String>())
    var focused by mutableStateOf<NowFocusTarget?>(null)
        private set
    var holdsFocus by mutableStateOf(false)
    private var lastCoverId by mutableStateOf<String?>(null)
    private var lastAction by mutableStateOf<NowFocusTarget>(NowFocusTarget.Browse)

    fun cover(id: String): FocusRequester = covers.getOrPut(id) { FocusRequester() }

    val coverEntry: FocusRequester
        get() = nowShelfEntryId(shelfIds, lastCoverId)?.let(::cover) ?: FocusRequester.Default

    val actionEntry: FocusRequester
        get() = if (lastAction == NowFocusTarget.Manage) manage else browse

    fun remember(coverId: String) {
        lastCoverId = coverId
    }

    fun onFocus(target: NowFocusTarget, isFocused: Boolean) {
        if (isFocused) {
            focused = target
            if (target is NowFocusTarget.Cover) lastCoverId = target.game.id else lastAction = target
        } else if (focused == target) {
            focused = null
        }
    }
}

internal fun Modifier.nowCoverFocus(game: JarnLibraryGame, focus: NowSystemFocus): Modifier {
    val target = NowFocusTarget.Cover(game)
    return this
        .focusRequester(focus.cover(game.id))
        .focusProperties { up = focus.actionEntry }
        .onFocusChanged { focus.onFocus(target, it.isFocused) }
}

internal fun Modifier.nowActionFocus(
    requester: FocusRequester,
    target: NowFocusTarget,
    focus: NowSystemFocus,
): Modifier = this
    .focusRequester(requester)
    .focusProperties { down = focus.coverEntry }
    .onFocusChanged { focus.onFocus(target, it.isFocused) }

@Composable
internal fun NowEntryFocus(focus: NowSystemFocus, entry: FocusRequester?, isRestoring: Boolean) {
    val active = LocalJarnScreenActive.current
    val tabs = LocalJarnTabsFocus.current
    val wasActive = remember { booleanArrayOf(active) }
    LaunchedEffect(active) {
        val activated = active && !wasActive[0]
        wasActive[0] = active
        if (!active || isRestoring) return@LaunchedEffect
        repeat(FRAMES_AFTER_SCREEN_FOCUS) { withFrameNanos { } }
        if (nowTakesEntryFocus(activated, focus.holdsFocus, tabs.hasFocus)) {
            entry?.let { runCatching { it.requestFocus() } }
        }
    }
}

@Composable
internal fun NowCoverRestore(
    focus: NowSystemFocus,
    entry: FocusRequester?,
    isLoaded: Boolean,
    pendingCoverId: String?,
    onRestored: () -> Unit,
) {
    LaunchedEffect(pendingCoverId, isLoaded, focus.shelfIds) {
        val id = pendingCoverId ?: return@LaunchedEffect
        if (!isLoaded) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        val target = if (id in focus.shelfIds) focus.cover(id).also { focus.remember(id) } else entry
        target?.let { runCatching { it.requestFocus() } }
        onRestored()
    }
}
