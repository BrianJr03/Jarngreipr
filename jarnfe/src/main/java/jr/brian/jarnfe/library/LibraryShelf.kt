package jr.brian.jarnfe.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnEasingStandard
import jr.brian.core.ui.theme.JarnTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SHELF_SLIDE_MS = 200

/** What the shelf hands a card: its slot shape, focus and click wiring, and how dim its art is. */
internal class LibraryShelfSlotCard(
    val aspectRatio: Float,
    val modifier: Modifier,
    val onClick: () -> Unit,
    val description: String,
    val artAlpha: () -> Float,
)

/**
 * What a shelf shows for one item. [onOpen] runs for A, or a tap on the centered card. [headline]
 * is the line under the centered card and [caption] an optional second line. [card] draws one
 * item, by default [art] on a card.
 */
internal class LibraryShelfContent<T>(
    val itemKey: (T) -> String,
    val itemTitle: (T) -> String,
    val onOpen: (T) -> Unit,
    val itemModifier: (T) -> Modifier = { Modifier },
    val headline: @Composable (T) -> String = { heroTitle(itemTitle(it)) },
    val caption: @Composable (T) -> String? = { null },
    val art: @Composable BoxScope.(T) -> Unit,
    val card: @Composable (T, LibraryShelfSlotCard) -> Unit = { item, slot ->
        LibraryShelfCard(slot) { art(this, item) }
    },
)

/**
 * One flat row with the focused item large in the center. Left and Right move one item and wrap
 * at [SHELF_WRAP_MIN_ITEMS] or more, Down is reserved, and only items near the center are
 * composed, so a list of thousands stays smooth. While a direction is held, the letter badge
 * shows and [onFocused] waits [SHELF_SETTLE_MS] for the row to settle, so the Deck doesn't
 * flicker through every cover.
 */
@Composable
internal fun <T : Any> LibraryShelf(
    items: List<T>,
    kind: LibraryShelfKind,
    content: LibraryShelfContent<T>,
    focusedKey: String?,
    onFocused: (String) -> Unit,
    modifier: Modifier = Modifier,
    refocusSignal: Int = 0,
) {
    if (items.isEmpty()) return
    var chosenKey by remember { mutableStateOf(focusedKey) }
    var fast by remember { mutableStateOf(false) }
    var badgeTick by remember { mutableIntStateOf(0) }
    val center = shelfCenterIndex(items, content.itemKey, chosenKey, focusedKey)
    val centerKey = content.itemKey(items[center])
    val requesters = remember { HashMap<String, FocusRequester>() }
    fun requesterFor(key: String) = requesters.getOrPut(key) { FocusRequester() }

    ShelfFollowsLibrary(focusedKey, items, content.itemKey, fast) { chosenKey = it }
    ShelfReportsFocus(centerKey, fast, onFocused)
    ShelfTakesFocus(centerKey, refocusSignal) { requesterFor(centerKey) }

    var swipeStepPx by remember { mutableFloatStateOf(0f) }

    fun stepBy(delta: Int) {
        chosenKey = content.itemKey(items[shelfStep(center, delta, items.size)])
    }

    val latestStep by rememberUpdatedState<(Int) -> Unit>(::stepBy)

    val onKey: (KeyEvent) -> Boolean = { event ->
        handleShelfKey(event, onFast = { repeat ->
            fast = repeat
            if (repeat) badgeTick++
        }, onStep = ::stepBy)
    }
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .onPreviewKeyEvent(onKey)
            .shelfSwipe({ swipeStepPx }) { delta -> latestStep(delta) }
            .focusProperties { onEnter = { requesterFor(centerKey).requestFocus() } }
            .focusGroup(),
    ) {
        val narrow = maxWidth < SHELF_NARROW_WIDTH
        val fit = shelfFit(
            width = maxWidth.value,
            height = maxHeight.value,
            kind = kind,
            narrow = narrow,
            topMargin = JarnTheme.space.focusEdge.value,
            captionReserve = kind.captionReserve,
        )
        val metrics = ShelfMetrics(kind, maxWidth, fit)
        val cardWidth = metrics.cardWidth
        swipeStepPx = with(LocalDensity.current) { (cardWidth * kind.neighborScale).toPx() }
        LibraryShelfWash(items[center], content)
        shelfComposedIndices(center, items.size).forEach { index ->
            val item = items[index]
            val itemKey = content.itemKey(item)
            key(itemKey) {
                ShelfSlot(
                    distance = shelfDistance(index, center, items.size),
                    metrics = metrics,
                    modifier = content.itemModifier(item).focusRequester(requesterFor(itemKey)),
                    onClick = { if (itemKey == centerKey) content.onOpen(item) else chosenKey = itemKey },
                    description = content.itemTitle(item),
                ) { slot -> content.card(item, slot) }
            }
        }
        LibraryShelfCaption(items[center], content, metrics)
        LibraryShelfBadge(content.itemTitle(items[center]), badgeTick, fast)
    }
}

internal val SHELF_NARROW_WIDTH = 600.dp

internal class ShelfMetrics(val kind: LibraryShelfKind, val width: Dp, fit: ShelfFit) {
    val cardWidth: Dp = fit.cardWidth.dp
    val cardHeight: Dp = fit.cardHeight.dp
    val centerY: Dp = fit.centerY.dp

    /** Room for the centered card's focus border, so it never reaches a neighbor. */
    val minGap: Dp = shelfMinGap(fit.cardWidth, fit.cardHeight).dp
}

private fun <T> shelfCenterIndex(items: List<T>, itemKey: (T) -> String, chosen: String?, focused: String?): Int {
    val byChosen = items.indexOfFirst { itemKey(it) == chosen }
    if (byChosen >= 0) return byChosen
    return items.indexOfFirst { itemKey(it) == focused }.coerceAtLeast(0)
}

/** A swipe steps once per neighbor width dragged, so a long drag runs through several items. */
private fun Modifier.shelfSwipe(stepPx: () -> Float, onStep: (Int) -> Unit): Modifier = pointerInput(Unit) {
    var dragged = 0f
    detectHorizontalDragGestures(onDragStart = { dragged = 0f }) { change, amount ->
        change.consume()
        dragged += amount
        val step = stepPx()
        while (step > 0f && abs(dragged) >= step) {
            val delta = if (dragged > 0f) -1 else 1
            onStep(delta)
            dragged += delta * step
        }
    }
}

/** Down is reserved, so it is swallowed rather than leaving the row. */
private fun handleShelfKey(event: KeyEvent, onFast: (Boolean) -> Unit, onStep: (Int) -> Unit): Boolean {
    val delta = when (event.key) {
        Key.DirectionLeft -> -1
        Key.DirectionRight -> 1
        Key.DirectionDown -> 0
        else -> return false
    }
    when (event.type) {
        KeyEventType.KeyDown -> if (delta != 0) {
            onFast(event.nativeKeyEvent.repeatCount > 0)
            onStep(delta)
        }
        KeyEventType.KeyUp -> if (delta != 0) onFast(false)
        else -> return false
    }
    return true
}

/** A change made elsewhere, such as B back to Systems or closing Search, recenters the row. */
@Composable
private fun <T> ShelfFollowsLibrary(
    focusedKey: String?,
    items: List<T>,
    itemKey: (T) -> String,
    fast: Boolean,
    onCenter: (String) -> Unit,
) {
    val currentFast by rememberUpdatedState(fast)
    LaunchedEffect(focusedKey, items) {
        if (focusedKey != null && !currentFast && items.any { itemKey(it) == focusedKey }) onCenter(focusedKey)
    }
}

@Composable
private fun ShelfReportsFocus(centerKey: String, fast: Boolean, onFocused: (String) -> Unit) {
    val report by rememberUpdatedState(onFocused)
    LaunchedEffect(centerKey, fast) {
        if (fast) delay(SHELF_SETTLE_MS)
        report(centerKey)
    }
}

/** Leaves a user on the tab row where they are, as the grid does. */
@Composable
private fun ShelfTakesFocus(centerKey: String, refocusSignal: Int, requester: () -> FocusRequester) {
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(centerKey, refocusSignal) {
        withFrameNanos { }
        if (!tabsFocus.hasFocus) requester().requestFocus()
    }
}

@Composable
private fun BoxScope.ShelfSlot(
    distance: Int,
    metrics: ShelfMetrics,
    modifier: Modifier,
    onClick: () -> Unit,
    description: String,
    card: @Composable (LibraryShelfSlotCard) -> Unit,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { metrics.width.toPx() }
    val cardPx = with(density) { metrics.cardWidth.toPx() }
    val minGapPx = with(density) { metrics.minGap.toPx() }
    val targetX = shelfSlotX(distance, widthPx, metrics.kind, cardPx, minGapPx)
    val targetScale = shelfSlotScale(distance, metrics.kind)
    val targetAlpha = shelfSlotAlpha(distance)
    val x = remember { Animatable(targetX) }
    val scale = remember { Animatable(targetScale) }
    val alpha = remember { Animatable(targetAlpha) }
    val reduced = JarnTheme.motion.reduced
    var previous by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(distance, targetX) {
        val snap = reduced || shelfSlotJumps(previous, distance)
        previous = distance
        val spec = tween<Float>(SHELF_SLIDE_MS, easing = JarnEasingStandard)
        listOf(x to targetX, scale to targetScale, alpha to targetAlpha).forEach { (value, target) ->
            launch { if (snap) value.snapTo(target) else value.animateTo(target, spec) }
        }
    }
    val top = with(density) { (metrics.centerY - metrics.cardHeight / 2).roundToPx() }
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .offset { IntOffset(x.value.roundToInt(), top) }
            .zIndex((SHELF_COMPOSED_REACH - abs(distance)).toFloat())
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = shelfSlotVisibility(alpha.value)
            }
            .width(metrics.cardWidth),
    ) {
        card(
            LibraryShelfSlotCard(
                aspectRatio = metrics.kind.aspectRatio,
                modifier = modifier,
                onClick = onClick,
                description = description,
                artAlpha = { shelfArtAlpha(alpha.value) },
            ),
        )
    }
}
