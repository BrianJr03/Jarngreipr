package jr.brian.frontend.library.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds custom collections. `LibraryMigrationsTest` checks the result against the exported v2 schema. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `collections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL COLLATE NOCASE,
                `createdAt` INTEGER NOT NULL,
                `position` INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_collections_name` ON `collections` (`name`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `collection_games` (
                `collectionId` INTEGER NOT NULL,
                `systemId` TEXT NOT NULL,
                `relativePath` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                PRIMARY KEY(`collectionId`, `systemId`, `relativePath`),
                FOREIGN KEY(`collectionId`) REFERENCES `collections`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_collection_games_systemId_relativePath` " +
                "ON `collection_games` (`systemId`, `relativePath`)",
        )
    }
}

val LIBRARY_MIGRATIONS = arrayOf(MIGRATION_1_2)
