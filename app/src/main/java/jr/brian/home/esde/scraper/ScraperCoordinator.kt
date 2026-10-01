package jr.brian.home.esde.scraper

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.ScraperMediaKind
import jr.brian.home.esde.scraper.model.ScrapeRequest
import jr.brian.home.esde.scraper.model.ScrapeState
import jr.brian.home.esde.scraper.model.ScrapedMedia
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.os.Environment
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped orchestrator for artwork scraping. Owns a supervisor scope so
 * one failed ROM doesn't cancel the whole run. Exposes state as a StateFlow
 * the UI can observe (progress + cancel).
 *
 * Scraped media is written to `/storage/emulated/0/Jarngreipr Media/{systemName}/
 * {folder}/{basename}.{ext}` — matching ES-DE's on-disk convention so the app's
 * existing `findFirstMedia()` fallback picks it up once the scraper root has
 * been added to the media roots list. This path survives app uninstall and is
 * reachable from any file manager without SAF. Requires MANAGE_EXTERNAL_STORAGE
 * (requested by SetupWizardManager); when it isn't granted, falls back to the
 * app-scoped external files dir so scraping still works.
 */
@Singleton
class ScraperCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val screenScraper: ScreenScraperClient,
    private val steamGridDb: SteamGridDbClient,
    private val hashCache: HashCache
) {
    private val supervisor = SupervisorJob()
    private val scope = CoroutineScope(supervisor + Dispatchers.IO)
    private var job: Job? = null

    private val _state = MutableStateFlow<ScrapeState>(ScrapeState.Idle)
    val state: StateFlow<ScrapeState> = _state.asStateFlow()

    fun scrapedRoot(): String {
        if (Environment.isExternalStorageManager()) {
            return File(Environment.getExternalStorageDirectory(), "Jarngreipr Media").absolutePath
        }
        val fallback = context.getExternalFilesDir(null) ?: context.filesDir
        return File(fallback, "Jarngreipr Media").absolutePath
    }

    fun start(
        request: ScrapeRequest,
        screenScraperCredentials: ScreenScraperClient.Credentials?,
        steamGridDbApiKey: String?,
        onFinished: (() -> Unit)? = null
    ) {
        if (_state.value is ScrapeState.Running) return
        val total = request.games.size
        _state.value = ScrapeState.Running(
            done = 0, total = total, downloads = 0, errors = 0, currentTitle = null
        )
        job = scope.launch {
            var downloads = 0
            var errors = 0
            var done = 0
            for (game in request.games) {
                if (!isActive) break
                val displayTitle = game.path.substringAfterLast('/')
                _state.value = ScrapeState.Running(done, total, downloads, errors, displayTitle)
                try {
                    val medias = discoverMedia(
                        game = game,
                        request = request,
                        screenScraperCredentials = screenScraperCredentials,
                        steamGridDbApiKey = steamGridDbApiKey
                    )
                    for (media in medias) {
                        if (!isActive) break
                        val dest = destinationFile(game, media) ?: continue
                        if (dest.exists() && dest.length() > 0) {
                            downloads++
                            continue
                        }
                        val ok = when {
                            media.url.contains("screenscraper.fr") ->
                                screenScraper.download(media.url, dest)
                            else -> steamGridDb.download(media.url, dest)
                        }
                        if (ok) downloads++ else errors++
                    }
                } catch (_: Exception) {
                    errors++
                }
                done++
                _state.value = ScrapeState.Running(done, total, downloads, errors, displayTitle)
            }
            _state.value = ScrapeState.Finished(done, downloads, errors)
            onFinished?.invoke()
        }
    }

    suspend fun cancel() {
        val current = job
        if (current != null) {
            current.cancelAndJoin()
        }
        _state.value = ScrapeState.Cancelled
    }

    fun clearFinished() {
        if (_state.value is ScrapeState.Finished || _state.value is ScrapeState.Cancelled) {
            _state.value = ScrapeState.Idle
        }
    }

    private suspend fun discoverMedia(
        game: GameInfo,
        request: ScrapeRequest,
        screenScraperCredentials: ScreenScraperClient.Credentials?,
        steamGridDbApiKey: String?
    ): List<ScrapedMedia> {
        val wanted = mutableMapOf<ScraperMediaKind, ScrapedMedia>()

        if (request.screenScraperEnabled && screenScraperCredentials?.hasDev == true) {
            val romFile = game.romAbsolutePath?.let(::File)?.takeIf { it.isFile }
            val crc = romFile?.let { hashCache.crc32(it) }
            val romName = romFile?.name ?: game.path.substringAfterLast('/')
            val systemeid = SystemIdMap.idFor(game.systemName)
            val result = screenScraper.lookup(
                creds = screenScraperCredentials,
                systemeid = systemeid,
                crc32 = crc,
                romName = romName,
                romSizeBytes = romFile?.length()
            ).getOrNull().orEmpty()
            for (media in result) {
                if (media.kind in request.mediaTypesScreenScraper) {
                    wanted.putIfAbsent(media.kind, media)
                }
            }
        }

        if (request.steamGridDbEnabled && !steamGridDbApiKey.isNullOrBlank()) {
            val stillWanted = request.mediaTypesSteamGridDb - wanted.keys
            if (stillWanted.isNotEmpty()) {
                val query = deriveQuery(game)
                val result = steamGridDb.lookup(
                    apiKey = steamGridDbApiKey,
                    query = query,
                    wantedKinds = stillWanted
                ).getOrNull().orEmpty()
                for (media in result) {
                    wanted.putIfAbsent(media.kind, media)
                }
            }
        }

        return wanted.values.toList()
    }

    private fun deriveQuery(game: GameInfo): String {
        val fileName = game.path.substringAfterLast('/').substringBeforeLast('.')
        return fileName
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]*]"), "")
            .replace('_', ' ')
            .replace('.', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun destinationFile(game: GameInfo, media: ScrapedMedia): File? {
        val system = game.systemName.ifBlank { return null }
        val baseName = game.path.substringAfterLast('/').substringBeforeLast('.')
            .ifBlank { return null }
        val ext = media.extensionHint?.lowercase() ?: "png"
        val folder = media.kind.folderName
        return File(File(File(scrapedRoot(), system), folder), "$baseName.$ext")
    }
}
