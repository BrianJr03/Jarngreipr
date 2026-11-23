package jr.brian.home.canvas.jarn

import androidx.core.net.toUri
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.data.FolderManager
import jr.brian.home.jarn.AppJarnAppOptions
import jr.brian.jarnfe.apps.JarnAppOptionsData
import jr.brian.jarnfe.canvas.JarnAppDisplay
import jr.brian.jarnfe.canvas.JarnCanvasFolders
import jr.brian.jarnfe.canvas.jarnFolderPackages
import javax.inject.Inject
import javax.inject.Singleton

internal fun DisplayPreference.toJarnDisplay(): JarnAppDisplay = when (this) {
    DisplayPreference.PRIMARY_DISPLAY -> JarnAppDisplay.Primary
    DisplayPreference.CURRENT_DISPLAY -> JarnAppDisplay.Current
}

internal fun JarnAppDisplay.toPreference(): DisplayPreference = when (this) {
    JarnAppDisplay.Primary -> DisplayPreference.PRIMARY_DISPLAY
    JarnAppDisplay.Current -> DisplayPreference.CURRENT_DISPLAY
}

/** Folder edits on the stores Classic reads. Deleting always takes the folder's tiles with it. */
@Singleton
class AppJarnCanvasFolders @Inject constructor(
    private val layouts: CanvasLayoutManager,
    private val folders: FolderManager,
    private val tiles: AppJarnCanvasTiles,
    appOptions: AppJarnAppOptions,
) : JarnCanvasFolders, JarnAppOptionsData by appOptions {

    override suspend fun renameFolder(pageIndex: Int, folderId: String, name: String) = tiles.edit {
        folders.renameFolder(pageIndex, folderId, name, CanvasTabType.VALUE)
    }

    override suspend fun editFolderApps(
        pageIndex: Int,
        folderId: String,
        added: Set<String>,
        removed: Set<String>,
    ) = tiles.edit {
        val folder = folders.getFolder(pageIndex, folderId, CanvasTabType.VALUE) ?: return@edit
        val next = jarnFolderPackages(folder.appPackageNames, added, removed)
        if (next.isEmpty()) {
            removeFolderAndTiles(pageIndex, folderId)
        } else {
            folders.updateFolder(pageIndex, folder.copy(appPackageNames = next), CanvasTabType.VALUE)
        }
    }

    override suspend fun moveAppTileToFolder(
        tilePage: Int,
        itemId: String,
        folderPage: Int,
        folderId: String,
    ): Boolean = tiles.edit {
        val tile = layouts.getLayout(tilePage).items.firstOrNull { it.id == itemId } as? CanvasItem.AppItem
            ?: return@edit false
        val hasFolderTile = layouts.getLayout(folderPage).items
            .any { it is CanvasItem.FolderItem && it.folderId == folderId }
        val folder = folders.getFolder(folderPage, folderId, CanvasTabType.VALUE)
        if (!hasFolderTile || folder == null) return@edit false
        if (tile.packageName !in folder.appPackageNames) {
            val next = jarnFolderPackages(folder.appPackageNames, setOf(tile.packageName), emptySet())
            folders.updateFolder(folderPage, folder.copy(appPackageNames = next), CanvasTabType.VALUE)
            val saved = folders.getFolder(folderPage, folderId, CanvasTabType.VALUE)
            if (saved == null || tile.packageName !in saved.appPackageNames) return@edit false
        }
        layouts.removeItem(tilePage, itemId)
        true
    }

    override suspend fun deleteFolder(pageIndex: Int, folderId: String) = tiles.edit {
        removeFolderAndTiles(pageIndex, folderId)
    }

    /** The folder goes first, so Classic's folder sync can never put a tile back. */
    private suspend fun removeFolderAndTiles(pageIndex: Int, folderId: String) {
        folders.deleteFolder(pageIndex, folderId, CanvasTabType.VALUE)
        layouts.getLayout(pageIndex).items
            .filter { it is CanvasItem.FolderItem && it.folderId == folderId }
            .forEach { layouts.removeItem(pageIndex, it.id) }
    }

    override suspend fun setFolderBackgroundColor(pageIndex: Int, folderId: String, argb: Int?) =
        tiles.edit {
            folders.updateFolderBackground(pageIndex, folderId, argb, null, CanvasTabType.VALUE)
        }

    override suspend fun setFolderBackgroundImage(pageIndex: Int, folderId: String, uri: String): Boolean {
        val path = folders.saveFolderBackgroundImage(folderId, uri.toUri()).getOrNull() ?: return false
        tiles.edit {
            folders.updateFolderBackground(pageIndex, folderId, null, path, CanvasTabType.VALUE)
        }
        return true
    }
}
