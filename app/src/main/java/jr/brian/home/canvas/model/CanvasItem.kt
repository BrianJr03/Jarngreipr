package jr.brian.home.canvas.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A placeable entity on a Unified Canvas page.
 *
 * Items carry only their stable [id] and a *reference* to an entity owned by
 * another manager (app package / folder id / rom key / widget id / rss
 * launcher) — never a duplicate of that entity's data, so icon-pack changes,
 * custom names, folder edits, etc. flow through automatically.
 *
 * Items are orientation-agnostic content. Their placement (col / row / colSpan
 * / rowSpan) lives in [CanvasLayout.verticalArrangement] and
 * [CanvasLayout.horizontalArrangement] — one map per scroll orientation — so
 * the two orientations can keep independent grids over the same shared
 * content set.
 */
@Serializable
sealed class CanvasItem {
    abstract val id: String

    @Serializable
    @SerialName("app")
    data class AppItem(
        override val id: String,
        val packageName: String
    ) : CanvasItem()

    @Serializable
    @SerialName("folder")
    data class FolderItem(
        override val id: String,
        val folderId: String
    ) : CanvasItem()

    @Serializable
    @SerialName("rom")
    data class RomItem(
        override val id: String,
        val romKey: String
    ) : CanvasItem()

    @Serializable
    @SerialName("widget")
    data class WidgetItem(
        override val id: String,
        val widgetId: Int
    ) : CanvasItem()

    @Serializable
    @SerialName("rss_launcher")
    data class RssLauncherItem(
        override val id: String
    ) : CanvasItem()
}

/**
 * Default 1×1 / 2×2 span used when an item is first placed into an arrangement
 * (or when a migrated item needs an auto-placement). Widgets default to 2×2;
 * everything else to 1×1. Users can resize per-orientation afterwards.
 */
fun defaultSpanFor(item: CanvasItem): Pair<Int, Int> = when (item) {
    is CanvasItem.WidgetItem -> 2 to 2
    is CanvasItem.AppItem,
    is CanvasItem.FolderItem,
    is CanvasItem.RomItem,
    is CanvasItem.RssLauncherItem -> 1 to 1
}
