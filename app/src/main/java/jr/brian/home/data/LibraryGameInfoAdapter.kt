package jr.brian.home.data

import jr.brian.core.model.ApplicationScope
import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.scraper.ScraperCoordinator
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverride
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
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
 * Combines the three observable flows (games, media, systems) into a
 * single `Flow<List<GameInfo>>` the ViewModel can collect once. User
 * overrides authored through the Edit Info sheet are overlaid on top of
 * each row — written by [UserMetadataOverrideStore], picked up here so
 * the UI reflects edits without a schema migration into Room.
 *
 * [allGameInfos] is shared: every collector reads one upstream, so the
 * full `games` + `media` reload and the GameInfo mapping run once per
 * change no matter how many screens are listening. It's debounced so a
 * scrape-all (one media upsert per game) doesn't rebuild the list for
 * every row. Use [snapshot] when you need a fresh read of current Room
 * state rather than the latest (possibly still-debouncing) emission.
 */
@Singleton
class LibraryGameInfoAdapter @Inject constructor(
    private val repository: LibraryRepository,
    private val mediaDao: MediaDao,
    private val scraperCoordinator: ScraperCoordinator,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val gameInfos: Flow<List<GameInfo>> = combine(
        repository.allGames,
        mediaDao.observeAll(),
    ) { games, media ->
        val mediaByGameId = media.associateBy { it.gameId }
        val overridesBySystem = loadOverridesByGame(games)
        games.map { game ->
            game.toGameInfo(mediaByGameId[game.id], overridesBySystem[game.id])
        }
    }

    @OptIn(FlowPreview::class)
    val allGameInfos: Flow<List<GameInfo>> = gameInfos
        .debounce(UPDATE_DEBOUNCE_MS)
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    /** One fresh read of Room, bypassing the shared flow's debounce and replay. */
    suspend fun snapshot(): List<GameInfo> = gameInfos.first()

    private fun loadOverridesByGame(games: List<GameEntity>): Map<Long, UserMetadataOverride> {
        val scrapedRoot = scraperCoordinator.scrapedRoot()
        if (scrapedRoot.isBlank()) return emptyMap()
        val perSystem = games
            .map { it.systemId }
            .distinct()
            .associateWith { UserMetadataOverrideStore.readAllForSystem(scrapedRoot, it) }
        if (perSystem.values.all { it.isEmpty() }) return emptyMap()
        val result = HashMap<Long, UserMetadataOverride>()
        for (game in games) {
            val overrides = perSystem[game.systemId] ?: continue
            if (overrides.isEmpty()) continue
            val basename = File(game.absolutePath ?: game.relativePath).nameWithoutExtension
            overrides[basename]?.let { result[game.id] = it }
        }
        return result
    }

    private companion object {
        const val UPDATE_DEBOUNCE_MS = 250L
    }
}

internal fun GameEntity.toGameInfo(
    media: MediaEntity?,
    override: UserMetadataOverride? = null,
): GameInfo = GameInfo(
    path = absolutePath ?: relativePath,
    name = override?.displayName ?: displayName,
    description = override?.description ?: description,
    rating = override?.rating ?: rating ?: 0f,
    releaseDate = override?.releaseDate ?: releaseDate,
    developer = override?.developer ?: developer,
    publisher = override?.publisher ?: publisher,
    genre = override?.genre ?: genre,
    players = override?.players ?: players,
    isFavorite = isFavorite,
    playCount = playCount,
    playTimeMinutes = playTimeMinutes,
    lastPlayed = lastPlayed?.let(::formatEsdeDate),
    systemName = systemId,
    artworkPath = override?.artworkPath ?: media?.coverPath,
    physicalMediaPath = media?.physicalMediaPath,
    marqueeImagePath = override?.marqueeImagePath ?: media?.marqueePath,
    screenshotPath = override?.screenshotPath ?: media?.screenshotPath,
    fanartPath = override?.fanartPath ?: media?.fanartPath,
    titlescreenPath = media?.titlescreenPath,
    miximagePath = media?.miximagePath,
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
