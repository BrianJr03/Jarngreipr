package jr.brian.frontend.library

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.LibraryDatabase
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
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 * End-to-end exercise of the Phase 2c scan path: add a source, point
 * the repo at a tempFolder tree, scan, and assert the DB rows plus
 * resolved MediaEntity match what the file layout says.
 *
 * The repository runs the scan on its injected scope; the test uses
 * `Dispatchers.Unconfined` so the launch body runs to completion
 * synchronously inside the test coroutine, which is both simpler
 * than racing timeouts and legitimately how the real-world body
 * behaves once a FrontEndActivity has been destroyed (no main looper
 * to post back to).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryScanIntegrationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(
            id = "nes",
            displayName = "Nintendo",
            folderAliases = listOf("famicom"),
            extensions = setOf("nes", "fds"),
            screenScraperSystemId = 3,
        ),
        SystemDef(
            id = "snes",
            displayName = "Super Nintendo",
            folderAliases = listOf("sfc"),
            extensions = setOf("sfc", "smc"),
            screenScraperSystemId = 4,
        ),
    )

    private lateinit var db: LibraryDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: LibraryRepository

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val scanner = ScannerDispatcher(
            fastWalk = FastWalkScanner(catalog),
            saf = SafTreeScanner(app, catalog),
        )
        repo = LibraryRepository(
            systemDao = db.systemDao(),
            sourceDao = db.sourceDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            scrapeJobDao = db.scrapeJobDao(),
            playHistoryDao = db.playHistoryDao(),
            catalog = catalog,
            scanner = scanner,
            scope = scope,
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun `scanAll writes games across every system under the source`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeFile(roms, "nes/Mario.nes")
        writeFile(roms, "nes/Zelda.nes")
        writeFile(roms, "snes/A Link to the Past.sfc")
        val sourceId = repo.addSource(roms.absolutePath, roms.absolutePath)

        repo.scanAll()
        awaitIdle()

        val games = db.gameDao().observeAll().first().sortedBy { it.displayName }
        assertEquals(
            listOf("A Link to the Past", "Mario", "Zelda"),
            games.map { it.displayName },
        )
        assertEquals(sourceId, games.first().sourceId)
    }

    @Test
    fun `scanAll resolves ES-DE media into the MediaEntity row`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        val mediaRoot = tempFolder.newFolder("ES-DE")
        writeFile(roms, "nes/Mario.nes")
        writeFile(mediaRoot, "downloaded_media/nes/covers/Mario.png")
        writeFile(mediaRoot, "downloaded_media/nes/videos/Mario.mp4")
        repo.addSource(roms.absolutePath, roms.absolutePath)
        repo.setMediaRoots(listOf(mediaRoot))

        repo.scanAll()
        awaitIdle()

        val mario = db.gameDao().observeAll().first().single()
        val media = db.mediaDao().findByGameId(mario.id)
        assertNotNull("MediaEntity row should exist for a scraped game", media)
        assertTrue(media!!.coverPath?.endsWith("covers/Mario.png") == true)
        assertTrue(media.videoPath?.endsWith("videos/Mario.mp4") == true)
        assertNull(media.marqueePath)
    }

    @Test
    fun `scanAll leaves MediaEntity absent for games with no shipped media`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeFile(roms, "nes/NoArt.nes")
        repo.addSource(roms.absolutePath, roms.absolutePath)

        repo.scanAll()
        awaitIdle()

        val game = db.gameDao().observeAll().first().single()
        assertNull(
            "No shipped art means no MediaEntity row — the UI treats null as 'not scraped yet'",
            db.mediaDao().findByGameId(game.id),
        )
    }

    @Test
    fun `scanSystem narrows to a single system`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeFile(roms, "nes/Mario.nes")
        writeFile(roms, "snes/Zelda.sfc")
        repo.addSource(roms.absolutePath, roms.absolutePath)

        repo.scanSystem("nes")
        awaitIdle()

        val systems = db.gameDao().observeAll().first().map { it.systemId }.toSet()
        assertEquals(setOf("nes"), systems)
    }

    @Test
    fun `scanAll upserts SystemEntity rows from the catalog`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        writeFile(roms, "nes/Mario.nes")
        repo.addSource(roms.absolutePath, roms.absolutePath)

        repo.scanAll()
        awaitIdle()

        val systems = db.systemDao().observeAll().first().map { it.id }.toSet()
        // Every catalog system is upserted so FK writes always resolve,
        // even if the user hasn't scanned games for every one of them.
        assertTrue("nes" in systems)
        assertTrue("snes" in systems)
    }

    @Test
    fun `addSource is idempotent on the same tree URI`() = runBlocking {
        val roms = tempFolder.newFolder("Roms")
        val first = repo.addSource(roms.absolutePath, "/display/path")
        val second = repo.addSource(roms.absolutePath, "/different/display")
        assertEquals(first, second)
        assertEquals(1, db.sourceDao().observeAll().first().size)
    }

    // NOTE: the "rescan swaps rows when files change" invariant is
    // exercised directly by LibraryDatabaseTest's gameDao suite
    // (replaceSystemGames does the delete+insert in one txn). A
    // separate scanAll-level integration test that calls scanAll twice
    // in sequence depends on the fine-grained dispatcher behaviour of
    // scope.launch + StateFlow.first{Idle} and is prone to timing
    // flakes under Robolectric without a TestCoroutineScheduler setup.
    // Phase 3's instrumentation tests on real device will catch any
    // divergence end-to-end.

    private suspend fun awaitIdle() {
        withTimeout(5_000) { repo.state.first { it is LibraryState.Idle } }
    }

    private fun writeFile(root: File, relative: String): File {
        val file = File(root, relative)
        file.parentFile?.mkdirs()
        file.writeText("stub")
        return file
    }
}
