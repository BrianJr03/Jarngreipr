package jr.brian.home.ui.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.UpdateAvailableDialog
import jr.brian.home.ui.components.settings.SettingsHeaderComponent
import jr.brian.home.ui.components.settings.sections.appearanceSection
import jr.brian.home.ui.components.settings.sections.extrasSection
import jr.brian.home.ui.components.settings.sections.layoutSection
import jr.brian.home.ui.components.settings.sections.supportSection
import jr.brian.home.ui.components.settings.sections.systemSection
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.util.rememberAutoFocus
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateInfo
import kotlinx.coroutines.launch

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
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appUpdateManager = LocalAppUpdateManager.current
    val appVisibilityManager = LocalAppVisibilityManager.current
    val showBackButton = appVisibilityManager.showSettingsBackButton
    
    val updateDialogState = rememberDialogState<UpdateInfo>()
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
                SettingsHeaderComponent(
                    showBackButton = showBackButton,
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
                    isCheckingForUpdates = isCheckingForUpdates,
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
    isCheckingForUpdates: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val firstItemFocusRequester = rememberAutoFocus()
    var expandedItem by remember { mutableStateOf<String?>(null) }

    val isThorDevice = remember {
        android.os.Build.MODEL == DeviceModel.THOR
    }

    fun isVisible(itemKey: String? = null): Boolean {
        return expandedItem == null || expandedItem == itemKey
    }

    BackHandler {
        if (expandedItem != null) {
            expandedItem = null
        } else {
            onDismiss()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        appearanceSection(
            expandedItem = expandedItem,
            onExpandedItemChange = { expandedItem = it },
            isVisible = ::isVisible,
            firstItemFocusRequester = firstItemFocusRequester,
            onNavigateToCustomTheme = onNavigateToCustomTheme,
            onIconPackChanged = onIconPackChanged
        )

        layoutSection(
            expandedItem = expandedItem,
            onExpandedItemChange = { expandedItem = it },
            isVisible = ::isVisible,
            isThorDevice = isThorDevice,
            allAppsUnfiltered = allAppsUnfiltered,
            onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut
        )

        systemSection(
            isVisible = ::isVisible,
            isCheckingForUpdates = isCheckingForUpdates,
            onCheckForUpdates = onCheckForUpdates,
            onNavigateToCrashLogs = onNavigateToCrashLogs,
            onNavigateToControlPad = onNavigateToControlPad,
            onNavigateToMonitor = onNavigateToMonitor
        )

        supportSection(
            isVisible = ::isVisible,
            context = context,
            onNavigateToFAQ = onNavigateToFAQ
        )

        extrasSection(
            isVisible = ::isVisible
        )
    }
}
