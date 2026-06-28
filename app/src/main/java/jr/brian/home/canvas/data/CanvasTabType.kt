package jr.brian.home.canvas.data

/**
 * Storage-namespace constant passed as the `tabType` parameter to
 * [jr.brian.home.data.FolderManager] and [jr.brian.home.data.PinnedRomManager]
 * for folders/ROMs that belong to a Unified Canvas page.
 *
 * Using a dedicated value keeps Canvas-owned entities isolated from the existing
 * apps/widget folders so the two systems can't collide on per-page indices.
 */
object CanvasTabType {
    const val VALUE = "canvas"
}
