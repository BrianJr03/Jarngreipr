package jr.brian.frontend.library.scan

/**
 * One ROM discovered during a scan. The scanner's output shape — repo
 * then persists it as a [jr.brian.frontend.library.db.GameEntity] after
 * joining with any decoded metadata.
 *
 * @property systemId canonical system id resolved from the folder name
 *   via [jr.brian.frontend.library.systems.SystemCatalog.findByFolderAlias].
 *   Rows for folders that don't resolve to a known system are not
 *   emitted — unknown-system support is Phase 4+.
 * @property relativePath path relative to the system directory
 *   (`Mario.z64` or `Discs/Xenogears.m3u/Disc1.bin`). This is the join
 *   key the metadata layer matches gamelist paths against.
 * @property absolutePath file-system path when the source is file-backed,
 *   null when it was discovered via SAF.
 * @property documentUri `content://` document URI when the source is
 *   SAF-backed, null when file-backed. Exactly one of
 *   [absolutePath] / [documentUri] is non-null.
 * @property isDirectoryAsFile true when we treated a directory whose own
 *   name ends in a ROM extension as a single game entry (ES-DE's
 *   multi-disc convention — `Xenogears.m3u/` is one game, not a folder
 *   to descend into).
 * @property fileSizeBytes bytes on disk; -1 when unknown (SAF providers
 *   that don't fill COLUMN_SIZE). Used later by the scraper's hash-first
 *   matching.
 * @property lastModifiedEpochMillis file mtime; 0 when unknown.
 */
data class ScannedRom(
    val systemId: String,
    val relativePath: String,
    val absolutePath: String? = null,
    val documentUri: String? = null,
    val isDirectoryAsFile: Boolean = false,
    val fileSizeBytes: Long = -1L,
    val lastModifiedEpochMillis: Long = 0L,
)
