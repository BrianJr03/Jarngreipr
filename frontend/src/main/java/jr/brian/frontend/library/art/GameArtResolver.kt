package jr.brian.frontend.library.art

import jr.brian.core.model.ArtSource
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.media.MediaKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks each art kind from the user's custom art, then the chosen source, then
 * the other sources, then the existing library row. Per kind, so a RetroHrai user with no marquee
 * still gets one from ES-DE. Never writes to Room or the disk.
 */
@Singleton
class GameArtResolver @Inject constructor(
    private val indexes: ArtIndexes,
    private val roots: ArtRoots,
    private val mediaDao: MediaDao,
    private val store: ArtSourceStore,
    private val customArt: CustomArtSource,
) {
    suspend fun resolve(game: GameEntity, preferred: ArtSource = store.source): GameArt =
        withContext(Dispatchers.IO) {
            val built = indexes.current()
            val stored = mediaDao.findByGameId(game.id)
            val names = artBasenames(game.relativePath)
            val custom = customArt.forSystem(game.systemId)[
                customArtKey(game.absolutePath, game.relativePath),
            ]
            val slots = MediaKind.entries.mapNotNull { kind ->
                val slot = fromCustom(custom, kind)
                    ?: fromSources(game.systemId, names, kind, preferred, built)
                    ?: fromLibraryRow(stored, kind)
                slot?.let { kind to it }
            }.toMap()
            GameArt(slots)
        }

    /** One image per game for tiles, batched so a large system stays one query per chunk. */
    suspend fun tileArt(games: List<LibraryGame>, preferred: ArtSource): Map<Long, String> =
        displayArt(games, preferred, GameArtChoices())
            .mapNotNull { (id, art) -> art.top?.let { id to it } }
            .toMap()

    /** Each screen's art for every game, in one batched pass. Never blank while any art exists. */
    suspend fun displayArt(
        games: List<LibraryGame>,
        preferred: ArtSource,
        choices: GameArtChoices,
    ): Map<Long, GameDisplayArt> = readSlots(games, preferred) { game, slot ->
        fun first(screen: ArtScreen, choice: ArtChoice): String? =
            artChain(screen, choice).firstNotNullOfOrNull(slot)?.path
        GameDisplayArt(
            top = first(ArtScreen.Top, choices.effective(ArtScreen.Top, game.key, game.systemId)),
            bottom = first(
                ArtScreen.Bottom,
                choices.effective(ArtScreen.Bottom, game.key, game.systemId),
            ),
            default = first(ArtScreen.Top, ArtChoice.Default),
        )
    }

    /** Wide art for a short list of games. Callers must cap the list. */
    suspend fun wideArt(
        games: List<LibraryGame>,
        preferred: ArtSource,
        choices: GameArtChoices,
    ): Map<Long, WideGameArt> = readSlots(games, preferred) { game, slot ->
        val choice = choices.effective(ArtScreen.Bottom, game.key, game.systemId)
        WideGameArt(
            wide = wideArtChain(choice).firstNotNullOfOrNull(slot)?.path,
            scene = SCENE_KINDS.firstNotNullOfOrNull(slot)?.path,
        )
    }

    private suspend fun <T> readSlots(
        games: List<LibraryGame>,
        preferred: ArtSource,
        read: (LibraryGame, (MediaKind) -> ArtSlot?) -> T,
    ): Map<Long, T> = withContext(Dispatchers.IO) {
        val built = indexes.current()
        val stored = games.map { it.id }.chunked(QUERY_CHUNK)
            .flatMap { mediaDao.findByGameIds(it) }
            .associateBy { it.gameId }
        val customBySystem = games.map { it.systemId }.distinct()
            .associateWith(customArt::forSystem)
        games.associate { game ->
            val names = artBasenames(game.relativePath)
            val custom = customBySystem[game.systemId]
                ?.get(customArtKey(game.absolutePath, game.relativePath))
            val found = HashMap<MediaKind, ArtSlot?>()
            val slot: (MediaKind) -> ArtSlot? = { kind ->
                found.getOrPut(kind) {
                    fromCustom(custom, kind)
                        ?: fromSources(game.systemId, names, kind, preferred, built)
                        ?: fromLibraryRow(stored[game.id], kind)
                }
            }
            game.id to read(game, slot)
        }
    }

    private fun fromCustom(custom: CustomGameArt?, kind: MediaKind): ArtSlot? =
        custom?.pathFor(kind)?.takeIf(::isReadable)?.let { ArtSlot(it, source = null) }

    private fun fromSources(
        systemId: String,
        names: List<String>,
        kind: MediaKind,
        preferred: ArtSource,
        built: Map<ArtSource, ArtFolderIndex>,
    ): ArtSlot? {
        for (source in preferred.priority()) {
            val path = built[source]?.find(indexes.foldersFor(systemId, source), names, kind)
            if (path != null) return ArtSlot(path, source)
        }
        return null
    }

    private fun fromLibraryRow(stored: MediaEntity?, kind: MediaKind): ArtSlot? {
        val path = stored?.pathFor(kind)?.takeIf(::isReadable) ?: return null
        return ArtSlot(path, roots.sourceOf(path))
    }

    private fun isReadable(path: String): Boolean =
        path.startsWith("content://") || File(path).exists()

    private companion object {
        const val QUERY_CHUNK = 500
    }
}

/**
 * ES-DE names media after the stem, or the full name for a folder game
 * such as `Xenogears.m3u/`, so both are tried.
 */
internal fun artBasenames(relativePath: String): List<String> {
    val file = File(relativePath)
    return listOf(file.nameWithoutExtension, file.name).filter { it.isNotEmpty() }.distinct()
}

internal fun MediaEntity.pathFor(kind: MediaKind): String? = when (kind) {
    MediaKind.Cover -> coverPath
    MediaKind.Marquee -> marqueePath
    MediaKind.Fanart -> fanartPath
    MediaKind.Screenshot -> screenshotPath
    MediaKind.Titlescreen -> titlescreenPath
    MediaKind.Miximage -> miximagePath
    MediaKind.PhysicalMedia -> physicalMediaPath
    MediaKind.Video -> videoPath
}
