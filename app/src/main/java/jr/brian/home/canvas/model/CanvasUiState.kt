package jr.brian.home.canvas.model

/**
 * What the [jr.brian.home.canvas.ui.UnifiedCanvasTab] composable renders.
 *
 * [pageIndex] of -1 means the screen hasn't bound a page yet (initial state
 * before [jr.brian.home.canvas.viewmodel.CanvasViewModel.setPageIndex] is called).
 */
data class CanvasUiState(
    val pageIndex: Int = -1,
    val layout: CanvasLayout = CanvasLayout(),
    val resolvedItems: List<ResolvedCanvasItem> = emptyList()
)
