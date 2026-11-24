package jr.brian.home.data

import jr.brian.frontend.library.art.CustomGameArt
import jr.brian.home.esde.scraper.sidecar.UserMetadataOverrideStore

/** The images saved through Classic's Edit Info for one system. Never writes. */
internal fun readUserOverrideArt(scrapedRoot: String, systemId: String): Map<String, CustomGameArt> {
    if (scrapedRoot.isBlank()) return emptyMap()
    return UserMetadataOverrideStore.readAllForSystem(scrapedRoot, systemId)
        .mapValues { (_, override) ->
            CustomGameArt(
                cover = override.artworkPath,
                marquee = override.marqueeImagePath,
                fanart = override.fanartPath,
                screenshot = override.screenshotPath,
            )
        }
        .filterValues { it != CustomGameArt() }
}
