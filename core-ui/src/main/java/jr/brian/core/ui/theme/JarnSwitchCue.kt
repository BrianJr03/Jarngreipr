package jr.brian.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** How a screen shows it just received control. The ids are stored in prefs and backups. */
enum class JarnSwitchCue(val id: String) {
    Pulse("pulse"),
    Dim("dim"),
    TabMark("tab_mark"),
    ;

    companion object {
        fun fromId(id: String?): JarnSwitchCue = entries.firstOrNull { it.id == id } ?: Pulse
    }
}

internal const val JARN_PULSE_UP_MILLIS = 110
internal const val JARN_PULSE_BACK_MILLIS = 170
internal const val JARN_PULSE_RING_EXTRA = 0.25f
private const val JARN_PULSE_REDUCED_RING_MILLIS = 120
private const val JARN_PULSE_NONE_BELOW_MILLIS = 150L
private const val JARN_PULSE_FULL_ABOVE_MILLIS = 700L
private const val JARN_PULSE_JOIN_MILLIS = 64L
private const val JARN_MARK_MIN_WIDTH = 0.4f
private const val JARN_MARK_IN_MILLIS = 160
private const val JARN_MARK_OUT_MILLIS = 120
private const val JARN_MARK_REDUCED_MILLIS = 120

internal val JarnPulseExtra = 1.5.dp

/** A focused tile never leaves its slot by more than this, pulse included. */
internal val JarnFocusMaxReach = 15.dp

/** Quick repeat switches fade the pulse out, so mashing Select stays calm. */
internal fun jarnPulseStrength(gapMillis: Long): Float {
    val span = (JARN_PULSE_FULL_ABOVE_MILLIS - JARN_PULSE_NONE_BELOW_MILLIS).toFloat()
    val x = ((gapMillis - JARN_PULSE_NONE_BELOW_MILLIS) / span).coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Focus arriving just after a switch is the handoff. Later focus is a move within the screen. */
internal fun jarnPulseAdmitsFocus(elapsedMillis: Long): Boolean = elapsedMillis < JARN_PULSE_JOIN_MILLIS

/** Shared by both windows, because a switch is measured against the last one on either screen. */
internal class JarnSwitchRhythm {
    private var lastSwitchAt: Long? = null

    fun strengthAt(nowMillis: Long): Float {
        val last = lastSwitchAt
        lastSwitchAt = nowMillis
        return if (last == null) 1f else jarnPulseStrength(nowMillis - last)
    }

    companion object {
        val Shared = JarnSwitchRhythm()
    }
}

@Stable
class JarnSwitchPulse internal constructor(enabled: Boolean = false) {
    internal var enabled by mutableStateOf(enabled)
    internal var generation by mutableIntStateOf(0)
        private set
    internal var startedAtMillis = 0L
        private set
    private val overshoot = Animatable(0f)
    private val ringExtra = Animatable(0f)
    private val ringFade = Animatable(1f)

    internal fun overshootFor(ignored: Int): Float = if (generation == ignored) 0f else overshoot.value

    internal fun ringExtraFor(ignored: Int): Float = if (generation == ignored) 0f else ringExtra.value

    internal fun ringFadeFor(ignored: Int): Float = if (generation == ignored) 1f else ringFade.value

    internal suspend fun rest() {
        overshoot.snapTo(0f)
        ringExtra.snapTo(0f)
        ringFade.snapTo(1f)
    }

    /** Runs before activation changes, so a reduced ring never shows for a frame at full alpha. */
    internal suspend fun arm(strength: Float, reduced: Boolean, nowMillis: Long) {
        generation++
        startedAtMillis = nowMillis
        ringExtra.snapTo(if (reduced) 0f else strength)
        ringFade.snapTo(if (reduced) 0f else 1f)
        if (reduced) overshoot.snapTo(0f)
    }

    internal suspend fun play(strength: Float, reduced: Boolean) {
        if (reduced) {
            ringFade.animateTo(1f, tween(JARN_PULSE_REDUCED_RING_MILLIS, easing = LinearEasing))
            return
        }
        coroutineScope {
            launch {
                ringExtra.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = JARN_PULSE_BACK_MILLIS,
                        delayMillis = JARN_PULSE_UP_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
                )
            }
            overshoot.animateTo(strength, tween(JARN_PULSE_UP_MILLIS, easing = JarnEasingStandard))
            overshoot.animateTo(0f, tween(JARN_PULSE_BACK_MILLIS, easing = FastOutSlowInEasing))
        }
    }
}

/** Opens only while rising, and keeps its width as it fades out. */
internal fun jarnMarkWidth(current: Float, progress: Float, rising: Boolean, reduced: Boolean): Float = when {
    reduced -> 1f
    progress <= 0f -> JARN_MARK_MIN_WIDTH
    rising -> maxOf(current, JARN_MARK_MIN_WIDTH + (1f - JARN_MARK_MIN_WIDTH) * progress)
    else -> current
}

@Stable
class JarnSwitchMark internal constructor(enabled: Boolean = false, lit: Boolean = false) {
    var enabled by mutableStateOf(enabled)
        internal set
    private val progress = Animatable(if (enabled && lit) 1f else 0f)
    private var width by mutableFloatStateOf(if (enabled && lit) 1f else JARN_MARK_MIN_WIDTH)

    val alpha: Float get() = progress.value
    val widthFraction: Float get() = width

    internal suspend fun moveTo(lit: Boolean, reduced: Boolean) {
        val target = if (lit) 1f else 0f
        val spec = when {
            reduced -> tween<Float>(JARN_MARK_REDUCED_MILLIS, easing = LinearEasing)
            lit -> tween(JARN_MARK_IN_MILLIS, easing = JarnEasingStandard)
            else -> tween(JARN_MARK_OUT_MILLIS, easing = JarnEasingExit)
        }
        progress.animateTo(target, spec) { width = jarnMarkWidth(width, value, lit, reduced) }
        width = jarnMarkWidth(width, progress.value, lit, reduced)
    }
}

val LocalJarnSwitchPulse = staticCompositionLocalOf { JarnSwitchPulse() }
val LocalJarnSwitchMark = staticCompositionLocalOf { JarnSwitchMark() }
