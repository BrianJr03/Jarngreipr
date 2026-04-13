package jr.brian.home.ui.components.settings.sections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.components.settings.CollapsibleSettingsSection
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.theme.managers.LocalNotificationManager
import jr.brian.home.util.SettingsTag

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
    }
}
