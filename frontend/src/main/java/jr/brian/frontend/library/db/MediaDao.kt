package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media WHERE gameId = :gameId LIMIT 1")
    fun observeByGameId(gameId: Long): Flow<MediaEntity?>

    @Query("SELECT * FROM media WHERE gameId = :gameId LIMIT 1")
    suspend fun findByGameId(gameId: Long): MediaEntity?

    @Query("SELECT * FROM media")
    fun observeAll(): Flow<List<MediaEntity>>

    @Upsert
    suspend fun upsert(media: MediaEntity)

    @Query("DELETE FROM media WHERE gameId = :gameId")
    suspend fun deleteByGameId(gameId: Long)

    @Upsert
    suspend fun upsertAll(media: List<MediaEntity>)

    @Query("DELETE FROM media WHERE gameId IN (:gameIds)")
    suspend fun deleteByGameIds(gameIds: List<Long>)

    /**
     * One system's media rows in one transaction, so observers of
     * `media` (the joined all-games query behind the frontend's
     * list) see a single invalidation per system instead of one per
     * game. A 5k-ROM scan used to re-run that full join thousands
     * of times.
     */
    @Transaction
    suspend fun replaceForSystem(deleteGameIds: List<Long>, upserts: List<MediaEntity>) {
        // Chunked to stay under SQLite's bound-variable limit.
        deleteGameIds.chunked(500).forEach { deleteByGameIds(it) }
        if (upserts.isNotEmpty()) upsertAll(upserts)
    }
}
