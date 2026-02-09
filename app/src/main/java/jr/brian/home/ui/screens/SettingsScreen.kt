package jr.brian.home.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.UpdateAvailableDialog
import jr.brian.home.ui.components.dialog.NotificationAccessDialog
import jr.brian.home.ui.components.dialog.openAppSettings
import jr.brian.home.ui.components.dialog.openNotificationAccessSettings
import jr.brian.home.ui.components.dialog.setNotificationAccessDeclined
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.sections.AppearanceSection
import jr.brian.home.ui.components.settings.sections.ExtrasSection
import jr.brian.home.ui.components.settings.sections.LayoutSection
import jr.brian.home.ui.components.settings.sections.SupportSection
import jr.brian.home.ui.components.settings.sections.SystemSection
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateInfo
import kotlinx.coroutines.launch

private const val SECTION_APPEARANCE = "appearance"
private const val SECTION_LAYOUT = "layout"
private const val SECTION_SYSTEM = "system"
private const val SECTION_SUPPORT = "support"
private const val SECTION_EXTRAS = "extras"

@Composable
fun SettingsScreen(
    allAppsUnfiltered: List<AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit,
    onNavigateToCustomTheme: () -> Unit,
    onIconPackChanged: () -> Unit,
    onNavigateToBackButtonShortcut: () -> Unit = {},
    onNavigateToMonitor: () -> Unit = {},
    onNavigateToControlPad: () -> Unit = {},
    onNavigateToCrashLogs: () -> Unit = {},
    onNavigateToVolumeControls: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {},
    onNavigateToEsdeSettings: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdateManager = LocalAppUpdateManager.current
    
    val updateDialogState = rememberDialogState<UpdateInfo>()
    val notificationAccessDialogState = rememberDialogState<Unit>()
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    
    val currentVersionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }
    
    Scaffold(
        containerColor = OledBackgroundColor,
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .systemBarsPadding(),
        ) {
            Column {
                ScreenHeader(
                    showVersion = true,
                    onBackClick = onDismiss
                )
                SettingsContent(
                    allAppsUnfiltered = allAppsUnfiltered,
                    onNavigateToFAQ = onNavigateToFAQ,
                    onNavigateToCustomTheme = onNavigateToCustomTheme,
                    onIconPackChanged = onIconPackChanged,
                    onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut,
                    onNavigateToMonitor = onNavigateToMonitor,
                    onNavigateToControlPad = onNavigateToControlPad,
                    onNavigateToCrashLogs = onNavigateToCrashLogs,
                    onNavigateToVolumeControls = onNavigateToVolumeControls,
                    onNavigateToDockSettings = onNavigateToDockSettings,
                    onNavigateToEsdeSettings = onNavigateToEsdeSettings,
                    isCheckingForUpdates = isCheckingForUpdates,
                    onNotificationBadgeClick = { notificationAccessDialogState.show(Unit) },
                    onCheckForUpdates = {
                        if (!isCheckingForUpdates) {
                            isCheckingForUpdates = true
                            scope.launch {
                                appUpdateManager.clearSkippedVersion(context)
                                appUpdateManager.clearDownloadedVersion(context)
                                
                                val update = UpdateChecker.checkForUpdate(currentVersionName)
                                isCheckingForUpdates = false
                                
                                if (update.isUpdateAvailable) {
                                    updateDialogState.show(update)
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.update_not_available),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    onDismiss = onDismiss
                )
            }
            
            updateDialogState.item?.let { updateInfo ->
                if (updateDialogState.isVisible) {
                    UpdateAvailableDialog(
                        updateInfo = updateInfo,
                        currentVersion = currentVersionName,
                        onDismiss = updateDialogState::dismiss,
                        onRemindLater = updateDialogState::dismiss,
                        onSkipVersion = {
                            appUpdateManager.skipVersion(context, updateInfo.latestVersion)
                            updateDialogState.dismiss()
                        },
                        onDownloadComplete = {
                            appUpdateManager.markVersionDownloaded(context, updateInfo.latestVersion)
                        }
                    )
                }
            }
            
            if (notificationAccessDialogState.isVisible) {
                NotificationAccessDialog(
                    onDismiss = notificationAccessDialogState::dismiss,
                    onGrantAccess = {
                        notificationAccessDialogState.dismiss()
                        openNotificationAccessSettings(context)
                    },
                    onOpenAppSettings = {
                        openAppSettings(context)
                    },
                    onNeverAskAgain = {
                        setNotificationAccessDeclined(context)
                        notificationAccessDialogState.dismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    allAppsUnfiltered: List<AppInfo> = emptyList(),
    onNavigateToFAQ: () -> Unit,
    onNavigateToCustomTheme: () -> Unit,
    onIconPackChanged: () -> Unit,
    onNavigateToBackButtonShortcut: () -> Unit = {},
    onNavigateToMonitor: () -> Unit = {},
    onNavigateToControlPad: () -> Unit = {},
    onNavigateToCrashLogs: () -> Unit = {},
    onNavigateToVolumeControls: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {},
    onNavigateToEsdeSettings: () -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    onNotificationBadgeClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var expandedSection by remember { mutableStateOf<String?>(null) }

    val isThorDevice = remember {
        android.os.Build.MODEL == DeviceModel.THOR
    }

    BackHandler {
        if (expandedSection != null) {
            expandedSection = null
        } else {
            onDismiss()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "coffee") {
            val url = stringResource(R.string.settings_buy_me_coffee_url)
            SettingItem(
                title = stringResource(id = R.string.settings_buy_me_coffee_title),
                description = stringResource(id = R.string.settings_buy_me_coffee_description),
                icon = Icons.Default.Coffee,
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        url.toUri()
                    )
                    context.startActivity(intent)
                },
            )
        }

        item(key = SECTION_APPEARANCE) {
            AppearanceSection(
                isExpanded = expandedSection == SECTION_APPEARANCE,
                onToggle = {
                    expandedSection = if (expandedSection == SECTION_APPEARANCE) null else SECTION_APPEARANCE
                },
                onNavigateToCustomTheme = onNavigateToCustomTheme,
                onIconPackChanged = onIconPackChanged,
                onNavigateToEsdeSettings = onNavigateToEsdeSettings
            )
        }

        item(key = SECTION_LAYOUT) {
            LayoutSection(
                isExpanded = expandedSection == SECTION_LAYOUT,
                onToggle = {
                    expandedSection = if (expandedSection == SECTION_LAYOUT) null else SECTION_LAYOUT
                },
                isThorDevice = isThorDevice,
                allAppsUnfiltered = allAppsUnfiltered,
                onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut,
                onNavigateToDockSettings = onNavigateToDockSettings
            )
        }

        item(key = SECTION_SYSTEM) {
            SystemSection(
                isExpanded = expandedSection == SECTION_SYSTEM,
                onToggle = {
                    expandedSection = if (expandedSection == SECTION_SYSTEM) null else SECTION_SYSTEM
                },
                isCheckingForUpdates = isCheckingForUpdates,
                onCheckForUpdates = onCheckForUpdates,
                onNavigateToCrashLogs = onNavigateToCrashLogs,
                onNavigateToControlPad = onNavigateToControlPad,
                onNavigateToMonitor = onNavigateToMonitor,
                onNavigateToVolumeControls = onNavigateToVolumeControls,
                onNotificationBadgeClick = onNotificationBadgeClick
            )
        }

        item(key = SECTION_SUPPORT) {
            SupportSection(
                isExpanded = expandedSection == SECTION_SUPPORT,
                onToggle = {
                    expandedSection = if (expandedSection == SECTION_SUPPORT) null else SECTION_SUPPORT
                },
                onNavigateToFAQ = onNavigateToFAQ
            )
        }

        item(key = SECTION_EXTRAS) {
            ExtrasSection(
                isExpanded = expandedSection == SECTION_EXTRAS,
                onToggle = {
                    expandedSection = if (expandedSection == SECTION_EXTRAS) null else SECTION_EXTRAS
                }
            )
        }
    }
}
