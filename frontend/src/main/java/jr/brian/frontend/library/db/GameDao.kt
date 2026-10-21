package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE isHidden = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeAllVisible(): Flow<List<GameEntity>>

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

    @Query("SELECT * FROM games WHERE systemId = :systemId AND sourceId = :sourceId")
    suspend fun getBySystemAndSource(systemId: String, sourceId: Long): List<GameEntity>

    @Query("SELECT * FROM games WHERE isFavorite = 1 AND isHidden = 0 ORDER BY displayName COLLATE NOCASE")
    fun observeFavorites(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE lastPlayed IS NOT NULL AND isHidden = 0 ORDER BY lastPlayed DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<GameEntity>>

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

    @Query("SELECT * FROM games WHERE absolutePath = :absolutePath LIMIT 1")
    suspend fun findByAbsolutePath(absolutePath: String): GameEntity?

    @Delete
    suspend fun delete(game: GameEntity)

    @Query("DELETE FROM games WHERE sourceId = :sourceId AND systemId = :systemId")
    suspend fun deleteBySourceAndSystem(sourceId: Long, systemId: String)

    @Transaction
    suspend fun replaceSystemGames(sourceId: Long, systemId: String, games: List<GameEntity>) {
        deleteBySourceAndSystem(sourceId, systemId)
        games.forEach { insert(it) }
    }
}
