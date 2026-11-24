package jr.brian.frontend.scrape

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/** Every response is faked by an interceptor, so nothing here reaches the network. */
class ScraperConnectionTesterTest {
    private var requests = mutableListOf<Request>()

    private fun tester(
        code: Int = 200,
        body: String = "{}",
        failure: IOException? = null,
        hasDev: Boolean = true,
        saved: UserScraperCredentials.Snapshot = UserScraperCredentials.Snapshot("", "", ""),
    ): ScraperConnectionTester {
        val http = OkHttpClient.Builder().addInterceptor(
            Interceptor { chain ->
                requests += chain.request()
                failure?.let { throw it }
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message("fake")
                    .body(body.toResponseBody("application/json".toMediaType()))
                    .build()
            },
        ).build()
        val config = mockk<ScreenScraperConfig>()
        every { config.hasDevCredentials } returns hasDev
        every { config.devId } returns "dev"
        every { config.devPassword } returns "devpass"
        val credentials = mockk<UserScraperCredentials>()
        every { credentials.state } returns MutableStateFlow(saved)
        return ScraperConnectionTester(
            http = http,
            json = Json { ignoreUnknownKeys = true },
            screenScraperLimiter = ScraperRateLimiter(initialMinIntervalMs = 0),
            steamGridDbLimiter = ScraperRateLimiter(initialMinIntervalMs = 0),
            config = config,
            saved = credentials,
        )
    }

    @Test
    fun `valid accounts pass`() = runBlocking {
        val screenScraper = tester(body = """{"response":{"ssuser":{"id":"a"}}}""")
        assertEquals(ScraperConnectionResult.Ok, screenScraper.testScreenScraper("user", "pass"))
        val steamGridDb = tester(body = """{"success":true,"data":[]}""")
        assertEquals(ScraperConnectionResult.Ok, steamGridDb.testSteamGridDb("key"))
    }

    @Test
    fun `http failures map to a fixed result`() = runBlocking {
        val expected = mapOf(
            401 to ScraperConnectionResult.BadCredentials,
            403 to ScraperConnectionResult.BadCredentials,
            429 to ScraperConnectionResult.RateLimited,
            430 to ScraperConnectionResult.RateLimited,
            500 to ScraperConnectionResult.ServiceProblem,
        )
        expected.forEach { (code, result) ->
            assertEquals(result, tester(code = code).testSteamGridDb("key"))
        }
    }

    @Test
    fun `network failure and bad bodies are reported without detail`() = runBlocking {
        assertEquals(
            ScraperConnectionResult.Offline,
            tester(failure = IOException("secret-in-message")).testSteamGridDb("key"),
        )
        assertEquals(
            ScraperConnectionResult.ServiceProblem,
            tester(body = "not json").testScreenScraper("user", "pass"),
        )
    }

    @Test
    fun `a key that cannot be sent is a credential problem`() = runBlocking {
        assertEquals(ScraperConnectionResult.BadCredentials, tester().testSteamGridDb("bad\nkey"))
    }

    @Test
    fun `missing account and missing dev keys never make a request`() = runBlocking {
        assertEquals(ScraperConnectionResult.MissingAccount, tester().testScreenScraper(null, null))
        assertEquals(ScraperConnectionResult.MissingAccount, tester().testSteamGridDb(null))
        assertEquals(
            ScraperConnectionResult.NotAvailable,
            tester(hasDev = false).testScreenScraper("user", "pass"),
        )
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `null draft values fall back to saved credentials`() = runBlocking {
        val saved = UserScraperCredentials.Snapshot("saved-user", "saved-pass", "saved-key")
        tester(body = """{"response":{"ssuser":{}}}""", saved = saved).testScreenScraper(null, "typed")
        val url = requests.single().url
        assertEquals("saved-user", url.queryParameter("ssid"))
        assertEquals("typed", url.queryParameter("sspassword"))
    }
}
