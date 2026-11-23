package jr.brian.frontend.library.art

import android.app.Application
import android.os.Environment
import androidx.room.Room
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.LibraryDatabase
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemEntity
import jr.brian.frontend.library.scan.StubSystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import jr.brian.frontend.scrape.LibraryScraper
import jr.brian.frontend.scrape.ScrapeRequest
import jr.brian.frontend.scrape.ScrapeState
import jr.brian.frontend.scrape.ScrapedMedia
import jr.brian.frontend.scrape.ScraperMediaKind
import jr.brian.frontend.scrape.ScreenScraperClient
import jr.brian.frontend.scrape.SteamGridDbClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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

/** The read API, explicit-only scraping, and scrape isolation from Classic. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class JarnLibraryDataTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val catalog = StubSystemCatalog(
        SystemDef(id = "gba", displayName = "GBA", extensions = setOf("gba")),
        SystemDef(id = "nds", displayName = "DS", extensions = setOf("nds")),
    )
    private val launches = MutableStateFlow<List<RecentLaunch>>(emptyList())
    private val visibility = MutableStateFlow(LibraryVisibility())
    private val scheduler = mockk<MissingArtScheduler>(relaxed = true)
    private val scraper = mockk<LibraryScraper>(relaxed = true)

    private lateinit var db: LibraryDatabase
    private lateinit var esde: File
    private lateinit var retroHrai: File
    private lateinit var jarnScraped: File
    private lateinit var roots: ArtRoots
    private lateinit var indexes: ArtIndexes
    private lateinit var store: ArtSourceStore
    private lateinit var resolver: GameArtResolver
    private lateinit var library: JarnLibraryData

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(app, LibraryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        esde = temp.newFolder("esde")
        retroHrai = temp.newFolder("hrai")
        jarnScraped = temp.newFolder("jarn")
        val shared = temp.newFolder("shared")
        roots = ArtRoots({ esde }, retroHrai, { shared }, jarnScraped)
        indexes = ArtIndexes(roots, catalog)
        store = ArtSourceStore(app)
        store.setSource(ArtSource.DEFAULT)
        resolver = GameArtResolver(indexes, roots, db.mediaDao(), store) { emptyMap() }
        every { scraper.state } returns MutableStateFlow<ScrapeState>(ScrapeState.Idle)
        library = JarnLibraryData(
            systemDao = db.systemDao(),
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            resolver = resolver,
            indexes = indexes,
            store = store,
            scraper = scraper,
            scheduler = scheduler,
            recentLaunches = { launches },
            visibilitySource = { visibility },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `systems list only those with visible games`() = runBlocking {
        insert("gba", "A.gba")
        insert("gba", "B.gba")
        insert("nds", "Hidden.nds", hidden = true)

        assertEquals(listOf(LibrarySystem("gba", "gba", 2)), library.systems.first().items)
    }

    @Test
    fun `games and favorites exclude hidden`() = runBlocking {
        insert("gba", "A.gba", favorite = true)
        insert("gba", "H.gba", favorite = true, hidden = true)

        assertEquals(listOf("A"), library.games("gba").first().items.map { it.title })
        assertEquals(listOf("A"), library.favorites.first().items.map { it.title })
    }

    @Test
    fun `recent merges launch history with imported dates newest first`() = runBlocking {
        val launched = insert("gba", "Launched.gba")
        insert("gba", "Imported.gba", lastPlayed = 200L)
        insert("gba", "Never.gba")
        launches.value = listOf(RecentLaunch("gba", launched.relativePath, 300L))

        assertEquals(listOf("Launched", "Imported"), library.recent().first().items.map { it.title })
    }

    @Test
    fun `art follows the preference and status reports each source`() = runBlocking {
        write(esde, "gba/covers/A.png")
        write(retroHrai, "gba/covers/A.png")
        val game = insert("gba", "A.gba").toLibraryGame()

        assertEquals(ArtSource.EsDe, library.art(game).first().cover?.source)
        library.setArtSource(ArtSource.RetroHrai)
        assertEquals(ArtSource.RetroHrai, library.art(game).first().cover?.source)

        val status = library.artSourceStatus().associateBy { it.source }
        assertTrue(status.getValue(ArtSource.EsDe).isAvailable)
        assertTrue(status.getValue(ArtSource.RetroHrai).isAvailable)
        assertFalse(status.getValue(ArtSource.Scraped).isAvailable)
    }

    @Test
    fun `user hide rules filter every feed and are counted`() = runBlocking {
        val hiddenGame = insert("gba", "Hidden.gba", favorite = true)
        insert("gba", "Shown.gba", favorite = true)
        insert("nds", "Other.nds")
        visibility.value = LibraryVisibility(
            hiddenSystems = setOf("nds"),
            hiddenGames = setOf("gba/${hiddenGame.relativePath}"),
        )

        val systems = library.systems.first()
        assertEquals(listOf(LibrarySystem("gba", "gba", 1)), systems.items)
        assertEquals(1, systems.hiddenCount)
        assertEquals(listOf("Shown"), library.games("gba").first().items.map { it.title })
        assertEquals(1, library.favorites.first().hiddenCount)
    }

    @Test
    fun `game keys are stable and survive colons in file names`() = runBlocking {
        val game = insert("gba", "Zelda: Minish Cap.gba").toLibraryGame()

        assertEquals(game.id, library.findGame(game.key)?.id)
        assertEquals(null, library.findGame("not a key"))
    }

    @Test
    fun `tile art uses the chosen source and falls back to other kinds`() = runBlocking {
        write(esde, "gba/covers/A.png")
        write(retroHrai, "gba/screenshots/A_ss_0.png")
        val game = insert("gba", "A.gba").toLibraryGame()
        val feed = kotlinx.coroutines.flow.flowOf(VisibleList(listOf(game)))

        assertTrue(library.withArt(feed).first().items.single().artPath!!.endsWith("covers/A.png"))
        File(esde, "gba/covers/A.png").delete()
        library.refreshArt()
        assertTrue(library.withArt(feed).first().items.single().artPath!!.endsWith("A_ss_0.png"))
    }

    @Test
    fun `reads never schedule a scrape`() = runBlocking {
        val game = insert("gba", "A.gba").toLibraryGame()
        every { scraper.canScrape(any()) } returns true

        library.systems.first()
        library.games("gba").first()
        library.favorites.first()
        library.recent().first()
        library.art(game).first()
        library.artSourceStatus()
        library.setArtSource(ArtSource.Scraped)
        library.refreshArt()

        verify(exactly = 0) { scheduler.schedule(any()) }
    }

    @Test
    fun `scrape runs only when called and only if configured`() {
        every { scraper.canScrape(any()) } returns false
        assertEquals(ScrapeMissingArtResult.NotConfigured, library.scrapeMissingArt("gba"))
        verify(exactly = 0) { scheduler.schedule(any()) }

        every { scraper.canScrape(any()) } returns true
        assertEquals(ScrapeMissingArtResult.Started, library.scrapeMissingArt("gba"))
        verify(exactly = 1) { scheduler.schedule("gba") }
    }

    @Test
    fun `missing art scrape requests only gaps and skips complete games`() = runBlocking {
        val partial = insert("gba", "Partial.gba")
        val complete = insert("gba", "Complete.gba")
        write(esde, "gba/covers/Partial.png")
        listOf("covers", "marquees", "fanart", "screenshots", "titlescreens", "miximages")
            .forEach { write(esde, "gba/$it/Complete.png") }
        var captured: Pair<List<GameEntity>, ScrapeRequest>? = null
        coEvery { scraper.scrape(any(), any()) } answers {
            captured = firstArg<List<GameEntity>>() to secondArg()
            jr.brian.frontend.scrape.ScrapeSummary()
        }

        MissingArtScraper(db.gameDao(), resolver, indexes, roots, scraper, { visibility }).run("gba")

        val (games, request) = captured!!
        assertEquals(listOf(partial.id), games.map { it.id })
        assertEquals(jarnScraped, request.isolatedRoot)
        val kinds = request.kindsForGame!!(partial)
        assertFalse(ScraperMediaKind.Covers in kinds)
        assertFalse(ScraperMediaKind.Videos in kinds)
        assertTrue(ScraperMediaKind.Marquees in kinds)
        assertTrue(request.kindsForGame!!(complete).isEmpty())
    }

    @Test
    fun `isolated scrape writes to its folder and leaves the classic row alone`() = runBlocking {
        val game = insert("gba", "A.gba")
        val classicRow = MediaEntity(gameId = game.id, coverPath = "/classic/cover.png")
        db.mediaDao().upsert(classicRow)
        val rowBefore = db.mediaDao().findByGameId(game.id)
        val real = realScraper(ScrapedMedia(ScraperMediaKind.Covers, URL, "png"))

        val summary = real.scrape(listOf(game), ScrapeRequest(isolatedRoot = jarnScraped))

        assertEquals(1, summary.downloads)
        assertTrue(File(jarnScraped, "gba/covers/A.png").exists())
        assertEquals(rowBefore, db.mediaDao().findByGameId(game.id))
    }

    @Test
    fun `default scrape still updates the classic row`() = runBlocking {
        val game = insert("gba", "A.gba")
        val real = realScraper(ScrapedMedia(ScraperMediaKind.Marquees, URL, "png"))
        mockkStatic(Environment::class)
        every { Environment.isExternalStorageManager() } returns false

        val summary = try {
            real.scrape(listOf(game), ScrapeRequest())
        } finally {
            unmockkStatic(Environment::class)
        }

        assertEquals(real.state.value.toString(), 1, summary.downloads)
        val path = db.mediaDao().findByGameId(game.id)?.marqueePath.orEmpty()
        assertTrue(path, path.endsWith("Jarngreipr Media/gba/marquees/A.png"))
    }

    private fun realScraper(media: ScrapedMedia): LibraryScraper {
        val screenScraper = mockk<ScreenScraperClient>()
        every { screenScraper.isReady() } returns true
        coEvery {
            screenScraper.lookup(any(), any(), any(), any(), any())
        } returns Result.success(listOf(media))
        coEvery { screenScraper.download(any(), any()) } answers {
            val dest = secondArg<File>()
            if (dest.startsWith(temp.root)) {
                dest.parentFile?.mkdirs()
                dest.writeBytes(byteArrayOf(1))
            }
            true
        }
        val steamGridDb = mockk<SteamGridDbClient>()
        every { steamGridDb.isReady() } returns false
        return LibraryScraper(
            context = RuntimeEnvironment.getApplication(),
            screenScraper = screenScraper,
            steamGridDb = steamGridDb,
            catalog = catalog,
            gameDao = db.gameDao(),
            mediaDao = db.mediaDao(),
            scrapeJobDao = db.scrapeJobDao(),
        )
    }

    private suspend fun insert(
        systemId: String,
        relativePath: String,
        favorite: Boolean = false,
        hidden: Boolean = false,
        lastPlayed: Long? = null,
    ): GameEntity {
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
            isFavorite = favorite,
            isHidden = hidden,
            lastPlayed = lastPlayed,
        )
        return entity.copy(id = db.gameDao().insert(entity))
    }

    private fun write(root: File, path: String) {
        File(root, path).apply {
            parentFile?.mkdirs()
            writeBytes(byteArrayOf(1))
        }
    }

    private companion object {
        const val TREE_URI = "/roms"
        const val URL = "https://screenscraper.fr/media"
    }
}
