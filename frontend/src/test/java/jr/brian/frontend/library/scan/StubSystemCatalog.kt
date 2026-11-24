package jr.brian.frontend.library.scan

import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.library.systems.SystemDef

/**
 * Test-only [SystemCatalog] with a fixed set of entries. Shared by
 * [FastWalkScannerTest] and [SafTreeScannerTest] so a scanner scenario
 * can be checked against both implementations with the same catalog.
 */
internal class StubSystemCatalog(private vararg val systems: SystemDef) : SystemCatalog {
    override fun all(): List<SystemDef> = systems.toList()
    override fun findById(id: String): SystemDef? =
        systems.firstOrNull { it.id.equals(id, ignoreCase = true) }
    override fun findByFolderAlias(folderName: String): SystemDef? =
        systems.firstOrNull { def ->
            def.id.equals(folderName, ignoreCase = true) ||
                def.folderAliases.any { it.equals(folderName, ignoreCase = true) }
        }
    override fun extensionsFor(id: String): Set<String> = findById(id)?.extensions.orEmpty()
}
