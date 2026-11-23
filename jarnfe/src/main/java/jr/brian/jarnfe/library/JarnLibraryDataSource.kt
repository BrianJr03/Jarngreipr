package jr.brian.jarnfe.library

import android.content.Context
import jr.brian.frontend.library.art.ArtSourceDetector
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.LibraryGame
import jr.brian.frontend.library.art.LibraryGameWithArt
import jr.brian.frontend.library.art.VisibleList
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.frontend.library.setup.LibrarySetupState
import jr.brian.jarnfe.systems.JarnSystems
import jr.brian.jarnfe.systems.jarnSystemManagerOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.NumberFormat

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnLibraryDataProvider {
    val jarnLibraryData: JarnLibraryData
    val jarnLibrarySetup: LibrarySetup
}

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnArtDetectorProvider {
    val jarnArtDetector: ArtSourceDetector
}

fun Context.jarnArtDetectorOrNull(): ArtSourceDetector? =
    (applicationContext as? JarnArtDetectorProvider)?.jarnArtDetector

fun Context.jarnLibraryDataOrNull(): JarnLibraryData? =
    (applicationContext as? JarnLibraryDataProvider)?.jarnLibraryData

fun Context.jarnLibrarySetupOrNull(): LibrarySetup? =
    (applicationContext as? JarnLibraryDataProvider)?.jarnLibrarySetup

/** Read-only bridge from the shared library to the Library screens. */
internal class JarnLibraryDataSource(
    private val data: JarnLibraryData,
    private val setup: LibrarySetup,
) : JarnLibrarySource {
    override fun observeSystems(): Flow<JarnLibraryFeed<JarnLibrarySystem>> =
        combine(data.systemsWithArt, setup.state) { list, setupState ->
            JarnLibraryFeed(
                items = list.items.map { (system, art) ->
                    JarnLibrarySystem(
                        id = system.id,
                        title = system.displayName,
                        gameCount = system.gameCount,
                        artPath = art.backgroundPath,
                        logoPath = art.logoPath,
                    )
                },
                status = setupState.toFeedStatus(),
                hiddenCount = list.hiddenCount,
            )
        }

    override fun observeGames(systemId: String) = data.games(systemId).asFeed()

    override fun observeFavorites() = data.favorites.asFeed()

    override fun observeRecent() = data.recent().asFeed()

    override fun refresh() {
        data.refreshArt()
        setup.retry()
    }

    private fun Flow<VisibleList<LibraryGame>>.asFeed(): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        data.withArt(this).map { list ->
            JarnLibraryFeed.ready(list.items.map { it.toJarnGame() }, list.hiddenCount)
        }
}

internal fun LibrarySetupState.toFeedStatus(): JarnLibraryStatus = when (this) {
    LibrarySetupState.Checking -> JarnLibraryStatus.Loading()
    is LibrarySetupState.Scanning -> JarnLibraryStatus.Loading(
        loaded = systemsDone.takeIf { systemsTotal > 0 },
        total = systemsTotal.takeIf { it > 0 },
    )
    LibrarySetupState.ScanFailed -> JarnLibraryStatus.Unavailable
    else -> JarnLibraryStatus.Ready
}

internal fun LibraryGameWithArt.toJarnGame() = JarnLibraryGame(
    id = game.key,
    systemId = game.systemId,
    title = game.title,
    artPath = artPath,
    deckArtPath = deckArtPath,
    defaultArtPath = defaultArtPath,
    metadata = JarnGameMetadata(
        description = game.description,
        developer = game.developer,
        publisher = game.publisher,
        genre = game.genre,
        players = game.players,
        releaseDate = game.releaseDate,
        rating = ratingText(game.rating),
        playtimeMinutes = game.playTimeMinutes.toLong(),
        lastPlayedAtMs = game.lastPlayed,
    ),
)

/** ES-DE stores ratings from 0 to 1. Zero means unrated. */
internal fun ratingText(rating: Float?): String? =
    rating?.takeIf { it > 0f && it <= 1f }?.let { NumberFormat.getPercentInstance().format(it) }

/** Safe to call from every activity: an already connected library is left as it is. */
internal fun connectJarnLibrary(context: Context) {
    JarnSystems.manager = context.jarnSystemManagerOrNull()
    if (JarnLibrary.source is JarnLibraryDataSource) return
    val data = context.jarnLibraryDataOrNull() ?: return
    val setup = context.jarnLibrarySetupOrNull() ?: return
    JarnLibrary.connect(JarnLibraryDataSource(data, setup))
    setup.start()
}
