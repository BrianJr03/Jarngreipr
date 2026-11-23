package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.firstFreeRect
import jr.brian.home.canvas.grid.reservedRectsFor
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.defaultSpanFor

/** How far along the scroll axis a bulk add may place tiles, so one add can't run a page off forever. */
const val JARN_CANVAS_ADD_LANES = CanvasLayout.MAX_AXIS

/** [layout] holds every tile in [placedIds]. [noRoom] counts the tiles that did not fit. */
data class JarnCanvasAddPlan(
    val layout: CanvasLayout,
    val placedIds: List<String>,
    val noRoom: Int,
)

/**
 * Places [items] in free cells of both orientations. Tiles already on the page never move, and an
 * item that doesn't fit in both orientations is left out entirely, so the layout stays valid.
 */
fun jarnPlanCanvasAdds(layout: CanvasLayout, items: List<CanvasItem>): JarnCanvasAddPlan {
    val vertical = layout.verticalArrangement.toMutableMap()
    val horizontal = layout.horizontalArrangement.toMutableMap()
    val placed = mutableListOf<CanvasItem>()
    var noRoom = 0
    items.forEach { item ->
        if (item.id in vertical || item.id in horizontal) return@forEach
        val (colSpan, rowSpan) = defaultSpanFor(item)
        val down = freeRect(layout, CanvasScrollOrientation.VERTICAL, vertical.values, colSpan, rowSpan)
        val right = freeRect(layout, CanvasScrollOrientation.HORIZONTAL, horizontal.values, colSpan, rowSpan)
        if (down == null || right == null) {
            noRoom += 1
        } else {
            vertical[item.id] = down
            horizontal[item.id] = right
            placed += item
        }
    }
    return JarnCanvasAddPlan(
        layout = layout.copy(
            items = layout.items + placed,
            verticalArrangement = vertical,
            horizontalArrangement = horizontal,
        ),
        placedIds = placed.map { it.id },
        noRoom = noRoom,
    )
}

/** How many single-cell tiles still fit on this page in both orientations. */
fun jarnCanvasFreeSlots(layout: CanvasLayout): Int = minOf(
    freeCells(layout, CanvasScrollOrientation.VERTICAL),
    freeCells(layout, CanvasScrollOrientation.HORIZONTAL),
)

private fun freeRect(
    layout: CanvasLayout,
    orientation: CanvasScrollOrientation,
    occupied: Collection<GridRect>,
    colSpan: Int,
    rowSpan: Int,
): GridRect? {
    val down = orientation == CanvasScrollOrientation.VERTICAL
    val cross = layout.crossAxisFor(orientation)
    val rect = firstFreeRect(
        occupied = occupied,
        crossAxisCount = cross,
        pushDirection = if (down) PushDirection.DOWN else PushDirection.RIGHT,
        colSpan = colSpan,
        rowSpan = rowSpan,
        reservedRects = reservedRectsFor(layout, orientation),
    )
    val fits = if (down) {
        rect.right <= cross && rect.bottom <= JARN_CANVAS_ADD_LANES
    } else {
        rect.bottom <= cross && rect.right <= JARN_CANVAS_ADD_LANES
    }
    return rect.takeIf { fits }
}

private fun freeCells(layout: CanvasLayout, orientation: CanvasScrollOrientation): Int {
    val down = orientation == CanvasScrollOrientation.VERTICAL
    val cross = layout.crossAxisFor(orientation)
    val taken = layout.arrangementFor(orientation).values + reservedRectsFor(layout, orientation)
    var free = 0
    for (lane in 0 until JARN_CANVAS_ADD_LANES) {
        for (slot in 0 until cross) {
            val cell = if (down) GridRect(slot, lane, 1, 1) else GridRect(lane, slot, 1, 1)
            if (taken.none { it.overlaps(cell) }) free += 1
        }
    }
    return free
}

/** Picks kept in the order they were made. [capacity] is null when there is no limit. */
internal data class CanvasPickSelection<T>(
    val picked: Map<String, T> = emptyMap(),
    val capacity: Int? = null,
) {
    val count: Int get() = picked.size
    val isFull: Boolean get() = capacity != null && picked.size >= capacity

    operator fun contains(key: String): Boolean = key in picked

    /** At capacity, a new pick changes nothing. Removing a pick always works. */
    fun toggled(key: String, item: T): CanvasPickSelection<T> = when {
        key in picked -> copy(picked = picked - key)
        isFull -> this
        else -> copy(picked = picked + (key to item))
    }

    /** Adds [items] in order until capacity. Existing picks are kept. */
    fun withAll(items: List<T>, keyOf: (T) -> String): CanvasPickSelection<T> {
        val next = LinkedHashMap(picked)
        for (item in items) {
            if (capacity != null && next.size >= capacity) break
            next.putIfAbsent(keyOf(item), item)
        }
        return copy(picked = next)
    }

    fun cleared(): CanvasPickSelection<T> = copy(picked = emptyMap())
}

internal sealed interface CanvasAddNotice {
    data object PageFull : CanvasAddNotice
    data class SomeDidNotFit(val count: Int) : CanvasAddNotice
    data object Failed : CanvasAddNotice
    data object AlreadyHere : CanvasAddNotice
    data class Added(val count: Int) : CanvasAddNotice
}

/** Null for one added tile, because it is picked up right away and needs no message. */
internal fun canvasAddNotice(result: JarnCanvasAddResult): CanvasAddNotice? {
    val added = result.addedIds.size
    return when {
        result.noRoom > 0 && added == 0 -> CanvasAddNotice.PageFull
        result.noRoom > 0 -> CanvasAddNotice.SomeDidNotFit(result.noRoom)
        result.failed > 0 -> CanvasAddNotice.Failed
        added == 0 && result.alreadyPlaced > 0 -> CanvasAddNotice.AlreadyHere
        added > 1 -> CanvasAddNotice.Added(added)
        else -> null
    }
}
