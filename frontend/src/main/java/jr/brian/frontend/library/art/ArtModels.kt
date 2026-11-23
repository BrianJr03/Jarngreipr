package jr.brian.frontend.library.art

import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.media.MediaKind
import kotlinx.coroutines.flow.Flow
import java.io.File

/** One resolved file. [source] is null for the user's custom art or a library-only row. */
data class ArtSlot(val path: String, val source: ArtSource?)

/** Resolved art for one game, keyed by kind. A missing kind has no art anywhere. */
data class GameArt(val slots: Map<MediaKind, ArtSlot> = emptyMap()) {
    operator fun get(kind: MediaKind): ArtSlot? = slots[kind]
    val cover: ArtSlot? get() = slots[MediaKind.Cover]
    val marquee: ArtSlot? get() = slots[MediaKind.Marquee]
    val fanart: ArtSlot? get() = slots[MediaKind.Fanart]
    val screenshot: ArtSlot? get() = slots[MediaKind.Screenshot]
    val titlescreen: ArtSlot? get() = slots[MediaKind.Titlescreen]
    val miximage: ArtSlot? get() = slots[MediaKind.Miximage]
    val physicalMedia: ArtSlot? get() = slots[MediaKind.PhysicalMedia]
    val video: ArtSlot? get() = slots[MediaKind.Video]
}

data class ArtSourceStatus(val source: ArtSource, val isAvailable: Boolean, val fileCount: Int)

data class LibrarySystem(val id: String, val displayName: String, val gameCount: Int)

/** [key] survives a rescan, unlike [id], which Room reassigns. */
data class LibraryGame(
    val id: Long,
    val key: String,
    val systemId: String,
    val title: String,
    val relativePath: String,
    val absolutePath: String?,
    val isFavorite: Boolean,
    val playCount: Int,
    val playTimeMinutes: Int = 0,
    val lastPlayed: Long?,
    val description: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val genre: String? = null,
    val players: String? = null,
    val releaseDate: String? = null,
    val rating: Float? = null,
    val emulatorPackage: String? = null,
    val launchCommand: String? = null,
)

data class LibraryGameWithArt(val game: LibraryGame, val artPath: String?)

/** A list after the user's hide rules, with how many entries those rules removed. */
data class VisibleList<out T>(val items: List<T>, val hiddenCount: Int = 0)

internal fun GameEntity.toLibraryGame() = LibraryGame(
    id = id,
    key = gameKey(sourceId, systemId, relativePath),
    systemId = systemId,
    title = displayName,
    relativePath = relativePath,
    absolutePath = absolutePath,
    isFavorite = isFavorite,
    playCount = playCount,
    playTimeMinutes = playTimeMinutes,
    lastPlayed = lastPlayed,
    description = description,
    developer = developer,
    publisher = publisher,
    genre = genre,
    players = players,
    releaseDate = releaseDate,
    rating = rating,
    emulatorPackage = emulatorPackage,
    launchCommand = launchCommand,
)

private const val KEY_SEPARATOR = ":"
private const val KEY_PARTS = 3

/** Mirrors the unique (source, system, path) index, so it is unique library-wide. */
internal fun gameKey(sourceId: Long, systemId: String, relativePath: String): String =
    listOf(sourceId, systemId, relativePath).joinToString(KEY_SEPARATOR)

internal data class ParsedGameKey(val sourceId: Long, val systemId: String, val relativePath: String)

/** The path is last and split is limited, so a colon in a file name stays intact. */
internal fun parseGameKey(key: String): ParsedGameKey? {
    val parts = key.split(KEY_SEPARATOR, limit = KEY_PARTS)
    if (parts.size != KEY_PARTS) return null
    val sourceId = parts[0].toLongOrNull() ?: return null
    return ParsedGameKey(sourceId, parts[1], parts[2])
}

enum class ScrapeMissingArtResult { Started, NotConfigured }

/** A launch recorded outside Room. [path] is the absolute or relative ROM path. */
data class RecentLaunch(val systemId: String, val path: String, val launchedAt: Long)

/** Launch history owned by `:app`. Room's `lastPlayed` is only set by imports. */
fun interface RecentLaunchSource {
    fun launches(): Flow<List<RecentLaunch>>
}

/** The user's configured ES-DE media folder, owned by `:app`. */
fun interface EsdeMediaRootProvider {
    fun esdeMediaRoot(): File?
}

/** The hide rules the user set. Game keys are `<system>/<absolute or relative path>`. */
data class LibraryVisibility(
    val hiddenSystems: Set<String> = emptySet(),
    val hiddenGames: Set<String> = emptySet(),
) {
    fun hides(systemId: String, absolutePath: String?, relativePath: String): Boolean =
        systemId in hiddenSystems || "$systemId/${absolutePath ?: relativePath}" in hiddenGames
}

/** Hide rules owned by `:app`, read-only here. */
fun interface LibraryVisibilitySource {
    fun visibility(): Flow<LibraryVisibility>
}
