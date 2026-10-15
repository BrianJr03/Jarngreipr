package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScrapeJobDao {
    @Query("SELECT * FROM scrape_jobs WHERE gameId = :gameId ORDER BY startedAt DESC")
    fun observeForGame(gameId: Long): Flow<List<ScrapeJobEntity>>

    @Query("SELECT * FROM scrape_jobs WHERE state = :state ORDER BY startedAt DESC")
    fun observeByState(state: String): Flow<List<ScrapeJobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: ScrapeJobEntity): Long

    @Update
    suspend fun update(job: ScrapeJobEntity)

    @Query("DELETE FROM scrape_jobs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scrape_jobs WHERE state = 'succeeded'")
    suspend fun purgeSucceeded()
}
