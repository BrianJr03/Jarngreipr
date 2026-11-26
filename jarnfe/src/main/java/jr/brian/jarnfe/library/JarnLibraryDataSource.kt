package jr.brian.jarnfe.library

import android.content.Context
import jr.brian.frontend.library.art.ArtSourceDetector
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.LibraryGame
import jr.brian.frontend.library.art.LibraryGameWithArt
import jr.brian.frontend.library.art.SystemActivity
import jr.brian.frontend.library.art.SystemShelfGroup
import jr.brian.frontend.library.art.VisibleList
import jr.brian.frontend.library.setup.LibrarySetup
import jr.brian.frontend.library.setup.LibrarySetupState
import jr.brian.jarnfe.systems.JarnManagedSystem
import jr.brian.jarnfe.systems.JarnSystemManager
import jr.brian.jarnfe.systems.JarnSystems
import jr.brian.jarnfe.systems.jarnSystemManagerOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    private val systems: JarnSystemManager? = null,
) : JarnLibrarySource {
    override val collections: JarnCollectionSource = JarnCollectionDataSource(data)

    override fun observeSystems(): Flow<JarnLibraryFeed<JarnLibrarySystem>> =
        combine(
            data.systemsWithArt,
            setup.state,
            data.systemActivity.onStart { emit(emptyMap()) },
            nowBackgrounds(systems),
        ) { list, setupState, activity, backgrounds ->
            JarnLibraryFeed(
                items = list.items.map { (system, art) ->
                    val played = activity[system.id] ?: SystemActivity()
                    JarnLibrarySystem(
                        id = system.id,
                        title = system.displayName,
                        gameCount = system.gameCount,
                        artPath = art.backgroundPath,
                        logoPath = art.logoPath,
                        lastPlayedAtMs = played.lastPlayedAt,
                        favoriteCount = played.favoriteCount,
                        playtimeMinutes = played.playTimeMinutes.toLong(),
                        nowBackgroundPath = backgrounds[system.id],
                    )
                },
                status = setupState.toFeedStatus(),
                hiddenCount = list.hiddenCount,
            )
        }

    override fun observeGames(systemId: String): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        combine(data.games(systemId).asFeed(), gameSort(systems, systemId)) { feed, sort ->
            feed.copy(items = feed.items.sortedFor(sort), sort = sort)
        }

    /** Sorted off the main thread, since a large library holds thousands of games. */
    override fun observeAllGames(): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        data.allGames().asFeed()
            .map { it.inNameOrder() }
            .flowOn(Dispatchers.Default)

    override fun observeFavorites() = data.favorites.asFeed()

    override fun observeRecent() = data.recent().asFeed()

    override fun observeSystemNow(systemId: String): Flow<JarnSystemNow> =
        data.systemShelf(systemId).map { shelf ->
            JarnSystemNow(
                systemId = systemId,
                shelf = shelf.games.map { JarnShelfGame(it.game.toJarnGame(), it.group.toJarnGroup()) },
                sceneArtPath = shelf.sceneArtPath,
                isLoaded = true,
            )
        }

    override fun observeWideArt(gameId: String): Flow<String?> = data.wideArt(gameId)

    override fun refresh() {
        data.refreshArt()
        setup.retry()
    }

    private fun Flow<VisibleList<LibraryGame>>.asFeed(): Flow<JarnLibraryFeed<JarnLibraryGame>> =
        data.withArt(this).map { list ->
            JarnLibraryFeed.ready(list.items.map { it.toJarnGame() }, list.hiddenCount)
        }
}

/** The system's picked order. A failing manager falls back to name order, never the library. */
internal fun gameSort(manager: JarnSystemManager?, systemId: String): Flow<JarnGameSort> =
    manager?.gameSorts
        ?.map { it[systemId] ?: JarnGameSort.Default }
        ?.catch { emit(JarnGameSort.Default) }
        ?.distinctUntilChanged()
        ?: flowOf(JarnGameSort.Default)

/** Each system's picked Now background. A failing manager only drops the picks, never the library. */
internal fun nowBackgrounds(manager: JarnSystemManager?): Flow<Map<String, String>> =
    manager?.systems
        ?.map(::nowBackgroundsById)
        ?.catch { emit(emptyMap()) }
        ?.distinctUntilChanged()
        ?: flowOf(emptyMap())

internal fun nowBackgroundsById(systems: List<JarnManagedSystem>): Map<String, String> =
    systems.mapNotNull { system ->
        system.appearance.focusBackgroundUri?.takeIf(String::isNotBlank)?.let { system.id to it }
    }.toMap()

private fun SystemShelfGroup.toJarnGroup(): JarnShelfGroup = when (this) {
    SystemShelfGroup.Recent -> JarnShelfGroup.Recent
    SystemShelfGroup.MostPlayed -> JarnShelfGroup.MostPlayed
    SystemShelfGroup.InSystem -> JarnShelfGroup.InSystem
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
        playCount = game.playCount.takeIf { it > 0 },
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
    JarnLibrary.connect(JarnLibraryDataSource(data, setup, JarnSystems.manager))
    setup.start()
}

/** The All games order: the Name sort's order, marked so letter jump turns on. */
internal fun JarnLibraryFeed<JarnLibraryGame>.inNameOrder(): JarnLibraryFeed<JarnLibraryGame> =
    copy(items = items.sortedFor(JarnGameSort.Name), sort = JarnGameSort.Name)
