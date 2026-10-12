package jr.brian.home.esde.scraper.sidecar

import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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

    /** Returns the parsed sidecar, or null when the file is missing/corrupt. */
    fun read(scrapedRoot: String, systemName: String, basename: String): GameMetadataSidecar? {
        val file = sidecarFile(scrapedRoot, systemName, basename)
        if (!file.exists() || !file.isFile) return null
        return runCatching { json.decodeFromString<GameMetadataSidecar>(file.readText()) }
            .onFailure { Log.w(TAG, "Failed to parse sidecar ${file.absolutePath}", it) }
            .getOrNull()
    }

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
            true
        }.onFailure { Log.w(TAG, "Failed to write sidecar ${file.absolutePath}", it) }
            .getOrDefault(false)
    }
}
