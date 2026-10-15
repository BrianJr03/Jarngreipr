package jr.brian.frontend.scrape

import android.content.Context
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.ScrapeJobDao
import jr.brian.frontend.library.db.ScrapeJobEntity
import jr.brian.frontend.library.systems.SystemCatalog
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase 4 scrape orchestrator. Walks a game list, calls
 * [ScreenScraperClient] (hash-first per plan §3.7) then
 * [SteamGridDbClient] for anything missing, writes media files
 * under [JarngreiprMediaLayout] and the resulting paths to
 * [MediaEntity] immediately so the UI sees art without a refresh.
 *
 * Replaces `:app`'s `ScraperCoordinator`. The `ScrapeJobEntity`
 * rows this writes give the Phase 4e summary UI something to render.
 *
 * The hash-first improvement from the plan: Phase 2a's `GameEntity`
 * already has `crc32` / `md5` / `fileSizeBytes` columns. Populating
 * them is the scanner's job (plan §3.7 "computed in a scan-time
 * background pass"); for now the scraper reads whatever's there and
 * falls through to name-only when the hash fields are null. The
 * hashing pass is a follow-up to this PR.
 */
@Singleton
class LibraryScraper @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val screenScraper: ScreenScraperClient,
    private val steamGridDb: SteamGridDbClient,
    private val catalog: SystemCatalog,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    private val scrapeJobDao: ScrapeJobDao,
) {

    private val _state = MutableStateFlow<ScrapeState>(ScrapeState.Idle)
    val state: StateFlow<ScrapeState> = _state.asStateFlow()

    /**
     * Scrape every game in [games], writing results into Room. Should
     * be called from a WorkManager worker (see [ScrapeWorker]) so a
     * foreground notification can keep it alive past a game launch.
     *
     * Idempotent: a game whose MediaEntity already carries non-null
     * paths for every requested kind is skipped. Rerun to pick up
     * assets the first pass couldn't find.
     */
    suspend fun scrape(
        games: List<GameEntity>,
        request: ScrapeRequest,
    ): ScrapeSummary = coroutineScope {
        if (games.isEmpty()) return@coroutineScope ScrapeSummary()
        if (!canScrape(request)) {
            Log.w(TAG, "Scraper not configured; aborting")
            _state.value = ScrapeState.Finished(
                processed = 0, downloads = 0, errors = 1,
                errorMessages = listOf(
                    ScrapeError(
                        gameTitle = "<pre-flight>",
                        message = "Scraper not configured. Set your account in Settings.",
                    ),
                ),
            )
            return@coroutineScope ScrapeSummary(errors = 1)
        }

        var downloads = 0
        var errors = 0
        val errorMessages = mutableListOf<ScrapeError>()
        for ((index, game) in games.withIndex()) {
            if (!isActive) break
            val displayTitle = game.displayName
            _state.value = ScrapeState.Running(index, games.size, downloads, errors, displayTitle, errorMessages.toList())

            val startedAt = System.currentTimeMillis()
            try {
                val medias = discoverMedia(game, request, errorMessages, displayTitle)
                val existing = mediaDao.findByGameId(game.id) ?: MediaEntity(gameId = game.id)
                var updated = existing
                var downloadsThisGame = 0
                for (media in medias) {
                    val dest = destinationFile(game, media) ?: continue
                    if (!dest.exists() || dest.length() == 0L) {
                        val ok = if (media.url.contains("screenscraper.fr")) {
                            screenScraper.download(media.url, dest)
                        } else {
                            steamGridDb.download(media.url, dest)
                        }
                        if (!ok) {
                            errors++
                            errorMessages += ScrapeError(displayTitle, "Download failed: ${media.url}")
                            continue
                        }
                    }
                    downloads++
                    downloadsThisGame++
                    updated = updated.applyMedia(media, dest.absolutePath)
                }
                if (updated != existing) mediaDao.upsert(updated)
                scrapeJobDao.insert(
                    ScrapeJobEntity(
                        gameId = game.id,
                        backend = BACKEND_COMBINED,
                        state = if (downloadsThisGame > 0) JOB_SUCCEEDED else JOB_PARTIAL,
                        startedAt = startedAt,
                        finishedAt = System.currentTimeMillis(),
                        mediaKindsWritten = downloadsThisGame,
                    ),
                )
            } catch (e: Exception) {
                errors++
                errorMessages += ScrapeError(displayTitle, e.message ?: e.javaClass.simpleName)
                scrapeJobDao.insert(
                    ScrapeJobEntity(
                        gameId = game.id,
                        backend = BACKEND_COMBINED,
                        state = JOB_FAILED,
                        errorMessage = e.message ?: e.javaClass.simpleName,
                        startedAt = startedAt,
                        finishedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
        val processed = games.size
        _state.value = ScrapeState.Finished(processed, downloads, errors, errorMessages.toList())
        ScrapeSummary(processed = processed, downloads = downloads, errors = errors)
    }

    private suspend fun discoverMedia(
        game: GameEntity,
        request: ScrapeRequest,
        errorMessages: MutableList<ScrapeError>,
        displayTitle: String,
    ): List<ScrapedMedia> {
        val wanted = linkedMapOf<ScraperMediaKind, ScrapedMedia>()

        if (request.screenScraperEnabled && screenScraper.isReady()) {
            val def = catalog.findById(game.systemId)
            val romName = File(game.relativePath).name
            val result = screenScraper.lookup(
                systemeid = def?.screenScraperSystemId,
                romName = romName,
                crc32 = game.crc32,
                md5 = game.md5,
                romSizeBytes = game.fileSizeBytes,
            )
            result.fold(
                onSuccess = { medias ->
                    for (media in medias) {
                        if (media.kind in request.screenScraperKinds) {
                            wanted.putIfAbsent(media.kind, media)
                        }
                    }
                },
                onFailure = { cause ->
                    errorMessages += ScrapeError(
                        displayTitle,
                        "ScreenScraper: ${cause.message ?: cause.javaClass.simpleName}",
                    )
                },
            )
        }

        if (request.steamGridDbEnabled && steamGridDb.isReady()) {
            val stillWanted = request.steamGridDbKinds - wanted.keys
            if (stillWanted.isNotEmpty()) {
                val query = deriveQuery(game)
                val result = steamGridDb.lookup(query, stillWanted)
                result.fold(
                    onSuccess = { medias ->
                        for (media in medias) wanted.putIfAbsent(media.kind, media)
                    },
                    onFailure = { cause ->
                        errorMessages += ScrapeError(
                            displayTitle,
                            "SteamGridDB: ${cause.message ?: cause.javaClass.simpleName}",
                        )
                    },
                )
            }
        }
        return wanted.values.toList()
    }

    private fun destinationFile(game: GameEntity, media: ScrapedMedia): File? {
        val basename = File(game.relativePath).nameWithoutExtension.ifBlank { return null }
        val ext = media.extensionHint?.lowercase() ?: "png"
        val root = scrapedRoot()
        return File(File(File(root, game.systemId), folderFor(media.kind)), "$basename.$ext")
    }

    private fun folderFor(kind: ScraperMediaKind): String = when (kind) {
        ScraperMediaKind.Covers -> "covers"
        ScraperMediaKind.Marquees -> "marquees"
        ScraperMediaKind.Fanart -> "fanart"
        ScraperMediaKind.Screenshots -> "screenshots"
        ScraperMediaKind.TitleScreens -> "titlescreens"
        ScraperMediaKind.MixImages -> "miximages"
        ScraperMediaKind.Videos -> "videos"
    }

    private fun scrapedRoot(): File {
        if (Environment.isExternalStorageManager()) {
            return File(Environment.getExternalStorageDirectory(), "Jarngreipr Media")
        }
        val fallback = context.getExternalFilesDir(null) ?: context.filesDir
        return File(fallback, "Jarngreipr Media")
    }

    private fun deriveQuery(game: GameEntity): String {
        val name = File(game.relativePath).nameWithoutExtension
        return name
            .replace(Regex("\\([^)]*\\)"), "")
            .replace(Regex("\\[[^]]*]"), "")
            .replace('_', ' ')
            .replace('.', ' ')
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun canScrape(request: ScrapeRequest): Boolean =
        (request.screenScraperEnabled && screenScraper.isReady()) ||
            (request.steamGridDbEnabled && steamGridDb.isReady())

    private fun MediaEntity.applyMedia(media: ScrapedMedia, path: String): MediaEntity = when (media.kind) {
        ScraperMediaKind.Covers -> copy(coverPath = path)
        ScraperMediaKind.Marquees -> copy(marqueePath = path)
        ScraperMediaKind.Fanart -> copy(fanartPath = path)
        ScraperMediaKind.Screenshots -> copy(screenshotPath = path)
        ScraperMediaKind.TitleScreens -> copy(titlescreenPath = path)
        ScraperMediaKind.MixImages -> copy(miximagePath = path)
        ScraperMediaKind.Videos -> copy(videoPath = path)
    }

    private companion object {
        const val TAG = "LibraryScraper"
        const val BACKEND_COMBINED = "combined"
        const val JOB_RUNNING = "running"
        const val JOB_SUCCEEDED = "succeeded"
        const val JOB_FAILED = "failed"
        const val JOB_PARTIAL = "partial"
    }
}

/** The user's request shape, built by the scrape summary UI. */
data class ScrapeRequest(
    val screenScraperEnabled: Boolean = true,
    val screenScraperKinds: Set<ScraperMediaKind> = ScraperMediaKind.entries.toSet(),
    val steamGridDbEnabled: Boolean = true,
    val steamGridDbKinds: Set<ScraperMediaKind> = ScraperMediaKind.entries.toSet(),
)

/** Terminal summary for a completed scrape run. */
data class ScrapeSummary(
    val processed: Int = 0,
    val downloads: Int = 0,
    val errors: Int = 0,
)

/** State the UI binds to. Shape ported from the Phase 0 scraper. */
sealed interface ScrapeState {
    data object Idle : ScrapeState
    data class Running(
        val done: Int,
        val total: Int,
        val downloads: Int,
        val errors: Int,
        val currentTitle: String?,
        val errorMessages: List<ScrapeError> = emptyList(),
    ) : ScrapeState
    data class Finished(
        val processed: Int,
        val downloads: Int,
        val errors: Int,
        val errorMessages: List<ScrapeError> = emptyList(),
    ) : ScrapeState
    data object Cancelled : ScrapeState
}

data class ScrapeError(
    val gameTitle: String,
    val message: String,
)
