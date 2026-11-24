package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.shell.JarnNotices

internal class CanvasTileHandlers(
    val onOpenList: (CanvasListTarget) -> Unit,
    val openFolder: (String) -> Unit,
    val launchApp: (JarnCanvasApp) -> Unit,
    val launchRom: (JarnCanvasRom) -> Unit,
    val choosePhoto: (CanvasItem.PhotoContainer) -> Unit,
    val openMusic: () -> Unit,
    val openRss: () -> Unit,
    val openTileMenu: (CanvasItem) -> Unit,
)

internal fun activateTile(
    item: CanvasItem,
    page: JarnCanvasPage,
    snapshot: JarnCanvasSnapshot,
    handlers: CanvasTileHandlers,
    useWidget: (CanvasItem.WidgetItem) -> Unit,
) {
    when (item) {
        is CanvasItem.AppItem -> page.apps[item.packageName]?.let(handlers.launchApp) ?: showMissing()
        is CanvasItem.FolderItem ->
            if (item.folderId in page.folders) handlers.openFolder(item.id) else showMissing()
        is CanvasItem.RomItem -> page.roms[item.romKey]?.let(handlers.launchRom) ?: showMissing()
        is CanvasItem.LastPlayedItem ->
            handlers.onOpenList(CanvasListTarget.LastPlayed(snapshot.recent.take(item.resolvedCount)))
        is CanvasItem.EsdeArtItem -> JarnSession.update { copy(deckTab = DeckTab.Now) }
        is CanvasItem.WidgetItem -> useWidget(item)
        is CanvasItem.PhotoContainer -> handlers.choosePhoto(item)
        is CanvasItem.RssLauncherItem -> handlers.openRss()
        is CanvasItem.RssMusicItem -> handlers.openMusic()
    }
}

private fun showMissing() = JarnNotices.show(R.string.jarnfe_canvas_notice_missing)

internal fun focusedConfirmLabel(data: JarnCanvasData, page: JarnCanvasPage, tile: CanvasItem?): Int? {
    if (tile == null) return R.string.jarnfe_hint_open
    val health = (tile as? CanvasItem.WidgetItem)?.let { data.widgetHealth(it.widgetId) }
    return canvasConfirmHint(tile, canvasItemResolved(tile, page), health)
}
