package jr.brian.frontend.library.migrate

import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SourceGameCount
import java.io.File

private const val GAMELISTS_DIR = "gamelists"
private const val GAMELIST_FILE = "gamelist.xml"
private const val CONTENT_SCHEME = "content://"

/** One gamelist.xml to apply to the games one source holds for one system. */
internal data class GamelistJob(val file: File, val sourceId: Long, val systemId: String)

/**
 * The gamelists to apply, plus the ES-DE ones no known system uses.
 *
 * @property notRecognised ES-DE gamelist folder names no known system uses.
 */
internal data class GamelistPlan(
    val jobs: List<GamelistJob>,
    val notRecognised: List<String> = emptyList(),
)

/** A system folder in a `gamelists` folder that holds a gamelist.xml. */
internal data class GamelistFolder(val folderName: String, val file: File)

/**
 * Where each source's gamelists come from. ES-DE keeps them in its own folder
 * (`ES-DE/gamelists/<system>/gamelist.xml`), apart from the ROMs, so those apply
 * to every source holding games for that system, SAF sources included. A
 * `gamelists` folder inside a file-path source is still read, but only for a
 * source and system the ES-DE folder didn't already cover, so no gamelist is
 * applied twice in one run.
 *
 * @param esdeDirs ES-DE folders in priority order. The first one with a
 *   gamelist for a system wins.
 * @param systemFor the catalog system id for a gamelist folder name, or null.
 */
internal fun planGamelistImports(
    esdeDirs: List<File>,
    sources: List<SourceEntity>,
    counts: List<SourceGameCount>,
    systemFor: (String) -> String?,
): GamelistPlan {
    val sourcesBySystem = counts.filter { it.gameCount > 0 }
        .groupBy({ it.systemId }, { it.sourceId })
    val jobs = LinkedHashMap<Pair<Long, String>, GamelistJob>()
    val notRecognised = sortedSetOf<String>()

    for (esdeDir in esdeDirs) {
        for (folder in gamelistFolders(esdeDir)) {
            val systemId = systemFor(folder.folderName)
            if (systemId == null) {
                notRecognised += folder.folderName
                continue
            }
            for (sourceId in sourcesBySystem[systemId].orEmpty()) {
                jobs.putIfAbsent(sourceId to systemId, GamelistJob(folder.file, sourceId, systemId))
            }
        }
    }

    for (source in sources) {
        if (source.treeUri.startsWith(CONTENT_SCHEME)) continue
        for (folder in gamelistFoldersIn(File(source.treeUri, GAMELISTS_DIR))) {
            val systemId = systemFor(folder.folderName) ?: continue
            jobs.putIfAbsent(source.id to systemId, GamelistJob(folder.file, source.id, systemId))
        }
    }
    return GamelistPlan(jobs.values.toList(), notRecognised.toList())
}

/** The gamelists in an ES-DE folder, by system folder name. */
internal fun gamelistFolders(esdeDir: File): List<GamelistFolder> =
    gamelistFoldersIn(File(esdeDir, GAMELISTS_DIR))

private fun gamelistFoldersIn(gamelistsDir: File): List<GamelistFolder> {
    val systemDirs = runCatching { gamelistsDir.listFiles() }.getOrNull().orEmpty()
    return systemDirs.filter { it.isDirectory }
        .sortedBy { it.name }
        .mapNotNull { dir ->
            val file = File(dir, GAMELIST_FILE)
            if (file.isFile) GamelistFolder(dir.name, file) else null
        }
}
