package jr.brian.frontend.library.migrate

import android.app.Application
import android.content.Context
import androidx.room.Room
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.scan.FastWalkScanner
import jr.brian.frontend.library.scan.SafTreeScanner
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * Covers [RomIndexImporter]. Given legacy `filesDir/rom_index/<sys>.json`
 * caches and a registered SourceEntity, the importer must land every
 * game as a GameEntity row, carry over user flags (isFavorite,
 * playCount), and attach a MediaEntity row for games with scraped art.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class RomIndexImporterTest {

    private val catalog = StubSystemCatalog(
        SystemDef(id = "nes", displayName = "Nintendo", extensions = setOf("nes")),
    )

    private lateinit var app: Application
    private lateinit var db: LibraryDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var importer: RomIndexImporter

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        File(app.filesDir, "rom_index").deleteRecursively()

        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        importer = RomIndexImporter(
            context = app,
            legacyPrefs = EsdeLegacyPrefs(app),
            sourceDao = db.sourceDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            catalog = catalog,
        )

        // Importer needs SystemEntity rows for its FK writes.
        runBlocking {
            db.systemDao().upsert(
                jr.brian.frontend.library.db.SystemEntity(
                    id = "nes",
                    displayName = "Nintendo",
                )
            )
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
        File(app.filesDir, "rom_index").deleteRecursively()
    }

    @Test
    fun `importer upserts games from legacy rom_index JSON`() = runBlocking {
        val sourceId = db.sourceDao().insert(
            SourceEntity(treeUri = "/storage/emulated/0/Roms", displayPath = "/Roms"),
        )
        writeIndexFile(
            name = "nes",
            json = """
                {
                  "stamp": {
                    "systemDirLastModified": 0,
                    "entryCount": 1,
                    "gamelistLastModified": -1,
                    "decorationEnabled": false,
                    "romsRootUsed": "/storage/emulated/0/Roms"
                  },
                  "games": [
                    {
                      "path": "./Mario.nes",
                      "name": "Super Mario Bros",
                      "description": "Classic platformer",
                      "systemName": "nes",
                      "isFavorite": true,
                      "playCount": 42,
                      "playTimeMinutes": 300,
                      "romAbsolutePath": "/storage/emulated/0/Roms/nes/Mario.nes"
                    }
                  ]
                }
            """.trimIndent(),
        )

        importer.run()

        val games = db.gameDao().observeAll().first()
        assertEquals(1, games.size)
        val mario = games.single()
        assertEquals("Super Mario Bros", mario.displayName)
        assertEquals("Classic platformer", mario.description)
        assertEquals(true, mario.isFavorite)
        assertEquals(42, mario.playCount)
        assertEquals(300, mario.playTimeMinutes)
        assertEquals(sourceId, mario.sourceId)
    }

    @Test
    fun `importer attaches a MediaEntity when the legacy entry has scraped art`() = runBlocking {
        db.sourceDao().insert(SourceEntity(treeUri = "/Roms", displayPath = "/Roms"))
        writeIndexFile(
            name = "nes",
            json = """
                {
                  "stamp": {"systemDirLastModified":0,"entryCount":1,"gamelistLastModified":-1,"decorationEnabled":false,"romsRootUsed":"/Roms"},
                  "games": [
                    {
                      "path": "./Mario.nes",
                      "name": "Mario",
                      "systemName": "nes",
                      "artworkPath": "/media/nes/covers/Mario.png",
                      "screenshotPath": "/media/nes/screenshots/Mario.png"
                    }
                  ]
                }
            """.trimIndent(),
        )

        importer.run()

        val mario = db.gameDao().observeAll().first().single()
        val media = db.mediaDao().findByGameId(mario.id)
        assertNotNull(media)
        assertEquals("/media/nes/covers/Mario.png", media!!.coverPath)
        assertEquals("/media/nes/screenshots/Mario.png", media.screenshotPath)
    }

    @Test
    fun `importer tolerates a malformed JSON blob without blocking others`() = runBlocking {
        db.sourceDao().insert(SourceEntity(treeUri = "/Roms", displayPath = "/Roms"))
        writeIndexFile(name = "broken", json = "not even json")
        writeIndexFile(
            name = "nes",
            json = """{"games":[{"path":"./Mario.nes","name":"Mario","systemName":"nes"}]}""",
        )

        importer.run()

        val games = db.gameDao().observeAll().first().map { it.displayName }
        assertEquals(listOf("Mario"), games)
    }

    @Test
    fun `importer is a no-op when the rom_index directory does not exist`() = runBlocking {
        db.sourceDao().insert(SourceEntity(treeUri = "/Roms", displayPath = "/Roms"))

        importer.run()

        assertTrue(db.gameDao().observeAll().first().isEmpty())
    }

    @Test
    fun `importer is a no-op when no sources are registered`() = runBlocking {
        writeIndexFile(
            name = "nes",
            json = """{"games":[{"path":"./Mario.nes","name":"Mario","systemName":"nes"}]}""",
        )

        importer.run()

        assertTrue(db.gameDao().observeAll().first().isEmpty())
    }

    @Test
    fun `importer updates an existing game row rather than inserting a duplicate`() = runBlocking {
        val sourceId = db.sourceDao().insert(SourceEntity(treeUri = "/Roms", displayPath = "/Roms"))
        // Pre-insert a bare row from a previous scan.
        db.gameDao().insert(
            jr.brian.frontend.library.db.GameEntity(
                sourceId = sourceId,
                systemId = "nes",
                relativePath = "Mario.nes",
                displayName = "Mario",
            )
        )
        writeIndexFile(
            name = "nes",
            json = """{"games":[{"path":"./Mario.nes","name":"Super Mario","description":"NEW","systemName":"nes","isFavorite":true}]}""",
        )

        importer.run()

        val games = db.gameDao().observeAll().first()
        assertEquals(1, games.size)
        assertEquals("Super Mario", games.single().displayName)
        assertEquals("NEW", games.single().description)
        assertEquals(true, games.single().isFavorite)
    }

    @Test
    fun `parseEsdeDate decodes the common ISO-ish variants`() {
        val short = parseEsdeDate("20240115T000000")
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = short
        assertEquals(2024, cal.get(java.util.Calendar.YEAR))
        assertEquals(0, cal.get(java.util.Calendar.MONTH))      // Jan
        assertEquals(15, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    private fun writeIndexFile(name: String, json: String) {
        val dir = File(app.filesDir, "rom_index").apply { mkdirs() }
        File(dir, "$name.json").writeText(json)
    }
}
