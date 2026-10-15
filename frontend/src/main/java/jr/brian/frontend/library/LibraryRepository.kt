package jr.brian.frontend.library

import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.db.MediaDao
import jr.brian.frontend.library.db.MediaEntity
import jr.brian.frontend.library.db.PlayHistoryDao
import jr.brian.frontend.library.db.ScrapeJobDao
import jr.brian.frontend.library.db.SourceDao
import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.db.SystemDao
import jr.brian.frontend.library.db.SystemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public read API over the library engine, injected by `:app` to replace
 * `RomSearchStateHolder` reads in later phases.
 *
 * ### Phase 2a scope
 *
 * This commit lands only the read side. Writes — `addSource`, `scanAll`,
 * `scanSystem`, `importGamelist` — are shaped by phases 2b–2d where the
 * scanner, catalog, and migration land. The state flow is here so the
 * UI can start consuming it under a feature flag before writes do
 * anything useful; `state.value == LibraryState.Idle` is the valid steady
 * state until then.
 *
 * ### Why the DAOs stay internal
 *
 * `:app` (and later `:frontend/ui`) depend on this repository instead
 * of the DAOs directly so swapping storage, caching, or combining
 * sources later is a one-file change. The repository is also the right
 * place to apply the hidden-games filter, the user-override merge, and
 * the per-source grant check without duplicating that logic across
 * every Compose consumer.
 */
@Singleton
class LibraryRepository @Inject constructor(
    private val systemDao: SystemDao,
    private val sourceDao: SourceDao,
    private val gameDao: GameDao,
    private val mediaDao: MediaDao,
    @Suppress("unused") private val scrapeJobDao: ScrapeJobDao,
    @Suppress("unused") private val playHistoryDao: PlayHistoryDao,
) {
    private val _state = MutableStateFlow<LibraryState>(LibraryState.Idle)

    /** Observable engine state — drives the "Scanning X / Y" UI chrome. */
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    /** All registered systems, display-name ordered. */
    val systems: Flow<List<SystemEntity>> = systemDao.observeAll()

    /** All configured sources, display-path ordered. */
    val sources: Flow<List<SourceEntity>> = sourceDao.observeAll()

    /** All visible games, display-name ordered. Hidden games are filtered out at the DAO. */
    val allGames: Flow<List<GameEntity>> = gameDao.observeAllVisible()

    /** Favorites row for Home. */
    val favorites: Flow<List<GameEntity>> = gameDao.observeFavorites()

    /** Visible games under one system id — Phase 3's Games screen. */
    fun gamesForSystem(systemId: String): Flow<List<GameEntity>> =
        gameDao.observeBySystem(systemId)

    /** Most recently played, capped. */
    fun recentGames(limit: Int = 20): Flow<List<GameEntity>> =
        gameDao.observeRecent(limit)

    /** Resolved media for one game, or null until the index pass runs. */
    fun mediaForGame(gameId: Long): Flow<MediaEntity?> =
        mediaDao.observeByGameId(gameId)

    /** Lookup helper for pinned-ROM / canvas tiles. */
    suspend fun findGameById(gameId: Long): GameEntity? = gameDao.findById(gameId)
    suspend fun findSystemById(id: String): SystemEntity? = systemDao.findById(id)
}
