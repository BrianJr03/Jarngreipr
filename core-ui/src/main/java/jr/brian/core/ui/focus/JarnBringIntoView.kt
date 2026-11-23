package jr.brian.core.ui.focus

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.theme.LocalJarnSpacing

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
    private val marginPx: Float,
    private val base: BringIntoViewSpec,
) : BringIntoViewSpec by base {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val contentTop = offset + scroll.value
        return if (jarnFitsAtScrollTop(contentTop, size, containerSize, topZonePx, marginPx)) {
            -scroll.value.toFloat()
        } else {
            base.calculateScrollDistance(offset, size, containerSize)
        }
    }
}

/** True when an item is near the page top and stays clear of the bottom edge at scroll zero. */
internal fun jarnFitsAtScrollTop(
    contentTop: Float,
    size: Float,
    container: Float,
    topZone: Float,
    margin: Float,
): Boolean = contentTop <= topZone && contentTop + size + margin <= container

/**
 * For a scrolling page whose top holds content that can't take focus. Focusing an item within
 * [topZone] scrolls all the way up, so that content comes back into view.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JarnRevealScrollTop(
    scroll: ScrollState,
    topZone: Dp = JarnScrollTopZone,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val topZonePx = with(density) { topZone.toPx() }
    val marginPx = with(density) { LocalJarnSpacing.current.focusEdge.toPx() }
    val base = LocalBringIntoViewSpec.current
    val spec = remember(scroll, topZonePx, marginPx, base) {
        JarnTopRevealSpec(scroll, topZonePx, marginPx, base)
    }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/**
 * For a scroller nested inside a [JarnRevealScrollTop] page. That rule measures the page's axis,
 * so a row running the other way needs its own, keeping focused items [margin] clear of each end.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JarnEdgeScroll(margin: Dp = LocalJarnSpacing.current.focusEdge, content: @Composable () -> Unit) {
    val marginPx = with(LocalDensity.current) { margin.toPx() }
    val base = LocalBringIntoViewSpec.current
    val spec = remember(marginPx, base) { JarnBringIntoViewSpec(marginPx, base) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}

/**
 * Scroll for a focused lazy item, measured on the whole item so a title inside it stays visible.
 * [toTop] and [toBottom] are set on the first and last items, so the list's padding is reachable.
 */
internal fun jarnLazyItemDistance(
    itemStart: Float,
    itemSize: Float,
    container: Float,
    margin: Float,
    toTop: Float?,
    toBottom: Float?,
): Float {
    val distance = jarnScrollDistance(itemStart, itemSize, container, margin)
    return when {
        itemSize + 2 * margin >= container -> distance
        toTop != null -> minOf(distance, toTop)
        toBottom != null -> maxOf(distance, toBottom)
        else -> distance
    }
}

@OptIn(ExperimentalFoundationApi::class)
private class JarnLazyRevealSpec(
    private val list: LazyListState,
    private val marginPx: Float,
    private val base: BringIntoViewSpec,
) : BringIntoViewSpec by base {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val layout = list.layoutInfo
        val center = offset + size / 2
        val item = layout.visibleItemsInfo.firstOrNull {
            val start = it.offset - layout.viewportStartOffset
            center >= start && center <= start + it.size
        } ?: return base.calculateScrollDistance(offset, size, containerSize)
        val start = (item.offset - layout.viewportStartOffset).toFloat()
        return jarnLazyItemDistance(
            itemStart = start,
            itemSize = item.size.toFloat(),
            container = containerSize,
            margin = marginPx,
            toTop = item.offset.toFloat().takeIf { item.index == 0 },
            toBottom = (start + item.size + layout.afterContentPadding - containerSize)
                .takeIf { item.index == layout.totalItemsCount - 1 },
        )
    }
}

/**
 * The [JarnRevealScrollTop] equivalent for a lazy list. Focusing anything in an item reveals the
 * whole item, so a title placed above that item's first row comes into view with it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JarnRevealLazyItems(state: LazyListState, content: @Composable () -> Unit) {
    val marginPx = with(LocalDensity.current) { LocalJarnSpacing.current.focusEdge.toPx() }
    val base = LocalBringIntoViewSpec.current
    val spec = remember(state, marginPx, base) { JarnLazyRevealSpec(state, marginPx, base) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
