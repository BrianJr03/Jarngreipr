package jr.brian.home.esde.launch

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.art.libraryHiddenGameKey
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.home.esde.data.ESDEPreferencesManager
import jr.brian.home.esde.data.addGameCompanion
import jr.brian.home.esde.data.clearGameCompanions
import jr.brian.home.esde.data.clearGameLaunchCommand
import jr.brian.home.esde.data.getGameCompanions
import jr.brian.home.esde.data.getGameCore
import jr.brian.home.esde.data.getGameEmulator
import jr.brian.home.esde.data.getSystemEmulator
import jr.brian.home.esde.data.hideGame
import jr.brian.home.esde.data.setGameCore
import jr.brian.home.esde.data.setGameEmulator
import jr.brian.home.esde.data.setGameLaunchCommand
import jr.brian.home.esde.data.setGameLaunchMaps
import jr.brian.home.esde.data.setSystemEmulator
import jr.brian.home.esde.data.unhideGame
import jr.brian.home.esde.util.EsdeCommandLauncher
import jr.brian.home.esde.util.compatibleEmulators
import jr.brian.home.esde.util.documentIdToStoragePath
import jr.brian.jarnfe.launch.JarnAppOption
import jr.brian.jarnfe.launch.JarnCoreOption
import jr.brian.jarnfe.launch.JarnEmulatorChoice
import jr.brian.jarnfe.launch.JarnEmulatorOption
import jr.brian.jarnfe.launch.JarnGameOptions
import jr.brian.jarnfe.launch.JarnGameRef
import jr.brian.jarnfe.launch.JarnGameSetup
import jr.brian.jarnfe.systems.systemStorageKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val RETROARCH_PREFIX = "com.retroarch"
private const val LIBRARY_KEY_PARTS = 3

/** Joins a SAF document id and a relative path, including a volume root like `primary:`. */
internal fun childDocumentId(parentId: String, relativePath: String): String {
    val relative = relativePath.removePrefix("./").trimStart('/')
    return if (parentId.endsWith(":")) "$parentId$relative" else "$parentId/$relative"
}

internal data class LibraryKeyParts(val sourceId: Long, val systemId: String, val relativePath: String)

internal fun parseLibraryKey(key: String): LibraryKeyParts? {
    val parts = key.split(":", limit = LIBRARY_KEY_PARTS)
    if (parts.size != LIBRARY_KEY_PARTS) return null
    val sourceId = parts[0].toLongOrNull() ?: return null
    return LibraryKeyParts(sourceId, parts[1], parts[2])
}

/** Reads and saves per-game choices in the same storage Classic uses. */
@Singleton
class AppJarnGameOptions @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val esdePrefs: ESDEPreferencesManager,
    private val engine: RomLaunchEngine,
    private val gameDao: GameDao,
    private val sourceDao: SourceDao,
    private val catalog: SystemCatalog,
) : JarnGameOptions {

    /**
     * Games scanned from a picked folder have no file path, so Classic's path lookup would
     * report them missing. This finds the real file inside that folder.
     */
    override suspend fun locate(game: JarnGameRef, libraryKey: String): JarnGameRef {
        if (game.absolutePath != null) return game
        val key = parseLibraryKey(libraryKey) ?: return game
        return withContext(Dispatchers.IO) {
            val treeUri = sourceDao.findById(key.sourceId)?.treeUri
                ?.takeIf { it.startsWith("content://") }
                ?: return@withContext game
            val path = runCatching { findInTree(treeUri.toUri(), key) }.getOrNull()
            game.copy(absolutePath = path, sourceTreeUri = treeUri)
        }
    }

    private fun findInTree(treeUri: Uri, key: LibraryKeyParts): String? {
        if (treeUri.authority != EXTERNAL_STORAGE_AUTHORITY) return null
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val candidates = systemFolderIds(treeUri, rootId, key.systemId) + rootId
        return candidates.asSequence()
            .map { childDocumentId(it, key.relativePath) }
            .firstOrNull { documentExists(treeUri, it) }
            ?.let(::documentIdToStoragePath)
    }

    private fun systemFolderIds(treeUri: Uri, rootId: String, systemId: String): List<String> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, rootId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val found = mutableListOf<String>()
        context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(2) != DocumentsContract.Document.MIME_TYPE_DIR) continue
                val name = cursor.getString(1) ?: continue
                if (catalog.findByFolderAlias(name)?.id == systemId) found += cursor.getString(0)
            }
        }
        return found
    }

    private fun documentExists(treeUri: Uri, documentId: String): Boolean = runCatching {
        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        context.contentResolver.query(uri, projection, null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    override suspend fun load(game: JarnGameRef, libraryKey: String): JarnGameSetup =
        withContext(Dispatchers.IO) {
            val key = prefsKey(game)
            val extension = File(game.absolutePath ?: game.path).extension
            val emulators = compatibleEmulators(context, game.systemName, extension).map {
                JarnEmulatorOption(it.packageName, it.displayName, it.command, it.requiredExtension)
            }
            val current = esdePrefs.getGameEmulator(key)
                ?: esdePrefs.getSystemEmulator(game.systemName)
                ?: game.emulatorPackage
            val usesRetroArch = current?.startsWith(RETROARCH_PREFIX) == true
            JarnGameSetup(
                emulators = emulators,
                currentEmulator = current,
                currentEmulatorLabel = current?.let { pkg ->
                    emulators.firstOrNull { it.packageName == pkg }?.label ?: appLabel(pkg)
                },
                usesRetroArch = usesRetroArch,
                cores = if (usesRetroArch) installedCores() else emptyList(),
                currentCore = esdePrefs.getGameCore(key),
                companions = appOptions(esdePrefs.getGameCompanions(key)),
                isFavorite = findEntity(libraryKey)?.isFavorite == true,
            )
        }

    private fun installedCores(): List<JarnCoreOption> =
        EsdeCommandLauncher.getInstalledCores(context).map { (label, path) ->
            JarnCoreOption(label, path)
        }

    override suspend fun retroArchCores(): List<JarnCoreOption> =
        withContext(Dispatchers.IO) { installedCores() }

    override suspend fun systemChoiceConflicts(systemId: String, choice: JarnEmulatorChoice): Int =
        withContext(Dispatchers.Default) {
            countSystemConflicts(savedLaunchMaps(), keysFor(systemId), choice)
        }

    /** The system default, with every game's own choice replaced so it takes effect. */
    override suspend fun applyToSystem(systemId: String, choice: JarnEmulatorChoice) =
        withContext(Dispatchers.IO) {
            val keys = keysFor(systemId)
            val games = gameDao.getBySystem(systemId).map {
                SystemGameRow("$systemId/${it.absolutePath ?: it.relativePath}", it.emulatorPackage)
            }
            val planned = planSystemEmulator(savedLaunchMaps(), keys, choice, games)
            keys.forEach { esdePrefs.setSystemEmulator(it, choice.packageName) }
            esdePrefs.setGameLaunchMaps(planned.emulators, planned.commands, planned.cores)
        }

    private fun savedLaunchMaps(): GameEmulatorMaps = esdePrefs.state.value.let {
        GameEmulatorMaps(it.gameEmulatorMap, it.gameCommandMap, it.gameCoreMap)
    }

    private fun keysFor(systemId: String): List<String> =
        systemStorageKeys(systemId, catalog.findById(systemId)?.folderAliases.orEmpty())

    override suspend fun installedApps(): List<JarnAppOption> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcher, 0)
            .map { JarnAppOption(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    override fun appOptions(packages: List<String>): List<JarnAppOption> =
        packages.map { JarnAppOption(it, appLabel(it)) }

    private fun appLabel(packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    /** A choice without a command clears the old one, which would otherwise keep launching. */
    override fun chooseEmulator(game: JarnGameRef, option: JarnEmulatorOption) {
        val key = prefsKey(game)
        esdePrefs.setGameEmulator(key, option.packageName)
        option.command?.let { esdePrefs.setGameLaunchCommand(key, it) }
            ?: esdePrefs.clearGameLaunchCommand(key)
    }

    override fun chooseCore(game: JarnGameRef, corePath: String) {
        esdePrefs.setGameCore(prefsKey(game), corePath)
    }

    override fun addCompanion(game: JarnGameRef, packageName: String) {
        esdePrefs.addGameCompanion(prefsKey(game), packageName)
    }

    override fun clearCompanions(game: JarnGameRef) {
        esdePrefs.clearGameCompanions(prefsKey(game))
    }

    override fun openCompanion(host: Activity, packageName: String) {
        engine.launchCompanionPkg(packageName, host)
    }

    override suspend fun setFavorite(libraryKey: String, favorite: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val entity = findEntity(libraryKey) ?: return@withContext false
            gameDao.setFavorite(entity.id, favorite)
            true
        }

    override suspend fun setHidden(libraryKey: String, hidden: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val entity = findEntity(libraryKey) ?: return@withContext false
            val key = libraryHiddenGameKey(entity.systemId, entity.absolutePath, entity.relativePath)
            if (hidden) esdePrefs.hideGame(key) else esdePrefs.unhideGame(key)
            true
        }

    private suspend fun findEntity(libraryKey: String): GameEntity? {
        val key = parseLibraryKey(libraryKey) ?: return null
        return gameDao.findByTriple(key.sourceId, key.systemId, key.relativePath)
    }

    private fun prefsKey(game: JarnGameRef) = "${game.systemName}/${game.path}"
}
