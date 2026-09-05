package jr.brian.home.esde.data

import jr.brian.home.esde.model.SystemFolderMapping
import java.io.File

/**
 * Availability signal for the ROMs sources during a single scan.
 *
 * A ROMs root that is configured but currently unreachable must be treated as
 * "unknown" — never as "empty". Threading this through the reconciliation step
 * keeps the "unreachable root" case explicitly distinct from "root is present
 * but empty" so a one-second SD-card mount delay after boot cannot destroy the
 * persisted index by pruning every system the missing root contains.
 *
 * See [computeRootAvailability] for how the sets are populated and
 * [cachedEntryIsFromUnavailableSource] for the pruning veto.
 */
data class RootAvailability(
    /** Configured `File`-based roots that are missing or not a directory. */
    val unavailableRoots: Set<String>,
    /**
     * SAF mapping tree URIs whose backing tree is not currently reachable —
     * detected via the on-disk [SystemFolderMapping.displayPath] because an
     * unmounted SD card produces a false `File.exists()` there.
     */
    val unavailableMappingUris: Set<String>,
) {
    val anyMissing: Boolean
        get() = unavailableRoots.isNotEmpty() || unavailableMappingUris.isNotEmpty()

    /**
     * True when the source that produced [entry] is currently unreachable, so
     * the entry must be carried forward from the cache instead of dropped.
     *
     * [SystemStamp.romsRootUsed] records either a File path (root scan) or a
     * SAF tree URI (mapping-fallback scan); both shapes are covered.
     */
    fun cachedEntryIsFromUnavailableSource(entry: SystemCacheEntry): Boolean {
        val source = entry.stamp.romsRootUsed
        return source in unavailableRoots || source in unavailableMappingUris
    }
}

/**
 * Snapshot which configured [romsPaths] and [mappings] actually resolve right
 * now. Any that don't are treated as "unavailable" and their cached systems
 * are protected from the save-set pruning [RomIndexCache.saveAll] would
 * otherwise do.
 *
 * The mapping availability check uses [SystemFolderMapping.displayPath] rather
 * than probing the SAF tree because the on-disk `File.exists()` is cheap and
 * consistent with how we detect a missing root; probing via `DocumentFile`
 * would work on some Android versions but blocks on others.
 */
fun computeRootAvailability(
    romsPaths: List<String>,
    mappings: List<SystemFolderMapping>,
): RootAvailability {
    val unavailableRoots = romsPaths
        .filter {
            val f = File(it)
            !(f.exists() && f.isDirectory)
        }
        .toSet()
    val unavailableMappingUris = mappings
        .filter {
            val dp = it.displayPath
            dp.isBlank() || !File(dp).exists()
        }
        .map { it.treeUri }
        .toSet()
    return RootAvailability(
        unavailableRoots = unavailableRoots,
        unavailableMappingUris = unavailableMappingUris,
    )
}
