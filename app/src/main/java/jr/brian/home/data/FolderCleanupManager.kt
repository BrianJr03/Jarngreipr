package jr.brian.home.data

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.model.state.DeleteResult
import jr.brian.home.model.state.DeleteType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized manager for folder cleanup operations.
 * Handles deletion of empty folders with optional systeminfo.txt handling.
 */
@Singleton
class FolderCleanupManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /**
     * Deletes empty subfolders in the given folder paths.
     * @param folderPaths List of URI strings pointing to root folders to scan
     * @param includeSystemInfo If true, folders containing only systeminfo.txt are considered empty
     * @return DeleteResult with success/failure information
     */
    suspend fun deleteEmptyFolders(
        folderPaths: List<String>,
        includeSystemInfo: Boolean = false
    ): DeleteResult = withContext(Dispatchers.IO) {
        if (folderPaths.isEmpty()) {
            return@withContext DeleteResult.Failure("No folders provided")
        }

        try {
            var deletedCount = 0
            var failedCount = 0

            folderPaths.forEach { uriString ->
                val uri = uriString.toUri()
                val documentFile = DocumentFile.fromTreeUri(context, uri)

                documentFile?.let { root ->
                    val result = deleteEmptyFoldersRecursively(root, includeSystemInfo)
                    deletedCount += result.first
                    failedCount += result.second
                }
            }

            return@withContext if (failedCount == 0) {
                DeleteResult.Success(deletedCount, DeleteType.FOLDERS)
            } else {
                DeleteResult.Failure("Deleted $deletedCount folders, failed to delete $failedCount folders")
            }
        } catch (e: Exception) {
            return@withContext DeleteResult.Failure(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Counts the number of empty folders in the given folder paths.
     * @param folderPaths List of URI strings pointing to root folders to scan
     * @param includeSystemInfo If true, folders containing only systeminfo.txt are considered empty
     * @return The total count of empty folders
     */
    suspend fun countEmptyFolders(
        folderPaths: List<String>,
        includeSystemInfo: Boolean = false
    ): Int = withContext(Dispatchers.IO) {
        var count = 0

        folderPaths.forEach { uriString ->
            val uri = uriString.toUri()
            val documentFile = DocumentFile.fromTreeUri(context, uri)

            documentFile?.let { root ->
                count += countEmptyFoldersRecursive(root, includeSystemInfo)
            }
        }

        return@withContext count
    }

    private fun deleteEmptyFoldersRecursively(
        directory: DocumentFile,
        includeSystemInfo: Boolean
    ): Pair<Int, Int> {
        var deletedCount = 0
        var failedCount = 0

        // Process subdirectories first (depth-first)
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                val result = deleteEmptyFoldersRecursively(file, includeSystemInfo)
                deletedCount += result.first
                failedCount += result.second

                // After processing children, check if this directory is now empty
                if (isDirectoryEmpty(file, includeSystemInfo)) {
                    // Delete systeminfo.txt if it's the only file and we're including it
                    if (includeSystemInfo) {
                        deleteSystemInfoIfPresent(file)
                    }
                    
                    if (file.delete()) {
                        deletedCount++
                    } else {
                        failedCount++
                    }
                }
            }
        }

        return Pair(deletedCount, failedCount)
    }

    private fun countEmptyFoldersRecursive(
        directory: DocumentFile,
        includeSystemInfo: Boolean
    ): Int {
        var count = 0
        directory.listFiles().forEach { file ->
            if (file.isDirectory) {
                if (isDirectoryEmpty(file, includeSystemInfo)) {
                    count++
                } else {
                    count += countEmptyFoldersRecursive(file, includeSystemInfo)
                }
            }
        }
        return count
    }

    private fun isDirectoryEmpty(
        directory: DocumentFile,
        includeSystemInfo: Boolean
    ): Boolean {
        val files = directory.listFiles()
        
        if (files.isEmpty()) return true
        
        if (includeSystemInfo && files.size == 1) {
            val singleFile = files[0]
            if (singleFile.isFile && singleFile.name?.equals("systeminfo.txt", ignoreCase = true) == true) {
                return true
            }
        }
        
        return false
    }

    private fun deleteSystemInfoIfPresent(directory: DocumentFile) {
        directory.listFiles().forEach { file ->
            if (file.isFile && file.name?.equals("systeminfo.txt", ignoreCase = true) == true) {
                file.delete()
            }
        }
    }
}
