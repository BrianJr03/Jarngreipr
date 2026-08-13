package jr.brian.home.esde.util

import android.util.Log
import android.util.Xml
import jr.brian.home.esde.util.EsdeCommandLauncher.systemExtensionsFallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File

/**
 * One ROM discovered on disk.
 *
 * @param systemName the ES-DE system directory this ROM sits under
 * @param absolutePath the on-disk path we found it at
 * @param relativePath the path relative to the system directory — e.g. `Disc1/Game.iso`,
 *   or `Xenogears.m3u/Xenogears.m3u` for the inner-file variant of a directory-as-file.
 *   This is the join key the metadata layer matches gamelist paths against.
 * @param isDirectoryAsFile true when we treated a directory whose own name ends in a
 *   ROM extension as a single game entry (ES-DE's multi-disc convention).
 */
data class ScannedRom(
    val systemName: String,
    val absolutePath: String,
    val relativePath: String,
    val isDirectoryAsFile: Boolean,
)

/**
 * Walks configured ROMs roots and reports the real ROM files. The filesystem is
 * the source of truth — anything not returned here does not exist as far as the
 * rest of the app is concerned.
 */
object RomScanner {
    private const val TAG = "RomScanner"

    /** Filenames and directories we always skip because they are never a ROM. */
    private val SKIPPED_NAMES = setOf("media", ".DS_Store", "systeminfo.txt")

    /** Extensions the scanner never treats as a ROM even if a system claims them. */
    private val ALWAYS_SKIPPED_EXTENSIONS = setOf(
        "txt", "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg",
        "sav", "srm", "state", "auto", "sram", "rtc", "dsv",
        "xml", "cfg", "ini", "log", "bak",
    )

    /** Bounds the recursive walk. Real ROM libraries rarely nest deeper than this. */
    private const val WALK_MAX_DEPTH = 6

    /**
     * Walk every configured ROMs root × every system directory beneath it.
     *
     * @param romsPaths ordered list of ROMs roots (primary first). Duplicates dropped.
     * @param esSystemsFile parsed for `<extension>` per system when present. When
     *   null or the file is missing, the extension set for each system is built
     *   transitively from [EmulatorRegistry] × [EsdeCommandLauncher] rules.
     * @return games grouped by system name. Systems with no ROMs are omitted from
     *   the map so a caller iterating it does not see empty entries.
     */
    suspend fun scan(
        romsPaths: List<String>,
        esSystemsFile: File? = null,
    ): Map<String, List<ScannedRom>> = withContext(Dispatchers.IO) {
        val roots = romsPaths.distinct().map(::File).filter { it.exists() && it.isDirectory }
        if (roots.isEmpty()) {
            Log.d(TAG, "No usable ROMs roots; scan is empty")
            return@withContext emptyMap()
        }
        val perSystemExtensions = esSystemsFile
            ?.takeIf { it.exists() }
            ?.let(::parseSystemExtensions)
            .orEmpty()

        // Collate system directories from every root. A system that happens to
        // exist under two roots (unusual, but valid) is scanned under each and
        // its results merged; duplicates by relativePath are resolved
        // first-root-wins to keep behaviour predictable when the user reorders
        // roots.
        val systemsPerRoot: List<Pair<File, File>> = roots.flatMap { root ->
            root.listFiles()
                ?.filter { it.isDirectory && it.name !in SKIPPED_NAMES }
                .orEmpty()
                .map { root to it }
        }

        coroutineScope {
            systemsPerRoot.map { (root, systemDir) ->
                async {
                    val systemName = systemDir.name
                    val extensions = extensionsFor(systemName, perSystemExtensions)
                    scanSystem(root, systemDir, systemName, extensions)
                }
            }.awaitAll()
        }
            .flatten()
            .groupBy { it.systemName }
            .mapValues { (_, roms) ->
                // First-root-wins dedup by relativePath.
                val seen = HashSet<String>(roms.size)
                roms.filter { seen.add(it.relativePath) }
            }
            .filterValues { it.isNotEmpty() }
    }

    private fun scanSystem(
        root: File,
        systemDir: File,
        systemName: String,
        extensions: Set<String>,
    ): List<ScannedRom> {
        val out = mutableListOf<ScannedRom>()
        walk(systemDir, systemDir, systemName, extensions, out, depthRemaining = WALK_MAX_DEPTH)
        return out
    }

    private fun walk(
        dir: File,
        systemDir: File,
        systemName: String,
        extensions: Set<String>,
        out: MutableList<ScannedRom>,
        depthRemaining: Int,
    ) {
        if (depthRemaining < 0) return
        val children = dir.listFiles() ?: return
        for (child in children) {
            val name = child.name
            if (name.startsWith(".") || name in SKIPPED_NAMES) continue

            if (child.isDirectory) {
                // ES-DE's "directories interpreted as files" convention: a directory
                // whose OWN name ends in a ROM extension is one game entry, not a
                // folder to descend into. Xenogears.m3u/, Foo.gdi/, etc.
                if (name.hasRomExtension(extensions)) {
                    out += scanned(child, systemDir, systemName, isDirectoryAsFile = true)
                } else {
                    walk(child, systemDir, systemName, extensions, out, depthRemaining - 1)
                }
            } else if (child.isFile) {
                if (name.hasRomExtension(extensions)) {
                    out += scanned(child, systemDir, systemName, isDirectoryAsFile = false)
                }
            }
        }
    }

    private fun scanned(
        entry: File,
        systemDir: File,
        systemName: String,
        isDirectoryAsFile: Boolean,
    ): ScannedRom {
        val rel = entry.absolutePath
            .removePrefix(systemDir.absolutePath)
            .removePrefix(File.separator)
        return ScannedRom(
            systemName = systemName,
            absolutePath = entry.absolutePath,
            relativePath = rel,
            isDirectoryAsFile = isDirectoryAsFile,
        )
    }

    private fun String.hasRomExtension(extensions: Set<String>): Boolean {
        val ext = substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return false
        if (ext in ALWAYS_SKIPPED_EXTENSIONS) return false
        return ext in extensions
    }

    /**
     * Extension set for [systemName]. Prefers the `<extension>` value parsed from
     * es_systems.xml; falls back to the union of extensions across every emulator
     * the registry lists for this system, built transitively.
     */
    private fun extensionsFor(systemName: String, perSystem: Map<String, Set<String>>): Set<String> {
        perSystem[systemName]?.takeIf { it.isNotEmpty() }?.let { return it }
        return systemExtensionsFallback(systemName)
    }

    /**
     * Parses per-system `<extension>` from es_systems.xml. ES-DE stores extensions
     * as a single dot-prefixed space-separated string (e.g. `.iso .bin .chd`) — we
     * strip the leading dot and lowercase for matching.
     */
    private fun parseSystemExtensions(esSystemsFile: File): Map<String, Set<String>> {
        val result = mutableMapOf<String, Set<String>>()
        try {
            val parser = Xml.newPullParser()
            esSystemsFile.inputStream().use { input ->
                parser.setInput(input, "UTF-8")
                var currentName: String? = null
                var currentExtensions: Set<String>? = null
                var inSystem = false
                val buf = StringBuilder()
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            buf.clear()
                            if (parser.name == "system") {
                                inSystem = true
                                currentName = null
                                currentExtensions = null
                            }
                        }
                        XmlPullParser.TEXT -> if (inSystem) buf.append(parser.text)
                        XmlPullParser.END_TAG -> {
                            if (inSystem) {
                                val text = buf.toString().trim()
                                when (parser.name) {
                                    "name" -> if (currentName == null) currentName = text
                                    "extension" -> if (currentExtensions == null && text.isNotEmpty()) {
                                        currentExtensions = text
                                            .split(Regex("\\s+"))
                                            .mapNotNull { token ->
                                                token.trim().removePrefix(".").lowercase()
                                                    .takeIf { it.isNotEmpty() }
                                            }
                                            .toSet()
                                    }
                                    "system" -> {
                                        val n = currentName
                                        val e = currentExtensions
                                        if (n != null && e != null) result[n] = e
                                        inSystem = false
                                    }
                                }
                            }
                            buf.clear()
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing es_systems.xml extensions", e)
        }
        return result
    }
}
