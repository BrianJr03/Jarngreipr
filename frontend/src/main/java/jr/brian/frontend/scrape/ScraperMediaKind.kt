package jr.brian.frontend.scrape

/**
 * Media the scraper can resolve. Mirrors
 * [jr.brian.frontend.library.media.MediaKind] but the two aren't the
 * same type on purpose — MediaKind is the on-disk layout contract
 * (used by MediaIndex to list folders) and ScraperMediaKind is the
 * network-request contract (used by clients to request specific
 * assets). Keeping them separate means a backend-specific kind can
 * land here without reshaping the on-disk layout.
 */
enum class ScraperMediaKind {
    Covers,
    Marquees,
    Fanart,
    Screenshots,
    TitleScreens,
    MixImages,
    Videos,
}
