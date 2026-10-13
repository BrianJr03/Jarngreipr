package jr.brian.home.esde.scraper.sidecar

import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * File-system backed read/write for [GameMetadataSidecar] blobs.
 *
 * Path shape: `<scrapedRoot>/<systemName>/.jarngreipr/<basename>.json`.
 * The `.jarngreipr` directory sits alongside the ES-DE media folders
 * (`covers`, `screenshots`, …) inside the same per-system tree, so wiping
 * `scrapedRoot()` clears both scraped art and sidecars in one shot.
 *
 * All I/O is synchronous and tolerant of missing files, bad JSON, and
 * unwritable paths — a corrupt or absent sidecar must never prevent a game
 * from appearing in the library. Caller is responsible for threading.
 */
object GameMetadataSidecarStore {
    private const val TAG = "GameMetadataSidecar"
    private const val SIDECAR_DIR = ".jarngreipr"

    /**
     * Lenient so an older sidecar written by a previous schema still decodes
     * when we add fields. Pretty-prints writes so the file is diffable if the
     * user inspects it in a file manager.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    fun sidecarFile(scrapedRoot: String, systemName: String, basename: String): File =
        File(File(File(scrapedRoot, systemName), SIDECAR_DIR), "$basename.json")

    private fun sidecarDir(scrapedRoot: String, systemName: String): File =
        File(File(scrapedRoot, systemName), SIDECAR_DIR)

    /** Returns the parsed sidecar, or null when the file is missing/corrupt. */
    fun read(scrapedRoot: String, systemName: String, basename: String): GameMetadataSidecar? {
        val file = sidecarFile(scrapedRoot, systemName, basename)
        if (!file.exists() || !file.isFile) return null
        return runCatching { json.decodeFromString<GameMetadataSidecar>(file.readText()) }
            .onFailure { Log.w(TAG, "Failed to parse sidecar ${file.absolutePath}", it) }
            .getOrNull()
    }

    /**
     * Bulk read every sidecar for a system in one directory listing, keyed by
     * ROM basename. Avoids the per-game `exists → readText → decode` round trip
     * [SidecarMetadataSource] used to do, which dominated frontend startup for
     * platforms with hundreds of scraped games.
     *
     * The result is cached in-memory per `(scrapedRoot, systemName)` and
     * invalidated when the directory's `lastModified` drifts — every scraper
     * write bumps the parent directory's mtime, so a real change can't be
     * missed. The cache is also cleared explicitly from [write] below so a
     * freshly-written sidecar is visible to the very next read.
     */
    fun readAllForSystem(scrapedRoot: String, systemName: String): Map<String, GameMetadataSidecar> {
        val dir = sidecarDir(scrapedRoot, systemName)
        val cacheKey = dir.absolutePath
        if (!dir.exists() || !dir.isDirectory) {
            systemCache.remove(cacheKey)
            return emptyMap()
        }
        val liveMtime = dir.lastModified()
        systemCache[cacheKey]?.takeIf { it.dirMtime == liveMtime }?.let { return it.entries }

        val files = dir.listFiles { f -> f.isFile && f.extension == "json" } ?: emptyArray()
        if (files.isEmpty()) {
            systemCache[cacheKey] = SystemBucket(liveMtime, emptyMap())
            return emptyMap()
        }
        val entries = HashMap<String, GameMetadataSidecar>(files.size)
        for (file in files) {
            val sidecar = runCatching { json.decodeFromString<GameMetadataSidecar>(file.readText()) }
                .onFailure { Log.w(TAG, "Failed to parse sidecar ${file.absolutePath}", it) }
                .getOrNull() ?: continue
            entries[file.nameWithoutExtension] = sidecar
        }
        systemCache[cacheKey] = SystemBucket(liveMtime, entries)
        return entries
    }

    private data class SystemBucket(
        val dirMtime: Long,
        val entries: Map<String, GameMetadataSidecar>,
    )

    private val systemCache = ConcurrentHashMap<String, SystemBucket>()

    /**
     * Overwrites the sidecar at the resolved path. Creates parent directories
     * as needed. Returns true on success. Intended for future scraper callers
     * (ScreenScraper, etc.) — nothing in-tree calls this yet.
     */
    fun write(
        scrapedRoot: String,
        systemName: String,
        basename: String,
        sidecar: GameMetadataSidecar
    ): Boolean {
        val file = sidecarFile(scrapedRoot, systemName, basename)
        return runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(sidecar))
            systemCache.remove(sidecarDir(scrapedRoot, systemName).absolutePath)
            true
        }.onFailure { Log.w(TAG, "Failed to write sidecar ${file.absolutePath}", it) }
            .getOrDefault(false)
    }
}
