package jr.brian.jarnfe.backdrop

import android.os.SystemClock
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import jr.brian.core.ui.components.LocalJarnTextShadow
import jr.brian.core.ui.theme.LocalJarnColors
import jr.brian.core.ui.theme.JarnTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** A few reads a second: enough to follow a cover change, far below the frame rate. */
internal const val JARN_ADAPTIVE_SAMPLE_MS = 250L
private const val TONE_FADE_MS = 200
private const val SHADOW_ALPHA = 0.45f
private const val SHADOW_BLUR = 6f

/** The tone shared by every label in one header, so the row never mixes light and dark text. */
@Stable
internal class JarnAdaptiveTextState {
    var tone by mutableStateOf(JarnTextTone.Theme)
        internal set
    internal val regions = HashMap<Any, Rect>()
}

internal val LocalJarnAdaptiveText = staticCompositionLocalOf<JarnAdaptiveTextState?> { null }

/**
 * Reads the backdrop under every label registered with [JarnAdaptiveContent] every
 * [JARN_ADAPTIVE_SAMPLE_MS] while [isNeeded] and [canSample] hold. When nothing needs it the
 * tone returns to the look's own. While [canSample] is false (screen off, Jarn behind a game)
 * sampling pauses and the tone holds.
 */
@Composable
internal fun rememberJarnAdaptiveText(
    sampler: JarnBackdropSampler?,
    isNeeded: State<Boolean>,
    canSample: State<Boolean>,
): JarnAdaptiveTextState {
    val state = remember { JarnAdaptiveTextState() }
    val colors by rememberUpdatedState(JarnTheme.colors)
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    LaunchedEffect(state, sampler, density, direction) {
        if (sampler == null) return@LaunchedEffect
        val throttle = JarnSampleThrottle(JARN_ADAPTIVE_SAMPLE_MS)
        snapshotFlow { isNeeded.value to canSample.value }.collectLatest { (needed, allowed) ->
            if (!needed) state.tone = JarnTextTone.Theme
            if (!needed || !allowed) return@collectLatest
            while (true) {
                delay(throttle.waitMillis(SystemClock.uptimeMillis()))
                throttle.mark(SystemClock.uptimeMillis())
                val pixels = sampler.capture(density, direction) ?: continue
                val backgrounds = state.regions.values.mapNotNull { pixels.meanLuminance(sampler.local(it)) }
                state.tone = jarnTextToneOver(colors, backgrounds, state.tone)
            }
        }
    }
    return state
}

/** Puts [content] under the shared tone and has the backdrop behind it sampled. */
@Composable
internal fun JarnAdaptiveContent(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = LocalJarnAdaptiveText.current
    if (state == null) {
        Box(modifier) { content() }
        return
    }
    val key = remember { Any() }
    DisposableEffect(state, key) { onDispose { state.regions.remove(key) } }
    Box(modifier.onGloballyPositioned { state.regions[key] = it.boundsInWindow() }) {
        JarnToneColors(state.tone, content)
    }
}

/** The look's text colors for [tone], faded in, with a faint opposite shadow as a safety net. */
@Composable
internal fun JarnToneColors(tone: JarnTextTone, content: @Composable () -> Unit) {
    val base = JarnTheme.colors
    val target = jarnTextColorsFor(base, tone)
    val spec: AnimationSpec<Color> = if (JarnTheme.motion.reduced) snap() else tween(TONE_FADE_MS)
    val high by animateColorAsState(target.textHigh, spec, label = "toneHigh")
    val mid by animateColorAsState(target.textMid, spec, label = "toneMid")
    val low by animateColorAsState(target.textLow, spec, label = "toneLow")
    val raised by animateColorAsState(target.raised, spec, label = "toneRaised")
    val shadow by animateColorAsState(jarnTextShadowColor(base, tone), spec, label = "toneShadow")
    CompositionLocalProvider(
        LocalJarnColors provides base.copy(textHigh = high, textMid = mid, textLow = low, raised = raised),
        LocalJarnTextShadow provides Shadow(shadow.copy(alpha = SHADOW_ALPHA), blurRadius = SHADOW_BLUR),
        content = content,
    )
}
