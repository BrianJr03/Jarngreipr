package jr.brian.frontend.library.migrate

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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Covers [GamelistImporter]. The importer DECORATES existing
 * GameEntity rows with gamelist metadata; it does not create new
 * rows. These tests pin both halves: fields update when a row
 * matches, nothing happens when the gamelist carries a path with
 * no matching row.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GamelistImporterTest {

    private lateinit var db: LibraryDatabase
    private lateinit var importer: GamelistImporter

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        importer = GamelistImporter(db.gameDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `fields on an existing game are updated from the XML`() = runBlocking {
        val (sourceId, _) = seedOneGame(path = "Mario.nes", displayName = "Mario")

        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <gameList>
              <game>
                <path>./Mario.nes</path>
                <name>Super Mario Bros</name>
                <desc>Classic platformer</desc>
                <rating>0.9</rating>
                <releasedate>19851018T000000</releasedate>
                <developer>Nintendo R&amp;D1</developer>
                <publisher>Nintendo</publisher>
                <genre>Platformer</genre>
                <players>1-2</players>
                <favorite>true</favorite>
                <playcount>42</playcount>
                <playtime>7200</playtime>
                <lasttimeplayed>20240115T193000</lasttimeplayed>
              </game>
            </gameList>
        """.trimIndent()

        val updated = importer.importStream(xml.byteInputStream(), sourceId, "nes")
        assertEquals(1, updated)

        val mario = db.gameDao().observeAll().first().single()
        assertEquals("Super Mario Bros", mario.displayName)
        assertEquals("Classic platformer", mario.description)
        assertEquals(0.9f, mario.rating!!, 0.001f)
        assertEquals("Nintendo R&D1", mario.developer)
        assertEquals("Nintendo", mario.publisher)
        assertEquals("Platformer", mario.genre)
        assertEquals("1-2", mario.players)
        assertEquals(true, mario.isFavorite)
        assertEquals(42, mario.playCount)
        assertEquals(120, mario.playTimeMinutes) // 7200 seconds / 60
    }

    @Test
    fun `path with no matching game does not create a new row`() = runBlocking {
        val (sourceId, _) = seedOneGame(path = "Mario.nes", displayName = "Mario")

        val xml = """
            <gameList>
              <game>
                <path>./DoesNotExist.nes</path>
                <name>Never scanned</name>
              </game>
            </gameList>
        """.trimIndent()

        val updated = importer.importStream(xml.byteInputStream(), sourceId, "nes")
        assertEquals(0, updated)

        val games = db.gameDao().observeAll().first()
        assertEquals(1, games.size)
        assertEquals("Mario", games.single().displayName)
    }

    @Test
    fun `missing XML fields leave the existing GameEntity values untouched`() = runBlocking {
        val (sourceId, _) = seedOneGame(path = "Mario.nes", displayName = "Mario Original")

        val xml = """
            <gameList>
              <game>
                <path>./Mario.nes</path>
                <name>Mario Updated</name>
              </game>
            </gameList>
        """.trimIndent()
        importer.importStream(xml.byteInputStream(), sourceId, "nes")

        val mario = db.gameDao().observeAll().first().single()
        assertEquals("Mario Updated", mario.displayName)
        assertNull(mario.description) // was null before, still null
        assertNull(mario.releaseDate)
        assertEquals(false, mario.isFavorite)
    }

    @Test
    fun `path normalisation strips the ES-DE leading-dot-slash prefix`() = runBlocking {
        val (sourceId, _) = seedOneGame(path = "sub/Mario.nes", displayName = "Mario")

        val xml = """
            <gameList>
              <game>
                <path>./sub/Mario.nes</path>
                <desc>found</desc>
              </game>
            </gameList>
        """.trimIndent()

        val updated = importer.importStream(xml.byteInputStream(), sourceId, "nes")
        assertEquals(1, updated)
        assertEquals("found", db.gameDao().observeAll().first().single().description)
    }

    @Test
    fun `malformed XML returns zero without throwing`() = runBlocking {
        val (sourceId, _) = seedOneGame(path = "Mario.nes", displayName = "Mario")

        val xml = """<gameList><game><path>./Mario.nes</path"""

        // Partial XML — the parser throws, the importer catches, returns 0.
        val updated = try {
            importer.importStream(xml.byteInputStream(), sourceId, "nes")
        } catch (_: Exception) {
            -1 // Should NOT be reached
        }
        assertTrue("Malformed XML must not throw: got $updated", updated == 0)
    }

    @Test
    fun `favorite true-false strings decode case-insensitively`() = runBlocking {
        val (sourceId, _) = seedOneGame(path = "Mario.nes", displayName = "Mario")
        val xml = """
            <gameList>
              <game><path>./Mario.nes</path><favorite>TRUE</favorite></game>
            </gameList>
        """.trimIndent()
        importer.importStream(xml.byteInputStream(), sourceId, "nes")
        assertEquals(true, db.gameDao().observeAll().first().single().isFavorite)
    }

    private suspend fun seedOneGame(path: String, displayName: String): Pair<Long, Long> {
        db.systemDao().upsert(SystemEntity(id = "nes", displayName = "Nintendo"))
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "/Roms", displayPath = "/Roms"),
        )
        val gameId = db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = "nes",
                relativePath = path,
                displayName = displayName,
            )
        )
        return sourceId to gameId
    }
}
