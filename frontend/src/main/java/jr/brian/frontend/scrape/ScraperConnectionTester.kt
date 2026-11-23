package jr.brian.frontend.scrape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** A fixed outcome with no server text, so a credential can never reach the UI or logs. */
enum class ScraperConnectionResult {
    Ok,
    MissingAccount,
    BadCredentials,
    RateLimited,
    Offline,
    ServiceProblem,
    NotAvailable,
}

/**
 * Checks an account before it is saved. A null argument means "use the saved value".
 * Nothing here logs, stores, or returns a credential.
 */
@Singleton
class ScraperConnectionTester @Inject constructor(
    @Named(FrontendScraperModule.OKHTTP) private val http: OkHttpClient,
    @Named(FrontendScraperModule.JSON) private val json: Json,
    @Named(FrontendScraperModule.SCREENSCRAPER_LIMITER) private val screenScraperLimiter: ScraperRateLimiter,
    @Named(FrontendScraperModule.STEAMGRIDDB_LIMITER) private val steamGridDbLimiter: ScraperRateLimiter,
    private val config: ScreenScraperConfig,
    private val saved: UserScraperCredentials,
) {
    suspend fun testScreenScraper(userId: String?, password: String?): ScraperConnectionResult {
        if (!config.hasDevCredentials) return ScraperConnectionResult.NotAvailable
        val user = userId ?: saved.state.value.ssUserId
        val secret = password ?: saved.state.value.ssUserPassword
        if (user.isBlank() || secret.isBlank()) return ScraperConnectionResult.MissingAccount
        return request(screenScraperLimiter, ::screenScraperBodyIsValid) {
            val url = SCREENSCRAPER_USER_URL.toHttpUrl().newBuilder()
                .addQueryParameter("devid", config.devId)
                .addQueryParameter("devpassword", config.devPassword)
                .addQueryParameter("ssid", user)
                .addQueryParameter("sspassword", secret)
                .addQueryParameter("softname", SOFTNAME)
                .addQueryParameter("output", "json")
                .build()
            Request.Builder().url(url).build()
        }
    }

    suspend fun testSteamGridDb(apiKey: String?): ScraperConnectionResult {
        val key = apiKey ?: saved.state.value.steamGridDbApiKey
        if (key.isBlank()) return ScraperConnectionResult.MissingAccount
        return request(steamGridDbLimiter, ::steamGridDbBodyIsValid) {
            Request.Builder()
                .url(STEAMGRIDDB_TEST_URL)
                .header("Authorization", "Bearer $key")
                .build()
        }
    }

    private suspend fun request(
        limiter: ScraperRateLimiter,
        isValid: (String) -> Boolean,
        build: () -> Request,
    ): ScraperConnectionResult = withContext(Dispatchers.IO) {
        try {
            val call = http.newCall(build())
            limiter.withPermit { call.execute().use { classify(it, isValid) } }
        } catch (_: IOException) {
            ScraperConnectionResult.Offline
        } catch (_: IllegalArgumentException) {
            ScraperConnectionResult.BadCredentials
        }
    }

    private fun classify(response: Response, isValid: (String) -> Boolean): ScraperConnectionResult =
        when (response.code) {
            in 200..299 -> {
                val body = response.body?.string().orEmpty()
                if (isValid(body)) ScraperConnectionResult.Ok else ScraperConnectionResult.ServiceProblem
            }
            401, 403 -> ScraperConnectionResult.BadCredentials
            429, 430, 431 -> ScraperConnectionResult.RateLimited
            else -> ScraperConnectionResult.ServiceProblem
        }

    private fun screenScraperBodyIsValid(body: String): Boolean = parses {
        json.parseToJsonElement(body).jsonObject["response"]?.jsonObject?.get("ssuser") != null
    }

    private fun steamGridDbBodyIsValid(body: String): Boolean = parses {
        json.parseToJsonElement(body).jsonObject["success"]?.jsonPrimitive?.booleanOrNull == true
    }

    private inline fun parses(check: () -> Boolean): Boolean = try {
        check()
    } catch (_: SerializationException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    }

    private companion object {
        const val SCREENSCRAPER_USER_URL = "https://api.screenscraper.fr/api2/ssuserInfos.php"
        const val STEAMGRIDDB_TEST_URL = "https://www.steamgriddb.com/api/v2/search/autocomplete/mario"
        const val SOFTNAME = "Jarngreipr"
    }
}
