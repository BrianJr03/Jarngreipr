package jr.brian.frontend.library.db

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.art.libraryHiddenGameKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class HiddenKeyQueryTest {
    private lateinit var db: LibraryDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seed(): List<GameEntity> {
        listOf("gba", "snes").forEach { db.systemDao().upsert(SystemEntity(id = it, displayName = it)) }
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = "content://roms", displayPath = "/Roms"))
        val games = listOf(
            GameEntity(sourceId = sourceId, systemId = "gba", relativePath = "a.gba", displayName = "A"),
            GameEntity(
                sourceId = sourceId,
                systemId = "gba",
                relativePath = "b.gba",
                absolutePath = "/Roms/gba/b.gba",
                displayName = "B",
            ),
            GameEntity(sourceId = sourceId, systemId = "snes", relativePath = "c.sfc", displayName = "C"),
        )
        games.forEach { db.gameDao().insert(it) }
        return games
    }

    @Test
    fun `stale keys are not counted, matching what the hidden list shows`() = runBlocking {
        val games = seed()
        val keys = listOf(
            libraryHiddenGameKey("gba", null, "a.gba"),
            libraryHiddenGameKey("gba", "/Roms/gba/b.gba", "b.gba"),
            "gba/deleted.gba",
            "snes/also-gone.sfc",
        )

        val counted = db.gameDao().observeSystemsOfHiddenKeys(keys).first()
        val listed = games.filter { libraryHiddenGameKey(it.systemId, it.absolutePath, it.relativePath) in keys }

        assertEquals(listOf("gba", "gba"), counted.sorted())
        assertEquals(listed.map { it.systemId }.sorted(), counted.sorted())
    }

    @Test
    fun `a game with a full path only matches its full-path key`() = runBlocking {
        seed()

        assertEquals(emptyList<String>(), db.gameDao().observeSystemsOfHiddenKeys(listOf("gba/b.gba")).first())
    }
}
