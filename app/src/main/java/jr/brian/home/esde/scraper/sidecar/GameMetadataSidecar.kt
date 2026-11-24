package jr.brian.home.esde.scraper.sidecar

import kotlinx.serialization.Serializable

/**
 * Per-game metadata sidecar written by scrapers (ScreenScraper, etc.) and read
 * as a fallback when ES-DE's gamelist.xml has no entry for the game.
 *
 * Stored at `<scrapedRoot>/<system>/.jarngreipr/<basename>.json` where
 * `<basename>` is the ROM filename without extension, matching the key the
 * `ScraperCoordinator` already uses for scraped media. Keeps scraper output
 * self-contained inside `scrapedRoot()` so a user can wipe the folder to reset.
 *
 * Every field is optional — a scraper that only knows a description writes a
 * sidecar with just that field populated. The reader overlays each non-null
 * field onto the primary [jr.brian.home.esde.util.RomMetadata], so a sidecar
 * never clobbers data that gamelist.xml already provides.
 */
@Serializable
data class GameMetadataSidecar(
    val description: String? = null,
    val genre: String? = null,
    val developer: String? = null,
    val publisher: String? = null,
    val players: String? = null,
    val rating: Float? = null,
    val releaseDate: String? = null,
)
