package jr.brian.jarnfe.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import jr.brian.core.ui.focus.JarnTabsFocus
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.jarnfe.splash.jarnSplashTile
import kotlinx.coroutines.flow.first

private const val HEADER_KEY = "library_header"
private const val ITEM_KEY_PREFIX = "item:"
private const val SKELETON_TILE_COUNT = 12
private const val FIRST_ROW_BELOW_HEADER = 1

/** Wider than a focused cover's border can reach sideways. */
internal val LibraryGridColumnSpacing = 12.dp

/**
 * A lazy grid that puts focus back on [focusedKey] whenever it enters composition, and again if
 * the focused item disappears in a refresh. Right after a Library tab switch it focuses its first
 * item instead, once the tab has slid open.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T : Any> LibraryFocusGrid(
    items: List<T>,
    itemKey: (T) -> String,
    focusedKey: String?,
    onFocused: (String) -> Unit,
    cellMinSize: Dp,
    modifier: Modifier = Modifier,
    refocusSignal: Int = 0,
    rowSpacing: Dp = JarnTheme.space.md,
    header: (@Composable () -> Unit)? = null,
    itemContent: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val requester = remember { FocusRequester() }
    var lastFocused by remember { mutableStateOf<String?>(null) }
    val keys = remember(items) { items.mapTo(HashSet(), itemKey) }
    val focusLost = lastFocused?.let { it !in keys } ?: false
    val gate = LocalLibraryRevealGate.current
    val target = remember(items.isEmpty(), focusLost, refocusSignal) {
        focusedKey.takeUnless { gate.preferFirst && lastFocused == null } ?: items.firstOrNull()?.let(itemKey)
    }
    val headerCount = if (header != null) 1 else 0
    var headerHeight by remember { mutableIntStateOf(0) }
    val spacingPx = with(LocalDensity.current) { JarnTheme.space.md.roundToPx() }
    val baseSpec = LocalBringIntoViewSpec.current
    val revealSpec = remember(gridState, baseSpec, spacingPx) {
        HeaderRevealSpec(gridState, baseSpec) { if (headerHeight > 0) headerHeight + spacingPx else 0 }
    }

    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(target, refocusSignal) {
        val index = items.indexOfFirst { itemKey(it) == target }
        if (index >= 0) gridState.restoreFocus(index + headerCount, requester, tabsFocus, gate)
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides if (header != null) revealSpec else baseSpec) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(cellMinSize),
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = libraryGridPadding(),
            horizontalArrangement = Arrangement.spacedBy(LibraryGridColumnSpacing),
            verticalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            header?.let { content ->
                item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = HEADER_KEY) {
                    Box(Modifier.jarnSplashTile(0).onSizeChanged { headerHeight = it.height }) { content() }
                }
            }
            itemsIndexed(
                items,
                key = { _, item -> ITEM_KEY_PREFIX + itemKey(item) },
                contentType = { _, _ -> ITEM_KEY_PREFIX },
            ) { index, item ->
                val id = itemKey(item)
                val restore = if (id == target) Modifier.focusRequester(requester) else Modifier
                itemContent(
                    item,
                    Modifier.jarnSplashTile(index + 1).then(restore).onFocusChanged { focus ->
                        if (focus.isFocused) {
                            lastFocused = id
                            onFocused(id)
                        }
                    },
                )
            }
        }
    }
}

/**
 * The header can't take focus, so a focused tile in the row below it scrolls far enough to show
 * the whole header. Every other row keeps [base]'s behavior.
 */
@OptIn(ExperimentalFoundationApi::class)
private class HeaderRevealSpec(
    private val grid: LazyGridState,
    private val base: BringIntoViewSpec,
    private val headerExtent: () -> Int,
) : BringIntoViewSpec by base {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val layout = grid.layoutInfo
        val firstRow = layout.visibleItemsInfo.firstOrNull { it.row == FIRST_ROW_BELOW_HEADER }
        val extent = headerExtent()
        if (firstRow == null || extent == 0) return base.calculateScrollDistance(offset, size, containerSize)
        val rowStart = (firstRow.offset.y - layout.viewportStartOffset).toFloat()
        val rowEnd = rowStart + firstRow.size.height
        val focusCenter = offset + size / 2
        if (focusCenter !in rowStart..rowEnd) return base.calculateScrollDistance(offset, size, containerSize)
        return (firstRow.offset.y - extent).toFloat()
    }
}

private suspend fun LazyGridState.restoreFocus(
    index: Int,
    requester: FocusRequester,
    tabsFocus: JarnTabsFocus,
    gate: LibraryRevealGate,
) {
    snapshotFlow { layoutInfo.totalItemsCount }.first { it > index }
    if (tabsFocus.hasFocus) return
    if (layoutInfo.visibleItemsInfo.none { it.index == index }) scrollToItem(index)
    snapshotFlow { layoutInfo.visibleItemsInfo.any { it.index == index } }.first { it }
    withFrameNanos { }
    gate.whenOpen { !tabsFocus.hasFocus && requester.requestFocus() }
}

@Composable
private fun libraryGridPadding() = PaddingValues(
    horizontal = JarnTheme.space.stageMargin,
    vertical = JarnTheme.space.focusEdge,
)

@Composable
internal fun LibrarySkeletonGrid(cellMinSize: Dp, tileRatio: Float, modifier: Modifier = Modifier) {
    val shape = JarnTheme.radius.mdShape
    val color = JarnTheme.colors.surface
    LazyVerticalGrid(
        columns = GridCells.Adaptive(cellMinSize),
        modifier = modifier.fillMaxSize(),
        userScrollEnabled = false,
        contentPadding = libraryGridPadding(),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
        verticalArrangement = Arrangement.spacedBy(JarnTheme.space.md),
    ) {
        items(SKELETON_TILE_COUNT) {
            Box(Modifier.aspectRatio(tileRatio).background(color, shape))
        }
    }
}
