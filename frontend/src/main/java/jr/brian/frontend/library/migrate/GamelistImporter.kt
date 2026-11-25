package jr.brian.frontend.library.migrate

import android.util.Log
import android.util.Xml
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.scan.scannedTitle
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses ES-DE's `gamelists/<system>/gamelist.xml` into updates for
 * existing [GameEntity] rows.
 *
 * ### What it does NOT do
 *
 * The importer **does not** create games from the gamelist — only the
 * scanner (or rom_index migration) decides which ROMs exist. A game
 * entry in the XML with no matching scanned ROM is silently ignored,
 * which matches ES-DE's own "the filesystem is authoritative"
 * convention. Prevents a stale gamelist from resurrecting games the
 * user already deleted.
 *
 * ### What it fills
 *
 * Metadata only fills gaps: a field is written when it is empty here,
 * and the name also when it is still the title a scan gives the file.
 * A name or description scraped or edited here always survives. The
 * same rule applies on every run. Favorites and play stats can only be
 * added or raised, never undone.
 *
 * ### Join key
 *
 * Match is on `(sourceId, systemId, relativePath)` after normalising
 * the ES-DE `./`-prefixed path. A `<path>./m3u/Game.cue</path>` entry
 * matches a GameEntity whose relativePath is `m3u/Game.cue`.
 *
 * ### Call site
 *
 * Phase 2d invokes the importer from LibraryMigration after
 * RomIndexImporter has populated the game rows, so there's always a
 * target to update. Phase 3+ callers (manual "Import ES-DE
 * gamelist" button) can call [importFile] directly with the file to
 * parse and the (sourceId, systemId) they want to attach to.
 */
@Singleton
class GamelistImporter @Inject constructor(
    private val gameDao: GameDao,
    private val catalog: SystemCatalog,
) {

    /**
     * Parse [file] as a gamelist.xml and update every matching
     * GameEntity under [sourceId] / [systemId]. Returns what changed,
     * or null when the file can't be read or parsed.
     *
     * @param seenFavorites paths whose gamelist favorite was already
     *   applied by an earlier import. Those aren't applied again, so a
     *   game the user unfavorited here stays unfavorited while the XML
     *   still says true. Updated in place after a successful parse.
     */
    suspend fun importFile(
        file: File,
        sourceId: Long,
        systemId: String,
        seenFavorites: MutableSet<String>? = null,
    ): GamelistFileCounts? = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) return@withContext null
        try {
            file.inputStream().use { input ->
                importStreamOrNull(input, sourceId, systemId, seenFavorites)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read gamelist ${file.absolutePath}", e)
            null
        }
    }

    internal suspend fun importStream(
        input: InputStream,
        sourceId: Long,
        systemId: String,
        seenFavorites: MutableSet<String>? = null,
    ): Int = importStreamOrNull(input, sourceId, systemId, seenFavorites)?.gamesUpdated ?: 0

    internal suspend fun importStreamOrNull(
        input: InputStream,
        sourceId: Long,
        systemId: String,
        seenFavorites: MutableSet<String>?,
    ): GamelistFileCounts? {
        val entries = try {
            parseEntries(input)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse gamelist stream", e)
            return null
        }
        val romExtensions = catalog.extensionsFor(systemId)
        var counts = GamelistFileCounts()
        val matchedFavorites = HashSet<String>()
        for (entry in entries) {
            val existing = gameDao.findByTriple(sourceId, systemId, entry.relativePath)
                ?: continue
            val xmlFavorite = entry.favorite == true
            if (xmlFavorite) matchedFavorites += entry.relativePath
            val isNewFavorite = xmlFavorite && seenFavorites?.contains(entry.relativePath) != true
            val scanTitle = scannedTitle(existing.relativePath, romExtensions)
            val merged = existing.copy(
                displayName = existing.displayName
                    .takeUnless { it.isBlank() || it == scanTitle }
                    ?: entry.name
                    ?: existing.displayName,
                description = existing.description.orGap(entry.description),
                rating = existing.rating?.takeIf { it > 0f } ?: entry.rating?.takeIf { it > 0f } ?: existing.rating,
                releaseDate = existing.releaseDate.orGap(entry.releaseDate),
                developer = existing.developer.orGap(entry.developer),
                publisher = existing.publisher.orGap(entry.publisher),
                genre = existing.genre.orGap(entry.genre),
                players = existing.players.orGap(entry.players),
                // The XML can be older than this app's state, so it can
                // add a favorite or raise a stat but never undo either.
                isFavorite = existing.isFavorite || isNewFavorite,
                playCount = maxOf(existing.playCount, entry.playCount ?: 0),
                playTimeMinutes = maxOf(existing.playTimeMinutes, entry.playTimeMinutes ?: 0),
                lastPlayed = listOfNotNull(existing.lastPlayed, entry.lastPlayedEpochMillis).maxOrNull(),
            )
            if (merged != existing) {
                gameDao.update(merged)
                counts += changeCounts(existing, merged)
            }
        }
        seenFavorites?.apply {
            clear()
            addAll(matchedFavorites)
        }
        return counts
    }

    private fun changeCounts(before: GameEntity, after: GameEntity) = GamelistFileCounts(
        gamesUpdated = 1,
        favoritesAdded = if (after.isFavorite && !before.isFavorite) 1 else 0,
        playStatsUpdated = if (
            after.playCount != before.playCount ||
            after.playTimeMinutes != before.playTimeMinutes ||
            after.lastPlayed != before.lastPlayed
        ) {
            1
        } else {
            0
        },
    )

    private fun parseEntries(input: InputStream): List<GamelistEntry> {
        val entries = mutableListOf<GamelistEntry>()
        val parser = Xml.newPullParser()
        parser.setInput(input, "UTF-8")

        var current: GamelistEntry.Builder? = null
        val buf = StringBuilder()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    buf.clear()
                    if (parser.name.equals("game", ignoreCase = true)) {
                        current = GamelistEntry.Builder()
                    }
                }
                XmlPullParser.TEXT -> buf.append(parser.text)
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase()
                    val text = buf.toString().trim()
                    val builder = current
                    when {
                        tag == "game" -> {
                            builder?.build()?.let { entries.add(it) }
                            current = null
                        }
                        builder != null -> builder.feed(tag, text)
                    }
                    buf.clear()
                }
            }
            event = parser.next()
        }
        return entries
    }

    private data class GamelistEntry(
        val relativePath: String,
        val name: String?,
        val description: String?,
        val rating: Float?,
        val releaseDate: String?,
        val developer: String?,
        val publisher: String?,
        val genre: String?,
        val players: String?,
        val favorite: Boolean?,
        val playCount: Int?,
        val playTimeMinutes: Int?,
        val lastPlayedEpochMillis: Long?,
    ) {
        class Builder {
            private var path: String? = null
            private var name: String? = null
            private var desc: String? = null
            private var rating: Float? = null
            private var releaseDate: String? = null
            private var developer: String? = null
            private var publisher: String? = null
            private var genre: String? = null
            private var players: String? = null
            private var favorite: Boolean? = null
            private var playCount: Int? = null
            private var playTimeMinutes: Int? = null
            private var lastPlayedEpochMillis: Long? = null

            fun feed(tag: String, text: String) {
                if (text.isEmpty()) return
                when (tag) {
                    "path" -> path = text
                    "name" -> name = text
                    "desc" -> desc = text
                    "rating" -> rating = text.toFloatOrNull()
                    "releasedate" -> releaseDate = text
                    "developer" -> developer = text
                    "publisher" -> publisher = text
                    "genre" -> genre = text
                    "players" -> players = text
                    "favorite" -> favorite = text.equals("true", ignoreCase = true)
                    "playcount" -> playCount = text.toIntOrNull()
                    "playtime" -> playTimeMinutes = text.toIntOrNull()?.let { it / 60 }
                    // ES-DE writes lastplayed. lasttimeplayed is an older spelling, kept as a fallback.
                    "lastplayed" -> lastPlayedEpochMillis = parseEsdeDateOrNull(text)
                    "lasttimeplayed" -> lastPlayedEpochMillis = lastPlayedEpochMillis ?: parseEsdeDateOrNull(text)
                }
            }

            fun build(): GamelistEntry? {
                val rel = path?.removePrefix("./")?.removePrefix("/")?.trim()
                if (rel.isNullOrEmpty()) return null
                return GamelistEntry(
                    relativePath = rel,
                    name = name,
                    description = desc,
                    rating = rating,
                    releaseDate = releaseDate,
                    developer = developer,
                    publisher = publisher,
                    genre = genre,
                    players = players,
                    favorite = favorite,
                    playCount = playCount,
                    playTimeMinutes = playTimeMinutes,
                    lastPlayedEpochMillis = lastPlayedEpochMillis,
                )
            }
        }
    }

    private companion object {
        const val TAG = "GamelistImporter"

        /** This value when it is set, otherwise the gamelist's. */
        fun String?.orGap(fromGamelist: String?): String? = takeUnless { it.isNullOrBlank() } ?: fromGamelist ?: this

        fun parseEsdeDateOrNull(text: String): Long? =
            try { parseEsdeDate(text) } catch (_: Exception) { null }
    }
}
