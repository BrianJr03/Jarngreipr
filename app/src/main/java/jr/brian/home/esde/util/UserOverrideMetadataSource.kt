package jr.brian.home.esde.util

import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore
import java.io.File

/**
 * Reads [jr.brian.home.esde.scraper.sidecar.UserMetadataOverride] blobs from
 * `<scrapedRoot>/<system>/.jarngreipr_user/<basename>.json` and surfaces them
 * as the top-priority [RomMetadataSource].
 *
 * Intended composition shape:
 *
 *   CompositeMetadataSource(
 *     primary = userOverrides,
 *     fallback = CompositeMetadataSource(
 *       primary = CompositeMetadataSource(gamelist, sidecar),
 *       fallback = mediaFallback,
 *     ),
 *   )
 *
 * The overlay in [CompositeMetadataSource] takes any non-null override field
 * and layers it over whatever gamelist / scraper / media-fallback produced,
 * so a user can patch exactly one field (e.g. just the cover art) without
 * losing the rest of the auto-resolved metadata.
 */
class UserOverrideMetadataSource(
    private val scrapedRoot: String,
) : RomMetadataSource {

    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> {
        if (scans.isEmpty()) return emptyMap()
        val all = UserMetadataOverrideStore.readAllForSystem(scrapedRoot, systemName)
        if (all.isEmpty()) return emptyMap()
        val result = HashMap<ScannedRom, RomMetadata>()
        for (scan in scans) {
            val basename = File(scan.relativePath).nameWithoutExtension.ifBlank { continue }
            val override = all[basename] ?: continue
            result[scan] = RomMetadata(
                displayName = override.displayName,
                description = override.description,
                rating = override.rating ?: 0f,
                releaseDate = override.releaseDate,
                developer = override.developer,
                publisher = override.publisher,
                genre = override.genre,
                players = override.players,
                artworkPath = override.artworkPath,
                screenshotPath = override.screenshotPath,
                fanartPath = override.fanartPath,
            )
        }
        return result
    }
}
