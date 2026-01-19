package jr.brian.home.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.folderDataStore by preferencesDataStore(name = "folders")

@Singleton
class FolderManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getFolders(pageIndex: Int): Flow<List<Folder>> {
        return context.folderDataStore.data.map { preferences ->
            val key = stringPreferencesKey("folders_page_$pageIndex")
            val foldersJson = preferences[key] ?: "[]"
            try {
                val folderDataList: List<FolderData> = json.decodeFromString(foldersJson)
                folderDataList.map { it.toFolder() }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun createFolder(pageIndex: Int, folder: Folder) {
        val key = stringPreferencesKey("folders_page_$pageIndex")
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders + folder.toData()
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun updateFolder(pageIndex: Int, folder: Folder) {
        val key = stringPreferencesKey("folders_page_$pageIndex")
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders.map {
                if (it.id == folder.id) folder.toData() else it
            }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun deleteFolder(pageIndex: Int, folderId: String) {
        val key = stringPreferencesKey("folders_page_$pageIndex")
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (e: Exception) {
                emptyList()
            }
            val updatedFolders = existingFolders.filter { it.id != folderId }
            preferences[key] = json.encodeToString(updatedFolders)
        }
    }

    suspend fun updateFolderPosition(pageIndex: Int, folderId: String, position: AppPosition) {
        val key = stringPreferencesKey("folders_page_$pageIndex")
        context.folderDataStore.edit { preferences ->
            val existingFoldersJson = preferences[key] ?: "[]"
            val existingFolders: List<FolderData> = try {
                json.decodeFromString(existingFoldersJson)
            } catch (e: Exception) {
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

    suspend fun getFolder(pageIndex: Int, folderId: String): Folder? {
        return getFolders(pageIndex).first().find { it.id == folderId }
    }

    suspend fun removeAppFromFolders(pageIndex: Int, packageName: String) {
        val key = stringPreferencesKey("folders_page_$pageIndex")
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

    @Serializable
    private data class FolderData(
        val id: String,
        val name: String,
        val appPackageNames: List<String>,
        val x: Float,
        val y: Float,
        val iconSize: Float
    )

    private fun Folder.toData() = FolderData(
        id = id,
        name = name,
        appPackageNames = appPackageNames,
        x = position.x,
        y = position.y,
        iconSize = position.iconSize
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
        )
    )
}
