package jr.brian.frontend.library.db

import android.app.Application
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Every schema step from the shipped v1, checked against the committed schema JSON. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryMigrationsTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LibraryDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `v1 library survives the move to v2 with games, favorites and play data`() {
        helper.createDatabase(TEST_DB, 1).use { it.insertV1Library() }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            assertEquals(2L, db.count("games"))
            assertEquals(1L, db.count("games WHERE isFavorite = 1 AND relativePath = 'Mario.z64'"))
            assertEquals(1L, db.count("games WHERE playCount = 7 AND playTimeMinutes = 90"))
            assertEquals(1L, db.count("play_history"))
            assertEquals(1L, db.count("media WHERE coverPath = '/covers/mario.png'"))
            assertEquals(0L, db.count("collections"))
            assertEquals(0L, db.count("collection_games"))
        }
    }

    @Test
    fun `migrated collection names are unique ignoring case`() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).use { db ->
            db.execSQL("INSERT INTO collections (name, createdAt) VALUES ('Party', 1)")
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("INSERT INTO collections (name, createdAt) VALUES ('pARTY', 2)")
            }
        }
    }

    @Test
    fun `the migrated database opens through Room and takes collections`() = runBlocking {
        helper.createDatabase(TEST_DB, 1).use { it.insertV1Library() }
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2).close()

        val db = Room.databaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java, TEST_DB)
            .addMigrations(*LIBRARY_MIGRATIONS)
            .allowMainThreadQueries()
            .build()
        try {
            assertEquals(listOf("Super Mario 64"), db.gameDao().observeFavorites().first().map { it.displayName })
            val id = db.collectionDao().insert(CollectionEntity(name = "Party", createdAt = 1L))
            db.collectionDao().addGame(CollectionGameEntity(id, "n64", "Mario.z64", addedAt = 1L))
            assertEquals(listOf("Super Mario 64"), db.collectionDao().observeGames(id).first().map { it.displayName })
        } finally {
            db.close()
        }
    }

    private fun SupportSQLiteDatabase.insertV1Library() {
        execSQL("INSERT INTO systems (id, displayName, folderAliases, extensions) VALUES ('n64', 'Nintendo 64', 'n64', 'z64')")
        execSQL(
            "INSERT INTO sources (id, treeUri, displayPath, hasPersistedGrant, lastScannedAt) " +
                "VALUES (1, '/storage/roms', '/Roms', 1, 0)",
        )
        execSQL(
            "INSERT INTO games (id, sourceId, systemId, relativePath, displayName, isFavorite, playCount, playTimeMinutes) " +
                "VALUES (1, 1, 'n64', 'Mario.z64', 'Super Mario 64', 1, 7, 90)",
        )
        execSQL("INSERT INTO games (id, sourceId, systemId, relativePath, displayName) VALUES (2, 1, 'n64', 'Zelda.z64', 'Zelda')")
        execSQL("INSERT INTO play_history (gameId, startedAt, durationSeconds) VALUES (1, 1700000000, 600)")
        execSQL("INSERT INTO media (gameId, coverPath) VALUES (1, '/covers/mario.png')")
    }

    private fun SupportSQLiteDatabase.count(from: String): Long =
        query("SELECT COUNT(*) FROM $from").use { cursor ->
            assertTrue(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val TEST_DB = "library-migration-test"
    }
}
