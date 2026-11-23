package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.JarnAppOptionsFlow
import jr.brian.jarnfe.apps.JarnAppOptionsTarget
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

internal sealed interface CanvasOverlay {
    data object Options : CanvasOverlay
    data object Pages : CanvasOverlay
    data object Add : CanvasOverlay
    data object PickApp : CanvasOverlay
    data object PickFolderApps : CanvasOverlay
    data object PickRom : CanvasOverlay
    data object PickWidget : CanvasOverlay
    data object Music : CanvasOverlay
    data object Rss : CanvasOverlay
    data class RebindWidget(val widgetId: Int) : CanvasOverlay
    data class TileMenu(val itemId: String) : CanvasOverlay
    data class ArtMedia(val itemId: String) : CanvasOverlay
    data class ConfirmRemove(val itemId: String) : CanvasOverlay
    data class Items(val target: CanvasListTarget) : CanvasOverlay
    data class Folder(val itemId: String) : CanvasOverlay
    data class AppOptions(val itemId: String) : CanvasOverlay
    data class AskDisplay(val packageName: String) : CanvasOverlay
}

/** What a tile menu needs from the page on screen. */
internal class CanvasTileCommands(
    val useWidget: (CanvasItem.WidgetItem) -> Unit,
    val moveTile: (CanvasItem) -> Unit,
    val choosePhoto: (CanvasItem.PhotoContainer) -> Unit,
    val playRandom: (CanvasItem.LastPlayedItem) -> Unit,
    val refreshWidgets: () -> Unit,
    val grabNewTile: (String) -> Unit,
    val previewArt: (CanvasArtPreview?) -> Unit,
    val removeTile: (CanvasItem) -> Unit,
)

internal fun canvasItemResolved(item: CanvasItem, page: JarnCanvasPage): Boolean = when (item) {
    is CanvasItem.AppItem -> item.packageName in page.apps
    is CanvasItem.FolderItem -> item.folderId in page.folders
    is CanvasItem.RomItem -> item.romKey in page.roms
    else -> true
}

@Composable
internal fun CanvasTileOverlays(
    overlay: CanvasOverlay?,
    data: JarnCanvasData,
    page: JarnCanvasPage?,
    snapshot: JarnCanvasSnapshot,
    commands: CanvasTileCommands,
    onOverlay: (CanvasOverlay?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val close = { onOverlay(null) }
    val folderName = stringResource(R.string.jarnfe_canvas_folder_default_name)
    val addTile: (JarnCanvasNewTile) -> Unit = { tile ->
        close()
        val index = page?.index
        if (index != null) {
            scope.launch {
                val id = data.addTile(index, tile)
                if (id == null) {
                    JarnNotices.show(R.string.jarnfe_canvas_notice_add_failed)
                } else {
                    commands.grabNewTile(id)
                }
            }
        }
    }
    when (overlay) {
        CanvasOverlay.Add -> CanvasAddSheet(
            onChoice = { choice ->
                when (choice) {
                    CanvasAddChoice.App -> onOverlay(CanvasOverlay.PickApp)
                    CanvasAddChoice.Folder -> onOverlay(CanvasOverlay.PickFolderApps)
                    CanvasAddChoice.Rom -> onOverlay(CanvasOverlay.PickRom)
                    CanvasAddChoice.Widget -> onOverlay(CanvasOverlay.PickWidget)
                    CanvasAddChoice.Art -> addTile(JarnCanvasNewTile.Art)
                    CanvasAddChoice.LastPlayed -> addTile(JarnCanvasNewTile.LastPlayed)
                    CanvasAddChoice.Photo -> addTile(JarnCanvasNewTile.Photo)
                    CanvasAddChoice.RssLauncher -> addTile(JarnCanvasNewTile.RssLauncher)
                    CanvasAddChoice.RssMusic -> addTile(JarnCanvasNewTile.RssMusic)
                }
            },
            onClose = close,
        )
        CanvasOverlay.PickApp -> CanvasAppPickerSheet(
            data = data,
            onPicked = { addTile(JarnCanvasNewTile.App(it.packageName)) },
            onClose = close,
        )
        CanvasOverlay.PickFolderApps -> CanvasFolderCreateFlow(
            data = data,
            defaultName = folderName,
            onCreate = { name, packages -> addTile(JarnCanvasNewTile.Folder(name, packages)) },
            onClose = close,
        )
        is CanvasOverlay.Folder -> {
            val item = page?.layout?.items
                ?.firstOrNull { it.id == overlay.itemId } as? CanvasItem.FolderItem
            val folder = item?.let { page.folders[it.folderId] }
            if (folder == null) {
                LaunchedEffect(overlay) { close() }
            } else {
                CanvasFolderRoute(data, page.index, folder, close)
            }
        }
        CanvasOverlay.PickRom -> CanvasRomPickerSheet(
            data = data,
            onPicked = { addTile(JarnCanvasNewTile.Rom(it.key)) },
            onClose = close,
        )
        CanvasOverlay.PickWidget -> CanvasWidgetPickerSheet(
            data = data,
            rebindId = null,
            onBound = { addTile(JarnCanvasNewTile.Widget(it.widgetId, it.columns, it.rows)) },
            onClose = close,
        )
        is CanvasOverlay.RebindWidget -> CanvasWidgetPickerSheet(
            data = data,
            rebindId = overlay.widgetId,
            onBound = {
                commands.refreshWidgets()
                close()
            },
            onClose = close,
        )
        CanvasOverlay.Music -> CanvasMusicSheet(data, snapshot.music, close)
        CanvasOverlay.Rss -> CanvasRssSheet(data, close)
        is CanvasOverlay.TileMenu -> {
            val item = page?.layout?.items?.firstOrNull { it.id == overlay.itemId }
            if (item == null) {
                close()
            } else {
                val health = (item as? CanvasItem.WidgetItem)?.let { data.widgetHealth(it.widgetId) }
                CanvasTileMenuSheet(
                    item = item,
                    actions = canvasTileActions(
                        item = item,
                        resolved = canvasItemResolved(item, page),
                        widgetHealth = health,
                        hasRecent = snapshot.recent.isNotEmpty(),
                    ),
                    onAction = { action ->
                        when (action) {
                            CanvasTileAction.UseWidget -> {
                                close()
                                (item as? CanvasItem.WidgetItem)?.let(commands.useWidget)
                            }
                            CanvasTileAction.ChooseWidget ->
                                (item as? CanvasItem.WidgetItem)?.let {
                                    onOverlay(CanvasOverlay.RebindWidget(it.widgetId))
                                }
                            CanvasTileAction.ChoosePhoto -> {
                                close()
                                (item as? CanvasItem.PhotoContainer)?.let(commands.choosePhoto)
                            }
                            CanvasTileAction.PlayRandom -> {
                                close()
                                (item as? CanvasItem.LastPlayedItem)?.let(commands.playRandom)
                            }
                            CanvasTileAction.Games ->
                                (item as? CanvasItem.LastPlayedItem)?.let {
                                    data.updateTile(page.index, it.withNextCount())
                                }
                            CanvasTileAction.MediaType -> onOverlay(CanvasOverlay.ArtMedia(item.id))
                            CanvasTileAction.Scale -> data.updateTile(page.index, item.withFlippedScale())
                            CanvasTileAction.TransparentBackground ->
                                (item as? CanvasItem.EsdeArtItem)?.let {
                                    data.updateTile(page.index, it.withCardTransparencyToggled())
                                }
                            CanvasTileAction.AppOptions -> onOverlay(CanvasOverlay.AppOptions(item.id))
                            CanvasTileAction.Move -> {
                                close()
                                commands.moveTile(item)
                            }
                            CanvasTileAction.Remove -> onOverlay(CanvasOverlay.ConfirmRemove(item.id))
                        }
                    },
                    onClose = close,
                )
            }
        }
        is CanvasOverlay.ArtMedia -> {
            val item = page?.layout?.items
                ?.firstOrNull { it.id == overlay.itemId } as? CanvasItem.EsdeArtItem
            if (item == null) {
                LaunchedEffect(overlay) { close() }
            } else {
                CanvasArtMediaSheet(
                    item = item,
                    onPreview = { commands.previewArt(CanvasArtPreview(item.id, it)) },
                    onConfirm = { type ->
                        commands.previewArt(CanvasArtPreview(item.id, type))
                        data.updateTile(page.index, item.withMediaType(type))
                        close()
                    },
                    onClose = {
                        commands.previewArt(null)
                        onOverlay(CanvasOverlay.TileMenu(item.id))
                    },
                )
            }
        }
        is CanvasOverlay.AppOptions -> {
            val item = page?.layout?.items
                ?.firstOrNull { it.id == overlay.itemId } as? CanvasItem.AppItem
            val app = item?.let { page.apps[it.packageName] }
            if (app == null) {
                LaunchedEffect(overlay) { close() }
            } else {
                JarnAppOptionsFlow(
                    data = data,
                    target = JarnAppOptionsTarget(app.packageName, app.label, app.iconPath != null),
                    extras = emptyList(),
                    onClose = { onOverlay(CanvasOverlay.TileMenu(overlay.itemId)) },
                    onLeave = close,
                )
            }
        }
        is CanvasOverlay.AskDisplay -> CanvasDisplayChoiceSheet(
            onPick = { display ->
                close()
                activity?.let { data.launchAppOn(it, overlay.packageName, display) }
            },
            onClose = close,
        )
        is CanvasOverlay.ConfirmRemove -> {
            val item = page?.layout?.items?.firstOrNull { it.id == overlay.itemId }
            if (item == null) {
                close()
            } else {
                CanvasRemoveDialog(
                    item = item,
                    onConfirm = {
                        close()
                        commands.removeTile(item)
                    },
                    onDismiss = { onOverlay(CanvasOverlay.TileMenu(item.id)) },
                )
            }
        }
        else -> Unit
    }
}
