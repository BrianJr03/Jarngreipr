package jr.brian.home.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

internal val Context.folderDataStore by preferencesDataStore(name = "folders")

@Singleton
class FolderManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _editRefused = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Fires whenever an edit is refused because a page's stored folders can't be read. */
    val editRefused: SharedFlow<Unit> = _editRefused.asSharedFlow()

    private val folderBackgroundsDir: File by lazy {
        File(context.filesDir, "folder_backgrounds").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getStorageKey(
        pageIndex: Int,
        tabType: String
    ): String {
        return if (tabType == TAB_TYPE_APPS) {
            "folders_page_$pageIndex"
        } else {
            "folders_${tabType}_page_$pageIndex"
        }
    }

    fun getFolders(
        pageIndex: Int,
        tabType: String = TAB_TYPE_APPS
    ): Flow<List<Folder>> {
        return context.folderDataStore.data.map { preferences ->
            val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
            val foldersJson = preferences[key] ?: "[]"
            try {
                val folderDataList: List<FolderData> = json.decodeFromString(foldersJson)
                folderDataList.map { it.toFolder() }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /** Unreadable stored data is left byte-for-byte as it is, and the edit reports false. */
    private suspend fun editFolders(
        pageIndex: Int,
        tabType: String,
        transform: (List<FolderData>) -> List<FolderData>
    ): Boolean {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        var readable = false
        context.folderDataStore.edit { preferences ->
            val existing = decodeStoredFolders(preferences[key])
            readable = existing != null
            if (existing != null) {
                preferences[key] = json.encodeToString(transform(existing))
            }
        }
        if (!readable) {
            Log.w(TAG, "Folder edit skipped: stored folders are unreadable")
            _editRefused.tryEmit(Unit)
        }
        return readable
    }

    private fun decodeStoredFolders(stored: String?): List<FolderData>? {
        if (stored == null) return emptyList()
        return try {
            json.decodeFromString<List<FolderData>>(stored)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun createFolder(
        pageIndex: Int,
        folder: Folder,
        tabType: String = TAB_TYPE_APPS
    ): Boolean = editFolders(pageIndex, tabType) { it + folder.toData() }

    @Suppress("unused")
    suspend fun updateFolder(
        pageIndex: Int,
        folder: Folder,
        tabType: String = TAB_TYPE_APPS
    ): Boolean = editFolders(pageIndex, tabType) { existing ->
        existing.map { if (it.id == folder.id) folder.toData() else it }
    }

    suspend fun renameFolder(
        pageIndex: Int,
        folderId: String,
        newName: String,
        tabType: String = TAB_TYPE_APPS
    ): Boolean = editFolders(pageIndex, tabType) { existing ->
        existing.map { if (it.id == folderId) it.copy(name = newName) else it }
    }

    suspend fun updateFolderApps(
        pageIndex: Int,
        folderId: String,
        appPackageNames: List<String>,
        tabType: String = TAB_TYPE_APPS
    ): Boolean = editFolders(pageIndex, tabType) { existing ->
        existing
            .map { if (it.id == folderId) it.copy(appPackageNames = appPackageNames) else it }
            .filter { it.appPackageNames.isNotEmpty() }
    }

    suspend fun deleteFolder(
        pageIndex: Int,
        folderId: String,
        tabType: String = TAB_TYPE_APPS
    ): Boolean {
        var removedImage: String? = null
        val saved = editFolders(pageIndex, tabType) { existing ->
            removedImage = existing.firstOrNull { it.id == folderId }?.backgroundImagePath
            existing.filter { it.id != folderId }
        }
        if (saved) removedImage?.let(::deleteBackgroundFileIfManaged)
        return saved
    }

    suspend fun updateFolderBackground(
        pageIndex: Int,
        folderId: String,
        backgroundColorArgb: Int?,
        backgroundImagePath: String?,
        tabType: String = TAB_TYPE_APPS
    ): Boolean {
        var replacedImage: String? = null
        val saved = editFolders(pageIndex, tabType) { existing ->
            replacedImage = existing.firstOrNull { it.id == folderId }
                ?.backgroundImagePath
                ?.takeIf { it != backgroundImagePath }
            existing.map {
                if (it.id == folderId) {
                    it.copy(
                        backgroundColorArgb = backgroundColorArgb,
                        backgroundImagePath = backgroundImagePath
                    )
                } else {
                    it
                }
            }
        }
        if (saved) replacedImage?.let(::deleteBackgroundFileIfManaged)
        return saved
    }

    /** For an image saved for an edit that was then refused, so it isn't left behind. */
    fun discardFolderBackgroundImage(path: String) = deleteBackgroundFileIfManaged(path)

    suspend fun saveFolderBackgroundImage(
        folderId: String,
        sourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(sourceUri)
            val extension = if (mimeType == "image/gif") "gif" else "png"
            val fileName = "${folderId}_${System.currentTimeMillis()}.$extension"
            val target = File(folderBackgroundsDir, fileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return@withContext Result.failure(Exception("Unable to open image"))
            Result.success(target.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun deleteBackgroundFileIfManaged(path: String) {
        try {
            val file = File(path)
            if (file.exists() && file.parentFile?.absolutePath == folderBackgroundsDir.absolutePath) {
                file.delete()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun updateFolderPosition(
        pageIndex: Int,
        folderId: String,
        position: AppPosition,
        tabType: String = TAB_TYPE_APPS
    ): Boolean = editFolders(pageIndex, tabType) { existing ->
        existing.map {
            if (it.id == folderId) {
                it.copy(x = position.x, y = position.y, iconSize = position.iconSize)
            } else {
                it
            }
        }
    }

    @Suppress("unused")
    suspend fun getFolder(
        pageIndex: Int,
        folderId: String,
        tabType: String = TAB_TYPE_APPS
    ): Folder? {
        return getFolders(pageIndex, tabType).first().find { it.id == folderId }
    }

    @Suppress("unused")
    suspend fun removeAppFromFolders(
        pageIndex: Int,
        packageName: String,
        tabType: String = TAB_TYPE_APPS
    ): Boolean = editFolders(pageIndex, tabType) { existing ->
        existing
            .map { folder ->
                folder.copy(appPackageNames = folder.appPackageNames.filter { it != packageName })
            }
            .filter { it.appPackageNames.isNotEmpty() }
    }

    suspend fun setAllFolders(
        pageIndex: Int,
        tabType: String,
        folders: List<Folder>
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            preferences[key] = json.encodeToString(folders.map { it.toData() })
        }
    }

    suspend fun reorderPages(
        oldIndicesInNewOrder: Map<Int, Int>,
        tabType: String = TAB_TYPE_APPS
    ) {
        context.folderDataStore.edit { preferences ->
            val oldEntries = mutableMapOf<Int, String>()
            preferences.asMap().forEach { (prefKey, value) ->
                val pageIndex = extractPageIndex(prefKey.name, tabType) ?: return@forEach
                (value as? String)?.let { oldEntries[pageIndex] = it }
            }
            oldEntries.keys.forEach { idx ->
                preferences.remove(stringPreferencesKey(getStorageKey(idx, tabType)))
            }
            oldIndicesInNewOrder.forEach { (newIndex, oldIndex) ->
                oldEntries[oldIndex]?.let { value ->
                    preferences[stringPreferencesKey(getStorageKey(newIndex, tabType))] = value
                }
            }
        }
    }

    suspend fun removePage(
        pageIndex: Int,
        tabType: String = TAB_TYPE_APPS
    ) {
        context.folderDataStore.edit { preferences ->
            val oldEntries = mutableMapOf<Int, String>()
            preferences.asMap().forEach { (prefKey, value) ->
                val idx = extractPageIndex(prefKey.name, tabType) ?: return@forEach
                (value as? String)?.let { oldEntries[idx] = it }
            }
            oldEntries.keys.forEach { idx ->
                preferences.remove(stringPreferencesKey(getStorageKey(idx, tabType)))
            }
            oldEntries.forEach { (idx, value) ->
                val newIdx = when {
                    idx < pageIndex -> idx
                    idx > pageIndex -> idx - 1
                    else -> return@forEach
                }
                preferences[stringPreferencesKey(getStorageKey(newIdx, tabType))] = value
            }
        }
    }

    private fun extractPageIndex(keyName: String, tabType: String): Int? {
        val prefix = if (tabType == TAB_TYPE_APPS) "folders_page_" else "folders_${tabType}_page_"
        if (!keyName.startsWith(prefix)) return null
        return keyName.removePrefix(prefix).toIntOrNull()
    }

    @Serializable
    private data class FolderData(
        val id: String,
        val name: String,
        val appPackageNames: List<String>,
        val x: Float,
        val y: Float,
        val iconSize: Float,
        val backgroundColorArgb: Int? = null,
        val backgroundImagePath: String? = null
    )

    private fun Folder.toData() = FolderData(
        id = id,
        name = name,
        appPackageNames = appPackageNames,
        x = position.x,
        y = position.y,
        iconSize = position.iconSize,
        backgroundColorArgb = backgroundColorArgb,
        backgroundImagePath = backgroundImagePath
    )

    private fun FolderData.toFolder() = Folder(
        id = id,
        name = name,
        appPackageNames = appPackageNames,
        position = AppPosition(
            packageName = id,
            x = x,
            y = y,
            iconSize = iconSize
        ),
        backgroundColorArgb = backgroundColorArgb,
        backgroundImagePath = backgroundImagePath
    )

    companion object {
        const val TAB_TYPE_APPS = "apps"
        const val TAB_TYPE_WIDGETS = "widgets"
        private const val TAG = "FolderManager"
    }
}
