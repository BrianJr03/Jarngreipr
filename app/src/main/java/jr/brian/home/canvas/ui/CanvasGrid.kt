package jr.brian.home.canvas.ui

import android.appwidget.AppWidgetHost
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.CanvasUiState
import jr.brian.home.canvas.model.ResolvedCanvasItem

/** Drag distance (in raw px) below which the gesture is treated as a long-press, not a drag. */
private const val DRAG_THRESHOLD_PX = 24f

@Composable
fun CanvasGrid(
    state: CanvasUiState,
    onTap: (ResolvedCanvasItem) -> Unit,
    onLongPress: (ResolvedCanvasItem) -> Unit,
    onAddItem: () -> Unit,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null
) {
    val gridState = rememberLazyGridState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoverTargetIndex by remember { mutableStateOf<Int?>(null) }
    val editMode = state.layout.editMode

    when (state.layout.orientation) {
        CanvasScrollOrientation.VERTICAL -> CanvasVerticalGrid(
            state = state,
            gridState = gridState,
            onTap = onTap,
            onLongPress = onLongPress,
            onAddItem = onAddItem,
            onReorder = onReorder,
            draggingIndex = draggingIndex,
            setDraggingIndex = { draggingIndex = it },
            dragOffset = dragOffset,
            setDragOffset = { dragOffset = it },
            hoverTargetIndex = hoverTargetIndex,
            setHoverTargetIndex = { hoverTargetIndex = it },
            modifier = modifier,
            appWidgetHost = appWidgetHost,
            editMode = editMode,
            onResizeWidget = onResizeWidget
        )
        CanvasScrollOrientation.HORIZONTAL -> CanvasHorizontalGrid(
            state = state,
            gridState = gridState,
            onTap = onTap,
            onLongPress = onLongPress,
            onAddItem = onAddItem,
            onReorder = onReorder,
            draggingIndex = draggingIndex,
            setDraggingIndex = { draggingIndex = it },
            dragOffset = dragOffset,
            setDragOffset = { dragOffset = it },
            hoverTargetIndex = hoverTargetIndex,
            setHoverTargetIndex = { hoverTargetIndex = it },
            modifier = modifier,
            appWidgetHost = appWidgetHost,
            editMode = editMode,
            onResizeWidget = onResizeWidget
        )
    }
}

@Composable
private fun CanvasVerticalGrid(
    state: CanvasUiState,
    gridState: LazyGridState,
    onTap: (ResolvedCanvasItem) -> Unit,
    onLongPress: (ResolvedCanvasItem) -> Unit,
    onAddItem: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    draggingIndex: Int?,
    setDraggingIndex: (Int?) -> Unit,
    dragOffset: Offset,
    setDragOffset: (Offset) -> Unit,
    hoverTargetIndex: Int?,
    setHoverTargetIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null,
    editMode: Boolean = false,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit = {}
) {
    val columns = state.layout.columns
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = state.resolvedItems.size,
            key = { state.resolvedItems[it].raw.id },
            span = { index ->
                GridItemSpan(state.resolvedItems[index].raw.colSpan.coerceAtMost(columns))
            }
        ) { index ->
            val resolved = state.resolvedItems[index]
            ReorderableCanvasTile(
                resolved = resolved,
                index = index,
                draggingIndex = draggingIndex,
                dragOffset = dragOffset,
                gridState = gridState,
                onTap = { onTap(resolved) },
                onLongPress = { onLongPress(resolved) },
                onReorder = onReorder,
                setDraggingIndex = setDraggingIndex,
                setDragOffset = setDragOffset,
                hoverTargetIndex = hoverTargetIndex,
                setHoverTargetIndex = setHoverTargetIndex,
                appWidgetHost = appWidgetHost,
                aspectRatio = resolved.raw.colSpan.toFloat() / resolved.raw.rowSpan,
                editMode = editMode,
                onResizeWidget = onResizeWidget
            )
        }
        item(span = { GridItemSpan(1) }, key = "__canvas_add__") {
            CanvasAddItemTile(
                onTap = onAddItem,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun CanvasHorizontalGrid(
    state: CanvasUiState,
    gridState: LazyGridState,
    onTap: (ResolvedCanvasItem) -> Unit,
    onLongPress: (ResolvedCanvasItem) -> Unit,
    onAddItem: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    draggingIndex: Int?,
    setDraggingIndex: (Int?) -> Unit,
    dragOffset: Offset,
    setDragOffset: (Offset) -> Unit,
    hoverTargetIndex: Int?,
    setHoverTargetIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null,
    editMode: Boolean = false,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit = {}
) {
    val rows = state.layout.rows
    LazyHorizontalGrid(
        state = gridState,
        rows = GridCells.Fixed(rows),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = state.resolvedItems.size,
            key = { state.resolvedItems[it].raw.id },
            span = { index ->
                GridItemSpan(state.resolvedItems[index].raw.rowSpan.coerceAtMost(rows))
            }
        ) { index ->
            val resolved = state.resolvedItems[index]
            ReorderableCanvasTile(
                resolved = resolved,
                index = index,
                draggingIndex = draggingIndex,
                dragOffset = dragOffset,
                gridState = gridState,
                onTap = { onTap(resolved) },
                onLongPress = { onLongPress(resolved) },
                onReorder = onReorder,
                setDraggingIndex = setDraggingIndex,
                setDragOffset = setDragOffset,
                hoverTargetIndex = hoverTargetIndex,
                setHoverTargetIndex = setHoverTargetIndex,
                appWidgetHost = appWidgetHost,
                aspectRatio = resolved.raw.colSpan.toFloat() / resolved.raw.rowSpan,
                editMode = editMode,
                onResizeWidget = onResizeWidget
            )
        }
        item(span = { GridItemSpan(1) }, key = "__canvas_add__") {
            CanvasAddItemTile(
                onTap = onAddItem,
                modifier = Modifier.aspectRatio(1f)
            )
        }
    }
}

@Composable
private fun ReorderableCanvasTile(
    resolved: ResolvedCanvasItem,
    index: Int,
    draggingIndex: Int?,
    dragOffset: Offset,
    hoverTargetIndex: Int?,
    setHoverTargetIndex: (Int?) -> Unit,
    gridState: LazyGridState,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    setDraggingIndex: (Int?) -> Unit,
    setDragOffset: (Offset) -> Unit,
    appWidgetHost: AppWidgetHost?,
    aspectRatio: Float,
    editMode: Boolean,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit
) {
    val isDragging = draggingIndex == index
    val translation = if (isDragging) dragOffset else Offset.Zero
    val dragModifier = if (editMode) {
        Modifier.pointerInput(resolved.raw.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    setDraggingIndex(index)
                    setDragOffset(Offset.Zero)
                    setHoverTargetIndex(null)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    val next = dragOffset + dragAmount
                    setDragOffset(next)
                    val target = computeTargetIndex(
                        gridState = gridState,
                        fromIndex = index,
                        dragOffset = next
                    )
                    setHoverTargetIndex(target?.takeIf { it != index })
                },
                onDragEnd = {
                    val finalOffset = dragOffset
                    if (finalOffset.getDistance() > DRAG_THRESHOLD_PX) {
                        val target = computeTargetIndex(
                            gridState = gridState,
                            fromIndex = index,
                            dragOffset = finalOffset
                        )
                        if (target != null && target != index) onReorder(index, target)
                    }
                    setDraggingIndex(null)
                    setDragOffset(Offset.Zero)
                    setHoverTargetIndex(null)
                },
                onDragCancel = {
                    setDraggingIndex(null)
                    setDragOffset(Offset.Zero)
                    setHoverTargetIndex(null)
                }
            )
        }
    } else {
        Modifier
    }
    CanvasItemTile(
        resolved = resolved,
        onTap = onTap,
        onLongPress = onLongPress,
        modifier = Modifier
            .aspectRatio(aspectRatio)
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationX = translation.x
                translationY = translation.y
                if (isDragging) {
                    scaleX = 1.05f
                    scaleY = 1.05f
                    alpha = 0.92f
                }
            }
            .then(dragModifier),
        appWidgetHost = appWidgetHost,
        suppressTileLongPress = editMode,
        editMode = editMode,
        isDropTarget = !isDragging && hoverTargetIndex == index,
        onResizeWidget = onResizeWidget
    )
}

/**
 * Find the item index closest to where the dragged tile's center is now sitting.
 * Uses [LazyGridState.layoutInfo] to read the visible items' positions in
 * viewport coordinates and snaps the drop to the nearest center.
 */
private fun computeTargetIndex(
    gridState: LazyGridState,
    fromIndex: Int,
    dragOffset: Offset
): Int? {
    val info = gridState.layoutInfo
    val from = info.visibleItemsInfo.firstOrNull { it.index == fromIndex } ?: return null
    val fromCenter = from.offset.let { o ->
        IntOffset(o.x + from.size.width / 2, o.y + from.size.height / 2)
    }
    val targetCenter = IntOffset(
        x = (fromCenter.x + dragOffset.x).toInt(),
        y = (fromCenter.y + dragOffset.y).toInt()
    )
    return info.visibleItemsInfo
        .filter { it.index < info.totalItemsCount - 1 } // skip the trailing "+" tile
        .minByOrNull { item ->
            val cx = item.offset.x + item.size.width / 2
            val cy = item.offset.y + item.size.height / 2
            val dx = (cx - targetCenter.x).toLong()
            val dy = (cy - targetCenter.y).toLong()
            dx * dx + dy * dy
        }?.index
}
