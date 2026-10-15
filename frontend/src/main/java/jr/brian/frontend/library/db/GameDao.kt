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
