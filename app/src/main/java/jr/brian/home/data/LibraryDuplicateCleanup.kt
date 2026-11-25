package jr.brian.home.data

import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.migrate.EsdeLegacyPrefs
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.removeRomsPath
import jr.brian.home.esde.util.documentIdToStoragePath
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Undoes the double listing from #459: picking one system's folder used to
 * also save its parent as a ROMs root, so both were scanned. Merges the
 * duplicate games, then drops a parent root only when that pick created it
 * and it holds nothing else.
 */
@Singleton
class LibraryDuplicateCleanup @Inject constructor(
    private val repository: LibraryRepository,
    private val esdePrefs: ESDEPreferencesManager,
    private val legacyPrefs: EsdeLegacyPrefs,
) {

    suspend fun run() {
        try {
            repository.mergeOverlappingGames()
            val state = esdePrefs.state.value
            val trees = legacyPrefs.safTreeUris() +
                state.systemFolderMappings.associate { it.systemName to it.treeUri }
            val stale = staleDerivedRoots(
                redundant = repository.redundantParentSources(),
                romsPaths = state.romsPaths,
                systemTrees = trees.mapValues { (_, uri) -> treeStoragePath(uri) },
            )
            for (source in stale) {
                esdePrefs.removeRomsPath(source.treeUri)
                repository.removeSource(source.id)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Duplicate cleanup failed", e)
        }
    }

    private fun treeStoragePath(uri: String): String? = runCatching {
        DocumentsContract.getTreeDocumentId(Uri.parse(uri))
    }.getOrNull()?.let(::documentIdToStoragePath)

    private companion object {
        const val TAG = "LibraryDuplicateCleanup"
    }
}

/**
 * The empty parent sources a single-system pick saved: a saved ROMs path whose
 * child folder, named after its system, is that system's own folder source.
 */
internal fun staleDerivedRoots(
    redundant: List<SourceEntity>,
    romsPaths: List<String>,
    systemTrees: Map<String, String?>,
): List<SourceEntity> = redundant.filter { source ->
    val root = source.treeUri.trimEnd('/')
    source.treeUri in romsPaths && systemTrees.any { (system, path) ->
        path != null && path.substringBeforeLast('/') == root &&
            path.substringAfterLast('/').equals(system, ignoreCase = true)
    }
}
