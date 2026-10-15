package jr.brian.frontend.scrape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * SteamGridDB client. Picks the first autocomplete match for a
 * query, then pulls one asset per requested [ScraperMediaKind].
 *
 * The plan §3.7 improvements (platform-similarity scoring on the
 * autocomplete hit, `dimensions=600x900` filter for covers,
 * `styles=alternate,white_logo` filter for logos, hero endpoint
 * for fanart) land in a follow-up — this port preserves the Phase 0
 * HTTP-error-propagation shape and nothing else.
 */
@Singleton
class SteamGridDbClient @Inject constructor(
    @Named(FrontendScraperModule.OKHTTP) private val http: OkHttpClient,
    @Named(FrontendScraperModule.JSON) private val json: Json,
    @Named(FrontendScraperModule.STEAMGRIDDB_LIMITER) private val rateLimiter: ScraperRateLimiter,
    private val userCredentials: UserScraperCredentials,
) {

    fun isReady(): Boolean = userCredentials.state.value.hasSteamGridDbKey

    suspend fun lookup(
        query: String,
        wantedKinds: Set<ScraperMediaKind>,
    ): Result<List<ScrapedMedia>> = runOnIo {
        val apiKey = userCredentials.state.value.steamGridDbApiKey
        if (apiKey.isBlank() || query.isBlank()) return@runOnIo emptyList<ScrapedMedia>()
        val gameId = firstMatchId(apiKey, query) ?: return@runOnIo emptyList<ScrapedMedia>()
        val out = mutableListOf<ScrapedMedia>()
        if (ScraperMediaKind.Covers in wantedKinds) {
            firstAsset(apiKey, "$BASE/grids/game/$gameId")?.let {
                out += ScrapedMedia(ScraperMediaKind.Covers, it.first, it.second)
            }
        }
        if (ScraperMediaKind.Fanart in wantedKinds) {
            firstAsset(apiKey, "$BASE/heroes/game/$gameId")?.let {
                out += ScrapedMedia(ScraperMediaKind.Fanart, it.first, it.second)
            }
        }
        if (ScraperMediaKind.Marquees in wantedKinds) {
            firstAsset(apiKey, "$BASE/logos/game/$gameId")?.let {
                out += ScrapedMedia(ScraperMediaKind.Marquees, it.first, it.second)
            }
        }
        out
    }

    private suspend fun firstMatchId(apiKey: String, query: String): Int? {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val body = try {
            get("$BASE/search/autocomplete/$encoded", apiKey)
        } catch (_: IOException) {
            return null
        }
        val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: return null
        for (element in data) {
            val id = element.jsonObject["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            if (id != null) return id
        }
        return null
    }

    private suspend fun firstAsset(apiKey: String, url: String): Pair<String, String?>? {
        val body = try {
            get(url, apiKey)
        } catch (_: IOException) {
            return null
        }
        val data = json.parseToJsonElement(body).jsonObject["data"]?.jsonArray ?: return null
        for (element in data) {
            val obj = element.jsonObject
            val u = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
            val mime = obj["mime"]?.jsonPrimitive?.contentOrNull
            val ext = extensionFromMime(mime) ?: extensionFromUrl(u)
            return u to ext
        }
        return null
    }

    private fun extensionFromMime(mime: String?): String? = when (mime?.lowercase()) {
        "image/png" -> "png"
        "image/jpeg" -> "jpg"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        else -> null
    }

    private fun extensionFromUrl(url: String): String? =
        url.substringAfterLast('.', missingDelimiterValue = "").takeIf { it.length in 2..5 }

    suspend fun download(url: String, dest: File): Boolean = runOnIo {
        rateLimiter.withPermit {
            val request = Request.Builder().url(url).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use false
                val body = response.body ?: return@use false
                dest.parentFile?.mkdirs()
                body.byteStream().use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                true
            }
        }
    }.getOrDefault(false)

    private suspend fun get(url: String, apiKey: String): String = rateLimiter.withPermit {
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty body")
        }
    }

    private suspend inline fun <T> runOnIo(crossinline block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }
        }

    private companion object {
        const val BASE = "https://www.steamgriddb.com/api/v2"
    }
}
