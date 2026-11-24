package jr.brian.frontend.library.media

import java.io.File

/**
 * Describes where media files for a given system id live under a given
 * media root. Each [MediaLayout] knows one disk convention; the index
 * can be fed multiple layouts and each is probed in order.
 *
 * Phase 2+ ships two:
 *  - [EsdeLayout] — ES-DE's `downloaded_media/<system>/<folder>/<basename>.<ext>`.
 *  - [JarngreiprLayout] — Jarngreipr's own scraper output under
 *    `Jarngreipr Media/<system>/<folder>/<basename>.<ext>` (same tree
 *    shape so the user can inspect both with any file manager).
 */
interface MediaLayout {
    /**
     * Returns the directory where files of [kind] live for a game under
     * [systemFolder] within [root]. Null when this layout doesn't know
     * about [kind] for this system (e.g. a future catalog-authored
     * layout might refuse to serve videos for text-only systems).
     */
    fun mediaDir(root: File, systemFolder: String, kind: MediaKind): File?
}

/**
 * ES-DE's `downloaded_media/<system>/<kind>/` convention. The user's
 * ES-DE install is the typical [root]; swapping systems is a per-system
 * folder rename, not a layout change.
 */
class EsdeLayout : MediaLayout {
    override fun mediaDir(root: File, systemFolder: String, kind: MediaKind): File =
        File(File(File(root, DOWNLOADED_MEDIA), systemFolder), kind.folderName)

    private companion object {
        const val DOWNLOADED_MEDIA = "downloaded_media"
    }
}

/**
 * Jarngreipr's own scraper output. The scraper writes to
 * `Jarngreipr Media/<system>/<kind>/<basename>.<ext>` — same tree shape
 * as ES-DE but under an app-owned root so the user can clear it
 * without touching ES-DE's own media. This layout is also probed by
 * the index at scan time, so a scrape run writes files AND the index
 * sees them on the next pass.
 */
class JarngreiprLayout : MediaLayout {
    override fun mediaDir(root: File, systemFolder: String, kind: MediaKind): File =
        File(File(root, systemFolder), kind.folderName)
}
