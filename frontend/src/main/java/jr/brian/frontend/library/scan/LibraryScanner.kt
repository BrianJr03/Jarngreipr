package jr.brian.frontend.library.scan

/**
 * Walks a single source and emits one [ScannedRom] per discovered ROM.
 *
 * The scanner does NOT resolve metadata (gamelist parse, scraper
 * sidecar) or media (cover/marquee paths). It emits a bare path shape;
 * the repository joins it with later passes to build the final
 * [jr.brian.frontend.library.db.GameEntity] row.
 *
 * Phase 2c ships two implementations: a direct `java.io.File`-based
 * [FastWalkScanner] for primary-storage sources, and a
 * `DocumentsContract`-projection scanner for SAF-only sources. The
 * repository picks the right one per source at scan time.
 */
interface LibraryScanner {

    /**
     * Scan every system under [sourceRoot] limited to [systems] when
     * non-null. Returns the full list of discovered ROMs in no
     * particular order — the caller groups + diffs them.
     *
     * @param sourceRoot opaque location understood by the implementation
     *   (a file path for [FastWalkScanner], a tree URI for the SAF
     *   scanner).
     * @param systems when non-null, only folders whose name matches an
     *   id or alias of a listed system are walked. Null means walk every
     *   system directory found under the root.
     */
    suspend fun scan(
        sourceRoot: String,
        systems: Set<String>? = null,
    ): List<ScannedRom>
}
