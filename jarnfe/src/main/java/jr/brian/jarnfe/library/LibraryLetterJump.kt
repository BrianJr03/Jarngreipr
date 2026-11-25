package jr.brian.jarnfe.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/** How long a direction is held before its repeats jump by letter instead of stepping. */
internal const val LETTER_JUMP_HOLD_MS = 1200L

/** Time spent on each letter while jumping, so the badge can be read and released on. */
internal const val LETTER_JUMP_DWELL_MS = 350L

/** Only a list in Name order jumps by letter. Recent, Favorites and the other sorts step as usual. */
internal fun JarnLibraryFeed<*>.letterJumpEnabled(): Boolean = sort == JarnGameSort.Name

/** The key the Name sort orders by, so letter groups and the badge follow the list's order. */
internal fun JarnLibraryFeed<*>.letterJumpKey(): (String) -> String = ::nameSortKey

/** The letter group an item belongs to, the same one the shelf badge shows, read from [titleKey]. */
internal fun letterJumpGroup(title: String, titleKey: (String) -> String = { it }): String =
    shelfBadgeLetter(titleKey(title))

/**
 * The index a letter jump from [from] lands on, or null when there is nowhere to go. Forward lands
 * on the first item of the next group. Backward lands on the first item of the current group, or
 * of the previous group when [from] already starts one. Jumps stop at the ends instead of
 * wrapping, so a hold can't spin round the list. Groups are runs of neighbors, so a list whose
 * "#" titles are split by the sort still jumps run by run.
 */
internal fun letterJumpTarget(count: Int, from: Int, forward: Boolean, groupAt: (Int) -> String): Int? {
    if (from !in 0 until count) return null
    val group = groupAt(from)
    if (forward) return (from + 1 until count).firstOrNull { groupAt(it) != group }
    val start = groupStart(from, groupAt)
    return when {
        start < from -> start
        start == 0 -> null
        else -> groupStart(start - 1, groupAt)
    }
}

private fun groupStart(index: Int, groupAt: (Int) -> String): Int {
    val group = groupAt(index)
    var start = index
    while (start > 0 && groupAt(start - 1) == group) start--
    return start
}

internal enum class LetterJumpPace { Step, Jump, Wait }

/** Turns a held direction's repeats into steps, then letter jumps paced by [dwellMs]. */
internal class LetterJumpPacer(
    private val holdMs: Long = LETTER_JUMP_HOLD_MS,
    private val dwellMs: Long = LETTER_JUMP_DWELL_MS,
) {
    private var lastJumpMs: Long? = null

    fun reset() {
        lastJumpMs = null
    }

    fun onRepeat(heldMs: Long, nowMs: Long): LetterJumpPace {
        if (heldMs < holdMs) return LetterJumpPace.Step
        val last = lastJumpMs
        if (last != null && nowMs - last < dwellMs) return LetterJumpPace.Wait
        lastJumpMs = nowMs
        return LetterJumpPace.Jump
    }

    /** Paces [event], a press or repeat of a direction. A fresh press always steps. */
    fun pace(event: KeyEvent): LetterJumpPace {
        val native = event.nativeKeyEvent
        if (native.repeatCount == 0) {
            reset()
            return LetterJumpPace.Step
        }
        return onRepeat(native.eventTime - native.downTime, native.eventTime)
    }
}

/** A grid's letter jump in progress: where focus should land and whether the badge shows. */
internal class GridLetterJump {
    val pacer = LetterJumpPacer()
    val requester = FocusRequester()
    var targetKey by mutableStateOf<String?>(null)
        private set
    var tick by mutableIntStateOf(0)
        private set
    var jumping by mutableStateOf(false)
        private set

    /** Where this hold last jumped to, so the next jump starts there even before focus lands. */
    var heldKey: String? = null
        private set

    fun land(key: String) {
        targetKey = key
        heldKey = key
        tick++
    }

    fun hold() {
        jumping = true
    }

    fun release() {
        pacer.reset()
        heldKey = null
        jumping = false
    }
}

/**
 * Up and Down held past [LETTER_JUMP_HOLD_MS] jump by letter, since a grid scrolls vertically.
 * Shorter holds and taps fall through to normal focus movement. [targetFor] gives the key to land
 * on, or null to stay put at an end.
 */
internal fun Modifier.gridLetterJumpKeys(
    jump: GridLetterJump,
    enabled: Boolean,
    targetFor: (forward: Boolean) -> String?,
): Modifier = if (!enabled) this else onPreviewKeyEvent { event ->
    val forward = when (event.key) {
        Key.DirectionDown -> true
        Key.DirectionUp -> false
        else -> return@onPreviewKeyEvent false
    }
    when (event.type) {
        KeyEventType.KeyUp -> {
            jump.release()
            false
        }
        KeyEventType.KeyDown -> gridLetterJumpPress(jump, event, forward, targetFor)
        else -> false
    }
}

private fun gridLetterJumpPress(
    jump: GridLetterJump,
    event: KeyEvent,
    forward: Boolean,
    targetFor: (Boolean) -> String?,
): Boolean = when (jump.pacer.pace(event)) {
    LetterJumpPace.Step -> false
    LetterJumpPace.Wait -> true
    LetterJumpPace.Jump -> {
        jump.hold()
        targetFor(forward)?.let(jump::land)
        true
    }
}
