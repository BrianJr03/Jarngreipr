package jr.brian.frontend.library.scan

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the reconciler's three-way decision table. The scanner's
 * incremental behaviour hinges entirely on this function, so the
 * tests are intentionally dense around the edge cases
 * (unavailable source, null live stamp, unchanged / changed stamps,
 * media dir mtime drift, extension drift).
 */
class SystemStampTest {

    private val base = SystemStamp(
        sourceId = 1L,
        systemId = "nes",
        systemDirLastModified = 100L,
        entryCount = 42,
        resolvedExtensions = listOf("nes", "fds"),
        mediaDirMtimes = mapOf("covers" to 50L),
        gamelistMtime = 10L,
    )

    @Test
    fun `unavailable source always carries forward even when live stamp exists`() {
        assertEquals(
            ReconcileAction.CarryForward,
            reconcile(liveStamp = base, cachedStamp = base, sourceAvailable = false),
        )
    }

    @Test
    fun `null live stamp carries forward so a transient read failure cannot nuke the index`() {
        assertEquals(
            ReconcileAction.CarryForward,
            reconcile(liveStamp = null, cachedStamp = base, sourceAvailable = true),
        )
    }

    @Test
    fun `missing cached stamp triggers a fresh scan`() {
        assertEquals(
            ReconcileAction.Rescan,
            reconcile(liveStamp = base, cachedStamp = null, sourceAvailable = true),
        )
    }

    @Test
    fun `matching stamps reuse the cached games`() {
        assertEquals(
            ReconcileAction.Reuse,
            reconcile(liveStamp = base, cachedStamp = base, sourceAvailable = true),
        )
    }

    @Test
    fun `drift in media dir mtime forces a rescan`() {
        // Plan problem #6 — a scrape bumps covers/'s mtime; the stamp
        // must notice so the UI doesn't keep serving art-less rows.
        val stale = base.copy(mediaDirMtimes = mapOf("covers" to 999L))
        assertEquals(
            ReconcileAction.Rescan,
            reconcile(liveStamp = stale, cachedStamp = base, sourceAvailable = true),
        )
    }

    @Test
    fun `drift in resolved extensions forces a rescan`() {
        // Catalog ships a new extension — files that were previously
        // ignored might now count as ROMs. Must rescan.
        val widened = base.copy(resolvedExtensions = listOf("nes", "fds", "unif"))
        assertEquals(
            ReconcileAction.Rescan,
            reconcile(liveStamp = widened, cachedStamp = base, sourceAvailable = true),
        )
    }

    @Test
    fun `drift in gamelist mtime forces a rescan`() {
        val newer = base.copy(gamelistMtime = 999L)
        assertEquals(
            ReconcileAction.Rescan,
            reconcile(liveStamp = newer, cachedStamp = base, sourceAvailable = true),
        )
    }

    @Test
    fun `source id drift is treated as a different system entirely`() {
        // Re-adding a source gets a new id even if the path is the
        // same — the stamp must miss so games get reassigned to the
        // new source. Without this, orphaned rows survive a source
        // delete + re-add cycle.
        val reAdded = base.copy(sourceId = 2L)
        assertEquals(
            ReconcileAction.Rescan,
            reconcile(liveStamp = reAdded, cachedStamp = base, sourceAvailable = true),
        )
    }
}
