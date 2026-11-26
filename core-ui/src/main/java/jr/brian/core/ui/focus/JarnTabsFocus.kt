package jr.brian.core.ui.focus

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/** How long the D-pad rests on a tab before focus drops into its content, so stepping stays on the strip. */
const val JARN_TAB_DPAD_SETTLE_MS = 500L

/** How long a handed-off strip lets content take focus, for content that is still loading. */
const val JARN_TAB_HANDOFF_MS = 1500L

enum class JarnTabSwitchSource { Shoulder, Dpad }

/** Which tab switches move focus from the strip into the new tab's content. */
@Immutable
data class JarnTabSwitchFocus(val onShoulder: Boolean, val onDpad: Boolean) {
    fun allows(source: JarnTabSwitchSource): Boolean = when (source) {
        JarnTabSwitchSource.Shoulder -> onShoulder
        JarnTabSwitchSource.Dpad -> onDpad
    }

    companion object {
        val Shoulders = JarnTabSwitchFocus(onShoulder = true, onDpad = false)
        val Both = JarnTabSwitchFocus(onShoulder = true, onDpad = true)
    }
}

val LocalJarnTabSwitchFocus = staticCompositionLocalOf { JarnTabSwitchFocus.Both }

/**
 * Whether a switch moves focus into the new tab. Focus that was already in the content always
 * follows it to the new tab; from the strip, [rule] decides by where the switch came from.
 */
fun jarnTabSwitchTakesFocus(rule: JarnTabSwitchFocus, source: JarnTabSwitchSource, stripHadFocus: Boolean): Boolean =
    !stripHadFocus || rule.allows(source)

/**
 * Tracks whether a tab row holds focus. Tabs switch as they are focused, so content appearing
 * under them checks [hasFocus] before taking focus and leaves the user on the tab row. When a
 * switch should move focus into the tab, the strip hands off: [hasFocus] reads false and
 * [handoffs] grows, so content that is already there asks again. A tab with nothing to focus
 * leaves focus on the strip.
 */
@Stable
class JarnTabsFocus {
    private var owner by mutableStateOf<Any?>(null)
    private var yielding by mutableStateOf<Any?>(null)

    /** Grows each time a strip hands focus to its content. Content keys its focus request on it. */
    var handoffs by mutableIntStateOf(0)
        private set

    val hasFocus: Boolean get() = owner != null && owner !== yielding

    internal fun update(group: Any, focused: Boolean) {
        if (focused) {
            owner = group
        } else if (owner === group) {
            owner = null
            if (yielding === group) yielding = null
        }
    }

    internal fun handOff(group: Any) {
        if (owner !== group) return
        yielding = group
        handoffs += 1
    }

    internal fun endHandOff(group: Any) {
        if (yielding === group) yielding = null
    }
}

val LocalJarnTabsFocus = staticCompositionLocalOf { JarnTabsFocus() }
