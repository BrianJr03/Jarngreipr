package jr.brian.home.data

import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameInfoRow
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.scraper.ScraperCoordinator
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverride
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges the Room-backed library engine to legacy's [GameInfo] model
 * so the current UI can read from Room without rewriting its render
 * tree. Writes still go through the legacy scan path; [LibraryMigration]
 * keeps Room in sync.
 *
 * Reads one joined [GameInfoRow] stream from [LibraryRepository]
 * (`games` LEFT JOIN `media`) and overlays [UserMetadataOverride]s
 * authored through the Edit Info sheet on each row. Previously this
 * combined two full-table flows (`allGames` + `observeAllMedia()`)
 * which materialised both tables into cursors on every emission and
 * OOM'd on large scraped libraries.
 *
 * [allGameInfos] is shared: every collector reads one upstream, so the
 * single joined query and the GameInfo mapping run once per change no
 * matter how many screens are listening. It's debounced so a
 * scrape-all (one media upsert per game) doesn't rebuild the list for
 * every row. Use [snapshot] when you need a fresh read of current Room
 * state rather than the latest (possibly still-debouncing) emission.
 */
@Singleton
class LibraryGameInfoAdapter @Inject constructor(
    private val repository: LibraryRepository,
    private val scraperCoordinator: ScraperCoordinator,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val gameInfos: Flow<List<GameInfo>> = repository.allGameRows.map { rows ->
        val overridesByGameId = loadOverridesByGame(rows)
        rows.map { it.toGameInfo(overridesByGameId[it.gameId]) }
    }

    @OptIn(FlowPreview::class)
    val allGameInfos: Flow<List<GameInfo>> = gameInfos
        .debounce(UPDATE_DEBOUNCE_MS)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** One fresh read of Room, bypassing the shared flow's debounce and replay. */
    suspend fun snapshot(): List<GameInfo> = gameInfos.first()

    private fun loadOverridesByGame(rows: List<GameInfoRow>): Map<Long, UserMetadataOverride> {
        val scrapedRoot = scraperCoordinator.scrapedRoot()
        if (scrapedRoot.isBlank()) return emptyMap()
        val perSystem = rows
            .map { it.systemId }
            .distinct()
            .associateWith { UserMetadataOverrideStore.readAllForSystem(scrapedRoot, it) }
        if (perSystem.values.all { it.isEmpty() }) return emptyMap()
        val result = HashMap<Long, UserMetadataOverride>()
        for (row in rows) {
            val overrides = perSystem[row.systemId] ?: continue
            if (overrides.isEmpty()) continue
            val basename = File(row.absolutePath ?: row.relativePath).nameWithoutExtension
            overrides[basename]?.let { result[row.gameId] = it }
        }
        return result
    }

    private companion object {
        const val UPDATE_DEBOUNCE_MS = 250L
    }
}

internal fun GameInfoRow.toGameInfo(
    override: UserMetadataOverride? = null,
): GameInfo = GameInfo(
    path = absolutePath ?: relativePath,
    name = override?.displayName ?: displayName,
    // description / releaseDate / players are deliberately absent
    // from [GameInfoRow] — see its KDoc. The user's Edit Info
    // override still applies when present; the DB-sourced detail-
    // only fields show as null in the all-games list until the
    // detail view fetches them via a per-id query.
    description = override?.description,
    rating = override?.rating ?: rating ?: 0f,
    releaseDate = override?.releaseDate,
    developer = override?.developer ?: developer,
    publisher = override?.publisher ?: publisher,
    genre = override?.genre ?: genre,
    players = override?.players,
    isFavorite = isFavorite,
    playCount = playCount,
    playTimeMinutes = playTimeMinutes,
    lastPlayed = lastPlayed?.let(::formatEsdeDate),
    systemName = systemId,
    artworkPath = override?.artworkPath ?: coverPath,
    physicalMediaPath = physicalMediaPath,
    marqueeImagePath = override?.marqueeImagePath ?: marqueePath,
    screenshotPath = override?.screenshotPath ?: screenshotPath,
    fanartPath = override?.fanartPath ?: fanartPath,
    titlescreenPath = titlescreenPath,
    miximagePath = miximagePath,
    emulatorPackage = emulatorPackage,
    romAbsolutePath = absolutePath,
    launchCommand = launchCommand,
)

/**
 * Mirror the ES-DE string format the legacy gamelist.xml uses
 * (`yyyyMMdd'T'HHmmss`). [RomIndexImporter.legacyLastPlayedToMillis]
 * is the inverse; keeping these symmetric means a migrate → read →
 * display → re-migrate round-trip is lossless.
 */
private fun formatEsdeDate(millis: Long): String =
    esdeDateFormatter.format(Date(millis))

private val esdeDateFormatter: SimpleDateFormat =
    SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
