package jr.brian.home.ui.components.settings.sections

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestorePage
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.theme.managers.LocalImportExportManager
import jr.brian.home.ui.theme.managers.LocalNotificationManager
import jr.brian.home.util.SettingsTag
import kotlinx.coroutines.launch

@Composable
fun SystemSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    isCheckingForUpdates: Boolean,
    onCheckForUpdates: () -> Unit,
    onNavigateToCrashLogs: () -> Unit,
    onNavigateToControlPad: () -> Unit,
    onNavigateToMonitor: () -> Unit,
    onNavigateToVolumeControls: () -> Unit,
    onNotificationBadgeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importExportManager = LocalImportExportManager.current

    val backupSuccessMsg = stringResource(R.string.settings_backup_success)
    val backupErrorMsg = stringResource(R.string.settings_backup_error)
    val restoreSuccessMsg = stringResource(R.string.settings_restore_success)
    val restoreErrorMsg = stringResource(R.string.settings_restore_error)

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        importExportManager.exportToStream(stream)
                    }
                }.fold(
                    onSuccess = { Toast.makeText(context, backupSuccessMsg, Toast.LENGTH_SHORT).show() },
                    onFailure = { Toast.makeText(context, backupErrorMsg, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    importExportManager.importFromStream(stream).fold(
                        onSuccess = { Toast.makeText(context, restoreSuccessMsg, Toast.LENGTH_SHORT).show() },
                        onFailure = { Toast.makeText(context, restoreErrorMsg, Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }

    CollapsibleSettingsSection(
        title = stringResource(id = R.string.settings_section_system),
        icon = Icons.Default.Settings,
        isExpanded = isExpanded,
        onToggle = onToggle
    ) {
        SettingItem(
            title = stringResource(id = R.string.settings_check_updates_title),
            description = if (isCheckingForUpdates) {
                stringResource(id = R.string.update_checking)
            } else {
                stringResource(id = R.string.settings_check_updates_description)
            },
            icon = Icons.Default.SystemUpdate,
            onClick = onCheckForUpdates,
            trailing = if (isCheckingForUpdates) {
                {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            } else null
        )

        SettingItem(
            title = stringResource(id = R.string.settings_crash_logs_title),
            description = stringResource(id = R.string.settings_crash_logs_description),
            icon = Icons.Default.BugReport,
            onClick = onNavigateToCrashLogs
        )

        SettingItem(
            title = stringResource(id = R.string.control_pad_screen_title),
            description = stringResource(id = R.string.control_pad_screen_description),
            icon = Icons.Default.GridView,
            onClick = onNavigateToControlPad,
            tag = SettingsTag.EXPERIMENTAL
        )

        SettingItem(
            title = stringResource(id = R.string.monitor_screen_title),
            description = stringResource(id = R.string.monitor_screen_description),
            icon = Icons.Default.Monitor,
            onClick = onNavigateToMonitor
        )

        SettingItem(
            title = stringResource(id = R.string.settings_volume_controls_title),
            description = stringResource(id = R.string.settings_volume_controls_description),
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            onClick = onNavigateToVolumeControls,
            tag = SettingsTag.EXPERIMENTAL
        )

        val notificationCountManager = LocalNotificationManager.current
        SettingItem(
            title = stringResource(id = R.string.settings_notification_badge_title),
            description = stringResource(id = R.string.settings_notification_badge_description),
            icon = Icons.Default.Notifications,
            onClick = onNotificationBadgeClick,
            trailing = {
                Text(
                    text = if (notificationCountManager.badgesVisible) "ON" else "OFF",
                    color = if (notificationCountManager.badgesVisible) Color.Green else Color.Gray,
                    modifier = Modifier.clickable { notificationCountManager.toggleBadgesVisible() }
                )
            }
        )

        SettingItem(
            title = stringResource(id = R.string.settings_backup_title),
            description = stringResource(id = R.string.settings_backup_description),
            icon = Icons.Default.SaveAlt,
            onClick = { backupLauncher.launch("jarngreipr-config.json") }
        )

        SettingItem(
            title = stringResource(id = R.string.settings_restore_title),
            description = stringResource(id = R.string.settings_restore_description),
            icon = Icons.Default.RestorePage,
            onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain")) }
        )
    }
}
