package jr.brian.frontend.look.themes

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipException
import java.util.zip.ZipFile

private const val EOCD_SIGNATURE = 0x06054b50
private const val CENTRAL_SIGNATURE = 0x02014b50
private const val EOCD_BYTES = 22
private const val MAX_COMMENT_BYTES = 0xFFFF
private const val CENTRAL_HEADER_BYTES = 46
private const val ZIP64_MARKER = 0xFFFFFFFFL
private const val ZIP64_COUNT_MARKER = 0xFFFF
private const val UNIX_HOST = 3
private const val FILE_TYPE_MASK = 0xF000
private const val SYMLINK_TYPE = 0xA000
private const val COPY_BUFFER = 64 * 1024

/** Caps that keep a hostile theme file from filling the disk or memory. */
data class ThemeArchiveLimits(
    val maxArchiveBytes: Long = 64L * 1024 * 1024,
    val maxTotalBytes: Long = 64L * 1024 * 1024,
    val maxEntryBytes: Long = 16L * 1024 * 1024,
    val maxEntries: Int = 2000,
    val maxRatio: Int = 100,
    /** Tiny entries can compress far past [maxRatio] honestly, so the ratio only counts above this. */
    val ratioFloorBytes: Long = 64L * 1024,
)

/** Why a theme file was refused. The jarnfe module maps each one to a message for the player. */
enum class ThemeImportProblem { Unreadable, NotATheme, TooLarge, TooManyEntries, UnsafeEntry, CompressionBomb, Damaged }

class ThemeImportException(val problem: ThemeImportProblem) : IOException(problem.name)

private fun refuse(problem: ThemeImportProblem): Nothing = throw ThemeImportException(problem)

/** One central directory record, read before anything is inflated. */
internal data class ThemeZipRecord(
    val name: String,
    val compressedSize: Long,
    val size: Long,
    val isSymlink: Boolean,
) {
    val isDirectory: Boolean get() = name.endsWith("/")
}

/**
 * Reads the central directory by hand, since java.util.zip hides the Unix mode that marks a
 * symlink. Zip64 is refused: nothing under the caps needs it.
 */
internal fun readCentralDirectory(file: File, limits: ThemeArchiveLimits): List<ThemeZipRecord> =
    RandomAccessFile(file, "r").use { raf ->
        val length = raf.length()
        if (length < EOCD_BYTES) refuse(ThemeImportProblem.NotATheme)
        val tailSize = minOf(length, (EOCD_BYTES + MAX_COMMENT_BYTES).toLong()).toInt()
        val tail = ByteArray(tailSize).also { raf.seek(length - tailSize); raf.readFully(it) }
        val buf = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
        val eocd = (tailSize - EOCD_BYTES downTo 0).firstOrNull { buf.getInt(it) == EOCD_SIGNATURE }
            ?: refuse(ThemeImportProblem.NotATheme)
        val count = buf.getShort(eocd + 10).toInt() and 0xFFFF
        val cdSize = buf.getInt(eocd + 12).toLong() and ZIP64_MARKER
        val cdOffset = buf.getInt(eocd + 16).toLong() and ZIP64_MARKER
        if (count == ZIP64_COUNT_MARKER || cdSize == ZIP64_MARKER || cdOffset == ZIP64_MARKER) {
            refuse(ThemeImportProblem.TooLarge)
        }
        if (count > limits.maxEntries) refuse(ThemeImportProblem.TooManyEntries)
        if (cdOffset + cdSize > length || cdSize > Int.MAX_VALUE) refuse(ThemeImportProblem.Damaged)
        val cd = ByteArray(cdSize.toInt()).also { raf.seek(cdOffset); raf.readFully(it) }
        parseRecords(ByteBuffer.wrap(cd).order(ByteOrder.LITTLE_ENDIAN), count)
    }

private fun parseRecords(cd: ByteBuffer, count: Int): List<ThemeZipRecord> {
    var at = 0
    return List(count) {
        if (at + CENTRAL_HEADER_BYTES > cd.limit() || cd.getInt(at) != CENTRAL_SIGNATURE) {
            refuse(ThemeImportProblem.Damaged)
        }
        val madeBy = (cd.getShort(at + 4).toInt() and 0xFFFF) shr 8
        val compressed = cd.getInt(at + 20).toLong() and ZIP64_MARKER
        val size = cd.getInt(at + 24).toLong() and ZIP64_MARKER
        val nameLength = cd.getShort(at + 28).toInt() and 0xFFFF
        val extraLength = cd.getShort(at + 30).toInt() and 0xFFFF
        val commentLength = cd.getShort(at + 32).toInt() and 0xFFFF
        val mode = (cd.getInt(at + 38) ushr 16) and FILE_TYPE_MASK
        if (compressed == ZIP64_MARKER || size == ZIP64_MARKER) refuse(ThemeImportProblem.TooLarge)
        val nameStart = at + CENTRAL_HEADER_BYTES
        if (nameStart + nameLength > cd.limit()) refuse(ThemeImportProblem.Damaged)
        val name = String(ByteArray(nameLength).also { cd.get(nameStart, it) }, Charsets.UTF_8)
        at = nameStart + nameLength + extraLength + commentLength
        ThemeZipRecord(name, compressed, size, isSymlink = madeBy == UNIX_HOST && mode == SYMLINK_TYPE)
    }
}

/**
 * A safe relative path: forward slashes, no empty, `.` or `..` segments, no drive, no control
 * characters. Anything else could land outside the theme folder, so it is refused.
 */
internal fun isSafeThemePath(path: String): Boolean {
    val trimmed = path.removeSuffix("/")
    if (trimmed.isEmpty() || trimmed.startsWith("/") || '\\' in trimmed || ':' in trimmed) return false
    if (trimmed.any { it.code < 0x20 || it.code == 0x7F }) return false
    return trimmed.split('/').none { it.isEmpty() || it == "." || it == ".." }
}

/**
 * Checks every record against the caps before inflating, then extracts the entries [keep] accepts
 * into [dest] (stripping [prefix]), counting real bytes so a lying header can't get past the caps.
 * Never creates links: every entry becomes a plain file.
 */
internal fun extractThemeArchive(
    archive: File,
    dest: File,
    limits: ThemeArchiveLimits,
    pickPrefix: (List<ThemeZipRecord>) -> String,
    keep: (String) -> Boolean,
) {
    if (archive.length() > limits.maxArchiveBytes) refuse(ThemeImportProblem.TooLarge)
    val records = readCentralDirectory(archive, limits)
    validateRecords(records, limits)
    val prefix = pickPrefix(records)
    val byName = records.associateBy { it.name }
    val root = dest.canonicalFile
    var total = 0L
    try {
        ZipFile(archive).use { zip ->
            for (entry in zip.entries()) {
                val record = byName[entry.name] ?: refuse(ThemeImportProblem.Damaged)
                if (record.isDirectory || !entry.name.startsWith(prefix)) continue
                val relative = entry.name.removePrefix(prefix)
                if (!keep(relative)) continue
                val target = File(root, relative).canonicalFile
                if (!target.path.startsWith(root.path + File.separator)) refuse(ThemeImportProblem.UnsafeEntry)
                target.parentFile?.mkdirs()
                val written = zip.getInputStream(entry).use { input ->
                    target.outputStream().use { output ->
                        copyCapped(input, output, limits.maxEntryBytes, limits.maxTotalBytes - total)
                    }
                }
                total += written
                val ratioCap = maxOf(record.compressedSize, 1L) * limits.maxRatio
                if (written > limits.ratioFloorBytes && written > ratioCap) refuse(ThemeImportProblem.CompressionBomb)
            }
        }
    } catch (e: ZipException) {
        refuse(ThemeImportProblem.Damaged)
    }
}

private fun validateRecords(records: List<ThemeZipRecord>, limits: ThemeArchiveLimits) {
    if (records.size > limits.maxEntries) refuse(ThemeImportProblem.TooManyEntries)
    if (records.any { it.isSymlink || !isSafeThemePath(it.name) }) refuse(ThemeImportProblem.UnsafeEntry)
    if (records.map { it.name.removeSuffix("/") }.toSet().size != records.size) refuse(ThemeImportProblem.UnsafeEntry)
    if (records.any { it.size > limits.maxEntryBytes }) refuse(ThemeImportProblem.TooLarge)
    if (records.sumOf { it.size } > limits.maxTotalBytes) refuse(ThemeImportProblem.TooLarge)
    records.filter { it.size > limits.ratioFloorBytes }.forEach {
        if (it.size > maxOf(it.compressedSize, 1L) * limits.maxRatio) refuse(ThemeImportProblem.CompressionBomb)
    }
}

private fun copyCapped(input: java.io.InputStream, output: java.io.OutputStream, entryCap: Long, totalLeft: Long): Long {
    val buffer = ByteArray(COPY_BUFFER)
    var written = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return written
        written += read
        if (written > entryCap || written > totalLeft) refuse(ThemeImportProblem.TooLarge)
        output.write(buffer, 0, read)
    }
}
