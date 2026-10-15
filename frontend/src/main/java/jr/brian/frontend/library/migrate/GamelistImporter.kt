package jr.brian.frontend.library.migrate

import android.util.Log
import android.util.Xml
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
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
) {

    /**
     * Parse [file] as a gamelist.xml and update every matching
     * GameEntity under [sourceId] / [systemId]. Returns the count of
     * rows updated.
     */
    suspend fun importFile(
        file: File,
        sourceId: Long,
        systemId: String,
    ): Int = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.isFile) return@withContext 0
        try {
            file.inputStream().use { input ->
                importStream(input, sourceId, systemId)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse gamelist ${file.absolutePath}", e)
            0
        }
    }

    internal suspend fun importStream(
        input: InputStream,
        sourceId: Long,
        systemId: String,
    ): Int {
        val entries = try {
            parseEntries(input)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse gamelist stream", e)
            return 0
        }
        var updated = 0
        for (entry in entries) {
            val existing = gameDao.findByTriple(sourceId, systemId, entry.relativePath)
                ?: continue
            val merged = existing.copy(
                displayName = entry.name ?: existing.displayName,
                description = entry.description ?: existing.description,
                rating = entry.rating ?: existing.rating,
                releaseDate = entry.releaseDate ?: existing.releaseDate,
                developer = entry.developer ?: existing.developer,
                publisher = entry.publisher ?: existing.publisher,
                genre = entry.genre ?: existing.genre,
                players = entry.players ?: existing.players,
                isFavorite = entry.favorite ?: existing.isFavorite,
                playCount = entry.playCount ?: existing.playCount,
                playTimeMinutes = entry.playTimeMinutes ?: existing.playTimeMinutes,
                lastPlayed = entry.lastPlayedEpochMillis ?: existing.lastPlayed,
            )
            if (merged != existing) {
                gameDao.update(merged)
                updated++
            }
        }
        return updated
    }

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
                    "lasttimeplayed" -> lastPlayedEpochMillis =
                        try { parseEsdeDate(text) } catch (_: Exception) { null }
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
    }
}
