package jr.brian.core.ui.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val JarnScrollTopZone = 96.dp

/**
 * How far to scroll so a focused item keeps [margin] clear of both edges. An item too large to
 * fit with its margins lines up with the start, so its beginning is always readable.
 */
internal fun jarnScrollDistance(offset: Float, size: Float, container: Float, margin: Float): Float {
    val leading = offset - margin
    val trailing = offset + size + margin - container
    return when {
        size + 2 * margin >= container -> leading
        leading < 0f -> leading
        trailing > 0f -> trailing
        else -> 0f
    }
}

/** Provided by JarnTheme, so every scrolling Jarn FE screen follows the same edge rule. */
@OptIn(ExperimentalFoundationApi::class)
internal class JarnBringIntoViewSpec(
    private val marginPx: Float,
    base: BringIntoViewSpec,
) : BringIntoViewSpec by base {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        jarnScrollDistance(offset, size, containerSize, marginPx)
}

@OptIn(ExperimentalFoundationApi::class)
private class JarnTopRevealSpec(
    private val scroll: ScrollState,
    private val topZonePx: Float,
    private val base: BringIntoViewSpec,
) : BringIntoViewSpec by base {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        if (offset + scroll.value <= topZonePx) {
            -scroll.value.toFloat()
        } else {
            base.calculateScrollDistance(offset, size, containerSize)
        }
}

/**
 * For a scrolling page whose top holds text that can't take focus. Focusing the first item
 * scrolls all the way up, so that text comes back into view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JarnRevealScrollTop(scroll: ScrollState, content: @Composable () -> Unit) {
    val topZonePx = with(LocalDensity.current) { JarnScrollTopZone.toPx() }
    val base = LocalBringIntoViewSpec.current
    val spec = remember(scroll, topZonePx, base) { JarnTopRevealSpec(scroll, topZonePx, base) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
