package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {
    @Query("SELECT * FROM sources ORDER BY displayPath COLLATE NOCASE")
    fun observeAll(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources ORDER BY displayPath COLLATE NOCASE")
    suspend fun getAll(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE id = :id")
    suspend fun findById(id: Long): SourceEntity?

    @Query("SELECT * FROM sources WHERE treeUri = :treeUri")
    suspend fun findByTreeUri(treeUri: String): SourceEntity?

    /**
     * IGNORE instead of REPLACE: SourceEntity's children (games,
     * then media via games) are declared ON DELETE CASCADE. A naive
     * REPLACE on a treeUri collision would silently nuke every game
     * attributed to the existing source. Callers that need to
     * update an existing row should call [findByTreeUri] + [update]
     * explicitly rather than relying on this path.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(source: SourceEntity): Long

    @Update
    suspend fun update(source: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE sources SET lastScannedAt = :epochMillis WHERE id = :id")
    suspend fun markScanned(id: Long, epochMillis: Long)

    @Query("UPDATE sources SET hasPersistedGrant = :granted WHERE id = :id")
    suspend fun setGranted(id: Long, granted: Boolean)
}
