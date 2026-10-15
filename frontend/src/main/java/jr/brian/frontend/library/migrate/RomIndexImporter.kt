package jr.brian.frontend.library.migrate

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports games from `:app`'s legacy `filesDir/rom_index/<system>.json`
 * cache into Room. Preserves per-game user flags (isFavorite,
 * playCount, playTimeMinutes, lastPlayed) and resolved media paths
 * so a user flipping onto the new engine keeps everything they
 * already have — the scrape, the favorites, the play history.
 *
 * Called by [LibraryMigration] as the second migration step. Depends
 * on step 1 having populated SourceEntity rows (so each GameEntity's
 * sourceId FK can resolve). The matching source is picked by
 * reasonable heuristics: a game's absolute path lives under the
 * source's file-path, OR the game's system name maps to the source
 * via a legacy SAF-tree-uri slot.
 */
@Singleton
class RomIndexImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val legacyPrefs: EsdeLegacyPrefs,
    private val sourceDao: SourceDao,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    private val catalog: SystemCatalog,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun run() = withContext(Dispatchers.IO) {
        val cacheDir = File(context.filesDir, LEGACY_ROM_INDEX_DIR)
        if (!cacheDir.exists() || !cacheDir.isDirectory) {
            Log.d(TAG, "No legacy rom_index directory at $cacheDir — nothing to import")
            return@withContext
        }
        val files = cacheDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            .orEmpty()
        if (files.isEmpty()) return@withContext

        val sources = sourceDao.getAll()
        if (sources.isEmpty()) {
            Log.i(TAG, "No sources registered yet; skipping game import")
            return@withContext
        }
        val safTreeUris = legacyPrefs.safTreeUris()

        var totalImported = 0
        for (file in files) {
            val entry = decodeOrNull(file) ?: continue
            val systemName = file.nameWithoutExtension
            val def = catalog.findByFolderAlias(systemName) ?: continue
            val source = resolveSource(entry, def.id, systemName, sources, safTreeUris)
                ?: continue
            totalImported += importOneSystem(source, def.id, entry.games)
        }
        Log.i(TAG, "Imported $totalImported games from legacy rom_index")
    }

    private suspend fun importOneSystem(
        source: SourceEntity,
        systemId: String,
        games: List<LegacyGame>,
    ): Int {
        var imported = 0
        for (game in games) {
            val relativePath = game.path.removePrefix("./").removePrefix("/").trim()
            if (relativePath.isBlank()) continue

            // Preserve the FK invariant by skipping any game whose
            // computed id would duplicate an existing row.
            val existing = gameDao.findByTriple(source.id, systemId, relativePath)
            val entity = GameEntity(
                id = existing?.id ?: 0L,
                sourceId = source.id,
                systemId = systemId,
                relativePath = relativePath,
                absolutePath = game.romAbsolutePath,
                displayName = game.name.ifBlank { File(relativePath).nameWithoutExtension },
                description = game.description,
                rating = game.rating.takeIf { it > 0f },
                releaseDate = game.releaseDate,
                developer = game.developer,
                publisher = game.publisher,
                genre = game.genre,
                players = game.players,
                isFavorite = game.isFavorite,
                playCount = game.playCount,
                playTimeMinutes = game.playTimeMinutes,
                lastPlayed = legacyLastPlayedToMillis(game.lastPlayed),
                emulatorPackage = game.emulatorPackage,
                launchCommand = game.launchCommand,
            )
            val gameId = if (existing == null) gameDao.insert(entity) else {
                gameDao.update(entity); existing.id
            }
            if (
                game.artworkPath != null || game.physicalMediaPath != null ||
                game.marqueeImagePath != null || game.screenshotPath != null ||
                game.fanartPath != null || game.titlescreenPath != null ||
                game.miximagePath != null
            ) {
                mediaDao.upsert(
                    MediaEntity(
                        gameId = gameId,
                        coverPath = game.artworkPath,
                        marqueePath = game.marqueeImagePath,
                        fanartPath = game.fanartPath,
                        screenshotPath = game.screenshotPath,
                        titlescreenPath = game.titlescreenPath,
                        miximagePath = game.miximagePath,
                        physicalMediaPath = game.physicalMediaPath,
                    )
                )
            }
            imported++
        }
        return imported
    }

    /**
     * Pick which SourceEntity to attribute these games to. Rules in
     * priority order:
     *  1. Match the game's absolute path to a file-path source.
     *  2. Use the legacy saf_tree_uris[systemName] entry if one
     *     existed and the matching tree URI is a source.
     *  3. Fall back to the first registered source (the plan's
     *     "best effort" — a user who only has one source gets
     *     everything attributed to it).
     */
    private fun resolveSource(
        entry: SystemCacheEntry,
        systemId: String,
        folderName: String,
        sources: List<SourceEntity>,
        safTreeUris: Map<String, String>,
    ): SourceEntity? {
        val firstAbsolute = entry.games.firstOrNull { it.romAbsolutePath != null }?.romAbsolutePath
        if (firstAbsolute != null) {
            val filePathSource = sources.firstOrNull { src ->
                !src.treeUri.startsWith("content://") && firstAbsolute.startsWith(src.treeUri)
            }
            if (filePathSource != null) return filePathSource
        }
        val safUri = safTreeUris[folderName] ?: safTreeUris[systemId]
        if (safUri != null) {
            val match = sources.firstOrNull { it.treeUri == safUri }
            if (match != null) return match
        }
        return sources.firstOrNull()
    }

    private fun decodeOrNull(file: File): SystemCacheEntry? = try {
        json.decodeFromString(SystemCacheEntry.serializer(), file.readText())
    } catch (e: Exception) {
        Log.w(TAG, "Failed to decode ${file.name}", e)
        null
    }

    /**
     * ES-DE writes `lastPlayed` as an ISO-ish string (`20250101T120000`).
     * We store it as epoch millis so the DB can order by it directly.
     * Null and unparseable values collapse to null — the UI uses null
     * as "never played".
     */
    private fun legacyLastPlayedToMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return try {
            parseEsdeDate(raw)
        } catch (_: Exception) {
            null
        }
    }

    @Serializable
    private data class SystemCacheEntry(
        val stamp: Stamp = Stamp(),
        val games: List<LegacyGame> = emptyList(),
    )

    @Serializable
    private data class Stamp(
        val systemDirLastModified: Long = 0L,
        val entryCount: Int = 0,
        val gamelistLastModified: Long = -1L,
        val decorationEnabled: Boolean = false,
        val romsRootUsed: String = "",
        val mappingTreeUris: List<String> = emptyList(),
        val resolvedExtensions: List<String> = emptyList(),
    )

    @Serializable
    private data class LegacyGame(
        val path: String,
        val name: String,
        val description: String? = null,
        val rating: Float = 0f,
        val releaseDate: String? = null,
        val developer: String? = null,
        val publisher: String? = null,
        val genre: String? = null,
        val players: String? = null,
        val isFavorite: Boolean = false,
        val playCount: Int = 0,
        val playTimeMinutes: Int = 0,
        val lastPlayed: String? = null,
        val systemName: String,
        val artworkPath: String? = null,
        val physicalMediaPath: String? = null,
        val marqueeImagePath: String? = null,
        val screenshotPath: String? = null,
        val fanartPath: String? = null,
        val titlescreenPath: String? = null,
        val miximagePath: String? = null,
        val emulatorPackage: String? = null,
        val romAbsolutePath: String? = null,
        val launchCommand: String? = null,
    )

    private companion object {
        const val TAG = "RomIndexImporter"
        const val LEGACY_ROM_INDEX_DIR = "rom_index"
    }
}

/**
 * Parses ES-DE's `20240115T123000` / `20240115T123000-0500` /
 * `20240115T000000` date-time string to epoch millis. Package-visible
 * so [GamelistImporter] can reuse it.
 */
internal fun parseEsdeDate(raw: String): Long {
    val cleaned = raw.trim()
    // Accept "yyyyMMdd" (no time) too, which some older gamelists write.
    val core = cleaned.substringBefore('-').substringBefore('+')
    val year = core.substring(0, 4).toInt()
    val month = core.substring(4, 6).toInt()
    val day = core.substring(6, 8).toInt()
    val tIdx = core.indexOf('T')
    val hour: Int; val min: Int; val sec: Int
    if (tIdx >= 0) {
        hour = core.substring(tIdx + 1, tIdx + 3).toInt()
        min = core.substring(tIdx + 3, tIdx + 5).toInt()
        sec = core.substring(tIdx + 5, tIdx + 7).toInt()
    } else {
        hour = 0; min = 0; sec = 0
    }
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    cal.clear()
    cal.set(year, month - 1, day, hour, min, sec)
    return cal.timeInMillis
}
