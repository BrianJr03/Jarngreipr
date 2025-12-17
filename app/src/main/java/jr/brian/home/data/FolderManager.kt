package jr.brian.home.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import jr.brian.home.model.AppFolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FolderManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _foldersByPage: SnapshotStateMap<Int, SnapshotStateMap<String, AppFolder>> =
        mutableStateMapOf()

    private val _openFolderId = MutableStateFlow<String?>(null)
    val openFolderId: StateFlow<String?> = _openFolderId.asStateFlow()

    init {
        loadAllFolders()
    }

    fun getFolders(pageIndex: Int): Map<String, AppFolder> {
        return _foldersByPage[pageIndex] ?: emptyMap()
    }

    fun getFolder(pageIndex: Int, folderId: String): AppFolder? {
        return _foldersByPage[pageIndex]?.get(folderId)
    }

    fun createFolder(
        pageIndex: Int,
        name: String,
        apps: List<String>,
        x: Float,
        y: Float,
        iconSize: Float = 64f
    ): String {
        val folderId = UUID.randomUUID().toString()
        val folder = AppFolder(folderId, name, apps, x, y, iconSize)
        val pageFolders = _foldersByPage.getOrPut(pageIndex) { mutableStateMapOf() }
        pageFolders[folderId] = folder
        saveFoldersForPage(pageIndex)
        return folderId
    }

    fun updateFolder(pageIndex: Int, folder: AppFolder) {
        val pageFolders = _foldersByPage.getOrPut(pageIndex) { mutableStateMapOf() }
        pageFolders[folder.id] = folder
        saveFoldersForPage(pageIndex)
    }

    fun deleteFolder(pageIndex: Int, folderId: String) {
        _foldersByPage[pageIndex]?.remove(folderId)
        saveFoldersForPage(pageIndex)
    }

    fun addAppToFolder(pageIndex: Int, folderId: String, packageName: String) {
        val folder = getFolder(pageIndex, folderId) ?: return
        if (packageName !in folder.apps) {
            val updatedApps = folder.apps + packageName
            updateFolder(pageIndex, folder.copy(apps = updatedApps))
        }
    }

    fun removeAppFromFolder(pageIndex: Int, folderId: String, packageName: String) {
        val folder = getFolder(pageIndex, folderId) ?: return
        val updatedApps = folder.apps - packageName
        if (updatedApps.isEmpty()) {
            deleteFolder(pageIndex, folderId)
        } else {
            updateFolder(pageIndex, folder.copy(apps = updatedApps))
        }
    }

    fun updateFolderName(pageIndex: Int, folderId: String, newName: String) {
        val folder = getFolder(pageIndex, folderId) ?: return
        updateFolder(pageIndex, folder.copy(name = newName))
    }

    fun updateFolderPosition(pageIndex: Int, folderId: String, x: Float, y: Float) {
        val folder = getFolder(pageIndex, folderId) ?: return
        updateFolder(pageIndex, folder.copy(x = x, y = y))
    }

    fun updateFolderSize(pageIndex: Int, folderId: String, iconSize: Float) {
        val folder = getFolder(pageIndex, folderId) ?: return
        updateFolder(pageIndex, folder.copy(iconSize = iconSize))
    }

    fun openFolder(folderId: String) {
        _openFolderId.value = folderId
    }

    fun closeFolder() {
        _openFolderId.value = null
    }

    fun isAppInAnyFolder(pageIndex: Int, packageName: String): String? {
        val folders = getFolders(pageIndex)
        return folders.values.firstOrNull { packageName in it.apps }?.id
    }

    private fun loadAllFolders() {
        val maxPages = 10
        for (pageIndex in 0 until maxPages) {
            loadFoldersForPage(pageIndex)
        }
    }

    private fun loadFoldersForPage(pageIndex: Int) {
        val foldersKey = "${KEY_FOLDERS}_$pageIndex"
        val foldersJson = prefs.getString(foldersKey, null) ?: return
        val pageFolders = mutableStateMapOf<String, AppFolder>()

        try {
            foldersJson.split(SEPARATOR_FOLDERS).forEach { folderData ->
                if (folderData.isNotBlank()) {
                    val parts = folderData.split(SEPARATOR_FOLDER_FIELDS)
                    if (parts.size >= 3) {
                        val id = parts[0]
                        val name = parts[1]
                        val appsStr = parts[2]
                        val apps = if (appsStr.isNotEmpty()) {
                            appsStr.split(SEPARATOR_APPS)
                        } else {
                            emptyList()
                        }
                        val x = if (parts.size >= 4) parts[3].toFloatOrNull() ?: 0f else 0f
                        val y = if (parts.size >= 5) parts[4].toFloatOrNull() ?: 0f else 0f
                        val iconSize = if (parts.size >= 6) parts[5].toFloatOrNull() ?: 64f else 64f

                        pageFolders[id] = AppFolder(id, name, apps, x, y, iconSize)
                    }
                }
            }
            if (pageFolders.isNotEmpty()) {
                _foldersByPage[pageIndex] = pageFolders
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveFoldersForPage(pageIndex: Int) {
        val folders = _foldersByPage[pageIndex] ?: return
        val foldersJson = folders.values.joinToString(SEPARATOR_FOLDERS) { folder ->
            val appsStr = folder.apps.joinToString(SEPARATOR_APPS)
            "${folder.id}$SEPARATOR_FOLDER_FIELDS${folder.name}$SEPARATOR_FOLDER_FIELDS$appsStr$SEPARATOR_FOLDER_FIELDS${folder.x}$SEPARATOR_FOLDER_FIELDS${folder.y}$SEPARATOR_FOLDER_FIELDS${folder.iconSize}"
        }
        prefs.edit().apply {
            putString("${KEY_FOLDERS}_$pageIndex", foldersJson)
            apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "folder_prefs"
        private const val KEY_FOLDERS = "folders"
        private const val SEPARATOR_FOLDERS = "|||"
        private const val SEPARATOR_FOLDER_FIELDS = "^^"
        private const val SEPARATOR_APPS = ","
    }
}
