package jr.brian.jarnfe.library

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import jr.brian.frontend.library.setup.EsdeRomsFolder
import jr.brian.frontend.library.setup.EsdeRomsFolderSource
import jr.brian.frontend.library.setup.storageDocumentId
import jr.brian.jarnfe.input.JarnScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"

/** Implemented by the host Application so :jarnfe never depends on :app. */
interface JarnEsdeRomsProvider {
    val jarnEsdeRomsFolder: EsdeRomsFolderSource
}

internal fun Context.jarnEsdeRomsFolderOrNull(): EsdeRomsFolderSource? =
    (applicationContext as? JarnEsdeRomsProvider)?.jarnEsdeRomsFolder

/** The folder picker's starting point, so it opens inside [EsdeRomsFolder.path]. */
internal fun EsdeRomsFolder.pickerHint(): Uri? =
    storageDocumentId(path)?.let { DocumentsContract.buildDocumentUri(EXTERNAL_STORAGE_AUTHORITY, it) }

/**
 * "Use your ES-DE ROMs folder": a notice first, then file access if it's missing, then the
 * folder picker opened at ES-DE's ROMs folder. The picked folder goes through the usual
 * library folder flow. Nothing reads ES-DE's settings before the player says Continue.
 */
internal object JarnEsdeRoms {
    /** The screen showing the notice, or null when it's closed. */
    var noticeOn by mutableStateOf<JarnScreen?>(null)
        private set

    private var awaitingAccess = false

    fun ask(screen: JarnScreen) {
        noticeOn = screen
    }

    fun cancel() {
        noticeOn = null
    }

    fun proceed(activity: Activity?) {
        noticeOn = null
        val host = activity ?: return
        if (Environment.isExternalStorageManager() || !requestFileAccess(host)) {
            openPicker(host)
        } else {
            awaitingAccess = true
        }
    }

    /** Back from the file access screen. Opens the picker whether or not access was granted. */
    fun onResumed(activity: Activity) {
        if (!awaitingAccess) return
        awaitingAccess = false
        openPicker(activity)
    }

    private fun requestFileAccess(host: Activity): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            "package:${host.packageName}".toUri(),
        )
        return runCatching { host.startActivity(intent) }.isSuccess
    }

    private fun openPicker(host: Activity) {
        val source = host.jarnEsdeRomsFolderOrNull()
        val scope = (host as? ComponentActivity)?.lifecycleScope
        if (source == null || scope == null) {
            JarnFolderPicker.open?.invoke(null)
            return
        }
        scope.launch {
            val folder = withContext(Dispatchers.IO) { runCatching { source.find() }.getOrNull() }
            JarnFolderPicker.open?.invoke(folder?.pickerHint())
        }
    }
}
