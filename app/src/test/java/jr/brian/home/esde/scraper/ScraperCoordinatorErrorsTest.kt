package jr.brian.home.esde.scraper

import android.app.Application
import io.mockk.coEvery
import io.mockk.mockk
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.model.ScrapeRequest
import jr.brian.home.esde.scraper.model.ScrapeState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * Phase 0 §6: ScreenScraper and SteamGridDB HTTP failures used to collapse
 * to empty-list results that the UI reported as "0 found". These tests
 * pin that the coordinator now carries the error messages through to
 * [ScrapeState.Finished].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ScraperCoordinatorErrorsTest {

    private lateinit var app: Application
    private lateinit var screenScraper: ScreenScraperClient
    private lateinit var steamGridDb: SteamGridDbClient
    private lateinit var hashCache: HashCache
    private lateinit var coordinator: ScraperCoordinator

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        screenScraper = mockk(relaxed = true)
        steamGridDb = mockk(relaxed = true)
        hashCache = mockk(relaxed = true)
        coordinator = ScraperCoordinator(
            context = app,
            screenScraper = screenScraper,
            steamGridDb = steamGridDb,
            hashCache = hashCache,
        )
    }

    @Test
    fun `ScreenScraper HTTP 429 propagates into ScrapeState errorMessages`() = runBlocking {
        coEvery {
            screenScraper.lookup(any(), any(), any(), any(), any())
        } returns Result.failure(IOException("HTTP 429"))

        val game = GameInfo(
            path = "./Super Mario 64.z64",
            name = "Super Mario 64",
            systemName = "n64",
        )
        val request = ScrapeRequest(
            games = listOf(game),
            mediaTypesScreenScraper = setOf(ScraperMediaKind.Covers),
            mediaTypesSteamGridDb = emptySet(),
            screenScraperEnabled = true,
            steamGridDbEnabled = false,
        )

        coordinator.start(
            request = request,
            screenScraperCredentials = ScreenScraperClient.Credentials(
                devId = "dev-id",
                devPassword = "dev-pass",
                userId = "",
                userPassword = "",
            ),
            steamGridDbApiKey = null,
        )

        val finished = coordinator.state.first { it is ScrapeState.Finished } as ScrapeState.Finished
        assertEquals(1, finished.processed)
        assertEquals(0, finished.downloads)
        assertEquals(1, finished.errors)
        assertEquals(1, finished.errorMessages.size)
        val error = finished.errorMessages.single()
        assertEquals("Super Mario 64.z64", error.gameTitle)
        assertTrue(
            "message must mention the HTTP code so users can tell it's a quota issue " +
                "rather than silently 0 found; was: ${error.message}",
            error.message.contains("429"),
        )
        assertTrue(
            "message must identify the backend; was: ${error.message}",
            error.message.contains("ScreenScraper"),
        )
    }

    @Test
    fun `SteamGridDB failure propagates even when ScreenScraper succeeds-but-empty`() = runBlocking {
        coEvery {
            screenScraper.lookup(any(), any(), any(), any(), any())
        } returns Result.success(emptyList())
        coEvery {
            steamGridDb.lookup(any(), any(), any())
        } returns Result.failure(IOException("HTTP 500"))

        val game = GameInfo(path = "./Game.rom", name = "Game", systemName = "n64")
        val request = ScrapeRequest(
            games = listOf(game),
            mediaTypesScreenScraper = setOf(ScraperMediaKind.Covers),
            mediaTypesSteamGridDb = setOf(ScraperMediaKind.Covers),
            screenScraperEnabled = true,
            steamGridDbEnabled = true,
        )

        coordinator.start(
            request = request,
            screenScraperCredentials = ScreenScraperClient.Credentials(
                devId = "dev-id",
                devPassword = "dev-pass",
                userId = "",
                userPassword = "",
            ),
            steamGridDbApiKey = "steamgriddb-key",
        )

        val finished = coordinator.state.first { it is ScrapeState.Finished } as ScrapeState.Finished
        assertEquals(1, finished.errorMessages.size)
        assertTrue(finished.errorMessages.single().message.contains("SteamGridDB"))
    }

    @Test
    fun `no failures leaves errorMessages empty`() = runBlocking {
        coEvery {
            screenScraper.lookup(any(), any(), any(), any(), any())
        } returns Result.success(emptyList())

        val request = ScrapeRequest(
            games = listOf(
                GameInfo(path = "./Game.rom", name = "Game", systemName = "n64"),
            ),
            mediaTypesScreenScraper = setOf(ScraperMediaKind.Covers),
            mediaTypesSteamGridDb = emptySet(),
            screenScraperEnabled = true,
            steamGridDbEnabled = false,
        )

        coordinator.start(
            request = request,
            screenScraperCredentials = ScreenScraperClient.Credentials(
                devId = "dev-id",
                devPassword = "dev-pass",
                userId = "",
                userPassword = "",
            ),
            steamGridDbApiKey = null,
        )

        val finished = coordinator.state.first { it is ScrapeState.Finished } as ScrapeState.Finished
        assertEquals(0, finished.errors)
        assertTrue(finished.errorMessages.isEmpty())
        assertFalse(
            "no-error finished state should NOT emit a stray error message",
            finished.errorMessages.isNotEmpty(),
        )
    }
}
