package jr.brian.frontend.look.themes

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val THEMES_DIR = "themes"
private const val COPY_BUFFER = 64 * 1024

/** The MIME types the theme file picker offers. `.jarntheme` has no type of its own, so zips and raw bytes. */
val JARN_THEME_FILE_TYPES = arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")

/**
 * Adds a theme file the player picked into `filesDir/themes`. The picked document is copied to the
 * cache first, under the archive cap, so the zip can be checked from its central directory.
 */
@Singleton
class JarnThemeImporter internal constructor(
    private val cacheDir: () -> File,
    private val open: (Uri) -> InputStream?,
    private val installer: JarnThemeInstaller,
    private val limits: ThemeArchiveLimits,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(
        cacheDir = context::getCacheDir,
        open = { context.contentResolver.openInputStream(it) },
        installer = JarnThemeInstaller(File(context.filesDir, THEMES_DIR)),
        limits = ThemeArchiveLimits(),
    )

    suspend fun import(uri: Uri, clash: ThemeIdClash = ThemeIdClash.KeepBoth): ThemeImportResult =
        withContext(Dispatchers.IO) {
            val copy = File(cacheDir(), "theme-import-${UUID.randomUUID()}.zip")
            try {
                val stream = runCatching { open(uri) }.getOrNull()
                    ?: return@withContext ThemeImportResult.Rejected(ThemeImportProblem.Unreadable)
                stream.use { input -> copy.outputStream().use { copyWithin(input, it, limits.maxArchiveBytes) } }
                installer.install(copy, clash)
            } catch (e: ThemeImportException) {
                ThemeImportResult.Rejected(e.problem)
            } catch (e: IOException) {
                ThemeImportResult.Rejected(ThemeImportProblem.Unreadable)
            } catch (e: SecurityException) {
                ThemeImportResult.Rejected(ThemeImportProblem.Unreadable)
            } finally {
                copy.delete()
            }
        }
}

private fun copyWithin(input: InputStream, output: java.io.OutputStream, cap: Long) {
    val buffer = ByteArray(COPY_BUFFER)
    var copied = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return
        copied += read
        if (copied > cap) throw ThemeImportException(ThemeImportProblem.TooLarge)
        output.write(buffer, 0, read)
    }
}
