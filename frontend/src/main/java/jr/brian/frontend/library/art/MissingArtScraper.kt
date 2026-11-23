package jr.brian.frontend.library.art

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.frontend.library.db.GameDao
import jr.brian.frontend.library.db.GameEntity
import jr.brian.frontend.library.media.MediaKind
import jr.brian.frontend.scrape.LibraryScraper
import jr.brian.frontend.scrape.ScrapeChoices
import jr.brian.frontend.scrape.ScrapeChoicesSource
import jr.brian.frontend.scrape.ScrapeSummary
import jr.brian.frontend.scrape.ScrapeWorker
import jr.brian.frontend.scrape.ScraperMediaKind
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues the background scrape. Only ever called from an explicit user action. */
@Singleton
class MissingArtScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun schedule(systemId: String?) = ScrapeWorker.enqueueMissingArt(context, systemId)

    fun cancel() = ScrapeWorker.cancel(context)
}

/**
 * Downloads only the image kinds a game has no art for, into Jarn FE's own
 * folder, without touching the shared `media` table Classic reads.
 */
@Singleton
class MissingArtScraper @Inject constructor(
    private val gameDao: GameDao,
    private val resolver: GameArtResolver,
    private val indexes: ArtIndexes,
    private val roots: ArtRoots,
    private val scraper: LibraryScraper,
    private val visibilitySource: LibraryVisibilitySource,
    private val choices: ScrapeChoicesSource = ScrapeChoicesSource { ScrapeChoices.All },
) {
    suspend fun run(systemId: String?): ScrapeSummary {
        val visibility = visibilitySource.visibility().first()
        val games = if (systemId == null) {
            gameDao.observeAllVisible().first()
        } else {
            gameDao.observeBySystem(systemId).first()
        }.filterNot { visibility.hides(it) }
        val chosen = choices.current()
        val missing = missingKinds(games, chosen.wantedKinds)
        if (missing.isEmpty()) {
            scraper.reportNothingMissing()
            return ScrapeSummary()
        }
        return try {
            scraper.scrape(
                games = games.filter { it.id in missing },
                request = chosen.toRequest(
                    isolatedRoot = roots.jarnScrapedRoot,
                    kindsForGame = { missing[it.id].orEmpty() },
                ),
            )
        } catch (e: CancellationException) {
            scraper.reportCancelled()
            throw e
        } finally {
            indexes.invalidate()
        }
    }

    private suspend fun missingKinds(
        games: List<GameEntity>,
        wanted: Set<ScraperMediaKind>,
    ): Map<Long, Set<ScraperMediaKind>> {
        val out = HashMap<Long, Set<ScraperMediaKind>>()
        for (game in games) {
            val art = resolver.resolve(game)
            val kinds = SCRAPABLE.filterKeys { art[it] == null }.values.filter { it in wanted }.toSet()
            if (kinds.isNotEmpty()) out[game.id] = kinds
        }
        return out
    }

    private companion object {
        /** Images only: videos are large and should be a deliberate choice. */
        val SCRAPABLE = mapOf(
            MediaKind.Cover to ScraperMediaKind.Covers,
            MediaKind.Marquee to ScraperMediaKind.Marquees,
            MediaKind.Fanart to ScraperMediaKind.Fanart,
            MediaKind.Screenshot to ScraperMediaKind.Screenshots,
            MediaKind.Titlescreen to ScraperMediaKind.TitleScreens,
            MediaKind.Miximage to ScraperMediaKind.MixImages,
        )
    }
}
