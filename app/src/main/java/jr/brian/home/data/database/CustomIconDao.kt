package jr.brian.home.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomIconDao {
    @Query("SELECT * FROM custom_icons")
    fun getAllCustomIcons(): Flow<List<CustomIconEntity>>

    @Query("SELECT * FROM custom_icons WHERE packageName = :packageName")
    suspend fun getCustomIcon(packageName: String): CustomIconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomIcon(customIcon: CustomIconEntity)

    @Query("DELETE FROM custom_icons WHERE packageName = :packageName")
    suspend fun deleteCustomIcon(packageName: String)

    @Query("DELETE FROM custom_icons")
    suspend fun deleteAllCustomIcons()
}
