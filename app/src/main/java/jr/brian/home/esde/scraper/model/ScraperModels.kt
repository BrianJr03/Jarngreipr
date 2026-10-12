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
        val currentTitle: String?
    ) : ScrapeState
    data class Finished(
        val processed: Int,
        val downloads: Int,
        val errors: Int
    ) : ScrapeState
    data object Cancelled : ScrapeState
}

data class ScrapedMedia(
    val kind: ScraperMediaKind,
    val url: String,
    val extensionHint: String?
)

data class ScraperTestResult(
    val ok: Boolean,
    val message: String
)
