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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
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
    /**
     * Optional directory where parsed gamelist entries are persisted. When
     * provided, a cold start can skip the XML parse for every system whose
     * gamelist.xml hasn't changed since the last run. Keep `null` for test
     * callers that don't want filesystem side effects.
     */
    private val parseCacheDir: File? = null,
) : RomMetadataSource {

    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> {
        val gamelistFile = File(esdeRootPath, "gamelists/$systemName/gamelist.xml")
        if (!gamelistFile.exists() || scans.isEmpty()) return emptyMap()

        val entries = loadEntriesWithCaches(systemName, gamelistFile)
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
     * In-memory → disk → XML parse, in that order. Each layer that hits
     * populates the ones above it so a hot second access is pure RAM.
     */
    private fun loadEntriesWithCaches(systemName: String, gamelistFile: File): List<GamelistEntry> {
        GamelistParseCache.get(gamelistFile)?.let { return it }
        parseCacheDir?.let { dir ->
            GamelistDiskCache.load(dir, systemName, gamelistFile)?.let { persisted ->
                GamelistParseCache.put(gamelistFile, persisted)
                return persisted
            }
        }
        val parsed = parseEntries(gamelistFile)
        GamelistParseCache.put(gamelistFile, parsed)
        parseCacheDir?.let { GamelistDiskCache.save(it, systemName, gamelistFile, parsed) }
        return parsed
    }

    /**
     * All `<game>` records from one gamelist.xml, minus rom-path resolution.
     * Kept as internal data classes so a future scraper source could reuse the
     * matcher without depending on ES-DE-specific field names.
     */
    @Serializable
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

    /**
     * Process-wide LRU of parsed `gamelist.xml` entries, keyed by absolute path
     * and invalidated when the file's `lastModified` or `length` drifts. ES-DE
     * rewrites the whole file on any gamelist edit, so one of those two always
     * moves when the content does — a stale cache cannot survive a real change.
     *
     * The on-disk XML is unchanged scan-to-scan in the common case, and parsing
     * it on every reconcile dominates frontend-startup cost for large systems.
     * Capacity is sized generously past the number of ES-DE systems a user is
     * realistically going to have.
     */
    private object GamelistParseCache {
        private const val MAX_ENTRIES = 64
        private data class Bucket(
            val lastModified: Long,
            val length: Long,
            val entries: List<GamelistEntry>,
        )

        private val lock = Any()
        private val map: LinkedHashMap<String, Bucket> =
            object : LinkedHashMap<String, Bucket>(16, 0.75f, true) {
                override fun removeEldestEntry(eldest: Map.Entry<String, Bucket>): Boolean =
                    size > MAX_ENTRIES
            }

        fun get(file: File): List<GamelistEntry>? = synchronized(lock) {
            val bucket = map[file.absolutePath] ?: return null
            if (bucket.lastModified == file.lastModified() && bucket.length == file.length()) {
                bucket.entries
            } else {
                map.remove(file.absolutePath)
                null
            }
        }

        fun put(file: File, entries: List<GamelistEntry>) = synchronized(lock) {
            map[file.absolutePath] = Bucket(
                lastModified = file.lastModified(),
                length = file.length(),
                entries = entries,
            )
        }
    }

    /**
     * On-disk persistence for parsed gamelist entries. Lives under
     * `<filesDir>/gamelist_parse/<sanitizedSystemName>.json` and carries the
     * originating gamelist.xml's absolute path, mtime and length as a header
     * so a stale cache can never shadow a changed file. ES-DE rewrites the
     * whole gamelist.xml on any edit, so mtime+length drift catches real
     * changes; the path check guards against a sanitized filename collision
     * between two ES-DE roots that happen to share a system name.
     */
    private object GamelistDiskCache {
        private const val DIR = "gamelist_parse"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        @Serializable
        private data class Record(
            val sourcePath: String,
            val lastModified: Long,
            val length: Long,
            val entries: List<GamelistEntry>,
        )

        private fun cacheFile(parseCacheDir: File, systemName: String): File {
            val safe = systemName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val dir = File(parseCacheDir, DIR).apply { if (!exists()) mkdirs() }
            return File(dir, "$safe.json")
        }

        fun load(parseCacheDir: File, systemName: String, gamelistFile: File): List<GamelistEntry>? {
            val file = cacheFile(parseCacheDir, systemName)
            if (!file.exists() || !file.isFile) return null
            val record = runCatching {
                json.decodeFromString(Record.serializer(), file.readText())
            }.onFailure { Log.w(TAG, "Failed to decode gamelist parse cache $file", it) }
                .getOrNull() ?: return null
            if (record.sourcePath != gamelistFile.absolutePath) return null
            if (record.lastModified != gamelistFile.lastModified()) return null
            if (record.length != gamelistFile.length()) return null
            return record.entries
        }

        fun save(
            parseCacheDir: File,
            systemName: String,
            gamelistFile: File,
            entries: List<GamelistEntry>,
        ) {
            val file = cacheFile(parseCacheDir, systemName)
            runCatching {
                val record = Record(
                    sourcePath = gamelistFile.absolutePath,
                    lastModified = gamelistFile.lastModified(),
                    length = gamelistFile.length(),
                    entries = entries,
                )
                file.writeText(json.encodeToString(Record.serializer(), record))
            }.onFailure { Log.w(TAG, "Failed to persist gamelist parse cache $file", it) }
        }
    }

    companion object {
        private const val TAG = "GamelistMetadataSource"
    }
}
