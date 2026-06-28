package jr.brian.home.canvas.model

import kotlinx.serialization.Serializable

@Serializable
enum class CanvasScrollOrientation {
    HORIZONTAL,
    VERTICAL
}

/**
 * Persisted layout for one Unified Canvas page.
 *
 * When [orientation] is [CanvasScrollOrientation.VERTICAL], [columns] is the
 * primary (cross-axis) count and content scrolls down. When [HORIZONTAL],
 * [rows] is the cross-axis count and content scrolls right.
 *
 * [version] is bumped whenever the persisted schema changes so callers can
 * migrate older blobs.
 */
@Serializable
data class CanvasLayout(
    val orientation: CanvasScrollOrientation = CanvasScrollOrientation.VERTICAL,
    val columns: Int = DEFAULT_COLUMNS,
    val rows: Int = DEFAULT_ROWS,
    val items: List<CanvasItem> = emptyList(),
    /** When true, long-press starts a drag-to-reorder gesture; when false it opens the remove dialog. */
    val editMode: Boolean = false,
    val version: Int = CURRENT_VERSION
) {
    companion object {
        const val CURRENT_VERSION = 1
        const val DEFAULT_COLUMNS = 4
        const val DEFAULT_ROWS = 6
        const val MIN_AXIS = 1
        const val MAX_AXIS = 12
    }
}
