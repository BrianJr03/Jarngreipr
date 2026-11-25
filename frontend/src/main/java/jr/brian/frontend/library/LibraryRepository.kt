package jr.brian.frontend.library

import android.util.Log
import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.GameInfoRow
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.PlayHistoryDao
import jr.brian.frontend.library.db.ScrapeJobDao
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemDao
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.media.EsdeLayout
import jr.brian.frontend.library.media.JarngreiprLayout
import jr.brian.frontend.library.media.MediaIndex
import jr.brian.frontend.library.scan.ScanReport
import jr.brian.frontend.library.scan.ScannedRom
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One source to scan. [systemFolder] marks a source that is a single system's
 * folder. [excludedSystems] are already held by a folder inside this one.
 */
data class SourceScanTarget(
    val source: SourceEntity,
    val systemFolder: SystemDef? = null,
    val excludedSystems: Set<String> = emptySet(),
)

/**
 * Public API over the library engine.
 *
 * ### Phase 2c scope
 *
 * The read side (Phase 2a) is unchanged. This commit adds write
 * methods — `addSource`, `scanAll`, `scanSystem` — plus the Scanning
 * state transitions the Phase 3 UI will consume. Writes run on the
 * `@ApplicationScope` so a game launch that finishes the frontend
 * activity doesn't cancel a scan mid-flight (same invariant as the
 * Phase 0 fix on `RomSearchViewModel`), and the loading flag is
 * reset via `Job.invokeOnCompletion` so a cancelled scan still
 * flips back to `Idle`.
 *
 * Media root config (`setMediaRoots`) is a plain setter rather than
 * a constructor parameter so :app can update it when the user
 * changes `ESDEPreferencesManager.mediaPath` without reconstructing
 * the repository. Phase 2d replaces that bridging with a direct
 * read of the migrated prefs.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val systemDao: SystemDao,
    private val sourceDao: SourceDao,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    @Suppress("unused") private val scrapeJobDao: ScrapeJobDao,
    @Suppress("unused") private val playHistoryDao: PlayHistoryDao,
    private val catalog: SystemCatalog,
    private val scanner: ScannerDispatcher,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<LibraryState>(LibraryState.Idle)
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    // Public read surface, unchanged from Phase 2a.
    val systems: Flow<List<SystemEntity>> = systemDao.observeAll()
    val sources: Flow<List<SourceEntity>> = sourceDao.observeAll()
    val allGames: Flow<List<GameEntity>> = gameDao.observeAllVisible()
    val favorites: Flow<List<GameEntity>> = gameDao.observeFavorites()

    /**
     * Thin joined projection: one row per visible game, with the
     * media columns the launcher needs. Replaces the previous
     * `combine(allGames, mediaDao.observeAll())` which materialised
     * two full tables into cursors and OOM'd on large libraries.
     */
    val allGameRows: Flow<List<GameInfoRow>> = gameDao.observeAllVisibleWithMedia()

    fun gamesForSystem(systemId: String): Flow<List<GameEntity>> =
        gameDao.observeBySystem(systemId)

    fun recentGames(limit: Int = 20): Flow<List<GameEntity>> =
        gameDao.observeRecent(limit)

    fun mediaForGame(gameId: Long): Flow<MediaEntity?> =
        mediaDao.observeByGameId(gameId)

    suspend fun findGameById(gameId: Long): GameEntity? = gameDao.findById(gameId)
    suspend fun findSystemById(id: String): SystemEntity? = systemDao.findById(id)

    // ──────────────────────── Media roots ────────────────────────

    @Volatile private var mediaRoots: List<File> = emptyList()

    /**
     * Set the ES-DE / scraper media roots the next scan will consult.
     * No immediate side effect — the next `scanAll` / `scanSystem`
     * picks these up when it builds the MediaIndex.
     */
    fun setMediaRoots(roots: List<File>) {
        mediaRoots = roots
    }

    @Volatile private var keepImportedMetadata: Boolean = true

    /**
     * Mirrors the "Read gamelist.xml" setting. When off, the next scan
     * returns surviving games to filename titles and clears imported
     * metadata. User-owned fields are kept either way.
     */
    fun setKeepImportedMetadata(keep: Boolean) {
        keepImportedMetadata = keep
    }

    // ──────────────────────── Sources ────────────────────────

    /**
     * Register a new ROM folder. [treeUri] is either a `content://`
     * SAF tree URI OR a file-system path (for primary-storage sources
     * where SAF is unnecessary); the scanner picks the right backend
     * at scan time.
     *
     * Returns the row id of the inserted source. If a source with the
     * same [treeUri] already exists, its existing id is returned
     * (idempotent — a double-tap on "Add folder" shouldn't duplicate).
     */
    suspend fun addSource(
        treeUri: String,
        displayPath: String,
        volumeId: String? = null,
    ): Long {
        val existing = sourceDao.findByTreeUri(treeUri)
        if (existing != null) return existing.id
        return sourceDao.insert(
            SourceEntity(
                treeUri = treeUri,
                displayPath = displayPath,
                volumeId = volumeId,
                hasPersistedGrant = true,
                lastScannedAt = 0L,
            )
        )
    }

    // ──────────────────────── Scans ────────────────────────

    /**
     * Scan every configured source. Walks each source, writes
     * discovered games + resolved media into Room. Idempotent —
     * reconciles each (sourceId, systemId) partition in place, so
     * surviving games keep their ids and user data.
     *
     * The caller observes [state] for progress; nothing is returned
     * from this call. The scan runs on `@ApplicationScope` so it
     * survives a FrontEndActivity.finish() that would otherwise cancel
     * a viewModelScope-launched scan.
     */
    fun scanAll() {
        if (_state.value is LibraryState.Scanning) return
        _state.value = LibraryState.Scanning(systemsDone = 0, systemsTotal = 0)
        launchGuarded {
            runScans(sourceDao.getAll(), systemsFilter = null)
        }
    }

    /**
     * Scan one system across every source. Useful after a scrape —
     * only the one system's media needs reindexing, and reconciling
     * the whole library would be wasteful.
     */
    fun scanSystem(systemId: String) {
        if (_state.value is LibraryState.Scanning) return
        _state.value = LibraryState.Scanning(systemsDone = 0, systemsTotal = 1, currentSystemId = systemId)
        launchGuarded {
            runScans(sourceDao.getAll(), systemsFilter = setOf(systemId))
        }
    }

    private fun launchGuarded(block: suspend () -> Unit): Job {
        val job = scope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _state.value = LibraryState.Error(e.message ?: e.javaClass.simpleName)
            }
        }
        job.invokeOnCompletion { cause ->
            // Reset to Idle unless an Error state was already set. Idle
            // is the valid steady-state for a cancelled scan too — the
            // state machine's "nothing in flight" rather than "error".
            if (_state.value !is LibraryState.Error) {
                _state.value = LibraryState.Idle
            }
            if (cause != null && cause !is kotlinx.coroutines.CancellationException) {
                Log.w(TAG, "Scan ended with $cause")
            }
        }
        return job
    }

    private suspend fun runScans(sources: List<SourceEntity>, systemsFilter: Set<String>?) {
        if (sources.isEmpty()) return

        // Pre-populate SystemEntity rows for every catalog system
        // referenced by the scan. GameEntity has an FK to SystemEntity,
        // so a game whose system doesn't exist yet would fail to
        // insert — this is the "catalog known at scan time" guarantee.
        upsertKnownSystems()

        val catalogSystems = catalog.all().associateBy { it.id.lowercase() }
        val knownFolders = catalogSystems.values.flatMap { def ->
            buildSet {
                add(def.id)
                addAll(def.folderAliases)
            }
        }.toSet()

        val mediaIndex = MediaIndex.build(
            mediaRoots = mediaRoots,
            layouts = listOf(EsdeLayout(), JarngreiprLayout()),
            systems = knownFolders,
        )

        var totalSystems = 0
        var done = 0
        for (source in innermostFirst(sources)) {
            if (!source.hasPersistedGrant) continue
            val filter = withoutCoveredSystems(source, sources, systemsFilter, catalogSystems.keys)
            if (filter?.isEmpty() == true) continue
            // No results is never treated as "everything was deleted":
            // an unmounted card or lost grant looks the same.
            val report = scanner.scanWithReport(source, systems = filter)
            if (report.roms.isEmpty()) continue

            val bySystem = report.roms.groupBy { it.systemId }
            totalSystems += bySystem.size

            for ((systemId, roms) in bySystem) {
                _state.value = LibraryState.Scanning(
                    systemsDone = done,
                    systemsTotal = totalSystems,
                    currentSystemId = systemId,
                )
                writeSystem(
                    sourceId = source.id,
                    systemId = systemId,
                    roms = roms,
                    removeMissing = report.isComplete(systemId),
                    mediaIndex = mediaIndex,
                    catalogSystems = catalogSystems,
                )
                done++
            }
            if (report.listingTrusted && report.unreadableSystems.isEmpty()) {
                sourceDao.markScanned(source.id, System.currentTimeMillis())
            }
        }
        mergeOverlappingGames()
    }

    /**
     * Drops the systems a folder inside [source] already holds, so a library
     * root and a system folder within it never both list the same games.
     * Returns [systemsFilter] unchanged when nothing is covered.
     */
    private suspend fun withoutCoveredSystems(
        source: SourceEntity,
        sources: List<SourceEntity>,
        systemsFilter: Set<String>?,
        catalogIds: Set<String>,
    ): Set<String>? {
        val covered = systemsCoveredInside(source, sources, gameDao.getSourceCounts())
            .mapTo(HashSet()) { it.lowercase() }
        if (covered.isEmpty()) return systemsFilter
        val wanted = systemsFilter?.mapTo(HashSet()) { it.lowercase() } ?: catalogIds
        return wanted - covered
    }

    /** Folds copies of one game seen through overlapping folders into one row. Safe to repeat. */
    suspend fun mergeOverlappingGames() {
        val duplicates = gameDao.getSharedPathGames()
        if (duplicates.isEmpty()) return
        val merges = planOverlapMerges(duplicates, sourceDao.getAll())
        if (merges.isNotEmpty()) gameDao.mergeDuplicates(merges)
    }

    /** Scanned sources left empty because system folders inside them hold all their games. */
    suspend fun redundantParentSources(): List<SourceEntity> =
        findRedundantParents(sourceDao.getAll(), gameDao.getSourceCounts())

    suspend fun removeSource(id: Long) = sourceDao.deleteById(id)

    /**
     * Scans only [targets], for Jarn FE's folder setup. [scanAll] and its
     * path are untouched. Returns false when a scan is already running.
     */
    fun scanSources(targets: List<SourceScanTarget>): Boolean {
        if (targets.isEmpty() || _state.value is LibraryState.Scanning) return false
        _state.value = LibraryState.Scanning(systemsDone = 0, systemsTotal = targets.size)
        launchGuarded { runTargets(targets) }
        return true
    }

    private suspend fun runTargets(targets: List<SourceScanTarget>) {
        upsertKnownSystems()
        val catalogSystems = catalog.all().associateBy { it.id.lowercase() }
        val mediaIndex = MediaIndex.build(
            mediaRoots = mediaRoots,
            layouts = listOf(EsdeLayout(), JarngreiprLayout()),
            systems = catalogSystems.values.flatMapTo(HashSet()) { it.folderAliases + it.id },
        )
        var total = 0
        var done = 0
        for (target in targets) {
            val report = scanTarget(target, catalogSystems.values)
            if (report.roms.isEmpty()) continue
            val bySystem = report.roms.groupBy { it.systemId }
            total += bySystem.size
            for ((systemId, roms) in bySystem) {
                _state.value = LibraryState.Scanning(done, total, systemId)
                writeSystem(
                    sourceId = target.source.id,
                    systemId = systemId,
                    roms = roms,
                    removeMissing = report.isComplete(systemId),
                    mediaIndex = mediaIndex,
                    catalogSystems = catalogSystems,
                )
                done++
            }
            if (report.listingTrusted && report.unreadableSystems.isEmpty()) {
                sourceDao.markScanned(target.source.id, System.currentTimeMillis())
            }
        }
        mergeOverlappingGames()
    }

    private suspend fun scanTarget(
        target: SourceScanTarget,
        systems: Collection<SystemDef>,
    ): ScanReport {
        target.systemFolder?.let { def ->
            if (def.id in target.excludedSystems) return ScanReport(roms = emptyList())
            return scanner.scanSystemFolder(target.source, def)
        }
        val filter = target.excludedSystems.takeIf { it.isNotEmpty() }
            ?.let { excluded -> systems.mapTo(HashSet()) { it.id } - excluded }
        return scanner.scanWithReport(target.source, systems = filter)
    }

    private suspend fun upsertKnownSystems() {
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

    private suspend fun writeSystem(
        sourceId: Long,
        systemId: String,
        roms: List<ScannedRom>,
        removeMissing: Boolean,
        mediaIndex: MediaIndex,
        catalogSystems: Map<String, SystemDef>,
    ) {
        val def = catalogSystems[systemId.lowercase()] ?: return
        val games = roms.map { rom ->
            GameEntity(
                sourceId = sourceId,
                systemId = def.id,
                relativePath = rom.relativePath,
                absolutePath = rom.absolutePath,
                // A folder game found by its markers keeps its whole name:
                // "Donkey Kong Bananza v1.0" has no ROM extension to strip.
                displayName = File(rom.relativePath).let { f ->
                    if (f.extension.lowercase() in def.extensions) f.nameWithoutExtension else f.name
                },
                fileSizeBytes = rom.fileSizeBytes.takeIf { it >= 0 },
            )
        }
        gameDao.reconcileSystemGames(
            sourceId = sourceId,
            systemId = def.id,
            scanned = games,
            removeMissing = removeMissing,
            keepImportedMetadata = keepImportedMetadata,
        )

        // Re-read for the ids of newly inserted games. Existing media
        // rows must be matched up too: an upsert with id 0 would hit the
        // unique gameId index and leave the old paths in place.
        val persisted = gameDao.getBySystemAndSource(def.id, sourceId)
        val existingMedia = persisted.map { it.id }.chunked(MEDIA_LOOKUP_CHUNK)
            .flatMap { mediaDao.getByGameIds(it) }
            .associateBy { it.gameId }
        // With a media root missing, "not found" says nothing about the
        // art, so known paths are kept until the root is back.
        val mediaRootsReadable = mediaRoots.isNotEmpty() && mediaRoots.all { it.isDirectory }
        val deletes = ArrayList<Long>()
        val upserts = ArrayList<MediaEntity>(persisted.size)
        for (game in persisted) {
            val resolved = mediaIndex.resolve(
                systemFolder = def.id,
                basename = File(game.relativePath).nameWithoutExtension,
            )
            val current = existingMedia[game.id]
            if (resolved == MediaIndex.Resolved.EMPTY) {
                if (mediaRootsReadable && current != null) deletes += game.id
                continue
            }
            val fallback = current.takeUnless { mediaRootsReadable }
            val row = MediaEntity(
                id = current?.id ?: 0L,
                gameId = game.id,
                coverPath = resolved.cover ?: fallback?.coverPath,
                marqueePath = resolved.marquee ?: fallback?.marqueePath,
                fanartPath = resolved.fanart ?: fallback?.fanartPath,
                screenshotPath = resolved.screenshot ?: fallback?.screenshotPath,
                titlescreenPath = resolved.titlescreen ?: fallback?.titlescreenPath,
                miximagePath = resolved.miximage ?: fallback?.miximagePath,
                physicalMediaPath = resolved.physicalMedia ?: fallback?.physicalMediaPath,
                videoPath = resolved.video ?: fallback?.videoPath,
            )
            if (row != current) upserts += row
        }
        // One transaction per system: one `media` invalidation, not
        // one per game (see MediaDao.replaceForSystem).
        mediaDao.replaceForSystem(deletes, upserts)
    }

    private companion object {
        const val TAG = "LibraryRepository"
        const val MEDIA_LOOKUP_CHUNK = 500
    }
}

