package jr.brian.home.esde.util

/**
 * Overlays [primary] on top of [fallback] field-by-field: for each scanned
 * ROM, primary's non-null/non-default fields win, and fallback fills every
 * null the primary left behind. Primary and fallback are queried in parallel
 * on the same scan set, so a scan that only the fallback knows about still
 * appears.
 *
 * Used to compose [GamelistMetadataSource] (ES-DE gamelist.xml, source of
 * truth) with [SidecarMetadataSource] (scraper-written JSON blobs under
 * `scrapedRoot()`): ES-DE wins everywhere, scraped text fills gaps.
 */
class CompositeMetadataSource(
    private val primary: RomMetadataSource,
    private val fallback: RomMetadataSource,
) : RomMetadataSource {

    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> {
        val primaryHits = primary.metadataFor(systemName, scans)
        val fallbackHits = fallback.metadataFor(systemName, scans)
        if (fallbackHits.isEmpty()) return primaryHits
        if (primaryHits.isEmpty()) return fallbackHits

        val merged = HashMap<ScannedRom, RomMetadata>(primaryHits.size + fallbackHits.size)
        merged.putAll(fallbackHits)
        for ((scan, p) in primaryHits) {
            val f = fallbackHits[scan]
            merged[scan] = if (f == null) p else overlay(primary = p, fallback = f)
        }
        return merged
    }

    private fun overlay(primary: RomMetadata, fallback: RomMetadata): RomMetadata = RomMetadata(
        displayName = primary.displayName ?: fallback.displayName,
        description = primary.description ?: fallback.description,
        rating = if (primary.rating > 0f) primary.rating else fallback.rating,
        releaseDate = primary.releaseDate ?: fallback.releaseDate,
        developer = primary.developer ?: fallback.developer,
        publisher = primary.publisher ?: fallback.publisher,
        genre = primary.genre ?: fallback.genre,
        players = primary.players ?: fallback.players,
        isFavorite = primary.isFavorite || fallback.isFavorite,
        playCount = if (primary.playCount > 0) primary.playCount else fallback.playCount,
        playTimeMinutes = if (primary.playTimeMinutes > 0) primary.playTimeMinutes else fallback.playTimeMinutes,
        lastPlayed = primary.lastPlayed ?: fallback.lastPlayed,
        artworkPath = primary.artworkPath ?: fallback.artworkPath,
        physicalMediaPath = primary.physicalMediaPath ?: fallback.physicalMediaPath,
        marqueeImagePath = primary.marqueeImagePath ?: fallback.marqueeImagePath,
        screenshotPath = primary.screenshotPath ?: fallback.screenshotPath,
        fanartPath = primary.fanartPath ?: fallback.fanartPath,
        titlescreenPath = primary.titlescreenPath ?: fallback.titlescreenPath,
        miximagePath = primary.miximagePath ?: fallback.miximagePath,
    )
}
