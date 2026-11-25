package jr.brian.frontend.library.setup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.SourceScanTarget
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SourceGameCount
import jr.brian.frontend.library.migrate.EsdeImportProgress
import jr.brian.frontend.library.migrate.EsdeAltEmulatorImport
import jr.brian.frontend.library.migrate.EsdeAltEmulatorResult
import jr.brian.frontend.library.migrate.EsdeImportSummary
import jr.brian.frontend.library.migrate.LibraryMigration
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LibrarySetup"

/** Readies `:app`'s media roots, so a scan never clears art it could not see. */
fun interface LibraryScanPreparer {
    fun prepare()
}

sealed interface FolderSaveResult {
    data class Found(val systemCount: Int) : FolderSaveResult
    data object NoSystemsFound : FolderSaveResult
    data object AlreadyConnected : FolderSaveResult
    data object Failed : FolderSaveResult
}

/**
 * Connects game folders and scans the ones that have no games yet. Only those
 * sources are scanned, because a rescan replaces rows and would reset the
 * favorites and play stats of games already there. Reads folders only. The
 * stored grant flag changes only when the user grants a folder here.
 */
@Singleton
class LibrarySetup @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sourceDao: SourceDao,
    private val gameDao: GameDao,
    private val repository: LibraryRepository,
    private val catalog: SystemCatalog,
    private val preparer: LibraryScanPreparer,
    private val gamelists: LibraryMigration,
    private val esdeDetector: EsdeDetector,
    private val esdeHistory: EsdeImportHistory,
    private val esdeEmulators: EsdeAltEmulatorImport,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<LibrarySetupState>(LibrarySetupState.Checking)
    val state: StateFlow<LibrarySetupState> = _state.asStateFlow()

    private val _folders = MutableStateFlow<List<GameFolder>>(emptyList())
    val folders: StateFlow<List<GameFolder>> = _folders.asStateFlow()

    private val started = AtomicBoolean(false)
    private val grantsRevision = MutableStateFlow(0)
    private val attempted = MutableStateFlow(emptySet<String>())

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            combine(
                sourceDao.observeAll(),
                gameDao.observeSourceCounts(),
                repository.state,
                attempted,
                grantsRevision,
            ) { sources, counts, library, tried, _ ->
                SetupInputs(sources, counts, library, tried)
            }.collectLatest(::evaluate)
        }
        importGamelistsWhenIdle(force = false)
    }

    /** Grants can change while the app is away, so this runs on every return. */
    fun recheck() = grantsRevision.update { it + 1 }

    fun retry() {
        if (_state.value == LibrarySetupState.ScanFailed) attempted.value = emptySet()
        recheck()
        importGamelistsWhenIdle(force = true)
    }

    /** What ES-DE has, counted without importing. For Jarn's first-run offer. */
    suspend fun detectEsde(): EsdeDetection = esdeDetector.detect()

    /** How far a gamelist import is, or null between runs. */
    val esdeImportProgress: StateFlow<EsdeImportProgress?> get() = gamelists.gamelistProgress

    /** The last "Import from ES-DE" summary, kept across restarts. Null before the first. */
    val lastEsdeImport: StateFlow<EsdeImportSummary?> get() = esdeHistory.last

    /**
     * "Import from ES-DE": reads every gamelist now, changed or not, once
     * no scan is running. Fills gaps only, like every import. Without file
     * access nothing is read, and the summary says so. Only this user-started
     * import brings ES-DE's emulator picks over; start, scan and Refresh never do.
     */
    suspend fun importFromEsde(): EsdeImportSummary {
        val esde = esdeDetector.check()
        if (!esde.hasFileAccess) return EsdeImportSummary(esdeFound = esde.found, accessMissing = true)
        repository.state.first { it !is LibraryState.Scanning }
        val gamelistSummary = gamelists.reimportGamelists(force = true).copy(esdeFound = esde.found)
        val emulators = runCatching { esdeEmulators.run() }
            .onFailure { Log.w(TAG, "ES-DE emulator import failed", it) }
            .getOrDefault(EsdeAltEmulatorResult())
        val summary = gamelistSummary.withEmulators(emulators)
        esdeHistory.save(summary)
        return summary
    }

    /** Accepts a folder of systems or one system's folder. Nothing is saved on no match. */
    suspend fun saveFolder(uri: Uri): FolderSaveResult = withContext(Dispatchers.IO) {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return@withContext FolderSaveResult.Failed
        val treeUri = uri.toString()
        val match = detectSystems(folderName(treeUri), childFolders(treeUri), catalog)
        if (match.systemIds.isEmpty()) return@withContext FolderSaveResult.NoSystemsFound
        if (isAlreadyConnected(treeUri)) return@withContext FolderSaveResult.AlreadyConnected
        val granted = runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
        if (!granted) return@withContext FolderSaveResult.Failed
        val sourceId = repository.addSource(treeUri, documentId, documentId.substringBefore(':'))
        sourceDao.setGranted(sourceId, true)
        attempted.update { it - treeUri }
        recheck()
        FolderSaveResult.Found(match.systemIds.size)
    }

    private suspend fun isAlreadyConnected(treeUri: String): Boolean {
        val location = sourceLocation(treeUri)
        val sources = sourceDao.getAll()
        val access = classifySources(sources, context.grantedTrees(), ::canReadFolder)
        return (access.scannable + access.covered).any { source ->
            source.treeUri == treeUri || (
                location != null &&
                    sourceLocation(source.treeUri)?.let { locationContains(it, location) } == true
                )
        }
    }

    private suspend fun evaluate(inputs: SetupInputs) {
        val access = withContext(Dispatchers.IO) {
            classifySources(inputs.sources, context.grantedTrees(), ::canReadFolder)
        }
        _folders.value = gameFolders(access, inputs.counts)
        val sourcesWithGames = inputs.counts.mapTo(HashSet()) { it.sourceId }
        val targets = access.scannable.filter {
            it.lastScannedAt == 0L && it.id !in sourcesWithGames && it.treeUri !in inputs.attempted
        }
        _state.value = reduceSetup(
            access = access,
            library = inputs.library,
            hasGames = inputs.counts.isNotEmpty(),
            hasPendingScan = targets.isNotEmpty(),
        )
        if (targets.isEmpty() || inputs.library is LibraryState.Scanning) return
        val plans = withContext(Dispatchers.IO) {
            targets.map { planScan(it, inputs.sources, inputs.counts) }
        }
        preparer.prepare()
        if (repository.scanSources(plans)) {
            attempted.update { tried -> tried + targets.map { it.treeUri } }
            importGamelistsWhenIdle(force = true)
        }
    }

    /**
     * Brings over ES-DE's favorites, play stats and metadata once no scan is
     * running. It never removes a favorite or lowers a play stat set here.
     * At start only changed gamelists are read; after a scan or a refresh
     * every one is, since new games can match an unchanged file.
     */
    private fun importGamelistsWhenIdle(force: Boolean) {
        scope.launch {
            repository.state.first { it !is LibraryState.Scanning }
            if (!repository.keepsImportedMetadata) return@launch
            runCatching { gamelists.reimportGamelists(force) }
                .onFailure { Log.w(TAG, "Gamelist import failed", it) }
        }
    }

    /** Skips systems a folder inside this one already holds, so no game appears twice. */
    private fun planScan(
        source: SourceEntity,
        sources: List<SourceEntity>,
        counts: List<SourceGameCount>,
    ): SourceScanTarget {
        val match = detectSystems(folderName(source.treeUri), childFolders(source.treeUri), catalog)
        val location = sourceLocation(source.treeUri)
        val insideIds = sources.filter { other ->
            val otherLocation = sourceLocation(other.treeUri)
            other.id != source.id && location != null && otherLocation != null &&
                locationContains(location, otherLocation)
        }.mapTo(HashSet()) { it.id }
        return SourceScanTarget(
            source = source,
            systemFolder = match.singleSystem,
            excludedSystems = counts.filter { it.sourceId in insideIds }
                .mapTo(HashSet()) { it.systemId },
        )
    }

    private fun gameFolders(access: SourceAccess, counts: List<SourceGameCount>): List<GameFolder> {
        val bySource = counts.groupBy { it.sourceId }.mapValues { (_, rows) ->
            rows.sumOf { it.gameCount }
        }
        fun List<SourceEntity>.toFolders(status: GameFolderStatus) = map {
            GameFolder(it.displayName(), it.treeUri, status, bySource[it.id] ?: 0)
        }
        return (
            access.unreadable.toFolders(GameFolderStatus.NeedsReconnect) +
                access.scannable.toFolders(GameFolderStatus.Connected) +
                access.covered.toFolders(GameFolderStatus.InsideAnother)
            ).sortedBy { it.name.lowercase() }
    }

    private fun childFolders(treeUri: String): List<String> =
        if (treeUri.startsWith("content://")) {
            safChildFolders(treeUri)
        } else {
            File(treeUri).listFiles()?.filter { it.isDirectory }?.map { it.name }.orEmpty()
        }

    private fun safChildFolders(treeUri: String): List<String> = runCatching {
        val uri = treeUri.toUri()
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            uri,
            DocumentsContract.getTreeDocumentId(uri),
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(0) ?: continue
                    if (cursor.getString(1) == DocumentsContract.Document.MIME_TYPE_DIR) add(name)
                }
            }
        }.orEmpty()
    }.getOrDefault(emptyList())

    private data class SetupInputs(
        val sources: List<SourceEntity>,
        val counts: List<SourceGameCount>,
        val library: LibraryState,
        val attempted: Set<String>,
    )
}
