package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R

/** Shows the tile on its new page at once, then saves. A failed save returns to the source page. */
internal fun moveCanvasTile(
    drop: CanvasGrabDrop.BetweenPages,
    pages: List<JarnCanvasPage>,
    editor: CanvasEditor,
    undo: CanvasUndoController,
    writes: CanvasUndoWrites,
    onArrival: (CanvasCarriedTile?) -> Unit,
    onEntryFocus: (String?) -> Unit,
    onFocused: (String) -> Unit,
) {
    val tile = pages.getOrNull(drop.source.pageIndex)?.layout?.items?.firstOrNull { it.id == drop.itemId }
    val rect = drop.destination.placements[drop.itemId]
    val orientation = pages.getOrNull(drop.destination.pageIndex)?.layout?.activeOrientation
    if (tile == null || rect == null || orientation == null) {
        JarnCanvas.selectedPage = drop.source.pageIndex
        CanvasCarryResult.Refused.showNotice()
        return
    }
    onFocused(drop.itemId)
    editor.settled = drop.destination.placements
    onArrival(canvasArrival(drop, tile, rect, pages))
    val move = JarnCanvasTileMove(
        itemId = drop.itemId,
        fromPage = drop.source.pageIndex,
        toPage = drop.destination.pageIndex,
        orientation = orientation,
        placements = drop.destination.placements,
    )
    undo.moveTile(writes, move, drop.isNewTile) { moved ->
        if (moved == null) {
            onArrival(null)
            editor.settled = null
            onEntryFocus(drop.itemId)
            JarnCanvas.selectedPage = drop.source.pageIndex
            CanvasCarryResult.Refused.showNotice()
        }
    }
}

private fun canvasArrival(
    drop: CanvasGrabDrop.BetweenPages,
    tile: CanvasItem,
    rect: GridRect,
    pages: List<JarnCanvasPage>,
): CanvasCarriedTile {
    val home = pages.getOrNull(drop.source.pageIndex)
    return CanvasCarriedTile(
        pageIndex = drop.destination.pageIndex,
        item = tile,
        rect = rect,
        apps = home?.apps.orEmpty(),
        roms = home?.roms.orEmpty(),
    )
}

/** Only a focused tile at the page's side edge gets a binding, and only toward a real Canvas page. */
internal fun canvasPagePush(
    page: JarnCanvasPage?,
    pages: List<JarnCanvasPage>,
    focusedId: String?,
    onPage: (Int, String?) -> Unit,
    onCarry: (Int) -> Unit,
): CanvasPagePush {
    val placements = page?.takeIf { it.kind == JarnCanvasPageKind.Canvas }?.layout?.activeArrangement.orEmpty()
    val reserved = page?.takeIf { it.kind == JarnCanvasPageKind.Canvas }?.let { reservedRectsForActive(it.layout) }.orEmpty()
    val focused = placements[focusedId]
    val pushTo: (Int) -> (() -> Unit)? = push@{ direction ->
        val current = page ?: return@push null
        val rect = focused ?: return@push null
        if (!canvasDpadAtEdge(placements.values + reserved, rect, direction)) return@push null
        val target = canvasPageInDirection(pages, current.index, direction) ?: return@push null
        ({ onPage(target.index, canvasEntryTile(target.layout.activeArrangement, direction, rect.row)) })
    }
    return CanvasPagePush(onPageLeft = pushTo(-1), onPageRight = pushTo(1), onCarry = onCarry)
}

@Composable
internal fun CanvasNotice(titleRes: Int, messageRes: Int) {
    JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
}
