package jr.brian.home.jarn

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import jr.brian.jarnfe.themes.JarnThemeShareSheet
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Matches the `theme_exports` cache path in `res/xml/file_paths.xml`. */
private const val EXPORT_DIR = "theme-exports"
private const val THEME_FILE_TYPE = "application/zip"

/** Shares Jarn theme files through the app's FileProvider and the system share sheet. */
@Singleton
class AppJarnThemeShareSheet @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : JarnThemeShareSheet {

    override val exportDir: File get() = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }

    override fun send(file: File, title: String): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = THEME_FILE_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            clipData = ClipData.newRawUri(title, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(chooser)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
