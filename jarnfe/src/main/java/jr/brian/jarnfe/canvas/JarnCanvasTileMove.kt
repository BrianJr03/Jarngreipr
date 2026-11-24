package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.grid.firstFreeRect
import jr.brian.home.canvas.grid.reservedRectsFor
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect

/** A tile leaving [fromPage] for [toPage], with the solved [placements] for [orientation] there. */
data class JarnCanvasTileMove(
    val itemId: String,
    val fromPage: Int,
    val toPage: Int,
    val orientation: CanvasScrollOrientation,
    val placements: Map<String, GridRect>,
)

/** Everything an undo needs. The "after" layouts show whether anything changed since the move. */
data class JarnCanvasTileMoved(
    val move: JarnCanvasTileMove,
    val sourceBefore: CanvasLayout,
    val sourceAfter: CanvasLayout,
    val destinationBefore: CanvasLayout,
    val destinationAfter: CanvasLayout,
    val romKey: String? = null,
    val pinAdded: Boolean = false,
    val pinRemoved: Boolean = false,
)

/**
 * Folders belong to their page. A game or widget already on [destination] stays single, because a
 * page shows each pinned game and each widget id once.
 */
fun jarnTileMayChangePage(item: CanvasItem, destination: CanvasLayout): Boolean = when {
    destination.items.any { it.id == item.id } -> false
    item is CanvasItem.FolderItem -> false
    item is CanvasItem.RomItem ->
        destination.items.none { it is CanvasItem.RomItem && it.romKey == item.romKey }
    item is CanvasItem.WidgetItem ->
        destination.items.none { it is CanvasItem.WidgetItem && it.widgetId == item.widgetId }
    else -> true
}

/**
 * [destination] with [item] placed in both orientations. The solved [move] placements apply to its
 * orientation. The other one takes the tile's size from [source], at its first free cell there.
 */
fun jarnLayoutWithArrival(
    destination: CanvasLayout,
    source: CanvasLayout,
    item: CanvasItem,
    move: JarnCanvasTileMove,
): CanvasLayout {
    val placed = move.placements.getValue(item.id)
    val active = destination.arrangementFor(move.orientation)
        .mapValues { (id, rect) -> move.placements[id] ?: rect } + (item.id to placed)
    val other = when (move.orientation) {
        CanvasScrollOrientation.HORIZONTAL -> CanvasScrollOrientation.VERTICAL
        CanvasScrollOrientation.VERTICAL -> CanvasScrollOrientation.HORIZONTAL
    }
    val otherArrangement = destination.arrangementFor(other)
    val size = source.arrangementFor(other)[item.id] ?: placed
    val cross = destination.crossAxisFor(other).coerceAtLeast(1)
    val direction = PushDirection.from(other)
    val inactive = firstFreeRect(
        occupied = otherArrangement.values,
        crossAxisCount = cross,
        pushDirection = direction,
        colSpan = if (direction == PushDirection.DOWN) size.colSpan.coerceAtMost(cross) else size.colSpan,
        rowSpan = if (direction == PushDirection.RIGHT) size.rowSpan.coerceAtMost(cross) else size.rowSpan,
        reservedRects = reservedRectsFor(destination, other),
    )
    return destination
        .copy(items = destination.items + item)
        .withArrangement(move.orientation, active)
        .withArrangement(other, otherArrangement + (item.id to inactive))
}
