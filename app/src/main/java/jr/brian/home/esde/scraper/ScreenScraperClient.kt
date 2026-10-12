package jr.brian.home.esde.scraper

import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.model.ScrapedMedia
import jr.brian.home.esde.scraper.model.ScraperTestResult
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
import okhttp3.ResponseBody
import jr.brian.home.esde.di.ScraperModule
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ScreenScraperClient @Inject constructor(
    private val http: OkHttpClient,
    private val json: Json,
    @Named(ScraperModule.SCREENSCRAPER_LIMITER) private val rateLimiter: ScraperRateLimiter
) {

    data class Credentials(
        val devId: String,
        val devPassword: String,
        val userId: String,
        val userPassword: String
    ) {
        val hasDev: Boolean get() = devId.isNotBlank() && devPassword.isNotBlank()
        val hasUser: Boolean get() = userId.isNotBlank() && userPassword.isNotBlank()
    }

    data class UserQuota(
        val maxThreads: Int,
        val maxRequestsPerMin: Int,
        val requestsToday: Int,
        val maxRequestsPerDay: Int
    )

    suspend fun fetchUserQuota(creds: Credentials): Result<UserQuota> = runOnIo {
        val url = BASE.toHttpUrl().newBuilder()
            .addPathSegment("ssuserInfos.php")
            .addQueryParameter("devid", creds.devId)
            .addQueryParameter("devpassword", creds.devPassword)
            .addQueryParameter("ssid", creds.userId)
            .addQueryParameter("sspassword", creds.userPassword)
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

    suspend fun testCredentials(creds: Credentials): ScraperTestResult {
        if (!creds.hasDev) return ScraperTestResult(false, "Dev ID / password required")
        if (!creds.hasUser) return ScraperTestResult(false, "User ID / password required")
        return fetchUserQuota(creds).fold(
            onSuccess = { q ->
                rateLimiter.updateLimits(q.maxThreads, minIntervalMs = 60_000L / q.maxRequestsPerMin.coerceAtLeast(1))
                ScraperTestResult(true, "OK · ${q.maxThreads} threads · ${q.requestsToday}/${q.maxRequestsPerDay} today")
            },
            onFailure = { ScraperTestResult(false, it.message ?: "Failed") }
        )
    }

    suspend fun lookup(
        creds: Credentials,
        systemeid: Int?,
        crc32: String?,
        romName: String,
        romSizeBytes: Long?
    ): Result<List<ScrapedMedia>> = runOnIo {
        if (!creds.hasDev) return@runOnIo emptyList<ScrapedMedia>()
        val builder = BASE.toHttpUrl().newBuilder()
            .addPathSegment("jeuInfos.php")
            .addQueryParameter("devid", creds.devId)
            .addQueryParameter("devpassword", creds.devPassword)
            .addQueryParameter("softname", SOFTNAME)
            .addQueryParameter("output", "json")
            .addQueryParameter("romnom", romName)
        if (creds.hasUser) {
            builder.addQueryParameter("ssid", creds.userId)
            builder.addQueryParameter("sspassword", creds.userPassword)
        }
        systemeid?.let { builder.addQueryParameter("systemeid", it.toString()) }
        crc32?.let { builder.addQueryParameter("crc", it) }
        romSizeBytes?.let { builder.addQueryParameter("romtaille", it.toString()) }
        val body = try {
            get(builder.build().toString())
        } catch (_: IOException) {
            return@runOnIo emptyList<ScrapedMedia>()
        }
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
        else -> null
    }

    private fun dedupeByKind(items: List<ScrapedMedia>): List<ScrapedMedia> {
        val byKind = LinkedHashMap<ScraperMediaKind, ScrapedMedia>()
        for (item in items) {
            byKind.putIfAbsent(item.kind, item)
        }
        return byKind.values.toList()
    }

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

    private suspend fun get(url: String): String = rateLimiter.withPermit {
        val request = Request.Builder().url(url).build()
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
        const val BASE = "https://api.screenscraper.fr/api2"
        const val SOFTNAME = "Jarngreipr"
    }
}
