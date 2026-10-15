package jr.brian.frontend.scrape

/**
 * One asset the scraper has located but not yet downloaded. The
 * [extensionHint] lets the coordinator write the file with the
 * right suffix (`png` vs `jpg` matters for the browser-side cache);
 * null falls back to the URL's path extension.
 */
data class ScrapedMedia(
    val kind: ScraperMediaKind,
    val url: String,
    val extensionHint: String?,
)
