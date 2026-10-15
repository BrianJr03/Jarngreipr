package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayHistoryDao {
    @Query("SELECT * FROM play_history WHERE gameId = :gameId ORDER BY startedAt DESC")
    fun observeForGame(gameId: Long): Flow<List<PlayHistoryEntity>>

    @Query("SELECT * FROM play_history ORDER BY startedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 50): Flow<List<PlayHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: PlayHistoryEntity): Long

    @Update
    suspend fun update(session: PlayHistoryEntity)

    @Query("DELETE FROM play_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM play_history WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)
}
