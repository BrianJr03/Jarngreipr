package jr.brian.home.canvas.ui

import android.appwidget.AppWidgetHost
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import jr.brian.home.canvas.grid.LayoutSnapshot
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasUiState
import jr.brian.home.canvas.model.ResolvedCanvasItem

/**
 * Public entry point for the unified canvas page. Delegates to
 * [CanvasGridLayout] which places items at absolute (col, row) coordinates
 * and runs drag/resize gestures through [jr.brian.home.canvas.grid.GridSolver].
 *
 * [onReorder] is retained for source-compatibility with the previous
 * LazyGrid-based implementation and is no longer driven from here — the
 * solver commits multi-item placement updates via [onCommitLayout] instead.
 */
@Composable
fun CanvasGrid(
    state: CanvasUiState,
    onTap: (ResolvedCanvasItem) -> Unit,
    onLongPress: (ResolvedCanvasItem) -> Unit,
    @Suppress("UNUSED_PARAMETER") onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    onResizeWidget: (CanvasItem.WidgetItem) -> Unit,
    onCommitLayout: (LayoutSnapshot) -> Unit,
    onRequestResizeDialog: (item: CanvasItem, minColSpan: Int, minRowSpan: Int) -> Unit,
    onAddClick: () -> Unit,
    onAddLongClick: () -> Unit,
    onDeleteClick: (ResolvedCanvasItem) -> Unit,
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null,
    scrollState: ScrollState = rememberScrollState(),
    onDoubleTap: (ResolvedCanvasItem) -> Unit = {},
    onAddTilePositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    CanvasGridLayout(
        state = state,
        onTap = onTap,
        onLongPress = onLongPress,
        onDoubleTap = onDoubleTap,
        onResizeWidget = onResizeWidget,
        onCommitLayout = onCommitLayout,
        onRequestResizeDialog = onRequestResizeDialog,
        onAddClick = onAddClick,
        onAddLongClick = onAddLongClick,
        onDeleteClick = onDeleteClick,
        modifier = modifier,
        appWidgetHost = appWidgetHost,
        scrollState = scrollState,
        onAddTilePositioned = onAddTilePositioned
    )
}
