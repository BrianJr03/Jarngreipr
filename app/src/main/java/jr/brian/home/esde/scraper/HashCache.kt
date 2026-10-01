package jr.brian.home.esde.scraper

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.CRC32
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches CRC32 hashes of ROM files keyed by absolutePath + size + mtime so
 * re-scrapes don't re-hash unchanged files. Large ROMs (multi-GB PS3 games)
 * would otherwise take seconds each to hash — this reduces re-hash cost to
 * effectively zero on repeat runs.
 */
@Singleton
class HashCache @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun crc32(file: File): String? {
        if (!file.isFile) return null
        val key = keyFor(file)
        prefs.getString(key, null)?.let { return it }
        val computed = computeCrc32(file) ?: return null
        prefs.edit { putString(key, computed) }
        return computed
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun keyFor(file: File): String =
        "crc32|${file.absolutePath}|${file.length()}|${file.lastModified()}"

    private fun computeCrc32(file: File): String? = try {
        val crc = CRC32()
        FileInputStream(file).use { stream ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                crc.update(buffer, 0, read)
            }
        }
        crc.value.toString(16).padStart(8, '0').uppercase()
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val PREFS_NAME = "scraper_hash_cache"
    }
}
