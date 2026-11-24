package jr.brian.frontend.library.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Phase 2+ library engine database. Separate from `:app`'s `AppDatabase`
 * so the launcher's prefs lifecycle doesn't couple to the library's.
 *
 * `exportSchema = false` for now — once v1 ships to a real device we
 * flip it on and start committing the schema JSON next to the module.
 */
@Database(
    entities = [
        SystemEntity::class,
        SourceEntity::class,
        GameEntity::class,
        MediaEntity::class,
        ScrapeJobEntity::class,
        PlayHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(StringCollectionConverters::class)
abstract class LibraryDatabase : RoomDatabase() {
    abstract fun systemDao(): SystemDao
    abstract fun sourceDao(): SourceDao
    abstract fun gameDao(): GameDao
    abstract fun mediaDao(): MediaDao
    abstract fun scrapeJobDao(): ScrapeJobDao
    abstract fun playHistoryDao(): PlayHistoryDao

    companion object {
        const val DATABASE_NAME = "jarngreipr_library"
    }
}
