package jr.brian.frontend.scrape

import jr.brian.frontend.library.db.GameEntity
import java.io.File

/** Which sources are on and which art each may fetch. Holds no credentials. */
data class ScrapeChoices(
    val screenScraperEnabled: Boolean,
    val screenScraperKinds: Set<ScraperMediaKind>,
    val steamGridDbEnabled: Boolean,
    val steamGridDbKinds: Set<ScraperMediaKind>,
) {
    val wantedKinds: Set<ScraperMediaKind>
        get() = (if (screenScraperEnabled) screenScraperKinds else emptySet()) +
            (if (steamGridDbEnabled) steamGridDbKinds else emptySet())

    fun toRequest(
        isolatedRoot: File? = null,
        kindsForGame: ((GameEntity) -> Set<ScraperMediaKind>)? = null,
    ) = ScrapeRequest(
        screenScraperEnabled = screenScraperEnabled,
        screenScraperKinds = screenScraperKinds,
        steamGridDbEnabled = steamGridDbEnabled,
        steamGridDbKinds = steamGridDbKinds,
        isolatedRoot = isolatedRoot,
        kindsForGame = kindsForGame,
    )

    companion object {
        val All = ScrapeChoices(
            screenScraperEnabled = true,
            screenScraperKinds = ScraperMediaKind.entries.toSet(),
            steamGridDbEnabled = true,
            steamGridDbKinds = ScraperMediaKind.entries.toSet(),
        )
    }
}

/** Implemented by the host app, which owns the saved scraper settings. */
fun interface ScrapeChoicesSource {
    fun current(): ScrapeChoices
}
