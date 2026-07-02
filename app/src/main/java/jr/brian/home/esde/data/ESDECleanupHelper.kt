package jr.brian.home.esde.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.data.ESDECleanupManager
import jr.brian.home.data.FolderCleanupManager
import jr.brian.home.model.state.DeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for ESDE-specific cleanup operations.
 * Provides utilities to clean up empty folders in the ESDE media directory.
 */
@Singleton
class ESDECleanupHelper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val folderCleanupManager: FolderCleanupManager,
    private val esdeCleanupManager: ESDECleanupManager
) {
    /**
     * Deletes all empty folders in the selected cleanup folders.
     * Folders containing only systeminfo.txt are always considered empty for ESDE cleanup.
     * 
     * @param folderPaths Optional list of folder paths. If null, uses saved folders
     * @return DeleteResult with success/failure information
     */
    suspend fun deleteEmptyESDEFolders(folderPaths: List<String>? = null): DeleteResult = withContext(Dispatchers.IO) {
        val paths = folderPaths ?: esdeCleanupManager.folderPaths.first().toList()
        
        if (paths.isEmpty()) {
            return@withContext DeleteResult.Failure("No folders selected for cleanup")
        }
        
        try {
            // Always include systeminfo.txt in cleanup for ESDE folders
            return@withContext folderCleanupManager.deleteEmptyFolders(
                paths,
                includeSystemInfo = true
            )
        } catch (e: Exception) {
            return@withContext DeleteResult.Failure("Error cleaning folders: ${e.message}")
        }
    }
    
    /**
     * Counts empty folders in the selected cleanup folders.
     * 
     * @param folderPaths Optional list of folder paths. If null, uses saved folders
     * @return The count of empty folders
     */
    suspend fun countEmptyFolders(folderPaths: List<String>? = null): Int = withContext(Dispatchers.IO) {
        val paths = folderPaths ?: esdeCleanupManager.folderPaths.first().toList()
        
        if (paths.isEmpty()) {
            return@withContext 0
        }
        
        try {
            return@withContext folderCleanupManager.countEmptyFolders(
                paths,
                includeSystemInfo = true
            )
        } catch (e: Exception) {
            return@withContext 0
        }
    }
}
