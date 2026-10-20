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
     * replaces each (sourceId, systemId) partition rather than
     * appending.
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
        for (source in sources) {
            if (!source.hasPersistedGrant) continue
            val scanned = scanner.scan(source, systems = systemsFilter)
            if (scanned.isEmpty()) continue

            val bySystem = scanned.groupBy { it.systemId }
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
                    mediaIndex = mediaIndex,
                    catalogSystems = catalogSystems,
                )
                done++
            }
            sourceDao.markScanned(source.id, System.currentTimeMillis())
        }
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
                displayName = File(rom.relativePath).nameWithoutExtension,
                fileSizeBytes = rom.fileSizeBytes.takeIf { it >= 0 },
            )
        }
        gameDao.replaceSystemGames(sourceId, def.id, games)

        // Resolve media per inserted row. We re-read the inserted ids
        // because replaceSystemGames did a delete+insert and the ids
        // the DAO assigned are not the ones we constructed with.
        val persisted = gameDao.getBySystemAndSource(def.id, sourceId)
        for (game in persisted) {
            val resolved = mediaIndex.resolve(
                systemFolder = def.id,
                basename = File(game.relativePath).nameWithoutExtension,
            )
            if (resolved == MediaIndex.Resolved.EMPTY) {
                mediaDao.deleteByGameId(game.id)
            } else {
                mediaDao.upsert(
                    MediaEntity(
                        gameId = game.id,
                        coverPath = resolved.cover,
                        marqueePath = resolved.marquee,
                        fanartPath = resolved.fanart,
                        screenshotPath = resolved.screenshot,
                        titlescreenPath = resolved.titlescreen,
                        miximagePath = resolved.miximage,
                        physicalMediaPath = resolved.physicalMedia,
                        videoPath = resolved.video,
                    )
                )
            }
        }
    }

    private companion object {
        const val TAG = "LibraryRepository"
    }
}

