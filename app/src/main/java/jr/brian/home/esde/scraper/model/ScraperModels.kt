package jr.brian.home.esde.scraper.model

import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.ScraperMediaKind

data class ScrapeRequest(
    val games: List<GameInfo>,
    val mediaTypesScreenScraper: Set<ScraperMediaKind>,
    val mediaTypesSteamGridDb: Set<ScraperMediaKind>,
    val screenScraperEnabled: Boolean,
    val steamGridDbEnabled: Boolean
)

sealed interface ScrapeState {
    data object Idle : ScrapeState
    data class Running(
        val done: Int,
        val total: Int,
        val downloads: Int,
        val errors: Int,
        val currentTitle: String?,
        /**
         * Per-game error messages surfaced from ScreenScraper / SteamGridDB
         * network calls (e.g. "HTTP 429" quota responses). Previously these
         * failures were swallowed into empty-list results and counted as
         * "0 found", leaving users to guess why nothing was scraped.
         */
        val errorMessages: List<ScrapeError> = emptyList(),
    ) : ScrapeState
    data class Finished(
        val processed: Int,
        val downloads: Int,
        val errors: Int,
        val errorMessages: List<ScrapeError> = emptyList(),
    ) : ScrapeState
    data object Cancelled : ScrapeState
}

/**
 * A single error surfaced during scraping, intended for direct display in
 * the UI. [message] already carries enough context ("HTTP 429 - quota
 * reached") to be a terminal line; UI should NOT prefix with a generic
 * "An error occurred" since that reads as redundant.
 */
data class ScrapeError(
    val gameTitle: String,
    val message: String,
)

data class ScrapedMedia(
    val kind: ScraperMediaKind,
    val url: String,
    val extensionHint: String?
)

data class ScraperTestResult(
    val ok: Boolean,
    val message: String
)
