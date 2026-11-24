package jr.brian.frontend.scrape

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * ScreenScraper API client. Dev credentials now come from
 * [ScreenScraperConfig] (shipped via CI secrets in Phase 4a); user
 * credentials from [UserScraperCredentials] (set in Settings when
 * Phase 4e lands the UI).
 *
 * Ported from `:app`'s original with the Phase 0 fix (HTTP errors
 * propagate as `Result.failure` rather than silently collapsing to
 * emptyList); `discoverMedia` in the coordinator decides how to
 * surface them.
 */
@Singleton
class ScreenScraperClient @Inject constructor(
    @Named(FrontendScraperModule.OKHTTP) private val http: OkHttpClient,
    @Named(FrontendScraperModule.JSON) private val json: Json,
    @Named(FrontendScraperModule.SCREENSCRAPER_LIMITER) private val rateLimiter: ScraperRateLimiter,
    private val config: ScreenScraperConfig,
    private val userCredentials: UserScraperCredentials,
) {

    data class UserQuota(
        val maxThreads: Int,
        val maxRequestsPerMin: Int,
        val requestsToday: Int,
        val maxRequestsPerDay: Int,
    )

    /** True when both dev AND user credentials are configured. */
    fun isReady(): Boolean =
        config.hasDevCredentials && userCredentials.state.value.hasScreenScraperUser

    suspend fun fetchUserQuota(): Result<UserQuota> = runOnIo {
        requireReady()
        val user = userCredentials.state.value
        val url = BASE.toHttpUrl().newBuilder()
            .addPathSegment("ssuserInfos.php")
            .addQueryParameter("devid", config.devId)
            .addQueryParameter("devpassword", config.devPassword)
            .addQueryParameter("ssid", user.ssUserId)
            .addQueryParameter("sspassword", user.ssUserPassword)
            .addQueryParameter("softname", SOFTNAME)
            .addQueryParameter("output", "json")
            .build()
        val body = get(url.toString())
        val ssuser = json.parseToJsonElement(body).jsonObject
            .get("response")?.jsonObject?.get("ssuser")?.jsonObject
            ?: error("Missing ssuser in response")
        val threads = ssuser["maxthreads"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        val perMin = ssuser["maxrequestspermin"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 60
        val today = ssuser["requeststoday"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0
        val perDay = ssuser["maxrequestsperday"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 20_000
        UserQuota(threads, perMin, today, perDay)
    }

    /**
     * Look up a game by hash-first then name. The hash fields come
     * from [jr.brian.frontend.library.db.GameEntity] (populated at
     * scan time per plan §3.7). Null hashes collapse to a name-only
     * lookup — ScreenScraper handles this via `romnom` alone.
     */
    suspend fun lookup(
        systemeid: Int?,
        romName: String,
        crc32: String? = null,
        md5: String? = null,
        romSizeBytes: Long? = null,
    ): Result<List<ScrapedMedia>> = runOnIo {
        requireReady()
        val user = userCredentials.state.value
        val builder = BASE.toHttpUrl().newBuilder()
            .addPathSegment("jeuInfos.php")
            .addQueryParameter("devid", config.devId)
            .addQueryParameter("devpassword", config.devPassword)
            .addQueryParameter("ssid", user.ssUserId)
            .addQueryParameter("sspassword", user.ssUserPassword)
            .addQueryParameter("softname", SOFTNAME)
            .addQueryParameter("output", "json")
            .addQueryParameter("romnom", romName)
        systemeid?.let { builder.addQueryParameter("systemeid", it.toString()) }
        crc32?.let { builder.addQueryParameter("crc", it) }
        md5?.let { builder.addQueryParameter("md5", it) }
        romSizeBytes?.let { builder.addQueryParameter("romtaille", it.toString()) }
        // IOException propagates; Phase 0 §6 fix. The coordinator
        // records the HTTP code so quota 429 surfaces as "quota
        // reached" rather than "0 found".
        val body = get(builder.build().toString())
        parseMedias(body)
    }

    private fun parseMedias(rawJson: String): List<ScrapedMedia> {
        val root = try {
            json.parseToJsonElement(rawJson).jsonObject
        } catch (_: Exception) {
            return emptyList()
        }
        val medias = root["response"]?.jsonObject
            ?.get("jeu")?.jsonObject
            ?.get("medias")?.jsonArray
            ?: return emptyList()
        val out = mutableListOf<ScrapedMedia>()
        for (element in medias) {
            val obj = element.jsonObject
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: continue
            val url = obj["url"]?.jsonPrimitive?.contentOrNull ?: continue
            val format = obj["format"]?.jsonPrimitive?.contentOrNull
            val kind = mediaKindFor(type) ?: continue
            out += ScrapedMedia(kind = kind, url = url, extensionHint = format)
        }
        return dedupeByKind(out)
    }

    private fun mediaKindFor(type: String): ScraperMediaKind? = when (type.lowercase()) {
        "box-2d", "box-3d", "box-texture" -> ScraperMediaKind.Covers
        "ss" -> ScraperMediaKind.Screenshots
        "sstitle" -> ScraperMediaKind.TitleScreens
        "wheel", "wheel-hd", "wheel-carbon", "wheel-steel", "marquee" -> ScraperMediaKind.Marquees
        "fanart" -> ScraperMediaKind.Fanart
        "video" -> ScraperMediaKind.Videos
        else -> null
    }

    private fun dedupeByKind(items: List<ScrapedMedia>): List<ScrapedMedia> {
        val byKind = LinkedHashMap<ScraperMediaKind, ScrapedMedia>()
        for (item in items) byKind.putIfAbsent(item.kind, item)
        return byKind.values.toList()
    }

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

    private suspend fun get(url: String): String = rateLimiter.withPermit {
        val request = Request.Builder().url(url).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string() ?: throw IOException("Empty body")
        }
    }

    private fun requireReady() {
        require(config.hasDevCredentials) { "ScreenScraper dev credentials not configured" }
        require(userCredentials.state.value.hasScreenScraperUser) {
            "ScreenScraper user credentials not configured"
        }
    }

    private suspend inline fun <T> runOnIo(crossinline block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }
        }

    private companion object {
        const val BASE = "https://api.screenscraper.fr/api2"
        const val SOFTNAME = "Jarngreipr"
    }
}
