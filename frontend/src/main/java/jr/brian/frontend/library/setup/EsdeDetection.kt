package jr.brian.frontend.library.setup

import android.os.Environment
import jr.brian.frontend.library.migrate.gamelistFolders
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private val GameEntry = Regex("""<game[\s>]""", RegexOption.IGNORE_CASE)
private val FavoriteEntry = Regex("""<favorite>\s*true\s*</favorite>""", RegexOption.IGNORE_CASE)

/**
 * What ES-DE has, counted before anything is imported. The counts are
 * gamelist entries, so they include games that aren't in this library.
 *
 * @property found an ES-DE folder is there. Known even without file access.
 * @property hasFileAccess all-files access is granted. Without it the counts
 *   are 0, because ES-DE's folder can't be read.
 * @property systems gamelists for systems Jarn knows.
 * @property games game entries in those gamelists.
 * @property favorites favorites among those entries.
 */
data class EsdeDetection(
    val found: Boolean,
    val hasFileAccess: Boolean,
    val systems: Int = 0,
    val games: Int = 0,
    val favorites: Int = 0,
) {
    /** Worth offering an import: ES-DE is there, and either unreadable yet or has games. */
    val worthOffering: Boolean get() = found && (!hasFileAccess || games > 0)
}

/**
 * Counts ES-DE's gamelists for the first-run offer. Reads each file as text
 * and counts tags, without an XML parse or any database work. Only reads:
 * ES-DE's files are never written.
 */
@Singleton
class EsdeDetector internal constructor(
    private val esde: EsdeRomsFolderFinder,
    private val catalog: SystemCatalog,
    private val hasFileAccess: () -> Boolean,
) {
    @Inject
    constructor(esde: EsdeRomsFolderFinder, catalog: SystemCatalog) :
        this(esde, catalog, Environment::isExternalStorageManager)

    /** Whether ES-DE is there and readable, without counting anything. */
    suspend fun check(): EsdeDetection = withContext(Dispatchers.IO) {
        EsdeDetection(
            found = runCatching { esde.hasEsde() }.getOrDefault(false),
            hasFileAccess = runCatching { hasFileAccess() }.getOrDefault(false),
        )
    }

    suspend fun detect(): EsdeDetection = withContext(Dispatchers.IO) {
        val checked = check()
        val found = checked.found
        val access = checked.hasFileAccess
        if (!found || !access) return@withContext checked

        val seenSystems = HashSet<String>()
        var games = 0
        var favorites = 0
        for (esdeDir in esde.esdeFolders()) {
            for (folder in gamelistFolders(esdeDir)) {
                val systemId = catalog.findByFolderAlias(folder.folderName)?.id ?: continue
                if (!seenSystems.add(systemId)) continue
                val (fileGames, fileFavorites) = countEntries(folder.file)
                games += fileGames
                favorites += fileFavorites
            }
        }
        EsdeDetection(found, access, seenSystems.size, games, favorites)
    }

    private fun countEntries(file: File): Pair<Int, Int> = runCatching {
        var games = 0
        var favorites = 0
        file.useLines { lines ->
            for (line in lines) {
                games += GameEntry.findAll(line).count()
                favorites += FavoriteEntry.findAll(line).count()
            }
        }
        games to favorites
    }.getOrDefault(0 to 0)
}
