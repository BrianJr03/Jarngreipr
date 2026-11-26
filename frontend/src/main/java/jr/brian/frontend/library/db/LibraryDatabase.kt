package jr.brian.frontend.library.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Phase 2+ library engine database. Separate from `:app`'s `AppDatabase`
 * so the launcher's prefs lifecycle doesn't couple to the library's.
 *
 * Schemas are exported to `frontend/schemas/` and committed. Every version
 * bump needs a migration in [LIBRARY_MIGRATIONS] and a migration test.
 */
@Database(
    entities = [
        SystemEntity::class,
        SourceEntity::class,
        GameEntity::class,
        MediaEntity::class,
        ScrapeJobEntity::class,
        PlayHistoryEntity::class,
        CollectionEntity::class,
        CollectionGameEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(StringCollectionConverters::class)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun systemDao(): SystemDao
    abstract fun sourceDao(): SourceDao
    abstract fun gameDao(): GameDao
    abstract fun mediaDao(): MediaDao
    abstract fun scrapeJobDao(): ScrapeJobDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun collectionDao(): CollectionDao

    companion object {
        const val DATABASE_NAME = "jarngreipr_library"
    }
}
