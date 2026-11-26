package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val SQLITE_VARIABLE_CHUNK = 500

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE isHidden = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeAllVisible(): Flow<List<GameEntity>>

    @Query(
        """
        SELECT id, systemId, displayName, relativePath, absolutePath FROM games
        WHERE isHidden = 0 ORDER BY displayName COLLATE NOCASE
        """,
    )
    fun observeSearchRows(): Flow<List<GameSearchRow>>

    @Query("SELECT * FROM games WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<GameEntity>

    @Query("SELECT * FROM games ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<GameEntity>>

    /**
     * Projection-only join over games and media. Returns one row per
     * visible game with the browse-side columns [GameInfoRow]
     * documents. Avoids the two full-table materialisations that
     * `observeAllVisible` + `MediaDao.observeAll` used to combine in
     * [LibraryGameInfoAdapter], which exhausted the CursorWindow on
     * large libraries. Detail-only fields (`description`,
     * `releaseDate`, `players`) are deliberately excluded; they
     * dominated per-row cursor memory and have no browse-grid
     * readers.
     */
    @Query(
        """
        SELECT
            g.id AS gameId,
            g.systemId AS systemId,
            g.absolutePath AS absolutePath,
            g.relativePath AS relativePath,
            g.displayName AS displayName,
            g.rating AS rating,
            g.developer AS developer,
            g.publisher AS publisher,
            g.genre AS genre,
            g.isFavorite AS isFavorite,
            g.playCount AS playCount,
            g.playTimeMinutes AS playTimeMinutes,
            g.lastPlayed AS lastPlayed,
            g.emulatorPackage AS emulatorPackage,
            g.launchCommand AS launchCommand,
            m.coverPath AS coverPath,
            m.physicalMediaPath AS physicalMediaPath,
            m.marqueePath AS marqueePath,
            m.screenshotPath AS screenshotPath,
            m.fanartPath AS fanartPath,
            m.titlescreenPath AS titlescreenPath,
            m.miximagePath AS miximagePath
        FROM games g
        LEFT OUTER JOIN media m ON m.gameId = g.id
        WHERE g.isHidden = 0
        ORDER BY g.displayName COLLATE NOCASE
        """
    )
    fun observeAllVisibleWithMedia(): Flow<List<GameInfoRow>>

    @Query("SELECT * FROM games WHERE systemId = :systemId AND isHidden = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeBySystem(systemId: String): Flow<List<GameEntity>>

    /** Includes hidden games, for changes that must reach the whole system. */
    @Query("SELECT * FROM games WHERE systemId = :systemId")
    suspend fun getBySystem(systemId: String): List<GameEntity>

    @Query("SELECT * FROM games WHERE systemId = :systemId AND sourceId = :sourceId")
    suspend fun getBySystemAndSource(systemId: String, sourceId: Long): List<GameEntity>

    @Query("SELECT * FROM games WHERE isFavorite = 1 AND isHidden = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeFavorites(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL AND isHidden = 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<GameEntity>>

    @Query(
        """
        SELECT * FROM games
        WHERE systemId = :systemId AND lastPlayed IS NOT NULL AND isHidden = 0
        ORDER BY lastPlayed DESC LIMIT :limit
        """
    )
    fun observeRecentInSystem(systemId: String, limit: Int): Flow<List<GameEntity>>

    @Query(
        """
        SELECT * FROM games
        WHERE systemId = :systemId AND playCount > 0 AND isHidden = 0
        ORDER BY playCount DESC, playTimeMinutes DESC, displayName COLLATE NOCASE
        LIMIT :limit
        """
    )
    fun observeMostPlayedInSystem(systemId: String, limit: Int): Flow<List<GameEntity>>

    @Query(
        """
        SELECT * FROM games
        WHERE systemId = :systemId AND isHidden = 0
        ORDER BY displayName COLLATE NOCASE LIMIT :limit
        """
    )
    fun observePreviewInSystem(systemId: String, limit: Int): Flow<List<GameEntity>>

    /** Only rows with activity, so per-system totals never load a whole large library. */
    @Query(
        """
        SELECT systemId, absolutePath, relativePath, lastPlayed, playTimeMinutes, isFavorite
        FROM games
        WHERE isHidden = 0 AND (lastPlayed IS NOT NULL OR playTimeMinutes > 0 OR isFavorite = 1)
        """
    )
    fun observePlayRows(): Flow<List<GamePlayRow>>

    /**
     * The system of each listed game whose hidden-game key (`system/path`) is in [keys], one row per
     * game. Keys with no game left are skipped. Callers keep [keys] under SQLite's variable limit.
     */
    @Query(
        """
        SELECT systemId FROM games
        WHERE isHidden = 0 AND (systemId || '/' || COALESCE(absolutePath, relativePath)) IN (:keys)
        """
    )
    fun observeSystemsOfHiddenKeys(keys: List<String>): Flow<List<String>>

    @Query("SELECT sourceId, systemId, COUNT(*) AS gameCount FROM games GROUP BY sourceId, systemId")
    fun observeSourceCounts(): Flow<List<SourceGameCount>>

    @Query("SELECT sourceId, systemId, COUNT(*) AS gameCount FROM games GROUP BY sourceId, systemId")
    suspend fun getSourceCounts(): List<SourceGameCount>

    @Query(
        """
        SELECT g.* FROM games g
        JOIN (
            SELECT systemId, relativePath FROM games
            GROUP BY systemId, relativePath HAVING COUNT(*) > 1
        ) d ON g.systemId = d.systemId AND g.relativePath = d.relativePath
        """
    )
    suspend fun getSharedPathGames(): List<GameEntity>

    @Query("UPDATE play_history SET gameId = :keepId WHERE gameId IN (:dropIds)")
    suspend fun movePlayHistory(keepId: Long, dropIds: List<Long>)

    @Query("SELECT gameId FROM media WHERE gameId IN (:gameIds)")
    suspend fun gamesWithMedia(gameIds: List<Long>): List<Long>

    @Query("UPDATE media SET gameId = :keepId WHERE gameId = :dropId")
    suspend fun moveMedia(keepId: Long, dropId: Long)

    /** Folds each duplicate into its kept copy, moving play history and art before the delete. */
    @Transaction
    suspend fun mergeDuplicates(merges: List<GameMerge>) {
        for (merge in merges) {
            update(merge.keep)
            movePlayHistory(merge.keep.id, merge.dropIds)
            val withMedia = gamesWithMedia(merge.dropIds + merge.keep.id)
            if (merge.keep.id !in withMedia) {
                merge.dropIds.firstOrNull { it in withMedia }?.let { moveMedia(merge.keep.id, it) }
            }
            deleteByIds(merge.dropIds)
        }
    }

    @Query("SELECT systemId, absolutePath, relativePath FROM games WHERE isHidden = 0")
    fun observeVisibleKeys(): Flow<List<GameKeyRow>>

    @Query(
        """
        SELECT * FROM games
        WHERE systemId = :systemId AND isHidden = 0
            AND (absolutePath = :path OR relativePath = :path)
        LIMIT 1
        """
    )
    suspend fun findVisibleByLaunchPath(systemId: String, path: String): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun findById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: Long): Flow<GameEntity?>

    @Query(
        """
        SELECT * FROM games
        WHERE sourceId = :sourceId AND systemId = :systemId AND relativePath = :relativePath
        """
    )
    suspend fun findByTriple(sourceId: Long, systemId: String, relativePath: String): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(game: GameEntity): Long

    @Update
    suspend fun update(game: GameEntity)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    /** Hidden favorites included, so a backup keeps them. */
    @Query("SELECT DISTINCT systemId, relativePath FROM games WHERE isFavorite = 1")
    suspend fun favoriteKeys(): List<PlayGameKey>

    /** Every copy of the game, whichever source lists it. Returns how many rows matched. */
    @Query("UPDATE games SET isFavorite = 1 WHERE systemId = :systemId AND relativePath = :relativePath")
    suspend fun favoriteByKey(systemId: String, relativePath: String): Int

    @Query("SELECT * FROM games WHERE absolutePath = :absolutePath LIMIT 1")
    suspend fun findByAbsolutePath(absolutePath: String): GameEntity?

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("DELETE FROM games WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(games: List<GameEntity>): List<Long>

    @Update
    suspend fun updateAll(games: List<GameEntity>)

    /**
     * Applies one system's scan in a single transaction. Surviving rows
     * are updated in place, so their ids and every row that references
     * them (media, play history, scrape jobs) stay valid. Reading the
     * existing rows inside the transaction means a favorite toggled
     * during a scan can't be overwritten.
     *
     * @param removeMissing false when the scan could not read the whole
     *   system, so a game it didn't see may still be on disk.
     */
    @Transaction
    suspend fun reconcileSystemGames(
        sourceId: Long,
        systemId: String,
        scanned: List<GameEntity>,
        removeMissing: Boolean,
        keepImportedMetadata: Boolean,
    ) {
        val existing = getBySystemAndSource(systemId, sourceId).associateBy { it.relativePath }
        val scannedByPath = scanned.associateBy { it.relativePath }

        val changed = scannedByPath.mapNotNull { (path, scan) ->
            val current = existing[path] ?: return@mapNotNull null
            current.refreshedFrom(scan, keepImportedMetadata).takeIf { it != current }
        }
        if (changed.isNotEmpty()) updateAll(changed)

        val added = scannedByPath.filterKeys { it !in existing }.values.toList()
        if (added.isNotEmpty()) insertAll(added)

        if (removeMissing) {
            existing.filterKeys { it !in scannedByPath }.values
                .map { it.id }
                .chunked(SQLITE_VARIABLE_CHUNK)
                .forEach { deleteByIds(it) }
        }
    }
}
