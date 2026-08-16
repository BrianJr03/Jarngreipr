package jr.brian.home.esde.data

import jr.brian.home.esde.model.GameInfo
import jr.brian.home.esde.model.SystemFolderMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Pure-function coverage for the availability signal that the ROM index scan
 * uses to decide whether a cached entry may be pruned. The rest of the
 * two-phase load is integration-tested through [RomSearchViewModelDegradedScanTest];
 * these locks the two decisions the pruning veto pivots on.
 */
class RomIndexReconciliationTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `missing root path reports unavailable`() {
        val presentRoot = tempFolder.newFolder("Internal").absolutePath
        val missingRoot = File(tempFolder.root, "sdcard-not-mounted").absolutePath

        val availability = computeRootAvailability(
            romsPaths = listOf(presentRoot, missingRoot),
            mappings = emptyList(),
        )

        assertEquals(setOf(missingRoot), availability.unavailableRoots)
        assertTrue(availability.anyMissing)
    }

    @Test
    fun `file that exists but is not a directory reports unavailable`() {
        // Guards against a caller pointing romsPaths at a stray regular file —
        // it exists() true but isn't scannable, so it should count as missing.
        val fileMasqueradingAsRoot = tempFolder.newFile("looks-like-root").absolutePath

        val availability = computeRootAvailability(
            romsPaths = listOf(fileMasqueradingAsRoot),
            mappings = emptyList(),
        )

        assertEquals(setOf(fileMasqueradingAsRoot), availability.unavailableRoots)
    }

    @Test
    fun `mapping whose displayPath is missing reports its tree uri unavailable`() {
        val mapping = SystemFolderMapping(
            systemName = "psx",
            treeUri = "content://tree/1A2B-3C4D%3ARoms%2Fpsx",
            displayPath = "/storage/1A2B-3C4D/Roms/psx",
        )
        val availability = computeRootAvailability(
            romsPaths = emptyList(),
            mappings = listOf(mapping),
        )

        assertEquals(setOf(mapping.treeUri), availability.unavailableMappingUris)
        assertTrue(availability.anyMissing)
    }

    @Test
    fun `mapping whose displayPath exists is available`() {
        val dir = tempFolder.newFolder("MappedRoms").absolutePath
        val mapping = SystemFolderMapping(
            systemName = "psx",
            treeUri = "content://tree/primary%3AMappedRoms",
            displayPath = dir,
        )
        val availability = computeRootAvailability(
            romsPaths = emptyList(),
            mappings = listOf(mapping),
        )

        assertTrue(availability.unavailableMappingUris.isEmpty())
        assertFalse(availability.anyMissing)
    }

    @Test
    fun `cached entry sourced from an unreachable root is protected from pruning`() {
        val missingRoot = "/storage/1A2B-3C4D/Roms"
        val availability = RootAvailability(
            unavailableRoots = setOf(missingRoot),
            unavailableMappingUris = emptySet(),
        )
        val entry = cachedEntry(romsRootUsed = missingRoot)

        assertTrue(availability.cachedEntryIsFromUnavailableSource(entry))
    }

    @Test
    fun `cached entry sourced from an unreachable mapping uri is protected`() {
        // The mapping-fallback stamp in RomIndexBuilder writes the mapping's
        // treeUri into romsRootUsed. The veto has to match on that shape too,
        // otherwise mapped-system-on-unmounted-SAF loses its cached entry.
        val treeUri = "content://tree/1A2B-3C4D%3AMyGames"
        val availability = RootAvailability(
            unavailableRoots = emptySet(),
            unavailableMappingUris = setOf(treeUri),
        )
        val entry = cachedEntry(romsRootUsed = treeUri)

        assertTrue(availability.cachedEntryIsFromUnavailableSource(entry))
    }

    @Test
    fun `cached entry sourced from an available root is not protected`() {
        // The pruning veto must ONLY fire for unreachable sources — otherwise
        // a normal scan with every root present would stop pruning genuinely
        // removed systems, which is the escape hatch the manual refresh relies
        // on for freshness.
        val availableRoot = "/tmp/roms"
        val availability = RootAvailability(
            unavailableRoots = setOf("/tmp/some-other-root"),
            unavailableMappingUris = emptySet(),
        )
        val entry = cachedEntry(romsRootUsed = availableRoot)

        assertFalse(availability.cachedEntryIsFromUnavailableSource(entry))
    }

    private fun cachedEntry(romsRootUsed: String): SystemCacheEntry = SystemCacheEntry(
        stamp = SystemStamp(
            systemDirLastModified = 1L,
            entryCount = 1,
            gamelistLastModified = -1L,
            decorationEnabled = false,
            romsRootUsed = romsRootUsed,
        ),
        games = listOf(GameInfo(path = "./One.iso", name = "One", systemName = "psx")),
    )
}
