package jr.brian.home.data

import android.util.Log
import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.migrate.EsdeLegacyPrefs
import jr.brian.home.esde.data.ESDEPreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mirrors legacy's `ESDEPreferencesManager` ROM paths + SAF trees +
 * folder mappings into Room's `SourceEntity` table whenever the user
 * adds a new location. Insert-only: a path removed from prefs leaves
 * its SourceEntity row in place rather than cascading a game delete.
 *
 * Why: :frontend engines (`ScrapeWorker`, `LibraryScraper`) read
 * sources from Room. The one-shot `LibraryMigration` seeds them at
 * install time, but anything added afterwards via Settings → Custom
 * Paths or the Add-System flow would be invisible to Room until the
 * next migration-version bump. This keeps Room continuously in sync
 * without rewiring legacy's readers / writers to go through Room.
 *
 * ### Lifecycle
 *
 * Started once from [jr.brian.home.JarngreiprApplication.onCreate].
 * Observes [ESDEPreferencesManager.state] on the application scope
 * so a game launch finishing the frontend activity doesn't cancel it
 * mid-sync.
 */
@Singleton
class SourcesSyncService @Inject constructor(
    private val esdePrefs: ESDEPreferencesManager,
    private val legacyPrefs: EsdeLegacyPrefs,
    private val sourceDao: SourceDao,
    private val repository: LibraryRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    private val reconcileLock = Mutex()

    fun start() {
        scope.launch {
            esdePrefs.state
                .map { state ->
                    val paths = state.romsPaths.toMutableSet()
                    state.systemFolderMappings.forEach { paths.add(it.treeUri) }
                    legacyPrefs.safTreeUris().values.forEach { paths.add(it) }
                    paths.filter { it.isNotBlank() }.toSet()
                }
                .distinctUntilChanged()
                .collectLatest { desired ->
                    try {
                        reconcile(desired, esdePrefs.state.value)
                    } catch (e: Exception) {
                        Log.w(TAG, "Sources sync failed", e)
                    }
                }
        }
    }

    private suspend fun reconcile(desired: Set<String>, snapshot: jr.brian.home.esde.model.ESDEPrefsState) {
        reconcileLock.withLock {
            val existing = sourceDao.getAll().map { it.treeUri }.toSet()
            val missing = desired - existing
            if (missing.isEmpty()) return
            for (treeUri in missing) {
                val display = displayFor(treeUri, snapshot) ?: treeUri
                repository.addSource(
                    treeUri = treeUri,
                    displayPath = display,
                    volumeId = volumeIdFor(treeUri),
                )
            }
        }
    }

    private fun displayFor(treeUri: String, state: jr.brian.home.esde.model.ESDEPrefsState): String? {
        val mapping = state.systemFolderMappings.firstOrNull { it.treeUri == treeUri }
        if (mapping != null) return "${mapping.systemName} (${mapping.displayPath})"
        val safEntry = legacyPrefs.safTreeUris().entries.firstOrNull { it.value == treeUri }
        if (safEntry != null) return "${safEntry.key} (SAF)"
        return treeUri.takeIf { !it.startsWith("content://") }
    }

    private fun volumeIdFor(path: String): String? {
        if (path.startsWith("content://")) return null
        if (path.startsWith("/storage/emulated/0")) return "primary"
        if (path.startsWith("/storage/")) {
            val after = path.removePrefix("/storage/")
            val slash = after.indexOf('/')
            return if (slash < 0) after else after.substring(0, slash)
        }
        return null
    }

    private companion object {
        const val TAG = "SourcesSyncService"
    }
}
