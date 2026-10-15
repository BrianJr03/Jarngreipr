package jr.brian.frontend.library.scan

/**
 * Fingerprint of the inputs that produced one system's rows. The
 * reconciler decides "do I need to rescan this system?" by comparing
 * a freshly-computed stamp to the one persisted on the matching DAO
 * row. Any difference means "rescan"; identical stamps mean "the
 * cached rows are still correct".
 *
 * ### What's in the stamp
 *
 * - `systemDirLastModified` + `entryCount` — the cheapest check. Most
 *   additions/removals bump the directory's mtime; the count catches
 *   the ones that don't (some filesystems don't propagate mtime up
 *   from a nested write).
 * - `resolvedExtensions` — if the catalog ships a new extension for a
 *   system, every scan has to re-examine the folder because files
 *   that were previously ignored might now count as ROMs.
 * - `mediaDirMtimes` — one entry per media kind we track. Plan
 *   problem #6: a scrape writes `<media>/covers/Game.png` and the
 *   old stamp never changed, so `loadGames` kept serving art-less
 *   rows until the user hit "refresh" manually. Including the media
 *   dirs here invalidates the stamp automatically on a scrape.
 * - `gamelistMtime` — only relevant when gamelist decoration is on;
 *   -1L otherwise.
 * - `sourceId` — the source this stamp was computed under. Protects
 *   against a cache miss when the user re-adds a source with a
 *   different id but same path (which would otherwise produce
 *   collisions on `systemId` alone).
 */
data class SystemStamp(
    val sourceId: Long,
    val systemId: String,
    val systemDirLastModified: Long,
    val entryCount: Int,
    val resolvedExtensions: List<String>,
    val mediaDirMtimes: Map<String, Long> = emptyMap(),
    val gamelistMtime: Long = -1L,
)

/** Pure decision over a (live, cached) stamp pair. */
enum class ReconcileAction {
    /** Live stamp disagrees with cached — scan this system fresh. */
    Rescan,

    /** Live and cached stamps match — reuse the cached games. */
    Reuse,

    /**
     * Live stamp could not be computed (source unreachable). Carry
     * forward the cached rows rather than treating absence as deletion.
     * This is the plan's `RootAvailability` invariant.
     */
    CarryForward,
}

/**
 * Picks the right action for one system. Pure — tests exercise it
 * directly without needing a DB or filesystem. Returning the typed
 * [ReconcileAction] rather than a boolean makes the three cases
 * explicit; "stamp didn't match" and "stamp couldn't be computed" are
 * genuinely different.
 */
fun reconcile(
    liveStamp: SystemStamp?,
    cachedStamp: SystemStamp?,
    sourceAvailable: Boolean,
): ReconcileAction {
    if (!sourceAvailable) return ReconcileAction.CarryForward
    if (liveStamp == null) {
        // Source is theoretically available but we got nothing back —
        // treat the same as unavailable so a transient failure doesn't
        // nuke the index.
        return ReconcileAction.CarryForward
    }
    if (cachedStamp == null) return ReconcileAction.Rescan
    return if (liveStamp == cachedStamp) ReconcileAction.Reuse else ReconcileAction.Rescan
}
