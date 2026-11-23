package jr.brian.core.ui.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import jr.brian.core.ui.focus.JarnBringIntoViewSpec
import jr.brian.core.ui.focus.JarnTabsFocus
import jr.brian.core.ui.focus.LocalJarnTabsFocus

val LocalJarnLook = staticCompositionLocalOf { JarnLooks.Default }
val LocalJarnColors = staticCompositionLocalOf { JarnLooks.Default.resolvedColors }
val LocalJarnType = staticCompositionLocalOf { JarnType() }
val LocalJarnSpacing = staticCompositionLocalOf { JarnSpacing() }
val LocalJarnRadius = staticCompositionLocalOf { JarnRadius() }
val LocalJarnMotion = staticCompositionLocalOf { JarnMotion() }
val LocalJarnScreenActive = staticCompositionLocalOf { true }

/** 0 on an inactive screen, rising to 1 as this screen takes control. */
val LocalJarnScreenActivation = staticCompositionLocalOf<State<Float>> { mutableFloatStateOf(1f) }

private const val JARN_SCREEN_SWITCH_MILLIS = 300

object JarnTheme {
    val look: JarnLook @Composable get() = LocalJarnLook.current
    val colors: JarnColors @Composable get() = LocalJarnColors.current
    val type: JarnType @Composable get() = LocalJarnType.current
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
    content: @Composable () -> Unit,
) {
    val animatorScale = rememberAnimatorDurationScale()
    val shouldReduce = jarnShouldReduceMotion(reducedMotion, animatorScale)
    val colors = remember(look) { look.resolvedColors }
    val motion = remember(shouldReduce) { JarnMotion(reduced = shouldReduce) }
    val focusEdgePx = with(LocalDensity.current) { LocalJarnSpacing.current.focusEdge.toPx() }
    val baseScrollSpec = LocalBringIntoViewSpec.current
    val scrollSpec = remember(focusEdgePx, baseScrollSpec) {
        JarnBringIntoViewSpec(focusEdgePx, baseScrollSpec)
    }
    val activation = remember { Animatable(if (screenActive) 1f else 0f) }
    LaunchedEffect(screenActive, shouldReduce) {
        when {
            !screenActive -> activation.snapTo(0f)
            shouldReduce -> activation.snapTo(1f)
            else -> activation.animateTo(1f, tween(JARN_SCREEN_SWITCH_MILLIS, easing = motion.panel.easing))
        }
    }
    val activationState = remember(activation) { activation.asState() }
    CompositionLocalProvider(
        LocalJarnLook provides look,
        LocalJarnColors provides colors,
        LocalJarnMotion provides motion,
        LocalJarnScreenActive provides screenActive,
        LocalJarnScreenActivation provides activationState,
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
