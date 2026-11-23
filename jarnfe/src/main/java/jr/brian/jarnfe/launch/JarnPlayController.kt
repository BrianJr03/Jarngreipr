package jr.brian.jarnfe.launch

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import jr.brian.frontend.library.art.JarnLibraryData
import jr.brian.frontend.library.art.LibraryGame
import jr.brian.jarnfe.R
import jr.brian.jarnfe.library.JarnLibraryGame
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.launch

/** The message for a launch that did not start a game. Null when nothing needs saying. */
@StringRes
internal fun launchNoticeRes(result: JarnLaunchResult): Int? = when (result) {
    is JarnLaunchResult.Launched -> null
    is JarnLaunchResult.NeedsEmulatorChoice -> when (result.kind) {
        JarnEmulatorChoiceKind.Emulator -> R.string.jarnfe_launch_needs_emulator
        JarnEmulatorChoiceKind.RetroArchCore -> R.string.jarnfe_launch_needs_core
    }
    is JarnLaunchResult.NeedsFileAccess -> R.string.jarnfe_launch_needs_folder
    JarnLaunchResult.MissingRom -> R.string.jarnfe_launch_missing_rom
    is JarnLaunchResult.Failed -> when (result.reason) {
        JarnLaunchFailure.EmulatorNotInstalled -> R.string.jarnfe_launch_emulator_missing
        JarnLaunchFailure.AppNotInstalled -> R.string.jarnfe_launch_app_missing
        JarnLaunchFailure.EmulatorRejected -> R.string.jarnfe_launch_emulator_rejected
        JarnLaunchFailure.RomNotReadable -> R.string.jarnfe_launch_rom_unreadable
        JarnLaunchFailure.Unknown -> R.string.jarnfe_launch_failed
    }
}

internal fun LibraryGame.toGameRef(artPath: String?) = JarnGameRef(
    systemName = systemId,
    path = absolutePath ?: relativePath,
    title = title,
    absolutePath = absolutePath,
    emulatorPackage = emulatorPackage,
    launchCommand = launchCommand,
    artworkPath = artPath,
)

/**
 * Turns a press of A into one launch. A folder grant never retries by itself: the user presses A
 * again, so nothing launches without a direct request.
 */
internal class JarnPlayController(
    private val activity: ComponentActivity,
    private val data: JarnLibraryData,
    private val launcher: JarnGameLauncher,
) {
    private var launching = false
    private var pendingGrant: JarnGameRef? = null

    private val folderPicker =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocumentTree(), ::onFolderPicked)

    fun play(game: JarnLibraryGame) {
        if (launching) return
        launching = true
        activity.lifecycleScope.launch {
            try {
                val ref = data.findGame(game.id)?.toGameRef(game.artPath)
                if (ref == null) {
                    JarnNotices.show(R.string.jarnfe_launch_missing_rom)
                } else {
                    handle(ref, launch(ref))
                }
            } finally {
                launching = false
            }
        }
    }

    private fun launch(ref: JarnGameRef): JarnLaunchResult =
        runCatching { launcher.launch(activity, JarnLaunchRequest(ref)) }
            .getOrElse { JarnLaunchResult.Failed(JarnLaunchFailure.Unknown) }

    private fun handle(ref: JarnGameRef, result: JarnLaunchResult) {
        launchNoticeRes(result)?.let(JarnNotices::show)
        if (result is JarnLaunchResult.NeedsFileAccess) requestFolder(ref, result.pickerHint)
    }

    private fun requestFolder(ref: JarnGameRef, hint: String?) {
        pendingGrant = ref
        try {
            folderPicker.launch(hint?.toUri())
        } catch (_: ActivityNotFoundException) {
            pendingGrant = null
            JarnNotices.show(R.string.jarnfe_launch_folder_unavailable)
        }
    }

    private fun onFolderPicked(uri: Uri?) {
        val ref = pendingGrant ?: return
        pendingGrant = null
        if (uri == null) return
        val saved = launcher.grantFileAccess(activity, ref, uri.toString())
        JarnNotices.show(
            if (saved) R.string.jarnfe_launch_folder_saved else R.string.jarnfe_launch_folder_failed,
        )
    }
}
