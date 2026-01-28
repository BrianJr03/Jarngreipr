package jr.brian.home.ui.components.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.SettingsSectionHeader

fun LazyListScope.systemSection(
    isVisible: (String?) -> Boolean,
    isCheckingForUpdates: Boolean,
    onCheckForUpdates: () -> Unit,
    onNavigateToCrashLogs: () -> Unit,
    onNavigateToControlPad: () -> Unit,
    onNavigateToMonitor: () -> Unit
) {
    item(key = "header_system") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsSectionHeader(
                title = stringResource(id = R.string.settings_section_system)
            )
        }
    }

    item(key = "updates") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
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
        }
    }

    item(key = "crash_logs") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingItem(
                title = stringResource(id = R.string.settings_crash_logs_title),
                description = stringResource(id = R.string.settings_crash_logs_description),
                icon = Icons.Default.BugReport,
                onClick = onNavigateToCrashLogs
            )
        }
    }

    item(key = "control_pad") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingItem(
                title = stringResource(id = R.string.control_pad_screen_title),
                description = stringResource(id = R.string.control_pad_screen_description),
                icon = Icons.Default.GridView,
                onClick = onNavigateToControlPad
            )
        }
    }

    item(key = "monitor") {
        AnimatedVisibility(
            visible = isVisible(null),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingItem(
                title = stringResource(id = R.string.monitor_screen_title),
                description = stringResource(id = R.string.monitor_screen_description),
                icon = Icons.Default.Monitor,
                onClick = onNavigateToMonitor
            )
        }
    }
}
