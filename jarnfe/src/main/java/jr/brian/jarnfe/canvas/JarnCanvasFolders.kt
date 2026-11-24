package jr.brian.jarnfe.canvas

import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.apps.JarnAppOptionsData

enum class JarnAppDisplay { Current, Primary }

/** Folder edits, on the same stores Classic reads. */
interface JarnCanvasFolders : JarnAppOptionsData {
    suspend fun renameFolder(pageIndex: Int, folderId: String, name: String)

    /** Applies the change to the stored list, so apps not installed right now are kept. */
    suspend fun editFolderApps(pageIndex: Int, folderId: String, added: Set<String>, removed: Set<String>)

    /**
     * Saves the app into the folder first and clears its tile only once that save is confirmed, so
     * a failure can leave the app in both places but never in neither. Returns whether it moved.
     */
    suspend fun moveAppTileToFolder(tilePage: Int, itemId: String, folderPage: Int, folderId: String): Boolean

    /** Removes the folder and every tile that points at it, so no missing tile is left. */
    suspend fun deleteFolder(pageIndex: Int, folderId: String)

    /** A null [argb] clears the background. Any previous image is released. */
    suspend fun setFolderBackgroundColor(pageIndex: Int, folderId: String, argb: Int?)

    /** Returns false when the image could not be kept. */
    suspend fun setFolderBackgroundImage(pageIndex: Int, folderId: String, uri: String): Boolean
}

fun showJarnFolderEditFailed() = JarnNotices.show(R.string.jarnfe_canvas_notice_folder_failed)

fun jarnFolderPackages(stored: List<String>, added: Set<String>, removed: Set<String>): List<String> =
    (stored.filterNot { it in removed } + added.filterNot { it in removed }).distinct()

internal fun jarnFolderPickerApps(
    installed: List<JarnCanvasApp>,
    inFolder: List<JarnCanvasApp>,
): List<JarnCanvasApp> =
    (installed + inFolder).distinctBy { it.packageName }.sortedBy { it.label.lowercase() }

internal fun jarnFolderNameOrNull(text: String): String? = text.trim().takeIf { it.isNotEmpty() }

internal fun JarnAppDisplay.opposite(): JarnAppDisplay =
    if (this == JarnAppDisplay.Primary) JarnAppDisplay.Current else JarnAppDisplay.Primary
