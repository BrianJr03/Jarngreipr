package jr.brian.home.canvas.ui

import android.appwidget.AppWidgetHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    modifier: Modifier = Modifier,
    appWidgetHost: AppWidgetHost? = null
) {
    CanvasGridLayout(
        state = state,
        onTap = onTap,
        onLongPress = onLongPress,
        onResizeWidget = onResizeWidget,
        onCommitLayout = onCommitLayout,
        onRequestResizeDialog = onRequestResizeDialog,
        modifier = modifier,
        appWidgetHost = appWidgetHost
    )
}
