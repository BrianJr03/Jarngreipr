package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE gameId = :gameId LIMIT 1")
    fun observeByGameId(gameId: Long): Flow<MediaEntity?>

    @Query("SELECT * FROM media WHERE gameId = :gameId LIMIT 1")
    suspend fun findByGameId(gameId: Long): MediaEntity?

    @Query("SELECT * FROM media")
    fun observeAll(): Flow<List<MediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(media: MediaEntity)

    @Query("DELETE FROM media WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)
}
