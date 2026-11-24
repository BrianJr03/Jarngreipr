package jr.brian.frontend.library.setup

import android.content.Context
import android.provider.DocumentsContract
import android.util.Log
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SourceEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LibraryFolderBackup"
private const val CONTENT_PREFIX = "content://"
private const val PARENT_SEGMENT = ".."

/** A game folder as stored in a settings backup. Access grants are never included. */
data class BackedUpFolder(
    val treeUri: String,
    val displayPath: String,
    val volumeId: String? = null,
)

/**
 * Saves and restores game folders. Restore only inserts missing rows, so
 * existing games and favorites are untouched, and it never starts a scan.
 * A folder without live access is stored as not granted, which the Library
 * shows as needing to be reconnected.
 */
@Singleton
class LibraryFolderBackup internal constructor(
    private val sourceDao: SourceDao,
    private val grantedTrees: () -> Set<String>,
    private val canReadPath: (String) -> Boolean,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        sourceDao: SourceDao,
    ) : this(sourceDao, context::grantedTrees, ::canReadFolder)

    /** Null when folders could not be read, so a restore leaves the device's folders alone. */
    suspend fun export(): List<BackedUpFolder>? = guarded(fallback = null) {
        sourceDao.getAll().map { BackedUpFolder(it.treeUri, it.displayPath, it.volumeId) }
    }

    /** Returns how many folders were added. A failure never fails the wider import. */
    suspend fun restore(folders: List<BackedUpFolder>): Int = guarded(fallback = 0) {
        val missing = foldersToRestore(folders, sourceDao.getAll())
        if (missing.isEmpty()) return@guarded 0
        val grants = grantedTrees()
        missing.count { folder ->
            val hasAccess = if (folder.treeUri.startsWith(CONTENT_PREFIX)) {
                folder.treeUri in grants
            } else {
                canReadPath(folder.treeUri)
            }
            sourceDao.insert(
                SourceEntity(
                    treeUri = folder.treeUri,
                    displayPath = folder.displayPath.ifBlank { folder.treeUri },
                    volumeId = folder.volumeId,
                    hasPersistedGrant = hasAccess,
                    lastScannedAt = 0L,
                ),
            ) > 0
        }
    }

    private suspend fun <T> guarded(fallback: T, block: suspend () -> T): T = try {
        withContext(Dispatchers.IO) { block() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Library folder backup step failed", e)
        fallback
    }
}

/** Valid folders not already present by URI or by storage location. */
internal fun foldersToRestore(
    backup: List<BackedUpFolder>,
    existing: List<SourceEntity>,
): List<BackedUpFolder> {
    val uris = existing.mapTo(HashSet()) { it.treeUri }
    val locations = existing.mapNotNullTo(HashSet()) { sourceLocation(it.treeUri) }
    return backup.filter { folder ->
        isRestorableFolder(folder.treeUri) &&
            uris.add(folder.treeUri) &&
            sourceLocation(folder.treeUri)?.let(locations::add) != false
    }
}

private fun isRestorableFolder(uri: String): Boolean =
    if (uri.startsWith(CONTENT_PREFIX)) {
        runCatching { DocumentsContract.getTreeDocumentId(uri.toUri()) }.isSuccess
    } else {
        uri.startsWith("/") && uri.trim('/').isNotEmpty() && PARENT_SEGMENT !in uri.split('/')
    }
