package jr.brian.jarnfe.look.commandpanel

import android.graphics.Typeface
import android.util.AtomicFile
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Matches Command Panel's own import limit. */
private const val MAX_FONT_BYTES = 5L * 1024 * 1024
private const val FONT_SUFFIX = ".ttf"
private const val PARTIAL_SUFFIX = ".part"

internal fun interface FontFileCheck {
    fun isFont(file: File): Boolean
}

internal fun interface FontFamilyLoader {
    fun load(file: File): FontFamily
}

/** An unparseable file gives the default face on newer APIs rather than throwing. */
internal val TypefaceFontFileCheck = FontFileCheck { file ->
    runCatching { Typeface.createFromFile(file) }.getOrNull().let { it != null && it != Typeface.DEFAULT }
}

internal val FileFontFamilyLoader = FontFamilyLoader { file -> FontFamily(Font(file)) }

/**
 * Copies of Command Panel's imported fonts, in `noBackupFilesDir/cp_fonts/<sha256>.ttf` so they're never backed up or
 * exported. The file name is the index: a copy is used only while its bytes still hash to its name and parse as a font.
 * Families are kept in memory by digest. Disk work only, so call it off the main thread.
 */
internal class CommandPanelFontCache(
    noBackupDir: File,
    private val check: FontFileCheck = TypefaceFontFileCheck,
    private val loader: FontFamilyLoader = FileFontFamilyLoader,
) {
    private val dir = File(noBackupDir, DIR_NAME)
    private val families = ConcurrentHashMap<String, FontFamily>()

    /**
     * The family for [sha256]: from memory, then a verified copy on disk, then a fresh copy from [open] (Command Panel's
     * file). A bad copy is deleted. Null when nothing valid is available; [open] null means never fetch.
     */
    fun family(sha256: String, open: (() -> InputStream?)?): FontFamily? {
        families[sha256]?.let { return it }
        val file = fileFor(sha256)
        if (file.isFile) {
            if (runCatching { isValid(file, sha256) }.getOrDefault(false)) return remember(sha256, file)
            file.delete()
        }
        val fetched = open?.let { fetch(sha256, it) } ?: return null
        return remember(sha256, fetched)
    }

    /** Drops every copy and family whose digest isn't in [keep]. */
    fun retain(keep: Set<String>) {
        families.keys.retainAll(keep)
        dir.listFiles().orEmpty().forEach { file ->
            if (file.name.removeSuffix(FONT_SUFFIX) !in keep || !file.name.endsWith(FONT_SUFFIX)) file.delete()
        }
    }

    fun clear() = retain(emptySet())

    private fun fileFor(sha256: String) = File(dir, sha256 + FONT_SUFFIX)

    private fun remember(sha256: String, file: File): FontFamily? =
        runCatching { loader.load(file) }.getOrNull()?.also { families[sha256] = it }

    private fun isValid(file: File, sha256: String): Boolean =
        file.length() in 1..MAX_FONT_BYTES && sha256Of(file) == sha256 && check.isFont(file)

    private fun fetch(sha256: String, open: () -> InputStream?): File? {
        if (!dir.isDirectory && !dir.mkdirs()) return null
        val partial = File(dir, sha256 + PARTIAL_SUFFIX)
        val target = fileFor(sha256)
        val ok = runCatching {
            val input = open() ?: return@runCatching false
            input.use { copyLimited(it, partial) } && sha256Of(partial) == sha256 && check.isFont(partial) &&
                partial.renameTo(target)
        }.getOrDefault(false)
        partial.delete()
        return target.takeIf { ok }
    }

    private fun copyLimited(input: InputStream, dest: File): Boolean {
        dest.outputStream().use { out ->
            val buffer = ByteArray(BUFFER_BYTES)
            var total = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > MAX_FONT_BYTES) return false
                out.write(buffer, 0, read)
            }
            return total > 0
        }
    }

    private companion object {
        const val DIR_NAME = "cp_fonts"
        const val BUFFER_BYTES = 16 * 1024
    }
}

internal fun sha256Of(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(16 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

/** The last good fonts row, so the first frame after a launch already uses Command Panel's fonts. */
internal class CommandPanelFontsRowCache(noBackupDir: File) {
    private val file = AtomicFile(File(noBackupDir, FILE_NAME))

    fun read(): CommandPanelFonts? = runCatching { decodeCommandPanelFonts(file.readFully().decodeToString()) }.getOrNull()

    fun write(fonts: CommandPanelFonts) {
        runCatching {
            val stream = file.startWrite()
            try {
                stream.write(encodeCommandPanelFonts(fonts).encodeToByteArray())
                file.finishWrite(stream)
            } catch (e: Exception) {
                file.failWrite(stream)
                throw e
            }
        }
    }

    fun clear() {
        runCatching { file.delete() }
    }

    private companion object {
        const val FILE_NAME = "command_panel_fonts"
    }
}
