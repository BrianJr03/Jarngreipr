package jr.brian.frontend.library

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers the Phase 2a read API: the repository proxies the DAOs
 * correctly and the state flow starts at Idle. Writes are not tested
 * here because 2a doesn't ship any — those tests arrive with the
 * scanner (2c) and migration (2d).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryRepositoryTest {

    private lateinit var db: LibraryDatabase
    private lateinit var repo: LibraryRepository

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = LibraryRepository(
            systemDao = db.systemDao(),
            sourceDao = db.sourceDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            scrapeJobDao = db.scrapeJobDao(),
            playHistoryDao = db.playHistoryDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `state starts at Idle`() {
        assertTrue(repo.state.value is LibraryState.Idle)
    }

    @Test
    fun `systems flow reflects DAO writes`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        db.systemDao().upsert(SystemEntity(id = "psx", displayName = "PlayStation"))

        val snapshot = repo.systems.first()
        assertEquals(listOf("Nintendo 64", "PlayStation"), snapshot.map { it.displayName })
    }

    @Test
    fun `allGames filters out hidden games`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )
        db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "n64", relativePath = "Mario.z64", displayName = "Super Mario 64"),
        )
        db.gameDao().insert(
            GameEntity(
                sourceId = sourceId, systemId = "n64", relativePath = "Beta.z64",
                displayName = "Hidden Beta", isHidden = true,
            ),
        )

        val visible = repo.allGames.first().map { it.displayName }
        assertEquals(listOf("Super Mario 64"), visible)
    }

    @Test
    fun `gamesForSystem narrows and still respects the hidden flag`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        db.systemDao().upsert(SystemEntity(id = "snes", displayName = "SNES"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )
        db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "n64", relativePath = "Mario.z64", displayName = "Super Mario 64"),
        )
        db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "snes", relativePath = "Zelda.sfc", displayName = "A Link to the Past"),
        )

        val n64 = repo.gamesForSystem("n64").first().map { it.displayName }
        assertEquals(listOf("Super Mario 64"), n64)
    }

    @Test
    fun `findGameById returns the inserted row`() = runBlocking {
        db.systemDao().upsert(SystemEntity(id = "n64", displayName = "Nintendo 64"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "content://example", displayPath = "/Roms"),
        )
        val id = db.gameDao().insert(
            GameEntity(sourceId = sourceId, systemId = "n64", relativePath = "Mario.z64", displayName = "Super Mario 64"),
        )
        assertEquals("Super Mario 64", repo.findGameById(id)?.displayName)
    }
}
