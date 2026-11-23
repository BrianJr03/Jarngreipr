package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import jr.brian.frontend.library.play.BackedUpPlayStat
import jr.brian.frontend.library.play.creditedMinutes
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

    /** Includes hidden games, which can still be launched from a pinned tile. */
    @Query("SELECT id FROM games WHERE systemId = :systemId AND (absolutePath = :path OR relativePath = :path) LIMIT 1")
    suspend fun findGameId(systemId: String, path: String): Long?

    @Query("DELETE FROM play_history WHERE durationSeconds IS NULL")
    suspend fun deleteOpenSessions()

    @Query("DELETE FROM play_history WHERE id = :id AND durationSeconds IS NULL")
    suspend fun deleteOpenSession(id: Long)

    @Query("SELECT gameId FROM play_history WHERE id = :id AND durationSeconds IS NULL")
    suspend fun openSessionGameId(id: Long): Long?

    @Query("UPDATE play_history SET durationSeconds = :seconds WHERE id = :id AND durationSeconds IS NULL")
    suspend fun closeSession(id: Long, seconds: Int): Int

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM play_history WHERE gameId = :gameId AND id != :exceptId")
    suspend fun totalSeconds(gameId: Long, exceptId: Long): Long

    @Query(
        """
        UPDATE games SET playCount = playCount + 1, playTimeMinutes = playTimeMinutes + :minutes,
            lastPlayed = MAX(COALESCE(lastPlayed, 0), (SELECT startedAt FROM play_history WHERE id = :sessionId))
        WHERE id = :gameId
        """
    )
    suspend fun addPlay(gameId: Long, sessionId: Long, minutes: Int)

    /** Closes an open session and adds it to its game, once. False if already closed or gone. */
    @Transaction
    suspend fun settleSession(id: Long, seconds: Int): Boolean {
        val gameId = openSessionGameId(id) ?: return false
        if (closeSession(id, seconds) == 0) return false
        addPlay(gameId, id, creditedMinutes(totalSeconds(gameId, id), seconds))
        return true
    }

    @Query(
        """
        SELECT systemId, relativePath, MAX(playCount) AS playCount, MAX(playTimeMinutes) AS playTimeMinutes
        FROM games WHERE playCount > 0 OR playTimeMinutes > 0
        GROUP BY systemId, relativePath
        """
    )
    suspend fun playStats(): List<BackedUpPlayStat>

    @Query("SELECT systemId, relativePath FROM games")
    suspend fun gameKeys(): List<PlayGameKey>

    @Query(
        """
        UPDATE games SET playCount = MAX(playCount, :playCount),
            playTimeMinutes = MAX(playTimeMinutes, :playTimeMinutes)
        WHERE systemId = :systemId AND relativePath = :relativePath
        """
    )
    suspend fun raiseStat(systemId: String, relativePath: String, playCount: Int, playTimeMinutes: Int)

    @Transaction
    suspend fun raiseStats(stats: List<BackedUpPlayStat>) {
        stats.forEach { raiseStat(it.systemId, it.relativePath, it.playCount, it.playTimeMinutes) }
    }
}

data class PlayGameKey(val systemId: String, val relativePath: String)
