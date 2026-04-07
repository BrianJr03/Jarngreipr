package jr.brian.home.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CustomIconEntity::class,
        RssFeedEntity::class,
        RssItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customIconDao(): CustomIconDao
    abstract fun rssFeedDao(): RssFeedDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS rss_feeds (
                        url TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        refreshIntervalMinutes INTEGER NOT NULL,
                        lastRefreshedAt INTEGER NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS rss_items (
                        id TEXT NOT NULL,
                        feedUrl TEXT NOT NULL,
                        title TEXT NOT NULL,
                        link TEXT NOT NULL,
                        description TEXT NOT NULL,
                        pubDate TEXT NOT NULL,
                        PRIMARY KEY(id, feedUrl)
                    )"""
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rss_items ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE rss_items ADD COLUMN videoUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rss_items ADD COLUMN audioUrl TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rss_feeds ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
