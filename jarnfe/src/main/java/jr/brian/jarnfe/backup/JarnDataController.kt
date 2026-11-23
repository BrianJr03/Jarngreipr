package jr.brian.jarnfe.backup

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import jr.brian.jarnfe.R
import jr.brian.jarnfe.shell.JarnNotices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

internal sealed interface JarnDataSheet {
    data class Working(@param:StringRes val messageRes: Int) : JarnDataSheet
    data class Message(@param:StringRes val titleRes: Int, @param:StringRes val messageRes: Int) : JarnDataSheet
    class RestorePreview(val candidate: JarnBackupCandidate) : JarnDataSheet
    data class UpdateOffered(val offer: JarnUpdateOffer) : JarnDataSheet
    data class UpdateNeedsPermission(val offer: JarnUpdateOffer, val variant: JarnUpdateVariant) : JarnDataSheet
    data class UpdateDownloading(
        val offer: JarnUpdateOffer,
        val variant: JarnUpdateVariant,
        val progress: JarnUpdateDownload.Progress?,
    ) : JarnDataSheet
    class UpdateReady(val file: JarnDownloadedUpdate) : JarnDataSheet
    data class UpdateFailed(val offer: JarnUpdateOffer, val variant: JarnUpdateVariant) : JarnDataSheet
}

/**
 * Runs outside composition, so a restore or download keeps going when the controls move between
 * screens. Only one sheet is ever open, and a running job can't be dismissed.
 */
internal class JarnDataController(private val scope: CoroutineScope) {
    var sheet by mutableStateOf<JarnDataSheet?>(null)
        private set

    var returnFocus: FocusRequester? = null
        private set

    val isBusy: Boolean
        get() = sheet is JarnDataSheet.Working || sheet is JarnDataSheet.UpdateDownloading

    fun dismiss() {
        if (!isBusy) sheet = null
    }

    fun export(backup: JarnBackup, target: Uri, returnTo: FocusRequester?) {
        if (!begin(returnTo, R.string.jarnfe_backup_saving)) return
        scope.launch {
            if (backup.export(target)) {
                sheet = null
                JarnNotices.show(R.string.jarnfe_backup_saved)
            } else {
                sheet = JarnDataSheet.Message(R.string.jarnfe_backup_failed_title, R.string.jarnfe_backup_failed_message)
            }
        }
    }

    fun inspect(backup: JarnBackup, source: Uri, returnTo: FocusRequester?) {
        if (!begin(returnTo, R.string.jarnfe_restore_checking)) return
        scope.launch {
            sheet = when (val result = backup.inspect(source)) {
                is JarnBackupInspection.Ready -> JarnDataSheet.RestorePreview(result.candidate)
                is JarnBackupInspection.Failed -> JarnDataSheet.Message(
                    R.string.jarnfe_restore_rejected_title,
                    result.problem.messageRes(),
                )
            }
        }
    }

    fun confirmRestore(backup: JarnBackup) {
        val pending = sheet as? JarnDataSheet.RestorePreview ?: return
        sheet = JarnDataSheet.Working(R.string.jarnfe_restore_applying)
        scope.launch {
            if (backup.restore(pending.candidate)) {
                sheet = null
                JarnNotices.show(R.string.jarnfe_restore_done)
            } else {
                sheet = JarnDataSheet.Message(R.string.jarnfe_restore_failed_title, R.string.jarnfe_restore_failed_message)
            }
        }
    }

    fun checkForUpdate(updates: JarnUpdates, returnTo: FocusRequester?) {
        if (!begin(returnTo, R.string.jarnfe_update_checking)) return
        scope.launch {
            sheet = when (val result = updates.check()) {
                is JarnUpdateCheck.Available -> JarnDataSheet.UpdateOffered(result.offer)
                JarnUpdateCheck.UpToDate -> JarnDataSheet.Message(
                    R.string.jarnfe_update_current_title,
                    R.string.jarnfe_update_current_message,
                )
                JarnUpdateCheck.Failed -> JarnDataSheet.Message(
                    R.string.jarnfe_update_check_failed_title,
                    R.string.jarnfe_update_check_failed_message,
                )
            }
        }
    }

    fun skip(updates: JarnUpdates, offer: JarnUpdateOffer) {
        updates.skip(offer.version)
        sheet = null
    }

    fun download(updates: JarnUpdates, offer: JarnUpdateOffer, variant: JarnUpdateVariant) {
        if (isBusy) return
        if (!updates.canInstall()) {
            sheet = JarnDataSheet.UpdateNeedsPermission(offer, variant)
            return
        }
        sheet = JarnDataSheet.UpdateDownloading(offer, variant, null)
        scope.launch {
            updates.download(variant).collect { state ->
                sheet = when (state) {
                    is JarnUpdateDownload.Progress -> JarnDataSheet.UpdateDownloading(offer, variant, state)
                    is JarnUpdateDownload.Ready -> {
                        updates.markDownloaded(offer.version)
                        JarnDataSheet.UpdateReady(state.file)
                    }
                    JarnUpdateDownload.Failed -> JarnDataSheet.UpdateFailed(offer, variant)
                }
            }
            if (sheet is JarnDataSheet.UpdateDownloading) {
                sheet = JarnDataSheet.UpdateFailed(offer, variant)
            }
        }
    }

    fun install(updates: JarnUpdates, file: JarnDownloadedUpdate) {
        sheet = if (updates.install(file)) {
            null
        } else {
            JarnDataSheet.Message(R.string.jarnfe_update_install_failed_title, R.string.jarnfe_update_install_failed_message)
        }
    }

    fun openReleasePage(updates: JarnUpdates, offer: JarnUpdateOffer) {
        sheet = if (updates.openReleasePage(offer)) {
            null
        } else {
            JarnDataSheet.Message(R.string.jarnfe_update_no_browser_title, R.string.jarnfe_update_no_browser_message)
        }
    }

    private fun begin(returnTo: FocusRequester?, @StringRes messageRes: Int): Boolean {
        if (sheet != null) return false
        returnFocus = returnTo
        sheet = JarnDataSheet.Working(messageRes)
        return true
    }
}

@StringRes
private fun JarnBackupProblem.messageRes(): Int = when (this) {
    JarnBackupProblem.NotABackup -> R.string.jarnfe_restore_not_backup
    JarnBackupProblem.Damaged -> R.string.jarnfe_restore_damaged
    JarnBackupProblem.TooNew -> R.string.jarnfe_restore_too_new
    JarnBackupProblem.Unreadable -> R.string.jarnfe_restore_unreadable
}

internal val JarnDataFlow: JarnDataController by lazy {
    JarnDataController(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
}
