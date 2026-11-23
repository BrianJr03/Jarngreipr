package jr.brian.jarnfe.canvas

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import jr.brian.jarnfe.R
import jr.brian.jarnfe.launch.JarnGameRef
import jr.brian.jarnfe.launch.JarnLaunchFailure
import jr.brian.jarnfe.launch.JarnLaunchRequest
import jr.brian.jarnfe.launch.JarnLaunchResult
import jr.brian.jarnfe.launch.jarnGameLauncherOrNull
import jr.brian.jarnfe.launch.launchNoticeRes
import jr.brian.jarnfe.shell.JarnNotices

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun JarnCanvasRom.toGameRef() = JarnGameRef(
    systemName = systemName,
    path = path,
    title = title,
    emulatorPackage = emulatorPackage,
    launchCommand = launchCommand,
    artworkPath = artPath,
)

/** A folder grant never retries by itself: the user presses A again, as in the Library. */
@Composable
internal fun rememberCanvasRomLauncher(): (JarnCanvasRom) -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val launcher = remember(context) { context.jarnGameLauncherOrNull() }
    var pendingGrant by remember { mutableStateOf<JarnGameRef?>(null) }
    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val ref = pendingGrant
        pendingGrant = null
        if (ref != null && uri != null && activity != null && launcher != null) {
            val saved = launcher.grantFileAccess(activity, ref, uri.toString())
            JarnNotices.show(
                if (saved) R.string.jarnfe_launch_folder_saved else R.string.jarnfe_launch_folder_failed,
            )
        }
    }
    return { rom ->
        if (activity == null || launcher == null) {
            JarnNotices.show(R.string.jarnfe_launch_failed)
        } else {
            val ref = rom.toGameRef()
            val result = runCatching { launcher.launch(activity, JarnLaunchRequest(ref)) }
                .getOrElse { JarnLaunchResult.Failed(JarnLaunchFailure.Unknown) }
            launchNoticeRes(result)?.let(JarnNotices::show)
            if (result is JarnLaunchResult.NeedsFileAccess) {
                pendingGrant = ref
                try {
                    folderPicker.launch(result.pickerHint?.toUri())
                } catch (_: ActivityNotFoundException) {
                    pendingGrant = null
                    JarnNotices.show(R.string.jarnfe_launch_folder_unavailable)
                }
            }
        }
    }
}
