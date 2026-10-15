package jr.brian.home.data

import jr.brian.frontend.library.LibraryRepository
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
 * single `Flow<List<GameInfo>>` the ViewModel can collect once.
 */
@Singleton
class LibraryGameInfoAdapter @Inject constructor(
    private val repository: LibraryRepository,
    private val mediaDao: MediaDao,
) {
    val allGameInfos: Flow<List<GameInfo>> = combine(
        repository.allGames,
        mediaDao.observeAll(),
    ) { games, media ->
        val mediaByGameId = media.associateBy { it.gameId }
        games.map { it.toGameInfo(mediaByGameId[it.id]) }
    }
}

internal fun GameEntity.toGameInfo(media: MediaEntity?): GameInfo = GameInfo(
    path = absolutePath ?: relativePath,
    name = displayName,
    description = description,
    rating = rating ?: 0f,
    releaseDate = releaseDate,
    developer = developer,
    publisher = publisher,
    genre = genre,
    players = players,
    isFavorite = isFavorite,
    playCount = playCount,
    playTimeMinutes = playTimeMinutes,
    lastPlayed = lastPlayed?.let(::formatEsdeDate),
    systemName = systemId,
    artworkPath = media?.coverPath,
    physicalMediaPath = media?.physicalMediaPath,
    marqueeImagePath = media?.marqueePath,
    screenshotPath = media?.screenshotPath,
    fanartPath = media?.fanartPath,
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
