package jr.brian.frontend.library.scan

import android.util.Log
import jr.brian.frontend.library.systems.SystemCatalog
import jr.brian.frontend.library.systems.SystemDef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * [LibraryScanner] backed by direct `java.io.File` walks. Used for
 * sources that live under a path the app can read without SAF (primary
 * storage with all-files access).
 *
 * The walk follows ES-DE's convention: `<sourceRoot>/<systemFolder>/<rom>`.
 * `<systemFolder>` is resolved to a canonical system id via
 * [SystemCatalog.findByFolderAlias]; unknown folder names are logged
 * and skipped.
 *
 * Scan rules — hidden filter, extension filter, directories-as-files,
 * depth bound — are shared with [ScanRules] so a later SAF-backed
 * scanner applies the same filtering.
 */
class FastWalkScanner @Inject constructor(
    private val catalog: SystemCatalog,
) : LibraryScanner {

    override suspend fun scan(
        sourceRoot: String,
        systems: Set<String>?,
    ): List<ScannedRom> = withContext(Dispatchers.IO) {
        val root = File(sourceRoot)
        if (!root.exists() || !root.isDirectory) {
            Log.d(TAG, "Source root is not a readable directory: $sourceRoot")
            return@withContext emptyList()
        }

        val filter = systems?.map { it.lowercase() }?.toSet()
        val systemDirs = root.listFiles()
            ?.filter { it.isDirectory && ScanRules.shouldDescend(it.name) }
            .orEmpty()

        val out = mutableListOf<ScannedRom>()
        for (dir in systemDirs) {
            val def = catalog.findByFolderAlias(dir.name)
            if (def == null) {
                Log.d(TAG, "Skipping unknown system folder '${dir.name}'")
                continue
            }
            if (filter != null && def.id.lowercase() !in filter) continue
            scanSystem(root = dir, systemDir = dir, def = def, out = out)
        }
        out
    }

    private fun scanSystem(
        root: File,
        systemDir: File,
        def: SystemDef,
        out: MutableList<ScannedRom>,
    ) {
        val extensions = def.extensions
        if (extensions.isEmpty()) return
        walk(
            dir = systemDir,
            systemDir = systemDir,
            systemId = def.id,
            extensions = extensions,
            out = out,
            depthRemaining = ScanRules.WALK_MAX_DEPTH,
        )
    }

    private fun walk(
        dir: File,
        systemDir: File,
        systemId: String,
        extensions: Set<String>,
        out: MutableList<ScannedRom>,
        depthRemaining: Int,
    ) {
        if (depthRemaining < 0) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            val name = child.name
            if (!ScanRules.shouldDescend(name)) continue

            if (child.isDirectory) {
                if (ScanRules.matchesExtension(name, extensions)) {
                    // ES-DE's "directories interpreted as files" — a dir
                    // whose own name ends in a ROM extension is one game.
                    out += toScan(child, systemDir, systemId, isDirectoryAsFile = true)
                } else {
                    walk(child, systemDir, systemId, extensions, out, depthRemaining - 1)
                }
            } else if (child.isFile && ScanRules.matchesExtension(name, extensions)) {
                out += toScan(child, systemDir, systemId, isDirectoryAsFile = false)
            }
        }
    }

    private fun toScan(
        entry: File,
        systemDir: File,
        systemId: String,
        isDirectoryAsFile: Boolean,
    ): ScannedRom {
        val rel = entry.absolutePath
            .removePrefix(systemDir.absolutePath)
            .removePrefix(File.separator)
        return ScannedRom(
            systemId = systemId,
            relativePath = rel,
            absolutePath = entry.absolutePath,
            documentUri = null,
            isDirectoryAsFile = isDirectoryAsFile,
            fileSizeBytes = if (entry.isFile) entry.length() else -1L,
            lastModifiedEpochMillis = entry.lastModified(),
        )
    }

    private companion object {
        const val TAG = "FastWalkScanner"
    }
}
