package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlin.math.abs

internal enum class CanvasCarryResult { Moved, NoPage, Folder, AlreadyThere, Refused }

/** A tile drawn on a page that doesn't store it yet, while carried or until its save arrives. */
internal data class CanvasCarriedTile(
    val pageIndex: Int,
    val item: CanvasItem,
    val rect: GridRect,
    val apps: Map<String, JarnCanvasApp> = emptyMap(),
    val roms: Map<String, JarnCanvasRom> = emptyMap(),
)

internal fun canvasCarriedTile(grab: CanvasGrab, pages: List<JarnCanvasPage>): CanvasCarriedTile? {
    val home = pages.getOrNull(grab.home.pageIndex) ?: return null
    val item = home.layout.items.firstOrNull { it.id == grab.itemId } ?: return null
    return CanvasCarriedTile(
        pageIndex = grab.pageIndex,
        item = item,
        rect = grab.rect,
        apps = (item as? CanvasItem.AppItem)?.let { app ->
            home.apps[app.packageName]?.let { mapOf(app.packageName to it) }
        }.orEmpty(),
        roms = (item as? CanvasItem.RomItem)?.let { rom ->
            home.roms[rom.romKey]?.let { mapOf(rom.romKey to it) }
        }.orEmpty(),
    )
}

internal fun JarnCanvasPage.withCarriedTile(carried: CanvasCarriedTile?): JarnCanvasPage {
    if (carried == null || carried.pageIndex != index) return this
    if (layout.items.any { it.id == carried.item.id }) return this
    val arrangement = layout.activeArrangement + (carried.item.id to carried.rect)
    return copy(
        layout = layout
            .copy(items = layout.items + carried.item)
            .withArrangement(layout.activeOrientation, arrangement),
        apps = apps + carried.apps,
        roms = roms + carried.roms,
    )
}

/** The closest Canvas page that way. Pages without a grid can't hold a tile or a D-pad cursor. */
internal fun canvasPageInDirection(pages: List<JarnCanvasPage>, from: Int, direction: Int): JarnCanvasPage? {
    if (direction == 0) return null
    var index = from + direction
    while (index in pages.indices) {
        if (pages[index].kind == JarnCanvasPageKind.Canvas) return pages[index]
        index += direction
    }
    return null
}

/** True when nothing lies further that way, so the D-pad has no tile left to move to. */
internal fun canvasDpadAtEdge(rects: Collection<GridRect>, focused: GridRect, direction: Int): Boolean =
    rects.none { other ->
        other != focused && if (direction > 0) other.col >= focused.right else other.right <= focused.col
    }

/** The tile on the side the cursor came in from, closest to the row it left. */
internal fun canvasEntryTile(placements: Map<String, GridRect>, direction: Int, fromRow: Int): String? {
    if (placements.isEmpty()) return null
    val edge = if (direction > 0) placements.values.minOf { it.col } else placements.values.maxOf { it.right }
    return placements.entries
        .filter { (_, rect) -> if (direction > 0) rect.col == edge else rect.right == edge }
        .minByOrNull { (_, rect) -> abs(rect.row - fromRow) }
        ?.key
}

/** Moves the one shared grab to the next page, with the same rules for finger and controller. */
internal class CanvasPageCarrier(
    private val editor: CanvasEditor,
    private val pages: () -> List<JarnCanvasPage>,
) {
    fun hasTarget(direction: Int): Boolean {
        val grab = editor.grab ?: return false
        return canvasPageInDirection(pages(), grab.pageIndex, direction) != null
    }

    /** With [enterAtEdge] the tile lands on the side it came from, as a D-pad push expects. */
    fun carry(direction: Int, enterAtEdge: Boolean): CanvasCarryResult {
        val grab = editor.grab ?: return CanvasCarryResult.Refused
        val all = pages()
        val target = canvasPageInDirection(all, grab.pageIndex, direction) ?: return CanvasCarryResult.NoPage
        if (grab.isFolder) return CanvasCarryResult.Folder
        val goingHome = target.index == grab.home.pageIndex
        val item = all.getOrNull(grab.home.pageIndex)?.layout?.items?.firstOrNull { it.id == grab.itemId }
            ?: return CanvasCarryResult.Refused
        if (!goingHome && !jarnTileMayChangePage(item, target.layout)) return CanvasCarryResult.AlreadyThere
        if (!editor.changePage(target)) return CanvasCarryResult.Refused
        if (!goingHome) {
            editor.drive(grab.driver) {
                movedTo(
                    col = if (!enterAtEdge) cursor.col else if (direction > 0) 0 else Int.MAX_VALUE,
                    row = cursor.row,
                )
            }
        }
        return CanvasCarryResult.Moved
    }
}

internal fun CanvasCarryResult.showNotice() {
    when (this) {
        CanvasCarryResult.Folder -> JarnNotices.show(R.string.jarnfe_canvas_notice_folder_stays)
        CanvasCarryResult.AlreadyThere -> JarnNotices.show(R.string.jarnfe_canvas_notice_already_on_page)
        CanvasCarryResult.Refused -> JarnNotices.show(R.string.jarnfe_canvas_notice_move_failed)
        CanvasCarryResult.Moved, CanvasCarryResult.NoPage -> Unit
    }
}
