package jr.brian.home.canvas.ui

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.home.R
import jr.brian.home.canvas.grid.GridSolver
import jr.brian.home.canvas.grid.LayoutSnapshot
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.grid.toSnapshot
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasUiState
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlin.math.max
import kotlin.math.roundToInt

private val CanvasOuterPadding = 8.dp
private val CanvasCellSpacing = 8.dp
private val CanvasResizeHandleSize = 28.dp
private val CanvasDeleteHandleSize = 28.dp
private val CanvasDeleteHandleColor = Color(0xFFD32F2F)

/**
 * Upper bound on a single cell's edge length, applied to both orientations.
 * On wide viewports (landscape phone, tablet, TV) the per-cell size that
 * `(viewport - padding) / crossAxisCount` would produce balloons well past
 * what any tile actually needs — a 56dp icon ends up alone in a 400dp cell.
 * Capping here keeps tiles at a consistent visual size and the grid is
 * centered in the cross-axis when the cap leaves headroom.
 */
private val CanvasMaxCellSize = 96.dp

/** Extra cells of empty push-axis space rendered past the furthest item. */
private const val CanvasPushAxisHeadroom = 1

/**
 * Absolute-coordinate canvas grid with solver-driven move and resize gestures.
 *
 * Rendering — items live at their persisted (col, row, colSpan, rowSpan).
 * The cross axis is bounded; the push axis (down for vertical scroll, right
 * for horizontal) is unbounded and is what the user scrolls.
 *
 * Move — in edit mode, long-press lifts a tile, captures the *committed
 * baseline* snapshot, and on each cell-boundary crossing re-solves via
 * [GridSolver.solveMove]. The dragged tile follows the finger directly (snap);
 * non-dragged tiles glide to their preview cells with a spring. On release
 * the preview is committed atomically via [onCommitLayout].
 *
 * Resize — in edit mode, the bottom-right corner handle drags to grow/shrink
 * the tile. Same baseline-frozen, cell-snapped re-solve via
 * [GridSolver.solveResize]; widget-derived min spans clamp the lower bound.
 * Tapping the handle (also focus-navigable) opens [CanvasResizeDialog] for
 * D-pad/gamepad accessibility via [onRequestResizeDialog].
 *
 * Reuses [CanvasItemTile] for every variant. Items are added from the top-bar
 * `+` button (see [jr.brian.home.canvas.ui.UnifiedCanvasTab]); no inline add
 * tile is rendered in the grid.
 */
@Composable
fun CanvasGridLayout(
    state: CanvasUiState,
    onTap: (ResolvedCanvasItem) -> Unit,
    onLongPress: (ResolvedCanvasItem) -> Unit,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit,
    onCommitLayout: (LayoutSnapshot) -> Unit,
    onRequestResizeDialog: (item: CanvasItem, minColSpan: Int, minRowSpan: Int) -> Unit,
    onAddClick: () -> Unit,
    onAddLongClick: () -> Unit,
    onDeleteClick: (ResolvedCanvasItem) -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null,
    scrollState: androidx.compose.foundation.ScrollState = rememberScrollState(),
    onDoubleTap: (ResolvedCanvasItem) -> Unit = {}
) {
    val pushDirection = PushDirection.from(state.layout.activeOrientation)
    val crossAxisCount = state.layout.activeCrossAxis.coerceAtLeast(1)
    val activeArrangement = state.layout.activeArrangement
    val editMode = state.layout.editMode
    val reservedRects = reservedRectsForActive(state.layout)
    val addTileRect = reservedRects.firstOrNull()
    val scrollModifier = when (pushDirection) {
        PushDirection.DOWN -> Modifier.verticalScroll(scrollState)
        PushDirection.RIGHT -> Modifier.horizontalScroll(scrollState)
    }

    var gestureState by remember { mutableStateOf<CanvasGestureState?>(null) }
    val activeGesture = gestureState
    // Live per-frame pointer offset for the dragged tile. Kept OUT of
    // [gestureState] so 60fps pointer events don't trigger a full
    // CanvasGridLayout recomposition — only the dragged slot, which reads
    // `.value` inside its own scope, recomposes.
    val pointerOffsetState = remember { mutableStateOf(IntOffset.Zero) }
    val latestState = rememberUpdatedState(state)
    val latestCommit = rememberUpdatedState(onCommitLayout)
    val context = LocalContext.current
    val widgetManager = remember(context) { AppWidgetManager.getInstance(context) }

    val tileRects = state.resolvedItems.associate { resolved ->
        val id = resolved.raw.id
        val baselineRect = activeArrangement[id] ?: FallbackRect
        val rect = when {
            activeGesture == null -> baselineRect
            activeGesture.mode == CanvasGestureState.Mode.Move && id == activeGesture.draggedId ->
                activeGesture.baselineRect
            else ->
                activeGesture.previewSnapshot.placements[id] ?: baselineRect
        }
        id to rect
    }
    val itemRects: Collection<GridRect> =
        activeGesture?.previewSnapshot?.placements?.values ?: activeArrangement.values
    val layoutAnchorRects: Collection<GridRect> = itemRects + reservedRects
    val pushAxisCells =
        (layoutAnchorRects.maxOfOrNull { it.pushEnd(pushDirection) } ?: 0) +
            CanvasPushAxisHeadroom

    BoxWithConstraints(modifier = modifier.then(scrollModifier)) {
        val crossViewport: Dp = when (pushDirection) {
            PushDirection.DOWN -> maxWidth
            PushDirection.RIGHT -> maxHeight
        }
        val fittedCellSize: Dp = ((crossViewport - CanvasOuterPadding * 2
            - CanvasCellSpacing * (crossAxisCount - 1)) / crossAxisCount)
            .coerceAtLeast(0.dp)
        val cellSize: Dp = fittedCellSize.coerceAtMost(CanvasMaxCellSize)
        val crossContentExtent: Dp = CanvasOuterPadding * 2 +
            cellSize * crossAxisCount +
            CanvasCellSpacing * (crossAxisCount - 1).coerceAtLeast(0)
        val pushExtent: Dp = CanvasOuterPadding * 2 +
            cellSize * pushAxisCells +
            CanvasCellSpacing * (pushAxisCells - 1).coerceAtLeast(0)
        val canvasSizeModifier = when (pushDirection) {
            PushDirection.DOWN -> Modifier.width(crossContentExtent).height(pushExtent)
            PushDirection.RIGHT -> Modifier.height(crossContentExtent).width(pushExtent)
        }
        val canvasAlignment = when (pushDirection) {
            PushDirection.DOWN -> Alignment.TopCenter
            PushDirection.RIGHT -> Alignment.CenterStart
        }

        val density = LocalDensity.current
        val cellPlusSpacingPxState = rememberUpdatedState(
            with(density) { (cellSize + CanvasCellSpacing).toPx() }
        )
        val cellSizePxState = rememberUpdatedState(with(density) { cellSize.toPx() })

        Box(
            modifier = canvasSizeModifier
                .align(canvasAlignment)
                .pointerInput(onAddClick) {
                    detectTapGestures(onLongPress = { onAddClick() })
                }
        ) {
            if (activeGesture != null) {
                val ghostRect = activeGesture.previewSnapshot.placements[activeGesture.draggedId]
                if (ghostRect != null) {
                    CanvasTileSlot(
                        rect = ghostRect,
                        cellSize = cellSize,
                        spacing = CanvasCellSpacing,
                        outerPadding = CanvasOuterPadding,
                        animationLabel = "__canvas_drop_ghost__"
                    ) {
                        DropTargetGhost()
                    }
                }
            }

            if (addTileRect != null) {
                CanvasTileSlot(
                    rect = addTileRect,
                    cellSize = cellSize,
                    spacing = CanvasCellSpacing,
                    outerPadding = CanvasOuterPadding,
                    animationLabel = "__canvas_add_tile__"
                ) {
                    AddTile(onClick = onAddClick, onLongClick = onAddLongClick)
                }
            }

            state.resolvedItems.forEach { resolved ->
                key(resolved.raw.id) {
                    val itemId = resolved.raw.id
                    val rect = tileRects[itemId] ?: FallbackRect
                    val isDragging = activeGesture != null && activeGesture.draggedId == itemId
                    val isMoveDragging =
                        isDragging && activeGesture.mode == CanvasGestureState.Mode.Move

                    val moveDragModifier: Modifier = if (editMode) {
                        Modifier.pointerInput(itemId) {
                            handleMoveDrag(
                                itemId = itemId,
                                latestState = latestState,
                                cellPlusSpacingPxState = cellPlusSpacingPxState,
                                pointerOffsetState = pointerOffsetState,
                                setGestureState = { gestureState = it },
                                getGestureState = { gestureState },
                                commit = { snapshot -> latestCommit.value(snapshot) }
                            )
                        }
                    } else Modifier

                    val editHandlesOverlay: (@Composable BoxScope.() -> Unit)? = if (editMode) {
                        {
                            CanvasDeleteHandle(
                                modifier = Modifier.align(Alignment.TopEnd),
                                onTap = { onDeleteClick(resolved) }
                            )
                            CanvasResizeHandle(
                                modifier = Modifier.align(Alignment.BottomEnd),
                                onTap = {
                                    val (minC, minR) = minSpansFor(
                                        resolved.raw,
                                        widgetManager,
                                        cellSizePxState.value
                                    )
                                    onRequestResizeDialog(resolved.raw, minC, minR)
                                },
                                dragModifier = Modifier.pointerInput(itemId) {
                                    handleResizeDrag(
                                        itemId = itemId,
                                        latestState = latestState,
                                        cellPlusSpacingPxState = cellPlusSpacingPxState,
                                        resolveMinSpans = {
                                            minSpansFor(
                                                resolved.raw,
                                                widgetManager,
                                                cellSizePxState.value
                                            )
                                        },
                                        setGestureState = { gestureState = it },
                                        getGestureState = { gestureState },
                                        commit = { snapshot -> latestCommit.value(snapshot) }
                                    )
                                }
                            )
                        }
                    } else null

                    CanvasTileSlot(
                        rect = rect,
                        cellSize = cellSize,
                        spacing = CanvasCellSpacing,
                        outerPadding = CanvasOuterPadding,
                        animationLabel = itemId,
                        pointerOffsetState = pointerOffsetState,
                        trackPointer = isMoveDragging,
                        isDragging = isDragging,
                        modifier = moveDragModifier,
                        overlay = editHandlesOverlay
                    ) {
                        CanvasItemTile(
                            resolved = resolved,
                            onTap = { onTap(resolved) },
                            onLongPress = { onLongPress(resolved) },
                            onDoubleTap = { onDoubleTap(resolved) },
                            appWidgetHost = appWidgetHost,
                            suppressTileLongPress = editMode,
                            editMode = editMode,
                            onResizeWidget = onResizeWidget
                        )
                    }
                }
            }

        }
    }
}

private suspend fun PointerInputScope.handleMoveDrag(
    itemId: String,
    latestState: androidx.compose.runtime.State<CanvasUiState>,
    cellPlusSpacingPxState: androidx.compose.runtime.State<Float>,
    pointerOffsetState: androidx.compose.runtime.MutableState<IntOffset>,
    setGestureState: (CanvasGestureState?) -> Unit,
    getGestureState: () -> CanvasGestureState?,
    commit: (LayoutSnapshot) -> Unit
) {
    var rawOffset = Offset.Zero
    detectDragGesturesAfterLongPress(
        onDragStart = {
            val baseline = latestState.value.layout.toSnapshot()
            val baseRect = baseline.placements[itemId] ?: return@detectDragGesturesAfterLongPress
            rawOffset = Offset.Zero
            pointerOffsetState.value = IntOffset.Zero
            setGestureState(
                CanvasGestureState(
                    mode = CanvasGestureState.Mode.Move,
                    draggedId = itemId,
                    baseline = baseline,
                    baselineRect = baseRect
                )
            )
        },
        onDrag = { change, dragAmount ->
            change.consume()
            rawOffset += dragAmount
            val current = getGestureState() ?: return@detectDragGesturesAfterLongPress
            val cellPx = cellPlusSpacingPxState.value
            val cellDeltaCol = if (cellPx > 0f) (rawOffset.x / cellPx).roundToInt() else 0
            val cellDeltaRow = if (cellPx > 0f) (rawOffset.y / cellPx).roundToInt() else 0
            val target = GridCell(
                current.baselineRect.col + cellDeltaCol,
                current.baselineRect.row + cellDeltaRow
            )
            // Live per-frame pointer offset — separate State<> so this update
            // doesn't churn gestureState (which would recompose the whole grid).
            pointerOffsetState.value =
                IntOffset(rawOffset.x.roundToInt(), rawOffset.y.roundToInt())
            val targetRect = current.baselineRect.withOrigin(target.col, target.row)
            // Solver re-run only on cell-boundary crossings, per §0.
            if (targetRect != current.targetRect) {
                val result = GridSolver.solveMove(
                    baseline = current.baseline,
                    itemId = itemId,
                    target = target,
                    reservedRects = reservedRectsForActive(latestState.value.layout)
                )
                setGestureState(
                    current.copy(previewSnapshot = result.snapshot, targetRect = targetRect)
                )
            }
        },
        onDragEnd = {
            val current = getGestureState()
            if (current != null) commit(current.previewSnapshot)
            pointerOffsetState.value = IntOffset.Zero
            setGestureState(null)
        },
        onDragCancel = {
            pointerOffsetState.value = IntOffset.Zero
            setGestureState(null)
        }
    )
}

private suspend fun PointerInputScope.handleResizeDrag(
    itemId: String,
    latestState: androidx.compose.runtime.State<CanvasUiState>,
    cellPlusSpacingPxState: androidx.compose.runtime.State<Float>,
    resolveMinSpans: () -> Pair<Int, Int>,
    setGestureState: (CanvasGestureState?) -> Unit,
    getGestureState: () -> CanvasGestureState?,
    commit: (LayoutSnapshot) -> Unit
) {
    var rawOffset = Offset.Zero
    detectDragGestures(
        onDragStart = {
            val baseline = latestState.value.layout.toSnapshot()
            val baseRect = baseline.placements[itemId] ?: return@detectDragGestures
            val (minC, minR) = resolveMinSpans()
            rawOffset = Offset.Zero
            setGestureState(
                CanvasGestureState(
                    mode = CanvasGestureState.Mode.Resize,
                    draggedId = itemId,
                    baseline = baseline,
                    baselineRect = baseRect,
                    minColSpan = minC,
                    minRowSpan = minR
                )
            )
        },
        onDrag = { change, dragAmount ->
            change.consume()
            rawOffset += dragAmount
            val current = getGestureState() ?: return@detectDragGestures
            val cellPx = cellPlusSpacingPxState.value
            val deltaCol = if (cellPx > 0f) (rawOffset.x / cellPx).roundToInt() else 0
            val deltaRow = if (cellPx > 0f) (rawOffset.y / cellPx).roundToInt() else 0
            val newColSpan =
                (current.baselineRect.colSpan + deltaCol).coerceAtLeast(current.minColSpan)
            val newRowSpan =
                (current.baselineRect.rowSpan + deltaRow).coerceAtLeast(current.minRowSpan)
            val newRect = current.baselineRect.withSize(newColSpan, newRowSpan)
            setGestureState(
                if (newRect != current.targetRect) {
                    val result = GridSolver.solveResize(
                        baseline = current.baseline,
                        itemId = itemId,
                        newRect = newRect,
                        minColSpan = current.minColSpan,
                        minRowSpan = current.minRowSpan,
                        reservedRects = reservedRectsForActive(latestState.value.layout)
                    )
                    current.copy(
                        previewSnapshot = result.snapshot,
                        targetRect = newRect
                    )
                } else current
            )
        },
        onDragEnd = {
            val current = getGestureState()
            if (current != null) commit(current.previewSnapshot)
            setGestureState(null)
        },
        onDragCancel = { setGestureState(null) }
    )
}

@Composable
private fun CanvasResizeHandle(
    onTap: () -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(CanvasResizeHandleSize)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(topStart = 10.dp))
            .background(ThemePrimaryColor.copy(alpha = if (isFocused) 0.95f else 0.85f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = Color.White.copy(alpha = if (isFocused) 0.9f else 0.5f),
                shape = RoundedCornerShape(topStart = 10.dp)
            )
            .focusable()
            .clickable { onTap() }
            .then(dragModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.OpenInFull,
            contentDescription = stringResource(R.string.canvas_resize_handle_description),
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
    }
}

/**
 * Top-right delete handle. Mirrors [CanvasResizeHandle] in size, focus behavior,
 * and D-pad reachability so removal and resize feel like a matched pair. Rendered
 * only in edit mode by the caller; its hit area is isolated on the tile chrome so
 * a tap here doesn't fall through to the tile's own move/tap/long-press handlers.
 */
@Composable
private fun CanvasDeleteHandle(
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(CanvasDeleteHandleSize)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(bottomStart = 10.dp))
            .background(CanvasDeleteHandleColor.copy(alpha = if (isFocused) 0.95f else 0.85f))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = Color.White.copy(alpha = if (isFocused) 0.9f else 0.5f),
                shape = RoundedCornerShape(bottomStart = 10.dp)
            )
            .focusable()
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.canvas_delete_handle_description),
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Sizes a tile to its (colSpan, rowSpan) and animates its top-left toward
 * the target cell with a spring. The optional pointer offset is layered on
 * top — when [trackPointer] is true the slot subscribes to
 * [pointerOffsetState] and tracks the finger directly (snap during drag,
 * spring back on release); when false the read is short-circuited so
 * non-dragged tiles never subscribe to per-frame pointer updates.
 */
@Composable
private fun CanvasTileSlot(
    rect: GridRect,
    cellSize: Dp,
    spacing: Dp,
    outerPadding: Dp,
    animationLabel: String,
    modifier: Modifier = Modifier,
    pointerOffsetState: androidx.compose.runtime.State<IntOffset>? = null,
    trackPointer: Boolean = false,
    isDragging: Boolean = false,
    overlay: (@Composable BoxScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val width = cellSize * rect.colSpan + spacing * (rect.colSpan - 1).coerceAtLeast(0)
    val height = cellSize * rect.rowSpan + spacing * (rect.rowSpan - 1).coerceAtLeast(0)
    val targetPx = with(LocalDensity.current) {
        IntOffset(
            (outerPadding + (cellSize + spacing) * rect.col).roundToPx(),
            (outerPadding + (cellSize + spacing) * rect.row).roundToPx()
        )
    }
    val animatedTarget by animateIntOffsetAsState(
        targetValue = targetPx,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 350f),
        label = "$animationLabel-cell"
    )
    // Short-circuit the State<> read for non-dragged tiles so they never
    // subscribe to per-frame pointer changes.
    val pointerTarget =
        if (trackPointer && pointerOffsetState != null) pointerOffsetState.value
        else IntOffset.Zero
    val animatedPointer by animateIntOffsetAsState(
        targetValue = pointerTarget,
        animationSpec = if (isDragging) snap() else spring(dampingRatio = 0.85f, stiffness = 450f),
        label = "$animationLabel-ptr"
    )
    val dragVisualModifier = if (isDragging) {
        Modifier
            .graphicsLayer {
                scaleX = 1.05f
                scaleY = 1.05f
                alpha = 0.92f
            }
            .zIndex(1f)
    } else Modifier
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .absoluteOffset { animatedTarget + animatedPointer }
            .then(dragVisualModifier)
            .then(modifier)
    ) {
        content()
        overlay?.invoke(this)
    }
}

/**
 * Inline "+" tile rendered at the orientation's reserved cell. Scrolls with the
 * grid like any other occupant. Pure click target — no drag, no resize, no
 * persisted entry in [CanvasUiState.resolvedItems]. When this cell scrolls
 * off-screen the long-press-empty-canvas gesture still opens the add dialog.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AddTile(onClick: () -> Unit, onLongClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .background(ThemePrimaryColor)
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                color = ThemePrimaryColor.copy(alpha = if (isFocused) 0.95f else 0.55f),
                shape = RoundedCornerShape(14.dp)
            )
            .focusable()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = stringResource(R.string.canvas_menu),
            tint = Color.White
        )
    }
}

@Composable
private fun DropTargetGhost() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(ThemePrimaryColor.copy(alpha = 0.18f))
            .border(3.dp, ThemePrimaryColor, RoundedCornerShape(14.dp))
    )
}

private val FallbackRect = GridRect(0, 0, 1, 1)

private fun GridRect.pushEnd(dir: PushDirection): Int = when (dir) {
    PushDirection.DOWN -> bottom
    PushDirection.RIGHT -> right
}

/**
 * Minimum cell spans the [item] can be resized to. Widgets translate their
 * AppWidgetProviderInfo min sizes into cells (ceiling-divide by cellSizePx);
 * other variants are 1×1. Returns (1, 1) if [cellSizePx] is non-positive.
 */
private fun minSpansFor(
    item: CanvasItem,
    widgetManager: AppWidgetManager,
    cellSizePx: Float
): Pair<Int, Int> {
    if (item !is CanvasItem.WidgetItem) return 1 to 1
    if (cellSizePx <= 0f) return 1 to 1
    val info: AppWidgetProviderInfo = widgetManager.getAppWidgetInfo(item.widgetId)
        ?: return 1 to 1
    val minW = max(info.minResizeWidth, info.minWidth)
    val minH = max(info.minResizeHeight, info.minHeight)
    val minColSpan = if (minW > 0) ((minW + cellSizePx - 1) / cellSizePx).toInt() else 1
    val minRowSpan = if (minH > 0) ((minH + cellSizePx - 1) / cellSizePx).toInt() else 1
    return minColSpan.coerceAtLeast(1) to minRowSpan.coerceAtLeast(1)
}

