package jr.brian.home.esde.launch

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.art.ArtSourceDetector
import jr.brian.frontend.library.art.GameSortStore
import jr.brian.frontend.library.art.libraryHiddenGameKey
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.SystemDao
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.addSystemFolderMapping
import jr.brian.home.esde.data.clearSafTreeUri
import jr.brian.home.esde.data.clearSystemCustomization
import jr.brian.home.esde.data.clearSystemEmulator
import jr.brian.home.esde.data.getSafTreeUri
import jr.brian.home.esde.data.removeSystemFolderMapping
import jr.brian.home.esde.data.setHiddenSystems
import jr.brian.home.esde.data.setSafTreeUri
import jr.brian.home.esde.data.setSystemCustomization
import jr.brian.home.esde.data.setSystemEmulator
import jr.brian.home.esde.data.unhideAllGames
import jr.brian.home.esde.model.SystemCustomization
import jr.brian.home.esde.model.SystemFolderMapping
import jr.brian.home.esde.util.compatibleEmulators
import jr.brian.home.esde.util.documentIdToStoragePath
import jr.brian.home.esde.util.persistSafTreeForSystem
import jr.brian.jarnfe.launch.JarnAppOption
import jr.brian.jarnfe.launch.JarnEmulatorOption
import jr.brian.jarnfe.launch.JarnGameOptions
import jr.brian.jarnfe.library.JarnGameSort
import jr.brian.jarnfe.systems.JarnAddedFolder
import jr.brian.jarnfe.systems.JarnFolderCheck
import jr.brian.jarnfe.systems.JarnHiddenGame
import jr.brian.jarnfe.systems.JarnManagedSystem
import jr.brian.jarnfe.systems.JarnSystemAppearance
import jr.brian.jarnfe.systems.JarnSystemChoice
import jr.brian.jarnfe.systems.JarnSystemManager
import jr.brian.jarnfe.systems.systemStorageKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

private const val ANDROID_APPS_SYSTEM = "androidapps"
private const val MAX_FOLDER_DEPTH = 6
private const val HIDDEN_KEY_CHUNK = 500

internal fun countBySystem(systemIds: List<String>): Map<String, Int> = systemIds.groupingBy { it }.eachCount()

/** The games in [games] that [hidden] hides, by title. */
internal fun hiddenGamesIn(games: List<GameEntity>, hidden: Set<String>): List<JarnHiddenGame> {
    if (hidden.isEmpty()) return emptyList()
    return games.mapNotNull { game ->
        libraryHiddenGameKey(game.systemId, game.absolutePath, game.relativePath)
            .takeIf { it in hidden }
            ?.let { JarnHiddenGame(it, game.displayName) }
    }.sortedBy { it.title.lowercase() }
}

/** Reads and writes Classic's system settings. Never starts a library scan itself. */
@Singleton
class AppJarnSystemManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val esdePrefs: ESDEPreferencesManager,
    private val systemDao: SystemDao,
    private val gameDao: GameDao,
    private val catalog: SystemCatalog,
    private val gameOptions: Provider<JarnGameOptions>,
    private val artDetector: Provider<ArtSourceDetector>,
    private val sortStore: GameSortStore,
) : JarnSystemManager {

    /** Folder choices live outside the observable prefs state. */
    private val folderRevision = MutableStateFlow(0)

    override val systems: Flow<List<JarnManagedSystem>> = combine(
        systemDao.observeAll(),
        gameDao.observeSourceCounts(),
        esdePrefs.state,
        folderRevision,
        hiddenGameCounts(),
    ) { systems, counts, prefs, _, hiddenCounts ->
        val bySystem = counts.groupBy { it.systemId }.mapValues { (_, rows) -> rows.sumOf { it.gameCount } }
        systems.filter { it.id != ANDROID_APPS_SYSTEM && (bySystem[it.id] ?: 0) > 0 }.map { system ->
            val keys = keysFor(system.id)
            val mapping = prefs.systemFolderMappings.firstOrNull { mapped ->
                keys.any { it.equals(mapped.systemName, ignoreCase = true) }
            }
            JarnManagedSystem(
                id = system.id,
                title = system.displayName,
                gameCount = bySystem[system.id] ?: 0,
                isHidden = system.id in prefs.hiddenSystems,
                appearance = keys.firstNotNullOfOrNull { prefs.systemCustomizations[it] }.toAppearance(),
                emulatorLabel = keys.firstNotNullOfOrNull { prefs.systemEmulatorMap[it] }?.let(::appLabel),
                folderPath = mapping?.displayPath
                    ?: keys.firstNotNullOfOrNull { esdePrefs.getSafTreeUri(it) }?.let(::readablePath),
                folderIsAdded = mapping != null,
                hiddenGameCount = hiddenCounts[system.id] ?: 0,
            )
        }
    }.flowOn(Dispatchers.IO)

    /** Counts only keys that still match a game, the same set [hiddenGames] lists. Stale keys stay saved. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun hiddenGameCounts(): Flow<Map<String, Int>> =
        esdePrefs.state.map { it.hiddenGames }.distinctUntilChanged().flatMapLatest { keys ->
            if (keys.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val chunks = keys.toList().chunked(HIDDEN_KEY_CHUNK).map(gameDao::observeSystemsOfHiddenKeys)
                combine(chunks) { parts -> countBySystem(parts.flatMap { it }) }
            }
        }

    override val addedFolders: Flow<List<JarnAddedFolder>> = esdePrefs.state.map { state ->
        state.systemFolderMappings.map { JarnAddedFolder(it.systemName, it.treeUri, it.displayPath) }
    }

    override fun setHidden(systemId: String, hidden: Boolean) {
        val keys = keysFor(systemId)
        val current = esdePrefs.state.value.hiddenSystems
        esdePrefs.setHiddenSystems(if (hidden) current + keys else current - keys.toSet())
    }

    /** Only touches the given systems, so unrelated hidden entries stay as they are. */
    override fun setAllHidden(systemIds: List<String>, hidden: Boolean) {
        val keys = systemIds.flatMapTo(HashSet(), ::keysFor)
        val current = esdePrefs.state.value.hiddenSystems
        esdePrefs.setHiddenSystems(if (hidden) current + keys else current - keys)
    }

    override fun setAppearance(systemId: String, appearance: JarnSystemAppearance) {
        val stored = SystemCustomization(
            backgroundUri = appearance.backgroundUri,
            showName = appearance.showName,
            solidColorArgb = appearance.solidColorArgb,
            focusBackgroundUri = appearance.focusBackgroundUri,
        )
        keysFor(systemId).forEach { esdePrefs.setSystemCustomization(it, stored) }
    }

    override fun resetAppearance(systemId: String) {
        keysFor(systemId).forEach { esdePrefs.clearSystemCustomization(it) }
    }

    override fun hiddenGames(systemId: String): Flow<List<JarnHiddenGame>> = combine(
        gameDao.observeBySystem(systemId),
        esdePrefs.state.map { it.hiddenGames }.distinctUntilChanged(),
    ) { games, hidden -> hiddenGamesIn(games, hidden) }.flowOn(Dispatchers.Default)

    override fun showGames(keys: Collection<String>) {
        esdePrefs.unhideAllGames(keys)
    }

    override val gameSorts: Flow<Map<String, JarnGameSort>> =
        sortStore.state.map { stored -> stored.mapValues { JarnGameSort.fromId(it.value) } }

    override fun setGameSort(systemId: String, sort: JarnGameSort) = sortStore.set(systemId, sort.name)

    override suspend fun emulators(systemId: String): List<JarnEmulatorOption>? =
        withContext(Dispatchers.IO) {
            val game = gameDao.observeBySystem(systemId).first().firstOrNull()
                ?: return@withContext null
            val extension = File(game.absolutePath ?: game.relativePath).extension
            compatibleEmulators(context, systemId, extension).map {
                JarnEmulatorOption(it.packageName, it.displayName, it.command, it.requiredExtension)
            }
        }

    override suspend fun installedApps(): List<JarnAppOption> = gameOptions.get().installedApps()

    override fun setEmulator(systemId: String, packageName: String) {
        keysFor(systemId).forEach { esdePrefs.setSystemEmulator(it, packageName) }
    }

    override fun clearEmulator(systemId: String) {
        keysFor(systemId).forEach { esdePrefs.clearSystemEmulator(it) }
    }

    override suspend fun checkFolder(systemId: String, treeUri: Uri): JarnFolderCheck =
        withContext(Dispatchers.IO) {
            val root = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull()
                ?.takeIf { it.canRead() }
                ?: return@withContext JarnFolderCheck.AccessLost
            val extensions = catalog.findById(systemId)?.extensions.orEmpty()
            val count = if (extensions.isEmpty()) 0 else countRoms(root, extensions, MAX_FOLDER_DEPTH)
            if (count == 0) JarnFolderCheck.NoGames else JarnFolderCheck.Found(count)
        }

    override suspend fun setFolder(systemId: String, treeUri: Uri): Boolean =
        withContext(Dispatchers.IO) { persistFolder(systemId, treeUri) != null }

    override fun clearFolder(systemId: String) {
        keysFor(systemId).forEach { esdePrefs.clearSafTreeUri(it) }
        folderRevision.update { it + 1 }
    }

    override suspend fun systemChoices(treeUri: Uri): List<JarnSystemChoice> =
        withContext(Dispatchers.Default) {
            val folderName = folderName(treeUri)
            val matched = folderName?.let { catalog.findByFolderAlias(it)?.id }
            catalog.all()
                .filter { it.id != ANDROID_APPS_SYSTEM }
                .map { JarnSystemChoice(it.id, it.displayName, it.id == matched) }
                .sortedByDescending { it.matchesFolder }
        }

    /** Saved the way Classic's Add Systems saves it, under the name that fits the folder. */
    override suspend fun addFolder(systemId: String, treeUri: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val storedName = persistFolder(systemId, treeUri) ?: return@withContext false
            esdePrefs.addSystemFolderMapping(
                SystemFolderMapping(
                    systemName = storedName,
                    treeUri = treeUri.toString(),
                    displayPath = readablePath(treeUri.toString()) ?: treeUri.toString(),
                ),
            )
            artDetector.get().check(mediaChanged = true)
            true
        }

    override fun removeFolder(folder: JarnAddedFolder) {
        esdePrefs.removeSystemFolderMapping(
            SystemFolderMapping(folder.systemName, folder.treeUri, folder.displayPath),
        )
        folderRevision.update { it + 1 }
    }

    private fun persistFolder(systemId: String, treeUri: Uri): String? {
        val keys = keysFor(systemId)
        val folder = folderName(treeUri)
        val storedName = keys.firstOrNull { it.equals(folder, ignoreCase = true) } ?: systemId
        val saved = runCatching {
            persistSafTreeForSystem(context, esdePrefs, storedName, treeUri, registerParentRoot = false)
        }.isSuccess
        if (!saved) return null
        keys.filter { it != storedName }.forEach { esdePrefs.setSafTreeUri(it, treeUri.toString()) }
        folderRevision.update { it + 1 }
        return storedName
    }

    private fun keysFor(systemId: String): List<String> =
        systemStorageKeys(systemId, catalog.findById(systemId)?.folderAliases.orEmpty())

    private fun folderName(treeUri: Uri): String? =
        runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
            ?.substringAfterLast(':')
            ?.trimEnd('/')
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }

    private fun readablePath(treeUri: String): String? {
        val uri = runCatching { treeUri.toUri() }.getOrNull() ?: return null
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: return treeUri
        return documentIdToStoragePath(documentId) ?: documentId
    }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    private fun countRoms(dir: DocumentFile, extensions: Set<String>, depth: Int): Int {
        if (depth < 0) return 0
        return runCatching { dir.listFiles() }.getOrDefault(emptyArray()).sumOf { child ->
            val name = child.name.orEmpty()
            val extension = name.substringAfterLast('.', "").lowercase()
            when {
                name.isEmpty() || name.startsWith(".") -> 0
                extension.isNotEmpty() && extension in extensions -> 1
                child.isDirectory -> countRoms(child, extensions, depth - 1)
                else -> 0
            }
        }
    }

    private fun SystemCustomization?.toAppearance(): JarnSystemAppearance =
        this?.let {
            JarnSystemAppearance(it.backgroundUri, it.focusBackgroundUri, it.showName, it.solidColorArgb)
        } ?: JarnSystemAppearance()
}
