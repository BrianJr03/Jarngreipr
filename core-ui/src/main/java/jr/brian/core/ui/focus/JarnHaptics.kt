package jr.brian.core.ui.focus

import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalView

/** No more than one tick this often, so a held direction purrs rather than buzzes. */
internal const val JARN_HAPTIC_MIN_GAP_MS = 60L

/** A focus change this soon after a D-pad press is that press's move, not a restore. */
internal const val JARN_HAPTIC_NAV_WINDOW_MS = 250L

/**
 * Decides when a tick may fire. Every tick shares one rate limit, which also keeps a press that
 * both acts and moves focus to a single tick. A navigation tick needs a recent D-pad press.
 */
internal class JarnHapticGate(private val minGapMs: Long = JARN_HAPTIC_MIN_GAP_MS) {
    private var lastTickAt = Long.MIN_VALUE / 2
    private var armedAt = Long.MIN_VALUE / 2

    fun allows(nowMs: Long, enabled: Boolean): Boolean {
        if (!enabled || nowMs - lastTickAt < minGapMs) return false
        lastTickAt = nowMs
        return true
    }

    fun arm(nowMs: Long) {
        armedAt = nowMs
    }

    /** True once per D-pad press, when focus arrives soon after it. */
    fun takeNavigation(nowMs: Long): Boolean {
        if (nowMs - armedAt > JARN_HAPTIC_NAV_WINDOW_MS) return false
        armedAt = Long.MIN_VALUE / 2
        return true
    }
}

/**
 * Jarn's one light haptic, `CLOCK_TICK`: the lightest system tick, made for selection steps, so a
 * long browse never feels heavy. [isEnabled] is Button vibration, set by Jarn so this module never
 * reads its prefs. Reduce motion leaves it alone.
 */
object JarnHaptics {
    var isEnabled: () -> Boolean = { true }

    internal var gate = JarnHapticGate()
    internal var clock: () -> Long = SystemClock::uptimeMillis
    internal var performer: (View, Int) -> Unit = { view, effect -> view.performHapticFeedback(effect) }

    /** One light tick for a press that does something, such as a hinted button. */
    fun tick(view: View) {
        if (gate.allows(clock(), isEnabled())) performer(view, HapticFeedbackConstants.CLOCK_TICK)
    }

    /** Called for every D-pad press and repeat. The tick fires when focus actually moves. */
    fun armNavigation() {
        gate.arm(clock())
    }

    /** Focus arrived on an item. Ticks only when a D-pad press moved it there. */
    fun onFocusArrived(view: View) {
        if (gate.takeNavigation(clock())) tick(view)
    }

    /** A deliberate effect of its own, such as a Canvas grab, gated by Button vibration only. */
    fun perform(view: View, effect: Int) {
        if (isEnabled()) performer(view, effect)
    }
}

/** Ticks when a D-pad press moves focus onto this item. Jarn's focus styles already include it. */
fun Modifier.jarnNavHaptic(): Modifier = this then JarnNavHapticElement

private data object JarnNavHapticElement : ModifierNodeElement<JarnNavHapticNode>() {
    override fun create() = JarnNavHapticNode()

    override fun update(node: JarnNavHapticNode) = Unit
}

private class JarnNavHapticNode :
    Modifier.Node(),
    FocusEventModifierNode,
    CompositionLocalConsumerModifierNode {
    private var focused = false

    override fun onFocusEvent(focusState: FocusState) {
        val now = focusState.isFocused
        if (now && !focused) JarnHaptics.onFocusArrived(currentValueOf(LocalView))
        focused = now
    }
}
