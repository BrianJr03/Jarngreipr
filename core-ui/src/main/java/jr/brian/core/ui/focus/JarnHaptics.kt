package jr.brian.core.ui.focus

import android.os.SystemClock
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
 * The effects Jarn plays: the light [Tick] for navigation, buttons and Canvas drags, and [Grab]
 * for a Canvas pick-up. [oneShotMs] and [oneShotAmplitude] are for a vibrator with neither the
 * primitive nor the predefined effect. [feedback] is the view haptic used only with no vibrator.
 *
 * `CLOCK_TICK` is never used: it maps to the texture tick, which many vibrators (the AYN Thor's
 * among them) don't support, and Android drops it silently with no fallback.
 */
enum class JarnHapticEffect(
    val predefined: Int,
    val oneShotMs: Long,
    val oneShotAmplitude: Int,
    val feedback: Int,
) {
    Tick(VibrationEffect.EFFECT_TICK, oneShotMs = 10, oneShotAmplitude = 60, HapticFeedbackConstants.KEYBOARD_TAP),
    Grab(VibrationEffect.EFFECT_HEAVY_CLICK, oneShotMs = 30, oneShotAmplitude = 180, HapticFeedbackConstants.LONG_PRESS),
}

/** A light tick primitive, about half strength, for D-pad moves. */
private const val TICK_PRIMITIVE_SCALE = 0.5f

/** Touch usage, so turning Android's touch vibration intensity off still silences Jarn. */
private val TouchVibration: VibrationAttributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)

/** The lightest form this vibrator supports: the tick primitive, then the predefined effect, then a one-shot. */
internal fun jarnVibrationFor(vibrator: Vibrator, effect: JarnHapticEffect): VibrationEffect = when {
    effect == JarnHapticEffect.Tick && vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK) ->
        VibrationEffect.startComposition()
            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, TICK_PRIMITIVE_SCALE)
            .compose()
    vibrator.areAllEffectsSupported(effect.predefined) != Vibrator.VIBRATION_EFFECT_SUPPORT_NO ->
        VibrationEffect.createPredefined(effect.predefined)
    else -> VibrationEffect.createOneShot(effect.oneShotMs, effect.oneShotAmplitude)
}

/**
 * Jarn's haptics, played through the system vibrator rather than the view's haptic feedback, so
 * they don't depend on effects the vibrator may not have. Button vibration ([isEnabled], set by
 * Jarn so this module never reads its prefs) turns them on or off, and the system touch intensity
 * still applies. A device with no vibrator falls back to the view's haptic. Reduce motion leaves
 * them alone.
 */
object JarnHaptics {
    var isEnabled: () -> Boolean = { true }

    internal var gate = JarnHapticGate()
    internal var clock: () -> Long = SystemClock::uptimeMillis
    internal var vibratorOf: (View) -> Vibrator? = { view ->
        view.context.getSystemService(VibratorManager::class.java)?.defaultVibrator?.takeIf { it.hasVibrator() }
    }
    internal var vibrate: (Vibrator, JarnHapticEffect) -> Unit = { vibrator, effect ->
        vibrator.vibrate(jarnVibrationFor(vibrator, effect), TouchVibration)
    }
    internal var viewFeedback: (View, JarnHapticEffect) -> Unit = { view, effect ->
        view.performHapticFeedback(effect.feedback)
    }

    /** One light tick for a press that does something, such as a hinted button. */
    fun tick(view: View) {
        if (gate.allows(clock(), isEnabled())) play(view, JarnHapticEffect.Tick)
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
    fun perform(view: View, effect: JarnHapticEffect) {
        if (isEnabled()) play(view, effect)
    }

    private fun play(view: View, effect: JarnHapticEffect) {
        val vibrator = vibratorOf(view)
        if (vibrator != null) vibrate(vibrator, effect) else viewFeedback(view, effect)
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
