package jr.brian.home.esde.data

import android.content.Context
import android.util.Log
import jr.brian.home.esde.model.GameInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Persistent per-system ROM-index cache.
 *
 * A recursive walk of every ROMs root on every launch is unacceptable —
 * multi-second UI jank over SAF for a large SD-card library. This cache
 * persists the built [GameInfo] list per system under `filesDir/rom_index/`
 * and reads it back before the next scan, so the UI paints with the last
 * good state while the scan runs to reconcile.
 *
 * ## Validity
 *
 * A per-system stamp records the inputs that produced the entry. Any drift
 * between the stamp and the current on-disk state causes that one system to
 * rescan; other systems keep serving from cache.
 *
 * Directory `lastModified` propagation is not reliable across all filesystems
 * — a file created deep in a subdirectory does not always bump the timestamp
 * of the system's own directory. The [entryCount] and [gamelistLastModified]
 * fields catch most changes the mtime alone would miss, but the ultimate
 * escape hatch is the manual "Refresh library" action, which invalidates
 * everything via [invalidateAll].
 *
 * ## Invalidation
 *
 * Callers must invoke [invalidateAll] when any of the following changes:
 *  - the configured ROMs paths,
 *  - the "read gamelist" toggle,
 *  - any SAF tree per system (all systems are safest here — a picker change
 *    can shift the on-disk path we scan under).
 *
 * A single system's rescan is not enough for these — the ROMs-paths set
 * defines which systems exist at all.
 */
class RomIndexCache(context: Context) {
    private val cacheDir: File = File(context.filesDir, CACHE_DIR).apply { mkdirs() }
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Load the cache. Returns whatever entries decoded cleanly; systems whose
     * blob fails to parse are simply omitted so a partially corrupt cache does
     * not block a startup.
     */
    suspend fun loadAll(): Map<String, SystemCacheEntry> = withContext(Dispatchers.IO) {
        val files = cacheDir.listFiles()?.filter { it.isFile && it.extension == "json" }.orEmpty()
        buildMap {
            for (file in files) {
                val name = file.nameWithoutExtension
                val entry = decodeOrNull(file) ?: continue
                put(name, entry)
            }
        }
    }

    /**
     * Save [systems]. Systems present in the cache directory but absent from
     * [systems] are deleted, so a system whose ROMs were all removed does not
     * linger in the cache.
     */
    suspend fun saveAll(systems: Map<String, SystemCacheEntry>) = withContext(Dispatchers.IO) {
        val known = systems.keys
        cacheDir.listFiles()
            ?.filter { it.isFile && it.extension == "json" }
            ?.filter { it.nameWithoutExtension !in known }
            ?.forEach { it.delete() }
        for ((systemName, entry) in systems) {
            runCatching {
                File(cacheDir, "$systemName.json")
                    .writeText(json.encodeToString(SystemCacheEntry.serializer(), entry))
            }.onFailure { Log.w(TAG, "Failed to persist cache for $systemName", it) }
        }
    }

    /** Nukes the entire persisted cache. Used as the hard invalidate. */
    suspend fun invalidateAll() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    private fun decodeOrNull(file: File): SystemCacheEntry? = runCatching {
        json.decodeFromString(SystemCacheEntry.serializer(), file.readText())
    }.getOrElse {
        Log.w(TAG, "Failed to decode cache blob ${file.name}", it)
        null
    }

    companion object {
        private const val TAG = "RomIndexCache"
        private const val CACHE_DIR = "rom_index"
    }
}

/**
 * One system's cached entry: the games it contained, and enough about the
 * inputs to decide whether the cache is still valid on the next boot.
 */
@Serializable
data class SystemCacheEntry(
    val stamp: SystemStamp,
    val games: List<GameInfo>,
)

/**
 * The inputs that produced a system's cache entry. [isValid] compares against
 * a live stamp built from the current state — inequality on any field
 * invalidates this one system only.
 */
@Serializable
data class SystemStamp(
    val systemDirLastModified: Long,
    val entryCount: Int,
    /** -1 means gamelist.xml was not consulted (decoration off or file absent). */
    val gamelistLastModified: Long,
    val decorationEnabled: Boolean,
    /**
     * The specific ROMs root this cache scanned under. Included so a stamp built
     * against a different root (e.g. the primary vs a secondary) correctly
     * misses instead of matching a different tree with the same mtime by
     * coincidence.
     */
    val romsRootUsed: String,
) {
    fun matches(other: SystemStamp): Boolean = this == other
}
