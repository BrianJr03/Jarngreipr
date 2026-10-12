package jr.brian.home.esde.util

import jr.brian.home.esde.scraper.sidecar.GameMetadataSidecarStore
import java.io.File

/**
 * Reads [jr.brian.home.esde.scraper.sidecar.GameMetadataSidecar] JSON blobs
 * out of `<scrapedRoot>/<system>/.jarngreipr/<basename>.json` and converts
 * each into a [RomMetadata] carrying the sidecar's text fields.
 *
 * Media paths are intentionally left null — scraped media lives alongside the
 * sidecar in the usual ES-DE folder layout and is resolved by the standard
 * `findFirstMedia()` pass elsewhere. This source only contributes TEXT
 * decoration (description, genre, developer, publisher, players, rating,
 * releaseDate) so composition with [GamelistMetadataSource] can't conflict
 * on image resolution.
 *
 * Intended composition shape: `CompositeMetadataSource(gamelist, sidecar)` —
 * gamelist.xml wins on any field it defines; the sidecar fills nulls only.
 * That ordering keeps ES-DE as the source of truth and makes scraped text
 * a strict addition.
 */
class SidecarMetadataSource(
    private val scrapedRoot: String,
) : RomMetadataSource {

    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> {
        if (scans.isEmpty()) return emptyMap()
        val result = HashMap<ScannedRom, RomMetadata>()
        for (scan in scans) {
            val basename = File(scan.relativePath).nameWithoutExtension.ifBlank { continue }
            val sidecar = GameMetadataSidecarStore.read(scrapedRoot, systemName, basename)
                ?: continue
            result[scan] = RomMetadata(
                description = sidecar.description,
                rating = sidecar.rating ?: 0f,
                releaseDate = sidecar.releaseDate,
                developer = sidecar.developer,
                publisher = sidecar.publisher,
                genre = sidecar.genre,
                players = sidecar.players,
            )
        }
        return result
    }
}
