package jr.brian.home.esde.scraper.sidecar

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * File-system backed read/write for [UserMetadataOverride] blobs plus the
 * images users attach through the Edit Info sheet.
 *
 * JSON path: `<scrapedRoot>/<systemName>/.jarngreipr_user/<basename>.json`
 * Image path: `<scrapedRoot>/<systemName>/.jarngreipr_user/images/<basename>_<slot>.<ext>`
 *
 * Mirrors [GameMetadataSidecarStore]'s synchronization model — all I/O is
 * synchronous, tolerant of missing/corrupt files, and per-system reads are
 * cached in-memory and invalidated when the directory's `lastModified`
 * drifts. [write] and [delete] clear the cache so a freshly-written override
 * is visible to the next read.
 */
object UserMetadataOverrideStore {
    private const val TAG = "UserMetadataOverride"
    private const val OVERRIDE_DIR = ".jarngreipr_user"
    private const val IMAGE_SUBDIR = "images"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        prettyPrint = true
    }

    fun overrideFile(scrapedRoot: String, systemName: String, basename: String): File =
        File(overrideDir(scrapedRoot, systemName), "$basename.json")

    private fun overrideDir(scrapedRoot: String, systemName: String): File =
        File(File(scrapedRoot, systemName), OVERRIDE_DIR)

    private fun imageDir(scrapedRoot: String, systemName: String): File =
        File(overrideDir(scrapedRoot, systemName), IMAGE_SUBDIR)

    fun read(scrapedRoot: String, systemName: String, basename: String): UserMetadataOverride? {
        val file = overrideFile(scrapedRoot, systemName, basename)
        if (!file.exists() || !file.isFile) return null
        return runCatching { json.decodeFromString<UserMetadataOverride>(file.readText()) }
            .onFailure { Log.w(TAG, "Failed to parse override ${file.absolutePath}", it) }
            .getOrNull()
    }

    fun readAllForSystem(scrapedRoot: String, systemName: String): Map<String, UserMetadataOverride> {
        val dir = overrideDir(scrapedRoot, systemName)
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
        val entries = HashMap<String, UserMetadataOverride>(files.size)
        for (file in files) {
            val entry = runCatching { json.decodeFromString<UserMetadataOverride>(file.readText()) }
                .onFailure { Log.w(TAG, "Failed to parse override ${file.absolutePath}", it) }
                .getOrNull() ?: continue
            entries[file.nameWithoutExtension] = entry
        }
        systemCache[cacheKey] = SystemBucket(liveMtime, entries)
        return entries
    }

    /**
     * Writes [override] to disk. An empty override (no fields set) is treated
     * as a delete request: the on-disk JSON is removed so the metadata source
     * stops contributing anything for the game.
     */
    fun write(
        scrapedRoot: String,
        systemName: String,
        basename: String,
        override: UserMetadataOverride,
    ): Boolean {
        if (override.isEmpty()) return delete(scrapedRoot, systemName, basename)
        val file = overrideFile(scrapedRoot, systemName, basename)
        return runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(override))
            systemCache.remove(overrideDir(scrapedRoot, systemName).absolutePath)
            true
        }.onFailure { Log.w(TAG, "Failed to write override ${file.absolutePath}", it) }
            .getOrDefault(false)
    }

    /**
     * Deletes the override JSON and every image owned by this basename under
     * the system's image directory. Any image the user picked but didn't copy
     * (e.g. SAF URI referencing an external file) is left alone — the store
     * only owns files it placed in [imageDir].
     */
    fun delete(scrapedRoot: String, systemName: String, basename: String): Boolean {
        val file = overrideFile(scrapedRoot, systemName, basename)
        var ok = true
        if (file.exists()) ok = file.delete() && ok
        val images = imageDir(scrapedRoot, systemName)
        if (images.exists() && images.isDirectory) {
            images.listFiles { f -> f.isFile && f.nameWithoutExtension.startsWith("${basename}_") }
                ?.forEach { it.delete() }
        }
        systemCache.remove(overrideDir(scrapedRoot, systemName).absolutePath)
        return ok
    }

    /**
     * Copies the content pointed to by [sourceUri] into
     * `<scrapedRoot>/<systemName>/.jarngreipr_user/images/<basename>_<slot>.<ext>`
     * and returns the destination's absolute path, or null on failure. The
     * extension is derived from the source URI; a URI with no detectable
     * extension falls back to `jpg` so Coil still loads the file. Previous
     * images for the same `(basename, slot)` with any extension are deleted
     * so a replace doesn't leave an orphan behind.
     */
    fun copyImage(
        context: Context,
        scrapedRoot: String,
        systemName: String,
        basename: String,
        slot: String,
        sourceUri: Uri,
    ): String? {
        val ext = resolveExtension(context, sourceUri).ifBlank { "jpg" }
        val dir = imageDir(scrapedRoot, systemName)
        if (!dir.exists() && !dir.mkdirs()) return null
        dir.listFiles { f -> f.isFile && f.nameWithoutExtension == "${basename}_$slot" }
            ?.forEach { it.delete() }
        val dest = File(dir, "${basename}_$slot.$ext")
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            systemCache.remove(overrideDir(scrapedRoot, systemName).absolutePath)
            dest.absolutePath
        }.onFailure { Log.w(TAG, "Failed to copy image to ${dest.absolutePath}", it) }
            .getOrNull()
    }

    private fun resolveExtension(context: Context, uri: Uri): String {
        val mime = context.contentResolver.getType(uri)
        val fromMime = when (mime) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/bmp" -> "bmp"
            else -> null
        }
        if (fromMime != null) return fromMime
        val lastSegment = uri.lastPathSegment ?: return ""
        val ext = lastSegment.substringAfterLast('.', "")
        return if (ext.length in 2..5) ext.lowercase() else ""
    }

    private data class SystemBucket(
        val dirMtime: Long,
        val entries: Map<String, UserMetadataOverride>,
    )

    private val systemCache = ConcurrentHashMap<String, SystemBucket>()
}
