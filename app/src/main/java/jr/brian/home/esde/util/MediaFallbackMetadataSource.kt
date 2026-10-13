package jr.brian.home.esde.util

import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_COVERS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_FANART
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MARQUEES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_MIXIMAGES
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_PHYSICALMEDIA
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_SCREENSHOTS
import jr.brian.home.esde.util.ESDEMediaConstants.FOLDER_TITLESCREENS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS
import jr.brian.home.esde.util.ESDEMediaConstants.IMAGE_EXTENSIONS_WITH_SVG
import jr.brian.home.esde.util.ESDEMediaConstants.MARQUEE_FALLBACK_DIRS
import jr.brian.home.esde.util.ESDEMediaConstants.getMediaSystemName

/**
 * Fills in media paths for scans that no gamelist.xml entry decorates.
 *
 * [GamelistMetadataSource] only produces [RomMetadata] for scans a `<game>`
 * block names, so a PC shortcut the user never scraped in ES-DE (Titanfall 2,
 * Mouse P.I. For Hire, Duck Duck Hotel, ...) never gets art attached — even
 * when the files are sitting under a configured media root. This source runs
 * [findFirstMedia] against the same [mediaPaths] using the scan's own stem,
 * so any cover/fanart/screenshot the user has on disk is still surfaced.
 *
 * Media-only on purpose. Text (name, description, release date, ...) stays
 * the gamelist's job; filename-derived titles come from `RomIndexBuilder`
 * when no source wins at all. Intended composition shape:
 * `CompositeMetadataSource(CompositeMetadataSource(gamelist, sidecar), mediaFallback)`
 * — gamelist media wins everywhere, this source fills every path it still
 * sees as null.
 */
class MediaFallbackMetadataSource(
    private val mediaPaths: List<String>,
) : RomMetadataSource {

    override suspend fun metadataFor(
        systemName: String,
        scans: List<ScannedRom>,
    ): Map<ScannedRom, RomMetadata> {
        if (scans.isEmpty() || mediaPaths.isEmpty()) return emptyMap()
        val systemNames = listOf(systemName, getMediaSystemName(systemName)).distinct()
        val result = HashMap<ScannedRom, RomMetadata>()
        for (scan in scans) {
            val filename = scan.relativePath
            fun media(folder: String) = findFirstMedia(
                mediaPaths = mediaPaths,
                systemNames = systemNames,
                folders = listOf(folder),
                gameFilename = filename,
                extensions = IMAGE_EXTENSIONS,
            )
            val marquee = findFirstMedia(
                mediaPaths = mediaPaths,
                systemNames = systemNames,
                folders = listOf(FOLDER_MARQUEES) + MARQUEE_FALLBACK_DIRS,
                gameFilename = filename,
                extensions = IMAGE_EXTENSIONS_WITH_SVG,
            )
            val artwork = media(FOLDER_COVERS)
            val physical = media(FOLDER_PHYSICALMEDIA)
            val screenshot = media(FOLDER_SCREENSHOTS)
            val fanart = media(FOLDER_FANART)
            val titlescreen = media(FOLDER_TITLESCREENS)
            val miximage = media(FOLDER_MIXIMAGES)
            if (artwork == null && physical == null && marquee == null &&
                screenshot == null && fanart == null && titlescreen == null &&
                miximage == null) continue
            result[scan] = RomMetadata(
                artworkPath = artwork,
                physicalMediaPath = physical,
                marqueeImagePath = marquee,
                screenshotPath = screenshot,
                fanartPath = fanart,
                titlescreenPath = titlescreen,
                miximagePath = miximage,
            )
        }
        return result
    }
}
