package jr.brian.home.data.config

import android.appwidget.AppWidgetManager
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout

/** Never handed out by the system, so a tile holding it can't pick up someone else's widget. */
internal const val MISSING_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID

/** The provider behind every widget tile, keyed by widget id, for [repairRestoredCanvas]. */
internal fun canvasWidgetProviders(
    layouts: Collection<CanvasLayout>,
    providerOf: (Int) -> String?,
): Map<String, String> = layouts
    .flatMap { it.items }
    .filterIsInstance<CanvasItem.WidgetItem>()
    .map { it.widgetId }
    .filter { it != MISSING_WIDGET_ID }
    .distinct()
    .mapNotNull { id -> providerOf(id)?.let { id.toString() to it } }
    .toMap()

/**
 * Widget ids belong to this install, so a restored id can be dead or now point at a different
 * widget. A tile keeps its id only while the same provider still answers to it. [backupProviders]
 * is null for backups made before v52, where a live id is the best check there is.
 *
 * Photo files aren't in backups either, so a photo tile whose picture this device can't open goes
 * back to its "pick a photo" state instead of showing a broken image.
 */
internal fun repairRestoredCanvas(
    layouts: Map<String, CanvasLayout>,
    backupProviders: Map<String, String>?,
    providerOf: (Int) -> String?,
    canOpenPhoto: (String) -> Boolean,
): Map<String, CanvasLayout> = layouts.mapValues { (_, layout) ->
    layout.copy(
        items = layout.items.map { item ->
            when (item) {
                is CanvasItem.WidgetItem ->
                    if (isWidgetStillOurs(item.widgetId, backupProviders, providerOf)) item
                    else item.copy(widgetId = MISSING_WIDGET_ID)
                is CanvasItem.PhotoContainer -> {
                    val uri = item.imageUri
                    if (uri == null || canOpenPhoto(uri)) item else item.copy(imageUri = null)
                }
                else -> item
            }
        }
    )
}

private fun isWidgetStillOurs(
    widgetId: Int,
    backupProviders: Map<String, String>?,
    providerOf: (Int) -> String?,
): Boolean {
    if (widgetId == MISSING_WIDGET_ID) return false
    val live = providerOf(widgetId) ?: return false
    return backupProviders == null || backupProviders[widgetId.toString()] == live
}
