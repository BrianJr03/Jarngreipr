package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemDao {
    @Query("SELECT * FROM systems ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<SystemEntity>>

    @Query("SELECT * FROM systems WHERE id = :id")
    suspend fun findById(id: String): SystemEntity?

    @Query("SELECT * FROM systems WHERE id = :id")
    fun observeById(id: String): Flow<SystemEntity?>

    /**
     * Must be @Upsert, not INSERT OR REPLACE: SystemEntity is the
     * parent of GameEntity.systemId with ON DELETE RESTRICT. REPLACE
     * on insert does DELETE old row + INSERT new, which trips the
     * RESTRICT trigger as soon as any GameEntity rows reference the
     * system — crashes on every migration re-run against a populated
     * database (fixed after a user hit this on CURRENT_VERSION bump
     * to 4). @Upsert emits `INSERT ... ON CONFLICT ... DO UPDATE`
     * which mutates the existing row in place and never deletes.
     */
    @Upsert
    suspend fun upsert(system: SystemEntity)

    @Transaction
    suspend fun upsertAll(systems: List<SystemEntity>) {
        systems.forEach { upsert(it) }
    }

    @Query("DELETE FROM systems WHERE id = :id")
    suspend fun deleteById(id: String)
}
