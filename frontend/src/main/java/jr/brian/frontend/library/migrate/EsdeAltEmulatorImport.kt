package jr.brian.frontend.library.migrate

import android.util.Log
import android.util.Xml
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.setup.EsdeRomsFolderFinder
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "EsdeAltEmulatorImport"
private val XmlProlog = Regex("""^\s*<\?xml[^>]*\?>""")

/** The key Jarn's launch path saves a game's emulator, core and command under. */
fun GameEntity.launchChoiceKey(): String = "$systemId/${absolutePath ?: relativePath}"

/** A game in a system, for writing a system choice per game. [rowEmulator] is its imported emulator. */
data class EsdeSystemGame(val key: String, val rowEmulator: String?)

/** ES-DE's emulator for a whole system. [esdeSystem] is ES-DE's folder name, which its labels belong to. */
data class EsdeSystemAltEmulator(
    val systemId: String,
    val esdeSystem: String,
    val label: String,
    val games: List<EsdeSystemGame>,
)

/** ES-DE's emulator for one game, keyed by [launchChoiceKey]. */
data class EsdeGameAltEmulator(
    val key: String,
    val esdeSystem: String,
    val label: String,
    val extension: String,
)

data class EsdeAltEmulators(
    val systems: List<EsdeSystemAltEmulator> = emptyList(),
    val games: List<EsdeGameAltEmulator> = emptyList(),
    /** Games a gamelist names that aren't in the library. */
    val gamesNotInLibrary: Int = 0,
)

/** [Unsupported]: the command needs something Jarn's launcher can't fill in yet. */
enum class EsdeAltEmulatorSkipReason { UnknownLabel, NotInstalled, Unsupported }

data class EsdeAltEmulatorSkip(val esdeSystem: String, val label: String, val reason: EsdeAltEmulatorSkipReason)

/** What an import did, for the ES-DE import summary. */
data class EsdeAltEmulatorResult(
    val systemsSet: Int = 0,
    val gamesSet: Int = 0,
    /** ES-DE choices not applied because a choice was already made in Jarngreipr. */
    val keptExisting: Int = 0,
    val gamesNotInLibrary: Int = 0,
    val skipped: List<EsdeAltEmulatorSkip> = emptyList(),
)

/**
 * Turns ES-DE labels into saved emulator choices. Implemented in `:app`, which owns ES-DE's command
 * knowledge and the launch prefs. Choices already saved there must never be replaced.
 */
fun interface EsdeAltEmulatorWriter {
    fun write(found: EsdeAltEmulators): EsdeAltEmulatorResult
}

/** What one gamelist.xml says. [gameLabels] maps a ROM path relative to the system folder to a label. */
internal data class GamelistAltEmulators(val systemLabel: String?, val gameLabels: Map<String, String>)

/**
 * Reads `<alternativeEmulator><label>` (the system) and `<game><altemulator>` (each game). ES-DE
 * writes `<alternativeEmulator>` as a second root next to `<gameList>`, so the text is wrapped in
 * one root before parsing. Throws on XML that is broken in any other way.
 */
internal fun parseAltEmulators(text: String): GamelistAltEmulators {
    val parser = Xml.newPullParser()
    parser.setInput(StringReader("<root>${text.removePrefix("\uFEFF").replace(XmlProlog, "")}</root>"))
    var systemLabel: String? = null
    val games = LinkedHashMap<String, String>()
    var inAlternative = false
    var inGame = false
    var path: String? = null
    var label: String? = null
    val buf = StringBuilder()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> {
                buf.clear()
                when (parser.name.lowercase()) {
                    "alternativeemulator" -> inAlternative = true
                    "game" -> { inGame = true; path = null; label = null }
                }
            }
            XmlPullParser.TEXT -> buf.append(parser.text)
            XmlPullParser.END_TAG -> {
                val value = buf.toString().trim()
                when (parser.name.lowercase()) {
                    "alternativeemulator" -> inAlternative = false
                    "label" -> if (inAlternative && value.isNotEmpty()) systemLabel = value
                    "path" -> if (inGame) path = value
                    "altemulator" -> if (inGame && value.isNotEmpty()) label = value
                    "game" -> {
                        val rel = path?.removePrefix("./")?.removePrefix("/")?.trim()
                        val chosen = label
                        if (!rel.isNullOrEmpty() && chosen != null) games[rel] = chosen
                        inGame = false
                    }
                }
                buf.clear()
            }
        }
        event = parser.next()
    }
    return GamelistAltEmulators(systemLabel, games)
}

/**
 * Brings over the emulators picked in ES-DE, from the same gamelists the metadata import reads.
 * Reads ES-DE's files only. Saving, and keeping Jarngreipr's own choices, is [EsdeAltEmulatorWriter]'s job.
 */
@Singleton
class EsdeAltEmulatorImport @Inject constructor(
    private val esdeFolders: EsdeRomsFolderFinder,
    private val sourceDao: SourceDao,
    private val gameDao: GameDao,
    private val catalog: SystemCatalog,
    private val writer: EsdeAltEmulatorWriter,
) {
    /**
     * Finds the gamelists the way the metadata import does, then imports them. Run it only from an
     * ES-DE import the user starts, never from app start or a scan. Choices made in Jarngreipr are
     * kept, but one the user cleared in Jarn has nothing to keep, so a re-import brings it back.
     */
    suspend fun run(): EsdeAltEmulatorResult = withContext(Dispatchers.IO) {
        val jobs = planGamelistImports(
            esdeDirs = runCatching { esdeFolders.esdeFolders() }.getOrDefault(emptyList()),
            sources = sourceDao.getAll(),
            counts = gameDao.getSourceCounts(),
            systemFor = { name -> catalog.findByFolderAlias(name)?.id },
        ).jobs
        importFrom(jobs)
    }

    internal suspend fun importFrom(jobs: List<GamelistJob>): EsdeAltEmulatorResult = withContext(Dispatchers.IO) {
        val found = collect(jobs)
        if (found.systems.isEmpty() && found.games.isEmpty()) {
            EsdeAltEmulatorResult(gamesNotInLibrary = found.gamesNotInLibrary)
        } else {
            writer.write(found).copy(gamesNotInLibrary = found.gamesNotInLibrary)
        }
    }

    private suspend fun collect(jobs: List<GamelistJob>): EsdeAltEmulators {
        val parsed = HashMap<File, GamelistAltEmulators?>()
        val systems = LinkedHashMap<String, EsdeSystemAltEmulator>()
        val games = LinkedHashMap<String, EsdeGameAltEmulator>()
        val unmatched = HashSet<Pair<File, String>>()
        val matched = HashSet<Pair<File, String>>()
        for (job in jobs) {
            val list = parsed.getOrPut(job.file) { readOrNull(job.file) } ?: continue
            val esdeSystem = job.file.parentFile?.name ?: continue
            for ((relativePath, label) in list.gameLabels) {
                val game = gameDao.findByTriple(job.sourceId, job.systemId, relativePath)
                if (game == null) {
                    unmatched += job.file to relativePath
                    continue
                }
                matched += job.file to relativePath
                val key = game.launchChoiceKey()
                games.putIfAbsent(key, EsdeGameAltEmulator(key, esdeSystem, label, File(relativePath).extension))
            }
            val systemLabel = list.systemLabel ?: continue
            if (job.systemId !in systems) {
                val rows = gameDao.getBySystem(job.systemId).map { EsdeSystemGame(it.launchChoiceKey(), it.emulatorPackage) }
                systems[job.systemId] = EsdeSystemAltEmulator(job.systemId, esdeSystem, systemLabel, rows)
            }
        }
        return EsdeAltEmulators(systems.values.toList(), games.values.toList(), (unmatched - matched).size)
    }

    private fun readOrNull(file: File): GamelistAltEmulators? = try {
        parseAltEmulators(file.readBytes().decodeToString())
    } catch (e: Exception) {
        Log.w(TAG, "Could not read emulator choices from ${file.path}", e)
        null
    }
}
