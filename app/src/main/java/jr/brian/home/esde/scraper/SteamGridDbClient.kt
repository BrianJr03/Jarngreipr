package jr.brian.home.esde.scraper

import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.model.ScrapedMedia
import jr.brian.home.esde.scraper.model.ScraperTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import jr.brian.home.esde.di.ScraperModule
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SteamGridDbClient @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json,
    @Named(ScraperModule.STEAMGRIDDB_LIMITER) private val rateLimiter: ScraperRateLimiter
) {

    suspend fun testApiKey(apiKey: String): ScraperTestResult {
        if (apiKey.isBlank()) return ScraperTestResult(false, "API key required")
        return runOnIo {
            val url = "$BASE/search/autocomplete/mario"
            val body = get(url, apiKey)
            val ok = json.parseToJsonElement(body).jsonObject["success"]
                ?.jsonPrimitive?.booleanOrNull == true
            if (ok) ScraperTestResult(true, "OK")
            else ScraperTestResult(false, "Invalid response")
        }.getOrElse { ScraperTestResult(false, it.message ?: "Failed") }
    }

    suspend fun lookup(
        apiKey: String,
        query: String,
        wantedKinds: Set<ScraperMediaKind>
    ): Result<List<ScrapedMedia>> = runOnIo {
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
                val body: ResponseBody = response.body ?: return@use false
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
