package jr.brian.frontend.library.scan

import jr.brian.frontend.library.db.SourceEntity
import jr.brian.frontend.library.systems.SystemDef
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks the right [LibraryScanner] for a given [SourceEntity]:
 *  - A tree URI (`content://...`) goes to [SafTreeScanner].
 *  - Anything else is treated as a file path for [FastWalkScanner].
 *
 * Kept small so Hilt can @Inject both scanners once and callers can
 * ask the dispatcher to run a scan without caring which backend
 * applies. Pending RomPathUtils-style volume detection, this is where
 * the "prefer fast walk when the volume is primary-storage" check
 * will land in Phase 4+.
 */
@Singleton
class ScannerDispatcher @Inject constructor(
    private val fastWalk: FastWalkScanner,
    private val saf: SafTreeScanner,
) {

    suspend fun scan(
        source: SourceEntity,
        systems: Set<String>? = null,
    ): List<ScannedRom> = pickScanner(source).scan(source.treeUri, systems)

    suspend fun scanWithReport(
        source: SourceEntity,
        systems: Set<String>? = null,
    ): ScanReport = pickScanner(source).scanWithReport(source.treeUri, systems)

    /** For a source that is one system's folder. Classic's [scan] never comes here. */
    suspend fun scanSystemFolder(source: SourceEntity, def: SystemDef): ScanReport =
        if (source.treeUri.startsWith("content://")) {
            saf.scanSystemFolder(source.treeUri, def)
        } else {
            fastWalk.scanSystemFolder(source.treeUri, def)
        }

    private fun pickScanner(source: SourceEntity): LibraryScanner =
        if (source.treeUri.startsWith("content://")) saf else fastWalk
}
