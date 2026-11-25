package jr.brian.frontend.library

import android.app.Application
import androidx.room.Room
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.PlayHistoryEntity
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
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

/** #459: a library root and a system folder inside it must list each game once. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LibraryOverlapTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val n3ds = SystemDef(
        id = "n3ds",
        displayName = "Nintendo 3DS",
        folderAliases = emptyList(),
        extensions = setOf("3ds"),
        screenScraperSystemId = 17,
    )
    private val catalog = StubSystemCatalog(n3ds)

    private lateinit var db: LibraryDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: LibraryRepository
    private lateinit var root: File
    private lateinit var systemFolder: File

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repo = LibraryRepository(
            systemDao = db.systemDao(),
            sourceDao = db.sourceDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            scrapeJobDao = db.scrapeJobDao(),
            playHistoryDao = db.playHistoryDao(),
            catalog = catalog,
            scanner = ScannerDispatcher(FastWalkScanner(catalog), SafTreeScanner(app, catalog)),
            scope = scope,
        )
        root = tempFolder.newFolder("Ayn Thor")
        systemFolder = File(root, "n3ds").apply { mkdirs() }
        File(systemFolder, "Zelda.3ds").writeText("stub")
        File(systemFolder, "Mario.3ds").writeText("stub")
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    @Test
    fun `system folder first then root lists each game once`() = runBlocking {
        val (_, folder) = addBoth()
        scanFolder(folder)
        scanRoot()

        assertOneRowEach(folder)
    }

    @Test
    fun `root first then system folder lists each game once`() = runBlocking {
        val (_, folder) = addBoth()
        scanRoot()
        scanFolder(folder)
        scanRoot()

        assertOneRowEach(folder)
    }

    @Test
    fun `merge keeps play history favorites and art from the dropped copy`() = runBlocking {
        val (rootSource, folder) = addBoth()
        scanRoot()
        val rootCopy = gamesOf(rootSource).first { it.relativePath == "Zelda.3ds" }
        db.gameDao().update(rootCopy.copy(isFavorite = true, playCount = 3, playTimeMinutes = 40, lastPlayed = 9L))
        db.playHistoryDao().insert(PlayHistoryEntity(gameId = rootCopy.id, startedAt = 5L, durationSeconds = 60))
        db.mediaDao().upsert(MediaEntity(gameId = rootCopy.id, coverPath = "/zelda.png"))

        scanFolder(folder)

        val kept = gamesOf(folder).single { it.relativePath == "Zelda.3ds" }
        assertTrue(kept.isFavorite)
        assertEquals(3, kept.playCount)
        assertEquals(40, kept.playTimeMinutes)
        assertEquals(9L, kept.lastPlayed)
        assertEquals(1, db.playHistoryDao().observeForGame(kept.id).first().size)
        assertEquals("/zelda.png", db.mediaDao().findByGameId(kept.id)?.coverPath)
        assertOneRowEach(folder)
    }

    @Test
    fun `emptied parent root is reported as redundant`() = runBlocking {
        val (rootSource, folder) = addBoth()
        scanRoot()
        scanFolder(folder)
        scanRoot()

        assertEquals(listOf(rootSource.id), repo.redundantParentSources().map { it.id })
    }

    @Test
    fun `a root with no system folder source inside keeps its games`() = runBlocking {
        val rootId = repo.addSource(root.absolutePath, root.absolutePath)
        scanRoot()

        assertEquals(2, gamesOf(db.sourceDao().findById(rootId)!!).size)
        assertTrue(repo.redundantParentSources().isEmpty())
    }

    private suspend fun addBoth(): Pair<SourceEntity, SourceEntity> {
        val rootId = repo.addSource(root.absolutePath, root.absolutePath)
        val folderId = repo.addSource(systemFolder.absolutePath, systemFolder.absolutePath)
        return db.sourceDao().findById(rootId)!! to db.sourceDao().findById(folderId)!!
    }

    private suspend fun scanRoot() {
        repo.scanAll()
        awaitIdle()
    }

    private suspend fun scanFolder(folder: SourceEntity) {
        assertTrue(repo.scanSources(listOf(SourceScanTarget(folder, systemFolder = n3ds))))
        awaitIdle()
    }

    private suspend fun awaitIdle() {
        withTimeout(5_000) { repo.state.first { it is LibraryState.Idle } }
    }

    private suspend fun gamesOf(source: SourceEntity): List<GameEntity> =
        db.gameDao().getBySystemAndSource("n3ds", source.id)

    private suspend fun assertOneRowEach(folder: SourceEntity) {
        val all = db.gameDao().getBySystem("n3ds")
        assertEquals(listOf("Mario.3ds", "Zelda.3ds"), all.map { it.relativePath }.sorted())
        all.forEach { assertEquals(folder.id, it.sourceId) }
        assertNotNull(all.first().absolutePath)
    }
}
