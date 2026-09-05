package jr.brian.home.esde.util

import android.util.Log
import android.util.Xml
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_COVERS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_FANART
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MARQUEES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MIXIMAGES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_PHYSICALMEDIA
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_SCREENSHOTS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_TITLESCREENS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS_WITH_SVG
import jr.brian.home.esde.util.ESDEMediaConstants.MARQUEE_FALLBACK_DIRS
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName
import org.xmlpull.v1.XmlPullParser
import java.io.File

/**
 * Case-fold + collapse variant dash characters (en-dash, em-dash, minus sign).
 * Public so [GamelistMetadataSource] and any future scraper source use the same
 * normalization when matching gamelist paths to scanned filenames.
 */
internal fun normalizeName(name: String): String {
    val sb = StringBuilder(name.length)
    for (ch in name) sb.append(if (ch == '–' || ch == '—' || ch == '−') '-' else ch)
    return sb.toString().lowercase()
}

/**
 * Reads ES-DE gamelist.xml files and joins each `<game>` entry to a scanned
 * ROM by relative path (exact → basename → normalized basename, in that order).
 * A gamelist entry with no matching scan is dropped — the scanner is the source
 * of truth, and a decoration we cannot attach to a real file is meaningless.
 *
 * @param esdeRootPath the ES-DE root directory (parent of `gamelists/`).
 * @param mediaPaths ordered list of scraped-media roots, primary first.
 */
class GamelistMetadataSource(
    private val esdeRootPath: String,
    private val mediaPaths: List<String>,
) : RomMetadataSource {

    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> {
        val gamelistFile = File(esdeRootPath, "gamelists/$systemName/gamelist.xml")
        if (!gamelistFile.exists() || scans.isEmpty()) return emptyMap()

        val entries = parseEntries(gamelistFile)
        if (entries.isEmpty()) return emptyMap()

        val matcher = ScanMatcher(scans)
        val result = HashMap<ScannedRom, RomMetadata>(entries.size)
        for (entry in entries) {
            val scan = matcher.match(entry.path)
            if (scan == null) {
                Log.d(TAG, "Dropping gamelist entry with no scanned file | system=$systemName path=${entry.path}")
                continue
            }
            result[scan] = entry.toMetadata(systemName, mediaPaths)
        }
        return result
    }

    /**
     * All `<game>` records from one gamelist.xml, minus rom-path resolution.
     * Kept as internal data classes so a future scraper source could reuse the
     * matcher without depending on ES-DE-specific field names.
     */
    private data class GamelistEntry(
        val path: String,
        val name: String,
        val description: String?,
        val rating: Float,
        val releaseDate: String?,
        val developer: String?,
        val publisher: String?,
        val genre: String?,
        val players: String?,
        val isFavorite: Boolean,
        val playCount: Int,
        val playTimeMinutes: Int,
        val lastPlayed: String?,
    ) {
        fun toMetadata(systemName: String, mediaPaths: List<String>): RomMetadata {
            val displayName = name.ifBlank { File(path).nameWithoutExtension }
            fun media(folder: String) = findFirstMedia(
                mediaPaths = mediaPaths,
                systemNames = listOf(systemName, getMediaSystemName(systemName)).distinct(),
                folders = listOf(folder),
                gameFilename = path,
                extensions = IMAGE_EXTENSIONS,
            )
            val marquee = findFirstMedia(
                mediaPaths = mediaPaths,
                systemNames = listOf(systemName, getMediaSystemName(systemName)).distinct(),
                folders = listOf(FOLDER_MARQUEES) + MARQUEE_FALLBACK_DIRS,
                gameFilename = path,
                extensions = IMAGE_EXTENSIONS_WITH_SVG,
            )
            return RomMetadata(
                displayName = displayName,
                description = description,
                rating = rating,
                releaseDate = releaseDate,
                developer = developer,
                publisher = publisher,
                genre = genre,
                players = players,
                isFavorite = isFavorite,
                playCount = playCount,
                playTimeMinutes = playTimeMinutes,
                lastPlayed = lastPlayed,
                artworkPath = media(FOLDER_COVERS),
                physicalMediaPath = media(FOLDER_PHYSICALMEDIA),
                marqueeImagePath = marquee,
                screenshotPath = media(FOLDER_SCREENSHOTS),
                fanartPath = media(FOLDER_FANART),
                titlescreenPath = media(FOLDER_TITLESCREENS),
                miximagePath = media(FOLDER_MIXIMAGES),
            )
        }
    }

    private fun parseEntries(gamelistFile: File): List<GamelistEntry> {
        val entries = mutableListOf<GamelistEntry>()
        try {
            val parser = Xml.newPullParser()
            gamelistFile.inputStream().use { input ->
                parser.setInput(input, "UTF-8")
                var inGame = false
                var path = ""
                var name = ""
                var desc: String? = null
                var rating = 0f
                var releaseDate: String? = null
                var developer: String? = null
                var publisher: String? = null
                var genre: String? = null
                var players: String? = null
                var favorite = false
                var playCount = 0
                var playTime = 0
                var lastPlayed: String? = null
                val buf = StringBuilder()
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            buf.clear()
                            if (parser.name == "game") {
                                inGame = true
                                path = ""; name = ""; desc = null; rating = 0f
                                releaseDate = null; developer = null; publisher = null
                                genre = null; players = null; favorite = false
                                playCount = 0; playTime = 0; lastPlayed = null
                            }
                        }
                        XmlPullParser.TEXT -> if (inGame) buf.append(parser.text)
                        XmlPullParser.END_TAG -> {
                            if (inGame) {
                                val text = buf.toString().trim()
                                when (parser.name) {
                                    "path" -> path = text.removePrefix("./")
                                    "name" -> name = text
                                    "desc" -> desc = text.ifBlank { null }
                                    "rating" -> rating = text.toFloatOrNull() ?: 0f
                                    "releasedate" -> releaseDate = text.ifBlank { null }
                                    "developer" -> developer = text.ifBlank { null }
                                    "publisher" -> publisher = text.ifBlank { null }
                                    "genre" -> genre = text.ifBlank { null }
                                    "players" -> players = text.ifBlank { null }
                                    "favorite" -> favorite = text.lowercase() == "true"
                                    "playcount" -> playCount = text.toIntOrNull() ?: 0
                                    "playtime" -> playTime = text.toIntOrNull() ?: 0
                                    "lastplayed" -> lastPlayed = text.ifBlank { null }
                                    "game" -> {
                                        if (path.isNotEmpty()) {
                                            entries += GamelistEntry(
                                                path = path,
                                                name = name,
                                                description = desc,
                                                rating = rating,
                                                releaseDate = releaseDate,
                                                developer = developer,
                                                publisher = publisher,
                                                genre = genre,
                                                players = players,
                                                isFavorite = favorite,
                                                playCount = playCount,
                                                playTimeMinutes = playTime,
                                                lastPlayed = lastPlayed,
                                            )
                                        }
                                        inGame = false
                                    }
                                }
                                buf.clear()
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing gamelist.xml: ${gamelistFile.absolutePath}", e)
        }
        return entries
    }

    /**
     * Path-precedence matcher: exact → basename → normalized basename. Deliberately
     * NOT fuzzy — `Foo (USA).iso` and `Foo (Europe).iso` are different discs, and
     * silently booting the wrong one is worse than showing an undecorated entry.
     * A scan is claimed by the first match; subsequent claims on the same scan
     * return null (a gamelist duplicate cannot decorate an already-attached scan).
     */
    private class ScanMatcher(scans: List<ScannedRom>) {
        private val claimed = HashSet<String>(scans.size)
        private val byRelative: Map<String, ScannedRom> =
            scans.associateBy { it.relativePath }
        private val byBasename: Map<String, ScannedRom> =
            scans.groupBy { File(it.relativePath).name }
                .mapValues { it.value.first() }
        private val byNormalizedBasename: Map<String, ScannedRom> =
            scans.groupBy { normalizeName(File(it.relativePath).name) }
                .mapValues { it.value.first() }

        fun match(gamelistPath: String): ScannedRom? {
            val candidate = byRelative[gamelistPath]
                ?: byBasename[File(gamelistPath).name]
                ?: byNormalizedBasename[normalizeName(File(gamelistPath).name)]
                ?: return null
            return if (claimed.add(candidate.absolutePath)) candidate else null
        }
    }

    companion object {
        private const val TAG = "GamelistMetadataSource"
    }
}
