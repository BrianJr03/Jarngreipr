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
): List<GamelistJob> {
    val sourcesBySystem = counts.filter { it.gameCount > 0 }
        .groupBy({ it.systemId }, { it.sourceId })
    val jobs = LinkedHashMap<Pair<Long, String>, GamelistJob>()

    for (esdeDir in esdeDirs) {
        for ((systemId, file) in gamelistsIn(File(esdeDir, GAMELISTS_DIR), systemFor)) {
            for (sourceId in sourcesBySystem[systemId].orEmpty()) {
                jobs.putIfAbsent(sourceId to systemId, GamelistJob(file, sourceId, systemId))
            }
        }
    }

    for (source in sources) {
        if (source.treeUri.startsWith(CONTENT_SCHEME)) continue
        for ((systemId, file) in gamelistsIn(File(source.treeUri, GAMELISTS_DIR), systemFor)) {
            jobs.putIfAbsent(source.id to systemId, GamelistJob(file, source.id, systemId))
        }
    }
    return jobs.values.toList()
}

private fun gamelistsIn(gamelistsDir: File, systemFor: (String) -> String?): List<Pair<String, File>> {
    val systemDirs = runCatching { gamelistsDir.listFiles() }.getOrNull().orEmpty()
    return systemDirs.filter { it.isDirectory }
        .sortedBy { it.name }
        .mapNotNull { dir ->
            val systemId = systemFor(dir.name) ?: return@mapNotNull null
            val file = File(dir, GAMELIST_FILE)
            if (file.isFile) systemId to file else null
        }
        .distinctBy { it.first }
}
