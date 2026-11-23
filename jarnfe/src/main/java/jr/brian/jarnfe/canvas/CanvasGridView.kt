package jr.brian.jarnfe.canvas

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.core.ui.focus.jarnInteractive
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.GridRect

private val CanvasOuterPadding = 20.dp
private val CanvasCellSpacing = 8.dp
private val CanvasMaxCellSize = 96.dp
private val EditOutlineWidth = 1.dp
private val GrabOutlineWidth = 3.dp
private const val PUSH_AXIS_HEADROOM = 1
private const val GRABBED_Z_INDEX = 1f
private const val GHOST_Z_INDEX = 0.5f

internal const val CANVAS_MENU_FOCUS_ID = "jarnfe-canvas-menu"

/** Classic's cell layout, with outer padding that keeps a focused tile clear of the edges. */
private data class CanvasMetrics(val cell: Dp, val width: Dp, val height: Dp) {
    fun offsetOf(index: Int): Dp = CanvasOuterPadding + (cell + CanvasCellSpacing) * index

    fun extentOf(span: Int): Dp = cell * span + CanvasCellSpacing * (span - 1).coerceAtLeast(0)

    fun originPx(rect: GridRect, density: Density): IntOffset = with(density) {
        IntOffset(offsetOf(rect.col).roundToPx(), offsetOf(rect.row).roundToPx())
    }
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

/** Whole cells that fit in [viewportPx] along the scroll axis, inside the outer padding. */
internal fun canvasVisibleCells(viewportPx: Float, cellPx: Float, spacingPx: Float, paddingPx: Float): Int {
    val pitch = cellPx + spacingPx
    if (cellPx <= 0f || viewportPx <= 0f) return 0
    return ((viewportPx - paddingPx * 2 + spacingPx) / pitch).toInt().coerceAtLeast(0)
}

@Composable
internal fun CanvasGrid(
    page: JarnCanvasPage,
    editor: CanvasEditor,
    driver: CanvasTouchDriver,
    boardModifier: Modifier,
    onBoardGeometry: (CanvasBoardGeometry) -> Unit,
    focusRequesterFor: (String) -> FocusRequester,
    onFocused: (String) -> Unit,
    onCellSize: (Float) -> Unit,
    onVisibleCells: (Int) -> Unit,
    onTileClick: (CanvasItem, cellSizePx: Float) -> Unit,
    onMenuClick: () -> Unit,
    tileContent: @Composable BoxScope.(CanvasItem) -> Unit,
) {
    val layout = page.layout
    val placements = editor.placements(layout)
    val editing = editor.editing
    val grabbedId = editor.grab?.itemId
    val touchHoldsGrab = editor.grab?.driver == CanvasGrabDriver.Touch
    val resizing = editor.grab?.resizeCorner != null
    val visual = driver.visual
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
    val port = remember(scroll, direction) { CanvasScrollPort(scroll, direction == PushDirection.RIGHT) }
    SideEffect { driver.scrollPort = port }
    CanvasEdgeDriveEffect(driver, touchHoldsGrab && !resizing && driver.inEdgeZone)
    CanvasFolderDwellEffect(driver)
    LaunchedEffect(page.index) {
        val arrival = driver.takeArrivalDirection()
        if (arrival != 0) {
            withFrameNanos { }
            scroll.scrollTo(if (arrival < 0) scroll.maxValue else 0)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().onPlaced { driver.viewport = it }.then(scrollModifier)) {
        val metrics = canvasMetrics(direction, crossAxisCount, pushAxisCells, maxWidth, maxHeight)
        val density = LocalDensity.current
        val cellPx = with(density) { metrics.cell.toPx() }
        val visibleCells = with(density) {
            canvasVisibleCells(
                viewportPx = scroll.viewportSize.toFloat(),
                cellPx = cellPx,
                spacingPx = CanvasCellSpacing.toPx(),
                paddingPx = CanvasOuterPadding.toPx(),
            )
        }
        val boardGeometry = with(density) {
            CanvasBoardGeometry(cellPx, CanvasCellSpacing.toPx(), CanvasOuterPadding.toPx())
        }
        SideEffect {
            onCellSize(cellPx)
            onVisibleCells(visibleCells)
            onBoardGeometry(boardGeometry)
        }
        val floatingId = visual.tileId
        CanvasDragSettle(visual, driver.heldId, floatingId?.let(placements::get)?.let { metrics.originPx(it, density) })
        val ghostRect = grabbedId?.takeIf { touchHoldsGrab && !resizing }?.let(placements::get)
        val alignment = if (direction == PushDirection.DOWN) Alignment.TopCenter else Alignment.CenterStart
        Box(
            Modifier
                .align(alignment)
                .width(metrics.width)
                .height(metrics.height)
                .then(boardModifier),
        ) {
            editor.grab?.let { held ->
                CanvasGridCells(
                    board = boardGeometry,
                    columns = if (direction == PushDirection.DOWN) crossAxisCount else pushAxisCells,
                    rows = if (direction == PushDirection.DOWN) pushAxisCells else crossAxisCount,
                )
                held.home.rect.takeIf { !held.isAwayFromHome && it != held.rect }?.let { homeRect ->
                    CanvasSlot(homeRect, metrics, grabbed = false) { CanvasHomeGhost() }
                }
            }
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
            if (menuRect != null && editing) {
                CanvasSlot(menuRect, metrics, grabbed = false) { CanvasLockedCell() }
            }
            if (ghostRect != null) {
                CanvasSlot(ghostRect, metrics, grabbed = false, restingZ = GHOST_Z_INDEX) { CanvasDragGhost() }
            }
            layout.items.forEach { item ->
                val rect = placements[item.id] ?: return@forEach
                val held = item.id == grabbedId
                val grabbed = held && !resizing
                val floating = item.id == floatingId
                val outlined = held || floating
                key(item.id) {
                    CanvasSlot(
                        rect = rect,
                        metrics = metrics,
                        grabbed = grabbed,
                        followsGrab = grabbed && !touchHoldsGrab,
                        floating = floating,
                        visual = visual,
                        tileId = item.id,
                    ) {
                        CanvasTileFrame(
                            requester = focusRequesterFor(item.id),
                            editing = editing,
                            grabbed = outlined,
                            showCard = (item as? CanvasItem.EsdeArtItem)?.isCardTransparent != true,
                            folderDrop = driver.folderDropState(item.id),
                            growsAsDrop = item.id != grabbedId,
                            onFocused = { onFocused(item.id) },
                            onClick = { onTileClick(item, cellPx) },
                        ) { tileContent(item) }
                    }
                }
            }
            driver.handleTileId?.let(placements::get)?.let { rect ->
                CanvasResizeHandles(
                    rect = rect,
                    x = metrics.offsetOf(rect.col),
                    y = metrics.offsetOf(rect.row),
                    width = metrics.extentOf(rect.colSpan),
                    height = metrics.extentOf(rect.rowSpan),
                    showSize = resizing,
                )
            }
            editor.grab?.let { held ->
                CanvasMoveChip(
                    grab = held,
                    board = boardGeometry,
                    visual = visual,
                    boardWidth = with(density) { metrics.width.toPx() },
                    boardHeight = with(density) { metrics.height.toPx() },
                    showSize = driver.handleTileId == null,
                )
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
    followsGrab: Boolean = grabbed,
    floating: Boolean = false,
    visual: CanvasDragVisual? = null,
    tileId: String? = null,
    restingZ: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    val motion = JarnTheme.motion
    val tileShape = JarnTheme.tileShape
    val spec: AnimationSpec<Dp> = if (grabbed || floating) snap() else motion.small.spec()
    val width by animateDpAsState(metrics.extentOf(rect.colSpan), spec)
    val height by animateDpAsState(metrics.extentOf(rect.rowSpan), spec)
    val origin = metrics.originPx(rect, LocalDensity.current)
    val glide = rememberCanvasSlotGlide(origin.x, origin.y, glides = !grabbed && !floating && !motion.reduced)
    val grabLift = animateFloatAsState(if (grabbed) 1f else 0f, motion.small.spec())
    val bringIntoView = remember { BringIntoViewRequester() }
    LaunchedEffect(followsGrab, rect) {
        if (followsGrab) bringIntoView.bringIntoView()
    }
    Box(
        modifier = Modifier
            .zIndex(if (grabbed || floating) GRABBED_Z_INDEX else restingZ)
            .offset { origin }
            .size(width, height)
            .canvasSlotLayer(glide, visual, tileId, origin.x, origin.y, grabLift, motion.reduced, tileShape)
            .bringIntoViewRequester(bringIntoView),
        content = content,
    )
}

/** Not focusable or clickable, so neither the cursor nor a press can stop on it. */
@Composable
private fun CanvasLockedCell() {
    val shape = JarnTheme.tileShape
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .border(EditOutlineWidth, JarnTheme.colors.line, shape),
    ) { CanvasLockedCellContent() }
}

@Composable
private fun CanvasTileFrame(
    requester: FocusRequester,
    editing: Boolean,
    grabbed: Boolean,
    showCard: Boolean = true,
    folderDrop: CanvasFolderDropState? = null,
    growsAsDrop: Boolean = false,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = JarnTheme.colors
    val shape = JarnTheme.tileShape
    val source = remember { MutableInteractionSource() }
    val outline = when {
        folderDrop != null -> Modifier
        grabbed -> Modifier.border(GrabOutlineWidth, colors.accent, shape)
        editing -> Modifier.border(EditOutlineWidth, colors.line, shape)
        else -> Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .canvasFolderDropTarget(folderDrop, growsAsDrop)
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .jarnInteractive(shape, source, onClick = onClick)
            .clip(shape)
            .then(if (showCard) Modifier.background(colors.raised) else Modifier)
            .then(outline),
        content = content,
    )
}
