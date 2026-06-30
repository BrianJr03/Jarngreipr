package jr.brian.home.esde.ui.frontend

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.home.esde.model.FrontendLayout
import kotlin.math.abs
import kotlinx.coroutines.delay

private val DEFAULT_ROW_TILE_WIDTH = 240.dp
private const val FOCUS_RESET_RETRIES = 3
private const val FOCUS_RESET_RETRY_DELAY_MS = 80L

private const val ROW_FALLOFF_STEP = 0.15f
private const val ROW_FALLOFF_FLOOR = 0.6f

private const val RAPID_SCROLL_WINDOW_MS = 100L

/**
 * Row mode renders this many virtual slots so focus can advance indefinitely in
 * either direction. items[virtualIndex % items.size] gives the real item.
 */
private const val INFINITE_ROW_VIRTUAL_COUNT = 10_000

@Composable
fun <T> FocusableTileLayout(
    items: List<T>,
    layout: FrontendLayout,
    columns: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = FrontendTokens.Spacing.M,
        vertical = FrontendTokens.Spacing.L
    ),
    itemSpacing: Dp = FrontendTokens.Spacing.S,
    rowItemWidth: Dp = DEFAULT_ROW_TILE_WIDTH,
    /**
     * Initial focus index in *real* items space (0..items.lastIndex). Row mode
     * translates this onto the virtual list internally. Callers use this to
     * restore focus to a remembered item after a route switch.
     */
    initialRealIndex: Int = 0,
    focusResetKey: Any? = Unit,
    onItemFocused: (T?) -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    itemKey: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (
        index: Int,
        item: T,
        focusRequester: FocusRequester,
        isFocused: Boolean,
        onFocused: () -> Unit
    ) -> Unit
) {
    when (layout) {
        FrontendLayout.Grid -> GridLayout(
            items = items,
            columns = columns,
            modifier = modifier,
            contentPadding = contentPadding,
            itemSpacing = itemSpacing,
            initialIndex = initialRealIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            focusResetKey = focusResetKey,
            onItemFocused = onItemFocused,
            header = header,
            itemKey = itemKey,
            itemContent = itemContent
        )
        FrontendLayout.Row -> RowLayout(
            items = items,
            modifier = modifier,
            contentPadding = contentPadding,
            itemSpacing = itemSpacing,
            rowItemWidth = rowItemWidth,
            initialRealIndex = initialRealIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)),
            focusResetKey = focusResetKey,
            onItemFocused = onItemFocused,
            header = header,
            itemContent = itemContent
        )
    }
}

@Composable
private fun <T> GridLayout(
    items: List<T>,
    columns: Int,
    modifier: Modifier,
    contentPadding: PaddingValues,
    itemSpacing: Dp,
    initialIndex: Int,
    focusResetKey: Any?,
    onItemFocused: (T?) -> Unit,
    header: (@Composable () -> Unit)?,
    itemKey: ((index: Int, item: T) -> Any)?,
    itemContent: @Composable (Int, T, FocusRequester, Boolean, () -> Unit) -> Unit
) {
    var focusedIndex by remember(initialIndex) { mutableIntStateOf(initialIndex) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val containerFocus = remember { FocusRequester() }

    val gridState = rememberLazyGridState()
    val headerOffset = if (header != null) 1 else 0

    var lastFocusChangeMs by remember { mutableLongStateOf(0L) }
    var isRapidScroll by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val verticalSpacingPx = with(density) { itemSpacing.roundToPx() }

    LaunchedEffect(focusedIndex) {
        val now = SystemClock.uptimeMillis()
        isRapidScroll = lastFocusChangeMs != 0L &&
                (now - lastFocusChangeMs) < RAPID_SCROLL_WINDOW_MS
        lastFocusChangeMs = now
        focusRequesters[focusedIndex]?.requestFocus()

        val layoutIndex = focusedIndex + headerOffset
        val info = gridState.layoutInfo
        val start = info.viewportStartOffset + info.beforeContentPadding
        val end = info.viewportEndOffset - info.afterContentPadding
        val target = info.visibleItemsInfo.firstOrNull { it.index == layoutIndex }

        when {
            isRapidScroll -> gridState.scrollToItem(layoutIndex)
            target != null -> when {
                target.offset.y < start ->
                    gridState.animateScrollBy(-(start - target.offset.y).toFloat())
                target.offset.y + target.size.height > end ->
                    gridState.animateScrollBy((target.offset.y + target.size.height - end).toFloat())
            }
            else -> {
                val sample = info.visibleItemsInfo.firstOrNull()
                if (sample != null && columns > 0) {
                    val rowPitch = sample.size.height + verticalSpacingPx
                    val firstRow = (sample.index - headerOffset) / columns
                    val targetRow = (layoutIndex - headerOffset) / columns
                    val estTop = sample.offset.y + (targetRow - firstRow) * rowPitch
                    val delta = when {
                        estTop < start -> (estTop - start).toFloat()
                        estTop + sample.size.height > end ->
                            (estTop + sample.size.height - end).toFloat()
                        else -> 0f
                    }
                    if (delta != 0f) gridState.animateScrollBy(delta)
                } else {
                    gridState.animateScrollToItem(layoutIndex)
                }
            }
        }
    }
    LaunchedEffect(focusResetKey) {
        if (items.isEmpty()) return@LaunchedEffect
        focusedIndex = initialIndex
        lastFocusChangeMs = 0L
        isRapidScroll = false
        withFrameNanos { }
        runCatching { containerFocus.requestFocus() }
        repeat(FOCUS_RESET_RETRIES) {
            delay(FOCUS_RESET_RETRY_DELAY_MS)
            runCatching { focusRequesters[initialIndex]?.requestFocus() }
        }
    }

    fun moveFocus(delta: Int) {
        val next = (focusedIndex + delta).coerceIn(0, items.lastIndex)
        if (next == focusedIndex) return
        focusedIndex = next
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier
            .fillMaxSize()
            .focusRequester(containerFocus)
            .focusable()
            .gridDpadHandler(columns, ::moveFocus),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
        horizontalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        if (header != null) {
            item(span = { GridItemSpan(maxLineSpan) }) { header() }
        }
        itemsIndexed(
            items,
            key = itemKey?.let { keyFn -> { index, item -> keyFn(index, item) } }
        ) { index, item ->
            FocusableTileItem(
                index = index,
                item = item,
                focusRequesters = focusRequesters,
                focusedIndex = focusedIndex,
                onFocusedIndexChange = { focusedIndex = it },
                onItemFocused = onItemFocused,
                itemContent = itemContent
            )
        }
    }
}

@Composable
private fun <T> RowLayout(
    items: List<T>,
    modifier: Modifier,
    contentPadding: PaddingValues,
    itemSpacing: Dp,
    rowItemWidth: Dp,
    initialRealIndex: Int,
    focusResetKey: Any?,
    onItemFocused: (T?) -> Unit,
    header: (@Composable () -> Unit)?,
    itemContent: @Composable (Int, T, FocusRequester, Boolean, () -> Unit) -> Unit
) {
    val realCount = items.size
    val virtualCount = if (realCount == 0) 0 else INFINITE_ROW_VIRTUAL_COUNT
    // Centre of the virtual range, snapped to a real-index boundary. Adding the caller's
    // initialRealIndex puts the focused virtual slot on the requested real item.
    val midAligned = remember(realCount) {
        if (realCount == 0) 0
        else (virtualCount / 2).let { mid -> mid - (mid % realCount) }
    }
    val initialVirtualIndex = remember(midAligned, initialRealIndex) {
        midAligned + initialRealIndex
    }

    var focusedIndex by remember(initialVirtualIndex) { mutableIntStateOf(initialVirtualIndex) }
    val focusRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val containerFocus = remember { FocusRequester() }

    val rowState = rememberLazyListState(initialFirstVisibleItemIndex = initialVirtualIndex)
    val headerOffset = if (header != null) 1 else 0

    var lastFocusChangeMs by remember { mutableLongStateOf(0L) }
    var isRapidScroll by remember { mutableStateOf(false) }

    // Centre the focused tile horizontally on every focus change. Also covers the seed:
    // initialVirtualIndex change triggers this once on first composition.
    LaunchedEffect(focusedIndex) {
        val now = SystemClock.uptimeMillis()
        isRapidScroll = lastFocusChangeMs != 0L &&
                (now - lastFocusChangeMs) < RAPID_SCROLL_WINDOW_MS
        lastFocusChangeMs = now
        focusRequesters[focusedIndex]?.requestFocus()
        centerOnFocused(rowState, focusedIndex + headerOffset, snap = isRapidScroll)
    }
    LaunchedEffect(focusResetKey) {
        if (realCount == 0) return@LaunchedEffect
        focusedIndex = initialVirtualIndex
        lastFocusChangeMs = 0L
        isRapidScroll = false
        withFrameNanos { }
        runCatching { containerFocus.requestFocus() }
        repeat(FOCUS_RESET_RETRIES) {
            delay(FOCUS_RESET_RETRY_DELAY_MS)
            runCatching { focusRequesters[initialVirtualIndex]?.requestFocus() }
        }
        centerOnFocused(rowState, initialVirtualIndex + headerOffset)
    }

    fun moveFocus(delta: Int) {
        if (realCount == 0) return
        val next = (focusedIndex + delta).coerceIn(0, virtualCount - 1)
        if (next == focusedIndex) return
        focusedIndex = next
        // Scroll handled by LaunchedEffect(focusedIndex).
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sidePadding = ((maxWidth - rowItemWidth) / 2).coerceAtLeast(0.dp)
        LazyRow(
            state = rowState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(containerFocus)
                .focusable()
                .rowDpadHandler(::moveFocus),
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (header != null) {
                item { header() }
            }
            items(
                count = virtualCount,
                key = { virtualIndex -> virtualIndex + headerOffset }
            ) { virtualIndex ->
                val item = items[virtualIndex % realCount]
                val targetFalloff by remember(virtualIndex) {
                    derivedStateOf {
                        val distance = abs(virtualIndex - focusedIndex)
                        (1f - ROW_FALLOFF_STEP * distance).coerceAtLeast(ROW_FALLOFF_FLOOR)
                    }
                }
                val falloff by animateFloatAsState(
                    targetValue = targetFalloff,
                    animationSpec = if (isRapidScroll) snap() else tween(
                        durationMillis = FrontendTokens.Motion.FocusMs,
                        easing = FrontendTokens.Motion.Easing
                    ),
                    label = "rowFalloff"
                )
                Box(
                    modifier = Modifier
                        .width(rowItemWidth)
                        .graphicsLayer {
                            scaleX = falloff
                            scaleY = falloff
                        }
                ) {
                    FocusableTileItem(
                        index = virtualIndex,
                        item = item,
                        focusRequesters = focusRequesters,
                        focusedIndex = focusedIndex,
                        onFocusedIndexChange = { focusedIndex = it },
                        onItemFocused = onItemFocused,
                        itemContent = itemContent
                    )
                }
            }
        }
    }
}

private suspend fun centerOnFocused(
    rowState: androidx.compose.foundation.lazy.LazyListState,
    layoutIndex: Int,
    snap: Boolean = false
) {
    val info = rowState.layoutInfo
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    val target = info.visibleItemsInfo.firstOrNull { it.index == layoutIndex }
    if (target == null) {
        rowState.scrollToItem(layoutIndex)
        return
    }
    val itemCenter = target.offset + target.size / 2
    val delta = (itemCenter - viewportCenter).toFloat()
    if (abs(delta) < 1f) return
    if (snap) {
        rowState.scrollToItem(layoutIndex)
        return
    }
    rowState.animateScrollBy(
        value = delta,
        animationSpec = tween(
            durationMillis = FrontendTokens.Motion.FocusMs,
            easing = FrontendTokens.Motion.Easing
        )
    )
}

@Composable
private fun <T> FocusableTileItem(
    index: Int,
    item: T,
    focusRequesters: MutableMap<Int, FocusRequester>,
    focusedIndex: Int,
    onFocusedIndexChange: (Int) -> Unit,
    onItemFocused: (T?) -> Unit,
    itemContent: @Composable (Int, T, FocusRequester, Boolean, () -> Unit) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    DisposableEffect(index) {
        focusRequesters[index] = focusRequester
        onDispose { focusRequesters.remove(index) }
    }
    val isFocused = focusedIndex == index
    // Re-claim focus when this item gains focus state (covers the case where the focused
    // virtual slot lived off-screen at the time focusedIndex changed and the top-level
    // LaunchedEffect couldn't grab the requester yet). Keyed on isFocused so it only
    // re-launches for the 2 items whose focus state actually flipped.
    LaunchedEffect(index, isFocused) {
        if (isFocused) {
            runCatching { focusRequester.requestFocus() }
        }
    }
    val onFocused: () -> Unit = {
        if (focusedIndex != index) onFocusedIndexChange(index)
        onItemFocused(item)
    }
    itemContent(index, item, focusRequester, isFocused, onFocused)
}

private fun Modifier.gridDpadHandler(columns: Int, moveFocus: (Int) -> Unit): Modifier =
    onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> { moveFocus(1); true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { moveFocus(-1); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { moveFocus(columns); true }
            KeyEvent.KEYCODE_DPAD_UP -> { moveFocus(-columns); true }
            else -> false
        }
    }

private fun Modifier.rowDpadHandler(moveFocus: (Int) -> Unit): Modifier =
    onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        when (event.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> { moveFocus(1); true }
            KeyEvent.KEYCODE_DPAD_LEFT -> { moveFocus(-1); true }
            // Consume up/down so focus doesn't escape the row to other on-screen elements.
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> true
            else -> false
        }
    }
