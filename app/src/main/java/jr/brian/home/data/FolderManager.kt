package jr.brian.home.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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

private val Context.folderDataStore by preferencesDataStore(name = "folders")

@Singleton
class FolderManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

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

    suspend fun createFolder(
        pageIndex: Int,
        folder: Folder,
        tabType: String = TAB_TYPE_APPS
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders + folder.toData()
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    @Suppress("unused")
    suspend fun updateFolder(
        pageIndex: Int,
        folder: Folder,
        tabType: String = TAB_TYPE_APPS
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders.map {
                if (it.id == folder.id) folder.toData() else it
            }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun renameFolder(
        pageIndex: Int,
        folderId: String,
        newName: String,
        tabType: String = TAB_TYPE_APPS
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders.map { folderData ->
                if (folderData.id == folderId) folderData.copy(name = newName) else folderData
            }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun updateFolderApps(
        pageIndex: Int,
        folderId: String,
        appPackageNames: List<String>,
        tabType: String = TAB_TYPE_APPS
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            // Update apps or delete folder if empty
            val updatedFolders = existingFolders
                .map { folderData ->
                    if (folderData.id == folderId) folderData.copy(appPackageNames = appPackageNames) else folderData
                }
                .filter { it.appPackageNames.isNotEmpty() }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun deleteFolder(
        pageIndex: Int,
        folderId: String,
        tabType: String = TAB_TYPE_APPS
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            existingFolders.firstOrNull { it.id == folderId }
                ?.backgroundImagePath
                ?.let { path -> deleteBackgroundFileIfManaged(path) }
            val updatedFolders = existingFolders.filter { it.id != folderId }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun updateFolderBackground(
        pageIndex: Int,
        folderId: String,
        backgroundColorArgb: Int?,
        backgroundImagePath: String?,
        tabType: String = TAB_TYPE_APPS
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            existingFolders.firstOrNull { it.id == folderId }
                ?.backgroundImagePath
                ?.takeIf { it != backgroundImagePath }
                ?.let { previousPath -> deleteBackgroundFileIfManaged(previousPath) }
            val updatedFolders = existingFolders.map { folderData ->
                if (folderData.id == folderId) {
                    folderData.copy(
                        backgroundColorArgb = backgroundColorArgb,
                        backgroundImagePath = backgroundImagePath
                    )
                } else {
                    folderData
                }
            }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

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
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (_: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders.map { folderData ->
                if (folderData.id == folderId) {
                    folderData.copy(
                        x = position.x,
                        y = position.y,
                        iconSize = position.iconSize
                    )
                } else {
                    folderData
                }
            }
            preferences[key] = json.encodeToString(updatedFolders)
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
    ) {
        val key = stringPreferencesKey(getStorageKey(pageIndex, tabType))
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (e: Exception) {
                emptyList()
            }
            // Remove the app from all folders and filter out empty folders
            val updatedFolders = existingFolders
                .map { folderData ->
                    folderData.copy(
                        appPackageNames = folderData.appPackageNames.filter { it != packageName }
                    )
                }
                .filter { it.appPackageNames.isNotEmpty() } // Delete folders that become empty
            preferences[key] = json.encodeToString(updatedFolders)
        }
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
    }
}
