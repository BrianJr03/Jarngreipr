package jr.brian.frontend.library.art

import jr.brian.core.model.ArtSource
import jr.brian.core.model.SystemArtSource
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.SystemDao
import jr.brian.frontend.library.media.MediaKind
import jr.brian.frontend.scrape.LibraryScraper
import jr.brian.frontend.scrape.ScrapeChoices
import jr.brian.frontend.scrape.ScrapeChoicesSource
import jr.brian.frontend.scrape.ScrapeState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The narrow read API `:jarnfe` builds its Library on. Nothing here scans,
 * scrapes, or writes art on its own; [scrapeMissingArt] is the only path to
 * a download and runs only when called.
 */
@Singleton
class JarnLibraryData @Inject constructor(
    private val systemDao: SystemDao,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    private val resolver: GameArtResolver,
    private val indexes: ArtIndexes,
    private val store: ArtSourceStore,
    private val scraper: LibraryScraper,
    private val scheduler: MissingArtScheduler,
    private val recentLaunches: RecentLaunchSource,
    private val visibilitySource: LibraryVisibilitySource,
    private val artChoiceStore: GameArtChoiceStore,
    private val choices: ScrapeChoicesSource = ScrapeChoicesSource { ScrapeChoices.All },
) {
    val artSource: StateFlow<ArtSource> = store.state
    val artRevision: StateFlow<Long> = indexes.revision
    val scrapeState: StateFlow<ScrapeState> = scraper.state
    val artChoices: StateFlow<GameArtChoices> = artChoiceStore.state

    /** Systems with a visible game, by display name. Fully hidden ones are only counted. */
    val systems: Flow<VisibleList<LibrarySystem>> = combine(
        systemDao.observeAll(),
        gameDao.observeVisibleKeys(),
        visibilitySource.visibility(),
    ) { systems, keys, visibility ->
        val bySystem = keys.groupBy { it.systemId }
        val shown = bySystem.mapValues { (_, rows) ->
            rows.count { !visibility.hides(it.systemId, it.absolutePath, it.relativePath) }
        }
        VisibleList(
            items = systems.mapNotNull { system ->
                shown[system.id]?.takeIf { it > 0 }
                    ?.let { LibrarySystem(system.id, system.displayName, it) }
            },
            hiddenCount = shown.count { it.value == 0 },
        )
    }.distinctUntilChanged()

    val systemArtSource: StateFlow<SystemArtSource> = store.systemState

    /** [systems] with their art, re-read when the source or art indexes change. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val systemsWithArt: Flow<VisibleList<LibrarySystemWithArt>> =
        combine(systems, store.systemState, indexes.revision) { list, source, _ -> list to source }
            .mapLatest { (list, source) ->
                val art = indexes.systemArt(list.items.map { it.id }, source)
                VisibleList(
                    items = list.items.map { LibrarySystemWithArt(it, art[it.id] ?: SystemArt()) },
                    hiddenCount = list.hiddenCount,
                )
            }

    val favorites: Flow<VisibleList<LibraryGame>> =
        visible(gameDao.observeFavorites())

    /** Every visible game, for searching across the whole library. */
    val allGames: Flow<VisibleList<LibraryGame>> = visible(gameDao.observeAllVisible())

    fun games(systemId: String): Flow<VisibleList<LibraryGame>> =
        visible(gameDao.observeBySystem(systemId))

    /** Newest first, merging Jarngreipr launches with imported play dates. */
    fun recent(limit: Int = DEFAULT_RECENT_LIMIT): Flow<VisibleList<LibraryGame>> = combine(
        recentLaunches.launches(),
        gameDao.observeRecent(limit),
        visibilitySource.visibility(),
    ) { launches, imported, visibility ->
        VisibleList(mergeRecent(launches, imported, visibility, limit))
    }.distinctUntilChanged()

    /** Last played, play time and favorites per system, after the user's hide rules. */
    val systemActivity: Flow<Map<String, SystemActivity>> = combine(
        recentLaunches.launches(),
        gameDao.observePlayRows(),
        visibilitySource.visibility(),
    ) { launches, rows, visibility ->
        val launched = launches.mapNotNull { launch ->
            gameDao.findVisibleByLaunchPath(launch.systemId, launch.path)
                ?.takeUnless { visibility.hides(it) }
                ?.let { it.systemId to launch.launchedAt }
        }
        val shown = rows.filterNot { visibility.hides(it.systemId, it.absolutePath, it.relativePath) }
        summarizeActivity(shown, launched)
    }.distinctUntilChanged()

    /** A capped shelf for one system. Art is resolved for the shelf's games only. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun systemShelf(systemId: String): Flow<SystemShelf> {
        val picks = visibilitySource.visibility()
            .flatMapLatest { shelfPicks(systemId, it) }
            .distinctUntilChanged()
        return combine(picks, store.state, indexes.revision, artChoiceStore.state) { games, source, _, choices ->
            Triple(games, source, choices)
        }.mapLatest { (games, source, choices) -> shelfWithArt(games, source, choices) }
            .distinctUntilChanged()
    }

    /** One game's wide image, following its art choice and any new art. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun wideArt(key: String): Flow<String?> = flow {
        val game = findGame(key)
        if (game == null) {
            emit(null)
            return@flow
        }
        val inputs = combine(
            store.state,
            indexes.revision,
            artChoiceStore.state,
            mediaDao.observeByGameId(game.id),
        ) { source, _, choices, _ -> source to choices }
        emitAll(
            inputs.mapLatest { (source, choices) ->
                resolver.wideArt(listOf(game), source, choices)[game.id]?.wide
            },
        )
    }.distinctUntilChanged()

    /** Adds each game's art, re-resolving when the source, indexes, or art choices change. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun withArt(games: Flow<VisibleList<LibraryGame>>): Flow<VisibleList<LibraryGameWithArt>> =
        combine(games, store.state, indexes.revision, artChoiceStore.state) { list, source, _, picks ->
            Triple(list, source, picks)
        }.mapLatest { (list, source, picks) ->
            val art = resolver.displayArt(list.items, source, picks)
            VisibleList(
                items = list.items.map { game ->
                    val resolved = art[game.id]
                    LibraryGameWithArt(game, resolved?.top, resolved?.bottom, resolved?.default)
                },
                hiddenCount = list.hiddenCount,
            )
        }

    /** Re-emits when the source, the indexes, or this game's library row change. */
    fun art(game: LibraryGame): Flow<GameArt> =
        combine(store.state, indexes.revision, mediaDao.observeByGameId(game.id)) { source, _, _ ->
            gameDao.findById(game.id)?.let { resolver.resolve(it, source) } ?: GameArt()
        }.distinctUntilChanged()

    /** The current row for a [LibraryGame.key], or null if it left the library. */
    suspend fun findGame(key: String): LibraryGame? {
        val parsed = parseGameKey(key) ?: return null
        return gameDao.findByTriple(parsed.sourceId, parsed.systemId, parsed.relativePath)
            ?.toLibraryGame()
    }

    /** The art kinds this game actually has a file for. */
    suspend fun availableArt(key: String): Set<MediaKind> {
        val entity = parseGameKey(key)?.let {
            gameDao.findByTriple(it.sourceId, it.systemId, it.relativePath)
        } ?: return emptySet()
        return resolver.resolve(entity, store.source).slots.keys
    }

    fun setGameArtChoice(screen: ArtScreen, gameKey: String, systemId: String, choice: ArtChoice) =
        artChoiceStore.setForGame(screen, gameKey, systemId, choice)

    fun setSystemArtChoice(screen: ArtScreen, systemId: String, choice: ArtChoice) =
        artChoiceStore.setForSystem(screen, systemId, choice)

    fun setArtSource(source: ArtSource) = store.setSource(source)

    fun setSystemArtSource(source: SystemArtSource) = store.setSystemSource(source)

    /** Re-reads the art folders, for after the user adds files outside the app. */
    fun refreshArt() = indexes.invalidate()

    suspend fun artSourceStatus(): List<ArtSourceStatus> {
        val built = indexes.current()
        return ArtSource.entries.map { source ->
            val count = built[source]?.fileCount ?: 0
            ArtSourceStatus(source, isAvailable = count > 0, fileCount = count)
        }
    }

    /** True when a source the user turned on has a saved account. */
    fun canScrape(): Boolean = scraper.canScrape(choices.current().toRequest())

    /** Queues a scrape of missing art, for one system or the whole library. */
    fun scrapeMissingArt(systemId: String? = null): ScrapeMissingArtResult {
        if (!canScrape()) return ScrapeMissingArtResult.NotConfigured
        scheduler.schedule(systemId)
        return ScrapeMissingArtResult.Started
    }

    fun cancelScrape() = scheduler.cancel()

    private fun visible(games: Flow<List<GameEntity>>): Flow<VisibleList<LibraryGame>> =
        combine(games, visibilitySource.visibility()) { list, visibility ->
            val shown = list.filterNot { visibility.hides(it) }
            VisibleList(shown.map { it.toLibraryGame() }, list.size - shown.size)
        }.distinctUntilChanged()

    /** Hide rules run after the query, so it over-fetches by what they could remove. */
    private fun shelfPicks(
        systemId: String,
        visibility: LibraryVisibility,
    ): Flow<List<Pair<LibraryGame, SystemShelfGroup>>> {
        val fetch = SHELF_MAX_GAMES + visibility.hiddenGames.count { it.startsWith("$systemId/") }
        return combine(
            recentLaunches.launches(),
            gameDao.observeRecentInSystem(systemId, fetch),
            gameDao.observeMostPlayedInSystem(systemId, fetch),
            gameDao.observePreviewInSystem(systemId, fetch),
        ) { launches, imported, mostPlayed, browse ->
            val here = launches.filter { it.systemId == systemId }
            pickShelf(
                recent = mergeRecent(here, imported, visibility, SHELF_GROUP_GAMES),
                mostPlayed = mostPlayed.filterNot { visibility.hides(it) }.map { it.toLibraryGame() },
                browse = browse.filterNot { visibility.hides(it) }.map { it.toLibraryGame() },
            )
        }
    }

    private suspend fun shelfWithArt(
        picks: List<Pair<LibraryGame, SystemShelfGroup>>,
        source: ArtSource,
        choices: GameArtChoices,
    ): SystemShelf {
        val games = picks.map { it.first }
        val display = resolver.displayArt(games, source, choices)
        val wide = resolver.wideArt(games, source, choices)
        return SystemShelf(
            games = picks.map { (game, group) ->
                val art = display[game.id]
                SystemShelfGame(LibraryGameWithArt(game, art?.top, art?.bottom, art?.default), group)
            },
            sceneArtPath = games.firstNotNullOfOrNull { wide[it.id]?.scene },
        )
    }

    private suspend fun mergeRecent(
        launches: List<RecentLaunch>,
        imported: List<GameEntity>,
        visibility: LibraryVisibility,
        limit: Int,
    ): List<LibraryGame> {
        val latest = LinkedHashMap<Long, Pair<GameEntity, Long>>()
        fun offer(game: GameEntity, at: Long) {
            if (visibility.hides(game)) return
            val current = latest[game.id]
            if (current == null || at > current.second) latest[game.id] = game to at
        }
        for (launch in launches) {
            gameDao.findVisibleByLaunchPath(launch.systemId, launch.path)
                ?.let { offer(it, launch.launchedAt) }
        }
        imported.forEach { game -> game.lastPlayed?.let { offer(game, it) } }
        return latest.values
            .sortedByDescending { it.second }
            .take(limit)
            .map { (game, at) -> game.toLibraryGame().copy(lastPlayed = at) }
    }

    private companion object {
        const val DEFAULT_RECENT_LIMIT = 20
    }
}

internal fun LibraryVisibility.hides(game: GameEntity): Boolean =
    hides(game.systemId, game.absolutePath, game.relativePath)
