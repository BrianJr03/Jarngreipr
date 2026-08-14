package jr.brian.home.esde.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The tree-document-id ↔ storage-path helpers are the single source of truth
 * used by [persistSafTreeForSystem] and [buildAetherDocUri]. Every launch
 * depends on the two directions round-tripping, so a regression here silently
 * breaks SD-card ROM launching without a stack trace.
 */
class VolumeIdRoundTripTest {

    @Test
    fun `primary document id resolves to internal storage path`() {
        assertEquals(
            "/storage/emulated/0/Roms/ps2",
            documentIdToStoragePath("primary:Roms/ps2"),
        )
    }

    @Test
    fun `sd document id resolves to volume-scoped storage path`() {
        assertEquals(
            "/storage/1A2B-3C4D/Roms/ps2",
            documentIdToStoragePath("1A2B-3C4D:Roms/ps2"),
        )
    }

    @Test
    fun `primary with empty relative resolves to storage root`() {
        assertEquals("/storage/emulated/0", documentIdToStoragePath("primary:"))
    }

    @Test
    fun `sd with empty relative resolves to volume root`() {
        assertEquals("/storage/1A2B-3C4D", documentIdToStoragePath("1A2B-3C4D:"))
    }

    @Test
    fun `internal storage path round-trips back to primary document id`() {
        assertEquals(
            "primary:Roms/ps2",
            storagePathToDocumentId("/storage/emulated/0/Roms/ps2"),
        )
    }

    @Test
    fun `sd storage path round-trips back to volume document id`() {
        assertEquals(
            "1A2B-3C4D:Roms/ps2",
            storagePathToDocumentId("/storage/1A2B-3C4D/Roms/ps2"),
        )
    }

    @Test
    fun `path with no storage prefix has no document id`() {
        assertNull(storagePathToDocumentId("/data/foo"))
    }

    @Test
    fun `sdCardVolumeId inverse round-trips`() {
        // sdCardVolumeId is the ES-DE-side of the same relationship: given a
        // real path, return the volume ID (or null when internal). Keeping
        // the two consistent is the whole point of extracting the shared
        // helper — this test locks them together.
        val absolutePath = "/storage/1A2B-3C4D/Roms/ps2/game.iso"
        val volumeId = sdCardVolumeId(absolutePath)
        assertEquals("1A2B-3C4D", volumeId)

        val docId = storagePathToDocumentId(absolutePath)
        assertEquals("1A2B-3C4D:Roms/ps2/game.iso", docId)
        assertEquals(absolutePath, documentIdToStoragePath(docId!!))
    }

    @Test
    fun `primary path has no sd volume id`() {
        assertNull(sdCardVolumeId("/storage/emulated/0/Roms/psx"))
    }

    @Test
    fun `splitTreeDocId returns null for id with no colon`() {
        assertNull(splitTreeDocId("primary"))
    }

    @Test
    fun `splitTreeDocId returns null for empty volume`() {
        assertNull(splitTreeDocId(":Roms"))
    }
}
