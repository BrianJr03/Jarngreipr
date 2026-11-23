package jr.brian.jarnfe.backup

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnNavigateRow
import jr.brian.core.ui.components.JarnToggleRow
import jr.brian.core.ui.components.JarnValueRow
import jr.brian.jarnfe.R
import jr.brian.jarnfe.settings.SettingsSectionTitle
import jr.brian.jarnfe.shell.JarnNotices

private const val BACKUP_FILE_NAME = "jarngreipr-config.json"
private const val BACKUP_MIME_TYPE = "application/json"
private val RestoreMimeTypes = arrayOf(BACKUP_MIME_TYPE, "text/plain", "application/octet-stream")

@Composable
internal fun JarnDataSection() {
    val context = LocalContext.current
    val services = remember(context) { context.jarnDataServicesOrNull() } ?: return
    SettingsSectionTitle(R.string.jarnfe_data_backup_title)
    BackupRow(services.backup)
    RestoreRow(services.backup)
    SettingsSectionTitle(R.string.jarnfe_data_updates_title)
    CheckForUpdatesRow(services.updates)
    ReleaseCandidateRow(services.updates)
}

@Composable
private fun BackupRow(backup: JarnBackup) {
    val focus = remember { FocusRequester() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE),
    ) { uri: Uri? -> uri?.let { JarnDataFlow.export(backup, it, focus) } }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_data_backup),
        onClick = { launchPicker { launcher.launch(BACKUP_FILE_NAME) } },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        supporting = stringResource(R.string.jarnfe_data_backup_hint),
    )
}

@Composable
private fun RestoreRow(backup: JarnBackup) {
    val focus = remember { FocusRequester() }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> uri?.let { JarnDataFlow.inspect(backup, it, focus) } }
    JarnNavigateRow(
        label = stringResource(R.string.jarnfe_data_restore),
        onClick = { launchPicker { launcher.launch(RestoreMimeTypes) } },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
        supporting = stringResource(R.string.jarnfe_data_restore_hint),
    )
}

@Composable
private fun CheckForUpdatesRow(updates: JarnUpdates) {
    val focus = remember { FocusRequester() }
    JarnValueRow(
        label = stringResource(R.string.jarnfe_data_check_updates),
        value = updates.currentVersion,
        onClick = { JarnDataFlow.checkForUpdate(updates, focus) },
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
}

@Composable
private fun ReleaseCandidateRow(updates: JarnUpdates) {
    JarnToggleRow(
        label = stringResource(R.string.jarnfe_data_rc),
        checked = updates.rcEnabled,
        onCheckedChange = updates::setRcEnabled,
        modifier = Modifier.fillMaxWidth(),
        supporting = stringResource(R.string.jarnfe_data_rc_hint),
    )
}

private inline fun launchPicker(launch: () -> Unit) {
    try {
        launch()
    } catch (_: ActivityNotFoundException) {
        JarnNotices.show(R.string.jarnfe_wallpaper_no_picker)
    }
}
