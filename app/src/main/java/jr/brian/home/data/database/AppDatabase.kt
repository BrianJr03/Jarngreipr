package jr.brian.home.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CustomIconEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customIconDao(): CustomIconDao
}
