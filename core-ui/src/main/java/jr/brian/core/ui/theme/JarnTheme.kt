package jr.brian.core.ui.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext

val LocalJarnLook = staticCompositionLocalOf { JarnLooks.Default }
val LocalJarnColors = staticCompositionLocalOf { JarnLooks.Default.resolvedColors }
val LocalJarnType = staticCompositionLocalOf { JarnType() }
val LocalJarnSpacing = staticCompositionLocalOf { JarnSpacing() }
val LocalJarnRadius = staticCompositionLocalOf { JarnRadius() }
val LocalJarnMotion = staticCompositionLocalOf { JarnMotion() }
val LocalJarnScreenActive = staticCompositionLocalOf { true }

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
    CompositionLocalProvider(
        LocalJarnLook provides look,
        LocalJarnColors provides colors,
        LocalJarnMotion provides motion,
        LocalJarnScreenActive provides screenActive,
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
