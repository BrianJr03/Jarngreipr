package jr.brian.frontend.library.migrate

import android.app.Application
import android.content.Context
import androidx.room.Room
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.scan.FastWalkScanner
import jr.brian.frontend.library.scan.SafTreeScanner
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.setup.EsdeRomsFolderFinder
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * ES-DE keeps gamelists in `ES-DE/gamelists/<system>/`, apart from the ROMs.
 * These pin that those reach the library, SAF sources included, and that a
 * second gamelist inside the ROMs folder is never applied on top.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EsdeGamelistImportTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(id = "nes", displayName = "Nintendo", extensions = setOf("nes")),
    )

    private lateinit var db: LibraryDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var migration: LibraryMigration
    private lateinit var volume: File

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        app.getSharedPreferences("esde_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        app.getSharedPreferences("library_migration_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        volume = tempFolder.newFolder("storage")
        val repo = LibraryRepository(
            systemDao = db.systemDao(),
            sourceDao = db.sourceDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            scrapeJobDao = db.scrapeJobDao(),
            playHistoryDao = db.playHistoryDao(),
            catalog = catalog,
            scanner = ScannerDispatcher(
                fastWalk = FastWalkScanner(catalog),
                saf = SafTreeScanner(app, catalog),
            ),
            scope = scope,
        )
        val legacyPrefs = EsdeLegacyPrefs(app)
        migration = LibraryMigration(
            context = app,
            legacyPrefs = legacyPrefs,
            repository = repo,
            sourceDao = db.sourceDao(),
            systemDao = db.systemDao(),
            catalog = catalog,
            romIndexImporter = RomIndexImporter(
                context = app,
                legacyPrefs = legacyPrefs,
                sourceDao = db.sourceDao(),
                gameDao = db.gameDao(),
                mediaDao = db.mediaDao(),
                catalog = catalog,
            ),
            gamelistImporter = GamelistImporter(db.gameDao(), catalog),
            gameDao = db.gameDao(),
            esdeFolders = EsdeRomsFolderFinder(
                mediaRoot = { null },
                volumes = { listOf(volume) },
                readText = { null },
                isDirectory = File::isDirectory,
            ),
        )
        runBlocking { migration.runOnce() }
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun `ES-DE folder gamelist reaches a SAF source`() = runBlocking {
        val sourceId = addSource(SAF_TREE)
        addGame(sourceId, "Mario.nes")
        writeEsdeGamelist(game("Mario.nes", name = "Super Mario Bros", playCount = 7))

        migration.reimportGamelists()

        val mario = game(sourceId, "Mario.nes")
        assertEquals("Super Mario Bros", mario.displayName)
        assertTrue(mario.isFavorite)
        assertEquals(7, mario.playCount)
        assertEquals(parseEsdeDate(LAST_PLAYED), mario.lastPlayed)
    }

    @Test
    fun `ES-DE folder gamelist reaches a file-path source`() = runBlocking {
        val sourceId = addSource(File(volume, "ROMs").path)
        addGame(sourceId, "sub/Zelda.nes")
        writeEsdeGamelist(game("sub/Zelda.nes", name = "Zelda", playCount = 2))

        migration.reimportGamelists()

        val zelda = game(sourceId, "sub/Zelda.nes")
        assertTrue(zelda.isFavorite)
        assertEquals(2, zelda.playCount)
        assertEquals(parseEsdeDate(LAST_PLAYED), zelda.lastPlayed)
    }

    @Test
    fun `ROMs folder gamelist is skipped when ES-DE has one for the system`() = runBlocking {
        val roms = File(volume, "ROMs")
        val sourceId = addSource(roms.path)
        addGame(sourceId, "Mario.nes")
        writeEsdeGamelist(game("Mario.nes", name = "From ES-DE", playCount = 3))
        writeGamelist(File(roms, "gamelists/nes"), game("Mario.nes", name = "From ROMs", playCount = 99))

        migration.reimportGamelists()

        val mario = game(sourceId, "Mario.nes")
        assertEquals("From ES-DE", mario.displayName)
        assertEquals(3, mario.playCount)
    }

    @Test
    fun `ROMs folder gamelist still applies without an ES-DE one`() = runBlocking {
        val roms = File(volume, "ROMs")
        val sourceId = addSource(roms.path)
        addGame(sourceId, "Mario.nes")
        writeGamelist(File(roms, "gamelists/nes"), game("Mario.nes", name = "From ROMs", playCount = 4))

        migration.reimportGamelists()

        val mario = game(sourceId, "Mario.nes")
        assertEquals("From ROMs", mario.displayName)
        assertEquals(4, mario.playCount)
    }

    @Test
    fun `a favorite removed here stays removed on the next import`() = runBlocking {
        val sourceId = addSource(SAF_TREE)
        addGame(sourceId, "Mario.nes")
        writeEsdeGamelist(game("Mario.nes", name = "Mario", playCount = 1))
        migration.reimportGamelists()
        db.gameDao().setFavorite(game(sourceId, "Mario.nes").id, false)

        migration.reimportGamelists()

        assertFalse(game(sourceId, "Mario.nes").isFavorite)
    }

    @Test
    fun `a stale ES-DE gamelist never lowers play stats`() = runBlocking {
        val sourceId = addSource(SAF_TREE)
        addGame(sourceId, "Mario.nes", playCount = 50, lastPlayed = parseEsdeDate("20300101T000000"))
        writeEsdeGamelist(game("Mario.nes", name = "Mario", playCount = 1))

        migration.reimportGamelists()

        val mario = game(sourceId, "Mario.nes")
        assertEquals(50, mario.playCount)
        assertEquals(parseEsdeDate("20300101T000000"), mario.lastPlayed)
    }

    @Test
    fun `an unchanged gamelist is skipped unless forced`() = runBlocking {
        val sourceId = addSource(SAF_TREE)
        addGame(sourceId, "Mario.nes")
        writeEsdeGamelist(game("Mario.nes", name = "Mario", playCount = 5))
        migration.reimportGamelists(force = false)
        val imported = game(sourceId, "Mario.nes")
        assertEquals(5, imported.playCount)
        // A game the file now matches for the first time, as after a scan.
        db.gameDao().update(imported.copy(playCount = 0))

        migration.reimportGamelists(force = false)
        assertEquals(0, game(sourceId, "Mario.nes").playCount)

        migration.reimportGamelists(force = true)
        assertEquals(5, game(sourceId, "Mario.nes").playCount)
    }

    @Test
    fun `a changed gamelist is read without forcing`() = runBlocking {
        val sourceId = addSource(SAF_TREE)
        addGame(sourceId, "Mario.nes")
        writeEsdeGamelist(game("Mario.nes", name = "Mario", playCount = 5))
        migration.reimportGamelists(force = false)
        assertEquals(5, game(sourceId, "Mario.nes").playCount)

        writeEsdeGamelist(game("Mario.nes", name = "Mario", playCount = 12))
        esdeGamelist().setLastModified(esdeGamelist().lastModified() + 10_000)

        migration.reimportGamelists(force = false)
        assertEquals(12, game(sourceId, "Mario.nes").playCount)
    }

    private suspend fun addSource(treeUri: String): Long =
        db.sourceDao().insert(SourceEntity(treeUri = treeUri, displayPath = treeUri))

    private suspend fun addGame(sourceId: Long, path: String, playCount: Int = 0, lastPlayed: Long? = null) {
        db.gameDao().insert(
            GameEntity(
                sourceId = sourceId,
                systemId = "nes",
                relativePath = path,
                displayName = path.substringAfterLast('/').substringBeforeLast('.'),
                playCount = playCount,
                lastPlayed = lastPlayed,
            ),
        )
    }

    private suspend fun game(sourceId: Long, path: String): GameEntity =
        db.gameDao().findByTriple(sourceId, "nes", path)!!

    private fun game(path: String, name: String, playCount: Int) = """
        <game>
          <path>./$path</path>
          <name>$name</name>
          <favorite>true</favorite>
          <playcount>$playCount</playcount>
          <lastplayed>$LAST_PLAYED</lastplayed>
        </game>
    """.trimIndent()

    private fun writeEsdeGamelist(vararg games: String) =
        writeGamelist(File(volume, "ES-DE/gamelists/nes"), *games)

    private fun esdeGamelist() = File(volume, "ES-DE/gamelists/nes/gamelist.xml")

    private fun writeGamelist(dir: File, vararg games: String) {
        dir.mkdirs()
        File(dir, "gamelist.xml").writeText(
            "<?xml version=\"1.0\"?>\n<gameList>\n${games.joinToString("\n")}\n</gameList>",
        )
    }

    private companion object {
        const val SAF_TREE = "content://com.android.externalstorage.documents/tree/primary%3AROMs"
        const val LAST_PLAYED = "20240115T193000"
    }
}
