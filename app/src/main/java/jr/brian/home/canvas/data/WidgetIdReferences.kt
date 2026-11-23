package jr.brian.home.canvas.data

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout

/** True while any saved tile or widget page still points at [widgetId]. */
internal fun isWidgetIdReferenced(
    layouts: Collection<CanvasLayout>,
    widgetPageIds: Collection<Int>,
    widgetId: Int,
): Boolean = widgetId in widgetPageIds || layouts.any { layout ->
    layout.items.any { it is CanvasItem.WidgetItem && it.widgetId == widgetId }
}
