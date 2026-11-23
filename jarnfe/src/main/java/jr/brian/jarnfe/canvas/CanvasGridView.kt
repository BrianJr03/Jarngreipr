package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.GridRect

private val CanvasOuterPadding = 12.dp
private val CanvasCellSpacing = 8.dp
private val CanvasMaxCellSize = 96.dp
private val EditOutlineWidth = 1.dp
private val GrabOutlineWidth = 3.dp
private const val PUSH_AXIS_HEADROOM = 1
private const val GRABBED_Z_INDEX = 1f

internal const val CANVAS_MENU_FOCUS_ID = "jarnfe-canvas-menu"

/** The same sizing Classic uses, so a saved page looks the same in both experiences. */
private data class CanvasMetrics(val cell: Dp, val width: Dp, val height: Dp) {
    fun offsetOf(index: Int): Dp = CanvasOuterPadding + (cell + CanvasCellSpacing) * index

    fun extentOf(span: Int): Dp = cell * span + CanvasCellSpacing * (span - 1).coerceAtLeast(0)
}

private fun canvasMetrics(
    direction: PushDirection,
    crossAxisCount: Int,
    pushAxisCells: Int,
    viewportWidth: Dp,
    viewportHeight: Dp,
): CanvasMetrics {
    val viewport = if (direction == PushDirection.DOWN) viewportWidth else viewportHeight
    val fitted = ((viewport - CanvasOuterPadding * 2 - CanvasCellSpacing * (crossAxisCount - 1)) / crossAxisCount)
        .coerceAtLeast(0.dp)
    val cell = fitted.coerceAtMost(CanvasMaxCellSize)
    val cross = CanvasOuterPadding * 2 + cell * crossAxisCount +
        CanvasCellSpacing * (crossAxisCount - 1).coerceAtLeast(0)
    val push = CanvasOuterPadding * 2 + cell * pushAxisCells +
        CanvasCellSpacing * (pushAxisCells - 1).coerceAtLeast(0)
    return if (direction == PushDirection.DOWN) {
        CanvasMetrics(cell, cross, push)
    } else {
        CanvasMetrics(cell, push, cross)
    }
}

private fun GridRect.pushEnd(direction: PushDirection): Int =
    if (direction == PushDirection.DOWN) bottom else right

@Composable
internal fun CanvasGrid(
    page: JarnCanvasPage,
    placements: Map<String, GridRect>,
    editing: Boolean,
    grabbedId: String?,
    focusRequesterFor: (String) -> FocusRequester,
    onFocused: (String) -> Unit,
    onTileClick: (CanvasItem, cellSizePx: Float) -> Unit,
    onMenuClick: () -> Unit,
    tileContent: @Composable BoxScope.(CanvasItem) -> Unit,
) {
    val layout = page.layout
    val direction = PushDirection.from(layout.activeOrientation)
    val crossAxisCount = layout.activeCrossAxis.coerceAtLeast(1)
    val menuRect = reservedRectsForActive(layout).firstOrNull()
    val anchors = placements.values + listOfNotNull(menuRect)
    val pushAxisCells = (anchors.maxOfOrNull { it.pushEnd(direction) } ?: 0) + PUSH_AXIS_HEADROOM
    val scroll = rememberScrollState()
    val scrollModifier = when (direction) {
        PushDirection.DOWN -> Modifier.verticalScroll(scroll)
        PushDirection.RIGHT -> Modifier.horizontalScroll(scroll)
    }
    BoxWithConstraints(Modifier.fillMaxSize().then(scrollModifier)) {
        val metrics = canvasMetrics(direction, crossAxisCount, pushAxisCells, maxWidth, maxHeight)
        val cellPx = with(LocalDensity.current) { metrics.cell.toPx() }
        val alignment = if (direction == PushDirection.DOWN) Alignment.TopCenter else Alignment.CenterStart
        Box(
            Modifier
                .align(alignment)
                .width(metrics.width)
                .height(metrics.height),
        ) {
            if (menuRect != null && !editing) {
                CanvasSlot(menuRect, metrics, grabbed = false) {
                    CanvasTileFrame(
                        requester = focusRequesterFor(CANVAS_MENU_FOCUS_ID),
                        editing = false,
                        grabbed = false,
                        onFocused = { onFocused(CANVAS_MENU_FOCUS_ID) },
                        onClick = onMenuClick,
                    ) { CanvasMenuTileContent() }
                }
            }
            layout.items.forEach { item ->
                val rect = placements[item.id] ?: return@forEach
                val grabbed = item.id == grabbedId
                key(item.id) {
                    CanvasSlot(rect, metrics, grabbed) {
                        CanvasTileFrame(
                            requester = focusRequesterFor(item.id),
                            editing = editing,
                            grabbed = grabbed,
                            onFocused = { onFocused(item.id) },
                            onClick = { onTileClick(item, cellPx) },
                        ) { tileContent(item) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CanvasSlot(
    rect: GridRect,
    metrics: CanvasMetrics,
    grabbed: Boolean,
    content: @Composable BoxScope.() -> Unit,
) {
    val glide = JarnTheme.motion.small.spec<Dp>()
    val spec: AnimationSpec<Dp> = if (grabbed) snap() else glide
    val x by animateDpAsState(metrics.offsetOf(rect.col), spec)
    val y by animateDpAsState(metrics.offsetOf(rect.row), spec)
    val width by animateDpAsState(metrics.extentOf(rect.colSpan), spec)
    val height by animateDpAsState(metrics.extentOf(rect.rowSpan), spec)
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(grabbed, rect) {
        if (grabbed) bringIntoView.bringIntoView()
    }
    Box(
        modifier = Modifier
            .zIndex(if (grabbed) GRABBED_Z_INDEX else 0f)
            .offset(x, y)
            .size(width, height)
            .bringIntoViewRequester(bringIntoView),
        content = content,
    )
}

@Composable
private fun CanvasTileFrame(
    requester: FocusRequester,
    editing: Boolean,
    grabbed: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.tileShape
    val source = remember { MutableInteractionSource() }
    val outline = when {
        grabbed -> Modifier.border(GrabOutlineWidth, colors.accent, shape)
        editing -> Modifier.border(EditOutlineWidth, colors.line, shape)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .jarnInteractive(shape, source, onClick = onClick)
            .clip(shape)
            .background(colors.raised)
            .then(outline),
        content = content,
    )
}
