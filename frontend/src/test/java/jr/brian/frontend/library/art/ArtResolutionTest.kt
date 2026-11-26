package jr.brian.frontend.library.art

import android.app.Application
import androidx.room.Room
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.media.MediaKind
import jr.brian.frontend.library.media.RetroHraiLayout
import jr.brian.frontend.library.scan.FastWalkScanner
import jr.brian.frontend.library.scan.SafTreeScanner
import jr.brian.frontend.library.scan.ScannerDispatcher
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.LibraryState
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
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

/** Source priority, RetroHrai naming, and the guarantee Classic is unchanged. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ArtResolutionTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(id = "gba", displayName = "GBA", extensions = setOf("gba")),
        SystemDef(id = "n3ds", displayName = "3DS", folderAliases = listOf("3ds"), extensions = setOf("3ds")),
        SystemDef(id = "xbox360", displayName = "360", extensions = setOf("iso")),
    )

    private lateinit var db: LibraryDatabase
    private lateinit var esde: File
    private lateinit var retroHrai: File
    private lateinit var jarnScraped: File
    private lateinit var sharedScraped: File
    private lateinit var indexes: ArtIndexes
    private lateinit var store: ArtSourceStore
    private lateinit var resolver: GameArtResolver

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        esde = temp.newFolder("ES-DE", "downloaded_media")
        retroHrai = temp.newFolder("RetroHrai!", "media")
        jarnScraped = temp.newFolder("Jarn FE Media")
        sharedScraped = temp.newFolder("Jarngreipr Media")
        val roots = ArtRoots(
            esdeRoot = { esde },
            retroHraiRoot = retroHrai,
            sharedScraped = { sharedScraped },
            jarnScrapedRoot = jarnScraped,
        )
        indexes = ArtIndexes(roots, catalog)
        store = ArtSourceStore(app)
        resolver = GameArtResolver(indexes, roots, db.mediaDao(), store) { emptyMap() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `retrohrai maps logos to marquee and has no unsupported kinds`() {
        val layout = RetroHraiLayout()
        assertEquals("logos", layout.mediaDir(retroHrai, "gba", MediaKind.Marquee)?.name)
        assertEquals("covers", layout.mediaDir(retroHrai, "gba", MediaKind.Cover)?.name)
        assertEquals("fanart", layout.mediaDir(retroHrai, "gba", MediaKind.Fanart)?.name)
        assertEquals("screenshots", layout.mediaDir(retroHrai, "gba", MediaKind.Screenshot)?.name)
        listOf(
            MediaKind.Titlescreen, MediaKind.Miximage, MediaKind.PhysicalMedia, MediaKind.Video,
        ).forEach { assertNull(layout.mediaDir(retroHrai, "gba", it)) }
    }

    @Test
    fun `retrohrai suffixed files resolve and user picks win`() = runBlocking {
        write(retroHrai, "gba/covers/Kirby (USA).png")
        write(retroHrai, "gba/covers/Kirby (USA)_usr_cover_0.png")
        write(retroHrai, "gba/logos/Kirby (USA)_logo_0.png")
        write(retroHrai, "gba/fanart/Kirby (USA)_fanart_0.jpg")
        write(retroHrai, "gba/screenshots/Kirby (USA)_ss_1.png")
        write(retroHrai, "gba/screenshots/Kirby (USA)_ss_0.png")

        val art = resolver.resolve(game("gba", "Kirby (USA).gba"), ArtSource.RetroHrai)

        assertTrue(art.cover!!.path.endsWith("Kirby (USA)_usr_cover_0.png"))
        assertTrue(art.marquee!!.path.endsWith("Kirby (USA)_logo_0.png"))
        assertTrue(art.fanart!!.path.endsWith("Kirby (USA)_fanart_0.jpg"))
        assertTrue(art.screenshot!!.path.endsWith("Kirby (USA)_ss_0.png"))
    }

    @Test
    fun `retrohrai system folders reach catalog systems`() = runBlocking {
        write(retroHrai, "3ds/covers/Pilotwings.png")
        write(retroHrai, "custom_x360/covers/Halo.png")

        val n3ds = resolver.resolve(game("n3ds", "Pilotwings.3ds"), ArtSource.RetroHrai)
        val x360 = resolver.resolve(game("xbox360", "Halo.iso"), ArtSource.RetroHrai)

        assertEquals(ArtSource.RetroHrai, n3ds.cover?.source)
        assertEquals(ArtSource.RetroHrai, x360.cover?.source)
    }

    @Test
    fun `es-de video in a folder alias reaches the catalog system`() = runBlocking {
        write(esde, "3ds/videos/Pilotwings.MP4")

        val art = resolver.resolve(game("n3ds", "Pilotwings.3ds"), ArtSource.EsDe)

        assertEquals(File(esde, "3ds/videos/Pilotwings.MP4").path, art.video?.path)
        assertEquals(ArtSource.EsDe, art.video?.source)
    }

    @Test
    fun `chosen source wins and others fill gaps per kind`() = runBlocking {
        write(esde, "gba/covers/Sonic.png")
        write(esde, "gba/marquees/Sonic.png")
        write(retroHrai, "gba/covers/Sonic.png")
        write(sharedScraped, "gba/miximages/Sonic.png")
        val sonic = game("gba", "Sonic.gba")

        val esdeFirst = resolver.resolve(sonic, ArtSource.EsDe)
        val hraiFirst = resolver.resolve(sonic, ArtSource.RetroHrai)

        assertEquals(ArtSource.EsDe, esdeFirst.cover?.source)
        assertEquals(ArtSource.RetroHrai, hraiFirst.cover?.source)
        assertEquals(ArtSource.EsDe, hraiFirst.marquee?.source)
        assertEquals(ArtSource.Scraped, hraiFirst.miximage?.source)
        assertNull(hraiFirst.video)
    }

    @Test
    fun `library row fills a gap only when its file still exists`() = runBlocking {
        val kept = write(temp.root, "elsewhere/cover.png")
        val game = game("gba", "Metroid.gba")
        db.mediaDao().upsert(
            MediaEntity(gameId = game.id, coverPath = kept.path, fanartPath = "/missing/fanart.png"),
        )

        val art = resolver.resolve(game, ArtSource.EsDe)

        assertEquals(kept.path, art.cover?.path)
        assertNull(art.cover?.source)
        assertNull(art.fanart)
    }

    @Test
    fun `resolving art never changes the user's folders or the media table`() = runBlocking {
        write(esde, "gba/covers/Sonic.png")
        write(retroHrai, "gba/logos/Sonic_logo_0.png")
        val before = snapshot(esde) + snapshot(retroHrai)

        resolver.resolve(game("gba", "Sonic.gba"), ArtSource.RetroHrai)

        assertEquals(before, snapshot(esde) + snapshot(retroHrai))
        assertTrue(db.mediaDao().observeAll().first().isEmpty())
    }

    @Test
    fun `classic scan keeps its winner whatever the jarn fe source is`() = runBlocking {
        write(esde, "gba/covers/Sonic.png")
        write(retroHrai, "gba/covers/Sonic.png")
        write(retroHrai, "gba/logos/Sonic_logo_0.png")
        write(sharedScraped, "gba/covers/Sonic.png")
        write(sharedScraped, "gba/fanart/Sonic.png")
        write(jarnScraped, "gba/screenshots/Sonic.png")
        val roms = temp.newFolder("Roms")
        write(roms, "gba/Sonic.gba")

        ArtSource.entries.forEach { source ->
            store.setSource(source)
            val media = classicScan(roms)

            assertEquals(File(esde, "gba/covers/Sonic.png").path, media.coverPath)
            assertEquals(File(sharedScraped, "gba/fanart/Sonic.png").path, media.fanartPath)
            assertNull("Classic must not read RetroHrai logo names", media.marqueePath)
            assertNull("Classic must not read Jarn FE's folder", media.screenshotPath)
        }
    }

    private suspend fun classicScan(roms: File): MediaEntity {
        val app = RuntimeEnvironment.getApplication()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val repo = LibraryRepository(
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
            repo.setMediaRoots(listOf(esde, retroHrai, sharedScraped))
            repo.addSource(roms.absolutePath, roms.absolutePath)
            repo.scanAll()
            withTimeout(SCAN_TIMEOUT_MS) {
                while (repo.state.value is LibraryState.Scanning) delay(10)
            }
            val scanned = db.gameDao().observeBySystem("gba").first().single()
            return db.mediaDao().findByGameId(scanned.id)!!
        } finally {
            scope.cancel()
        }
    }

    private suspend fun game(systemId: String, relativePath: String): GameEntity {
        val sourceId = db.sourceDao().findByTreeUri(TREE_URI)?.id ?: db.sourceDao().insert(
            SourceEntity(
                treeUri = TREE_URI,
                displayPath = TREE_URI,
                hasPersistedGrant = true,
                lastScannedAt = 0L,
            ),
        )
        db.systemDao().upsert(SystemEntity(id = systemId, displayName = systemId))
        val entity = GameEntity(
            sourceId = sourceId,
            systemId = systemId,
            relativePath = relativePath,
            displayName = File(relativePath).nameWithoutExtension,
        )
        return entity.copy(id = db.gameDao().insert(entity))
    }

    private fun write(root: File, path: String): File =
        File(root, path).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }

    private fun snapshot(root: File): List<String> =
        root.walkTopDown().map { "${it.path}:${it.length()}" }.sorted().toList()

    private companion object {
        const val TREE_URI = "/roms"
        const val SCAN_TIMEOUT_MS = 10_000L
    }
}
