package jr.brian.frontend.library.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemDao {
    @Query("SELECT * FROM systems ORDER BY displayName COLLATE NOCASE")
    fun observeAll(): Flow<List<SystemEntity>>

    @Query("SELECT * FROM systems WHERE id = :id")
    suspend fun findById(id: String): SystemEntity?

    @Query("SELECT * FROM systems WHERE id = :id")
    fun observeById(id: String): Flow<SystemEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(system: SystemEntity)

    @Transaction
    suspend fun upsertAll(systems: List<SystemEntity>) {
        systems.forEach { upsert(it) }
    }

    @Query("DELETE FROM systems WHERE id = :id")
    suspend fun deleteById(id: String)
}
