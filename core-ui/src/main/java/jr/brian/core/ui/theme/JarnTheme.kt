package jr.brian.core.ui.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import jr.brian.core.ui.focus.JarnBringIntoViewSpec
import jr.brian.core.ui.focus.JarnTabsFocus
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import kotlinx.coroutines.launch

val LocalJarnLook = staticCompositionLocalOf { JarnLooks.Default }
val LocalJarnColors = staticCompositionLocalOf { JarnLooks.Default.resolvedColors }
private val JarnDefaultType = JarnType()

val LocalJarnType = staticCompositionLocalOf { JarnDefaultType }
val LocalJarnSpacing = staticCompositionLocalOf { JarnSpacing() }
val LocalJarnRadius = staticCompositionLocalOf { JarnRadius() }
val LocalJarnMotion = staticCompositionLocalOf { JarnMotion() }
val LocalJarnScreenActive = staticCompositionLocalOf { true }

/** 0 on an inactive screen, rising to 1 as this screen takes control. */
val LocalJarnScreenActivation = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(1f) }

/** True while a focused game or system may keep floating. Off behind a game and on battery saver. */
val LocalJarnFocusFloatAllowed = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(false) }

private const val JARN_SCREEN_SWITCH_MILLIS = 300

object JarnTheme {
    val look: JarnLook @Composable get() = LocalJarnLook.current
    val colors: JarnColors @Composable get() = LocalJarnColors.current
    val type: JarnType @Composable get() = LocalJarnType.current

    /** Jarn's own type, unaffected by font overrides. For captions, numbers, settings and game names. */
    val baseType: JarnType get() = JarnDefaultType
    val space: JarnSpacing @Composable get() = LocalJarnSpacing.current
    val radius: JarnRadius @Composable get() = LocalJarnRadius.current
    val motion: JarnMotion @Composable get() = LocalJarnMotion.current
    val tileShape: Shape @Composable get() = look.tileShape ?: radius.mdShape
    val cardStyle: JarnCardStyle @Composable get() = look.cardStyle ?: JarnCardStyle.Tonal
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JarnTheme(
    look: JarnLook = JarnLooks.Default,
    reducedMotion: Boolean = false,
    screenActive: Boolean = true,
    switchCue: JarnSwitchCue = JarnSwitchCue.Pulse,
    type: JarnType? = null,
    content: @Composable () -> Unit,
) {
    val animatorScale = rememberAnimatorDurationScale()
    val shouldReduce = jarnShouldReduceMotion(reducedMotion, animatorScale)
    val colors = remember(look) { look.resolvedColors }
    val animationsOff = animatorScale == 0f
    val motion = remember(shouldReduce, reducedMotion, animationsOff) {
        JarnMotion(reduced = shouldReduce, ambientReduced = reducedMotion, instant = animationsOff)
    }
    val focusEdgePx = with(LocalDensity.current) { LocalJarnSpacing.current.focusEdge.toPx() }
    val baseScrollSpec = LocalBringIntoViewSpec.current
    val scrollSpec = remember(focusEdgePx, baseScrollSpec) {
        JarnBringIntoViewSpec(focusEdgePx, baseScrollSpec)
    }
    val activation = remember { Animatable(if (screenActive) 1f else 0f) }
    val pulses = switchCue == JarnSwitchCue.Pulse
    val marks = switchCue == JarnSwitchCue.TabMark
    val pulse = remember { JarnSwitchPulse(pulses) }
    val mark = remember { JarnSwitchMark(marks, screenActive) }
    val wasActive = remember { booleanArrayOf(screenActive) }
    SideEffect {
        pulse.enabled = pulses
        mark.enabled = marks
    }
    LaunchedEffect(screenActive, marks, shouldReduce) { mark.moveTo(screenActive && marks, shouldReduce) }
    LaunchedEffect(screenActive, shouldReduce, pulses) {
        val received = screenActive && !wasActive[0]
        wasActive[0] = screenActive
        if (received && pulses) {
            val now = SystemClock.uptimeMillis()
            val strength = JarnSwitchRhythm.Shared.strengthAt(now)
            pulse.arm(strength, shouldReduce, now)
            launch { pulse.play(strength, shouldReduce) }
        } else {
            pulse.rest()
        }
        when {
            !screenActive -> activation.snapTo(0f)
            shouldReduce -> activation.snapTo(1f)
            pulses -> activation.animateTo(1f, tween(JARN_PULSE_UP_MILLIS, easing = JarnEasingStandard))
            else -> activation.animateTo(1f, tween(JARN_SCREEN_SWITCH_MILLIS, easing = motion.panel.easing))
        }
    }
    val activationState = remember(activation) { activation.asState() }
    CompositionLocalProvider(
        LocalJarnLook provides look,
        LocalJarnColors provides colors,
        LocalJarnType provides (type ?: JarnDefaultType),
        LocalJarnMotion provides motion,
        LocalJarnScreenActive provides screenActive,
        LocalJarnScreenActivation provides activationState,
        LocalJarnSwitchPulse provides pulse,
        LocalJarnSwitchMark provides mark,
        LocalJarnTabsFocus provides remember { JarnTabsFocus() },
        LocalBringIntoViewSpec provides scrollSpec,
        content = content,
    )
}

@Composable
private fun rememberAnimatorDurationScale(): Float {
    val resolver = LocalContext.current.contentResolver
    var scale by remember(resolver) { mutableFloatStateOf(readAnimatorDurationScale(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scale = readAnimatorDurationScale(resolver)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return scale
}

private fun readAnimatorDurationScale(resolver: ContentResolver): Float =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
