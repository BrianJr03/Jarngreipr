package jr.brian.jarnfe.canvas

import android.app.Activity
import androidx.compose.runtime.Immutable

enum class JarnAppDisplay { Current, Primary }

@Immutable
data class JarnFolderAppOptions(
    val display: JarnAppDisplay,
    val askOnLaunch: Boolean,
    val customName: String?,
    val hasSecondDisplay: Boolean,
)

/** Folder and per-app edits, on the same stores Classic reads. */
interface JarnCanvasFolders {
    suspend fun renameFolder(pageIndex: Int, folderId: String, name: String)

    /** Applies the change to the stored list, so apps not installed right now are kept. */
    suspend fun editFolderApps(pageIndex: Int, folderId: String, added: Set<String>, removed: Set<String>)

    /** Removes the folder and every tile that points at it, so no missing tile is left. */
    suspend fun deleteFolder(pageIndex: Int, folderId: String)

    /** A null [argb] clears the background. Any previous image is released. */
    suspend fun setFolderBackgroundColor(pageIndex: Int, folderId: String, argb: Int?)

    /** Returns false when the image could not be kept. */
    suspend fun setFolderBackgroundImage(pageIndex: Int, folderId: String, uri: String): Boolean

    fun folderAppOptions(packageName: String): JarnFolderAppOptions

    fun setAppDisplay(packageName: String, display: JarnAppDisplay)

    fun setAskDisplayOnLaunch(packageName: String, ask: Boolean)

    /** A null [name] goes back to the app's own name. */
    fun setAppName(packageName: String, name: String?)

    /** Returns false when the image could not be used. */
    suspend fun setAppIcon(packageName: String, uri: String): Boolean

    suspend fun clearAppIcon(packageName: String)

    fun launchAppOn(host: Activity, packageName: String, display: JarnAppDisplay)
}

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
