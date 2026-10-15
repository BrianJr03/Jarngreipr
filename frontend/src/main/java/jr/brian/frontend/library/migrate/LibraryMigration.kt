package jr.brian.frontend.library.migrate

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemDao
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot migration from the pre-Phase-2 engine (esde_prefs +
 * rom_index/[sys].json caches) into the Room-backed engine.
 *
 * ### Lifecycle
 *
 * `runOnce()` is idempotent. It records a per-step completion
 * version in its own small prefs file (`library_migration_prefs`);
 * subsequent calls see the version is current and return quickly.
 * `:app` calls `runOnce()` early on startup; nothing in the UI
 * depends on its completion so a slow first-boot migration doesn't
 * block the launcher.
 *
 * ### Phase 2d scope
 *
 * This commit lands the SourceEntity + SystemEntity migration. The
 * rom_index game import and the gamelist.xml decoration come in the
 * next two commits as [runOnce] gains additional steps.
 */
@Singleton
class LibraryMigration @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val legacyPrefs: EsdeLegacyPrefs,
    private val repository: LibraryRepository,
    private val sourceDao: SourceDao,
    private val systemDao: SystemDao,
    private val catalog: SystemCatalog,
) {
    private val migrationPrefs by lazy {
        context.getSharedPreferences(MIGRATION_PREFS, Context.MODE_PRIVATE)
    }

    // In-process guard against a double runOnce from two startup paths
    // racing before the SharedPreferences flush has landed.
    private val lock = Mutex()

    /**
     * Idempotent: safe to call on every launch. Does nothing when
     * [MIGRATION_VERSION_KEY] already matches [CURRENT_VERSION].
     */
    suspend fun runOnce() {
        lock.withLock {
            val stored = migrationPrefs.getInt(MIGRATION_VERSION_KEY, 0)
            if (stored >= CURRENT_VERSION) {
                return
            }
            runMigration()
            migrationPrefs.edit().putInt(MIGRATION_VERSION_KEY, CURRENT_VERSION).apply()
        }
    }

    /**
     * Reset the migration marker. Test-only: lets a developer
     * reproduce the first-run codepath without reinstalling. Not
     * exposed to end users.
     */
    @androidx.annotation.VisibleForTesting
    internal fun resetForTesting() {
        migrationPrefs.edit().remove(MIGRATION_VERSION_KEY).apply()
    }

    private suspend fun runMigration() = withContext(Dispatchers.IO) {
        Log.i(TAG, "Running library migration v$CURRENT_VERSION")
        upsertCatalogSystems()
        importSources()
        Log.i(TAG, "Library migration complete")
    }

    /**
     * Make every catalog system a row in [SystemEntity] so later
     * steps writing [jr.brian.frontend.library.db.GameEntity] rows
     * (which have an FK to systems) always have a parent to reference.
     * Same work the scanner does on its first pass; running it eagerly
     * here means the first UI render after migration has catalog
     * systems available even before any scan.
     */
    private suspend fun upsertCatalogSystems() {
        val rows = catalog.all().map { def ->
            SystemEntity(
                id = def.id,
                displayName = def.displayName,
                folderAliases = def.folderAliases,
                extensions = def.extensions,
                screenScraperSystemId = def.screenScraperSystemId,
                retroArchSystemId = def.retroArchSystemId,
            )
        }
        systemDao.upsertAll(rows)
    }

    /**
     * One SourceEntity per legacy source. The three legacy sources —
     * `roms_paths` file paths, per-system `saf_tree_uris`, and
     * user-declared `system_folder_mappings` — collapse to a single
     * SourceEntity model (plan problem #10). The scanner's
     * ScannerDispatcher picks the right scanner (fast-walk vs SAF)
     * at scan time based on the URI shape.
     *
     * Dedup rule: addSource short-circuits on an existing treeUri,
     * so running this twice (or against partially-migrated state)
     * is safe.
     */
    private suspend fun importSources() {
        for (path in legacyPrefs.romsPaths()) {
            if (path.isBlank()) continue
            repository.addSource(
                treeUri = path,
                displayPath = path,
                volumeId = volumeIdForPath(path),
            )
        }

        for ((systemName, treeUri) in legacyPrefs.safTreeUris()) {
            if (treeUri.isBlank()) continue
            repository.addSource(
                treeUri = treeUri,
                displayPath = displayPathForTree(systemName, treeUri),
                volumeId = null,
            )
        }

        for (mapping in legacyPrefs.systemFolderMappings()) {
            if (mapping.treeUri.isBlank()) continue
            repository.addSource(
                treeUri = mapping.treeUri,
                displayPath = mapping.displayPath,
                volumeId = null,
            )
        }
    }

    /**
     * `primary` for internal storage, otherwise the first `/storage/`
     * child segment. The scanner uses this later to pick a backend;
     * for now it's just metadata.
     */
    private fun volumeIdForPath(path: String): String? {
        if (path.startsWith("/storage/emulated/0")) return "primary"
        if (path.startsWith("/storage/")) {
            val after = path.removePrefix("/storage/")
            val slash = after.indexOf('/')
            return if (slash < 0) after else after.substring(0, slash)
        }
        return null
    }

    private fun displayPathForTree(systemName: String, treeUri: String): String {
        // The legacy writer used the raw treeUri as the display path;
        // most UIs show it as "<systemName> (SAF)" rather than the
        // raw URI. Favor the human-readable form here.
        return "$systemName (SAF)".ifBlank { treeUri }
    }

    private companion object {
        const val TAG = "LibraryMigration"
        const val MIGRATION_PREFS = "library_migration_prefs"
        const val MIGRATION_VERSION_KEY = "migration_version"

        /**
         * Bumped whenever a new migration step is added. Lets a user
         * who ran migration v1 pick up v2's work on next launch
         * without re-doing v1.
         */
        const val CURRENT_VERSION = 1
    }
}
