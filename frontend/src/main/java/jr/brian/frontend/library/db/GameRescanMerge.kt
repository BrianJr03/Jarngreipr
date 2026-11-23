package jr.brian.frontend.library.db

/**
 * Merges a fresh scan into an existing row.
 *
 * Scan-owned: [GameEntity.absolutePath] and [GameEntity.fileSizeBytes],
 * kept when the scanner can't report them (SAF has no absolute path).
 * Hashes follow the file, so they clear when its size changes.
 *
 * User-owned, never touched: favorite, hidden, play stats, and the
 * per-game emulator, launch command, and core.
 *
 * Imported metadata (name, description, rating, release date, developer,
 * publisher, genre, players) is kept unless [keepImportedMetadata] is
 * false, which is how "Read gamelist.xml" off returns to filename titles.
 */
internal fun GameEntity.refreshedFrom(
    scan: GameEntity,
    keepImportedMetadata: Boolean,
): GameEntity {
    val size = scan.fileSizeBytes ?: fileSizeBytes
    val sameFile = fileSizeBytes == null || size == fileSizeBytes
    val refreshed = copy(
        absolutePath = scan.absolutePath ?: absolutePath,
        fileSizeBytes = size,
        crc32 = crc32.takeIf { sameFile },
        md5 = md5.takeIf { sameFile },
    )
    if (keepImportedMetadata) return refreshed
    return refreshed.copy(
        displayName = scan.displayName,
        description = null,
        rating = null,
        releaseDate = null,
        developer = null,
        publisher = null,
        genre = null,
        players = null,
    )
}
