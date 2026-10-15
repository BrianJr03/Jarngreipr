package jr.brian.frontend.library.db

import android.app.Application
import androidx.room.Room
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Phase 2a smoke test for the library schema: the entities compile into
 * a real SQLite database and every DAO can round-trip + observe its
 * backing table. Also pins the two FK rules the write path depends on:
 *
 *  - Deleting a game cascades to its media + play history.
 *  - Deleting a source cascades to its games (and transitively, media).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryDatabaseTest {

    private lateinit var db: LibraryDatabase

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `system round trip`() = runBlocking {
        val system = SystemEntity(
            id = "n64",
            displayName = "Nintendo 64",
            folderAliases = listOf("n64", "nintendo64"),
            extensions = setOf("z64", "n64", "v64"),
            screenScraperSystemId = 14,
        )
        db.systemDao().upsert(system)

        val loaded = db.systemDao().findById("n64")
        assertEquals(system, loaded)
    }

    @Test
    fun `game observe emits when a new game is inserted`() = runTest {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )

        db.gameDao().observeBySystem("n64").test {
            assertEquals(emptyList<GameEntity>(), awaitItem())
            db.gameDao().insert(
                GameEntity(
                    sourceId = sourceId,
                    systemId = "n64",
                    relativePath = "Mario.z64",
                    displayName = "Super Mario 64",
                ),
            )
            val next = awaitItem()
            assertEquals(1, next.size)
            assertEquals("Super Mario 64", next.single().displayName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleting a game cascades to media and play history`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )
        val gameId = db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = "n64",
                relativePath = "Mario.z64",
                displayName = "Super Mario 64",
            ),
        )
        db.mediaDao().upsert(MediaEntity(gameId = gameId, coverPath = "/cover.png"))
        db.playHistoryDao().insert(PlayHistoryEntity(gameId = gameId, startedAt = 1_700_000_000L))

        assertNotNull(db.mediaDao().findByGameId(gameId))
        assertTrue(db.playHistoryDao().observeForGame(gameId).first().isNotEmpty())

        db.gameDao().delete(db.gameDao().findById(gameId)!!)

        assertNull(db.mediaDao().findByGameId(gameId))
        assertTrue(db.playHistoryDao().observeForGame(gameId).first().isEmpty())
    }

    @Test
    fun `deleting a source cascades to its games`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )
        db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = "n64",
                relativePath = "Mario.z64",
                displayName = "Super Mario 64",
            ),
        )
        assertEquals(1, db.gameDao().observeAll().first().size)

        db.sourceDao().deleteById(sourceId)

        assertEquals(emptyList<GameEntity>(), db.gameDao().observeAll().first())
    }

    @Test
    fun `string collection converters round trip through the system table`() = runBlocking {
        val system = SystemEntity(
            id = "sat",
            displayName = "Sega Saturn",
            folderAliases = listOf("sat", "saturn"),
            extensions = setOf("iso", "chd", "mdf"),
        )
        db.systemDao().upsert(system)

        val loaded = db.systemDao().findById("sat")!!
        assertEquals(listOf("sat", "saturn"), loaded.folderAliases)
        assertEquals(setOf("iso", "chd", "mdf"), loaded.extensions)
    }

    @Test
    fun `foreign key RESTRICT prevents dropping a system that still has games`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )
        db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = "n64",
                relativePath = "Mario.z64",
                displayName = "Super Mario 64",
            ),
        )
        var threw = false
        try {
            db.systemDao().deleteById("n64")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            threw = true
        }
        assertTrue(
            "RESTRICT must prevent a system drop while games still reference it — otherwise " +
                "a mid-run catalog refresh could orphan the user's scanned library",
            threw,
        )
    }
}
