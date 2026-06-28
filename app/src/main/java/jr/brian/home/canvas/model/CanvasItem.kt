package jr.brian.home.canvas.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A placeable item on a Unified Canvas page.
 *
 * Every variant carries a stable [id] (unique within a [CanvasLayout]) and its grid
 * placement (top-left [col]/[row] plus [colSpan]/[rowSpan]). The variant payload is
 * a *reference* to an entity owned by another manager — never a duplicate of that
 * entity's data — so icon-pack changes, custom names, folder edits, etc. flow
 * through automatically.
 */
@Serializable
sealed class CanvasItem {
    abstract val id: String
    abstract val col: Int
    abstract val row: Int
    abstract val colSpan: Int
    abstract val rowSpan: Int

    @Serializable
    @SerialName("app")
    data class AppItem(
        override val id: String,
        override val col: Int,
        override val row: Int,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1,
        val packageName: String
    ) : CanvasItem()

    @Serializable
    @SerialName("folder")
    data class FolderItem(
        override val id: String,
        override val col: Int,
        override val row: Int,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1,
        val folderId: String
    ) : CanvasItem()

    @Serializable
    @SerialName("rom")
    data class RomItem(
        override val id: String,
        override val col: Int,
        override val row: Int,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1,
        val romKey: String
    ) : CanvasItem()

    @Serializable
    @SerialName("widget")
    data class WidgetItem(
        override val id: String,
        override val col: Int,
        override val row: Int,
        override val colSpan: Int = 2,
        override val rowSpan: Int = 2,
        val widgetId: Int
    ) : CanvasItem()

    @Serializable
    @SerialName("rss_launcher")
    data class RssLauncherItem(
        override val id: String,
        override val col: Int,
        override val row: Int,
        override val colSpan: Int = 1,
        override val rowSpan: Int = 1
    ) : CanvasItem()
}

/**
 * Variant-preserving copy of a [CanvasItem] with new placement/size.
 * Needed because `.copy()` isn't available on the sealed base type.
 */
fun CanvasItem.withPlacement(
    col: Int = this.col,
    row: Int = this.row,
    colSpan: Int = this.colSpan,
    rowSpan: Int = this.rowSpan
): CanvasItem = when (this) {
    is CanvasItem.AppItem -> copy(col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
    is CanvasItem.FolderItem -> copy(col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
    is CanvasItem.RomItem -> copy(col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
    is CanvasItem.WidgetItem -> copy(col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
    is CanvasItem.RssLauncherItem -> copy(col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
}
