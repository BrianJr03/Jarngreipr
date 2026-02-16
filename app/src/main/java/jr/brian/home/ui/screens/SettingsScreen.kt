package jr.brian.home.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
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
import jr.brian.home.ui.components.settings.sections.ESDEDisplaySection
import jr.brian.home.ui.components.settings.sections.ExtrasSection
import jr.brian.home.ui.components.settings.sections.LayoutSection
import jr.brian.home.ui.components.settings.sections.SupportSection
import jr.brian.home.ui.components.settings.sections.SystemSection
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import kotlin.math.roundToInt
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateInfo
import kotlinx.coroutines.launch

private const val SECTION_APPEARANCE = "appearance"
private const val SECTION_ESDE = "esde"
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
    onRunSetupWizard: () -> Unit = {},
    onNavigateToMarqueePressShortcut: () -> Unit = {},
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
                    onRunSetupWizard = onRunSetupWizard,
                    onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut,
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
    onRunSetupWizard: () -> Unit = {},
    onNavigateToMarqueePressShortcut: () -> Unit = {},
    isCheckingForUpdates: Boolean = false,
    onNotificationBadgeClick: () -> Unit = {},
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expandedSection by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var containerHeight by remember { mutableStateOf(0) }
    val thumbHeight = 80.dp
    val thumbHeightPx = with(density) { thumbHeight.toPx() }

    val isThorDevice = remember { android.os.Build.MODEL == DeviceModel.THOR }

    val scrollProgress by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems == 0) 0f
            else {
                val firstVisible = listState.firstVisibleItemIndex
                val firstVisibleOffset = listState.firstVisibleItemScrollOffset
                val avgItemSize = listState.layoutInfo.visibleItemsInfo.map { it.size }.average().takeIf { it > 0 } ?: 1.0
                ((firstVisible + firstVisibleOffset / avgItemSize) / (totalItems - 1).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)
            }
        }
    }

    BackHandler {
        if (expandedSection != null) expandedSection = null else onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize().onSizeChanged { containerHeight = it.height }
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 4.dp),
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
                        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    },
                )
            }

            item(key = SECTION_APPEARANCE) {
                AppearanceSection(
                    isExpanded = expandedSection == SECTION_APPEARANCE,
                    onToggle = { expandedSection = if (expandedSection == SECTION_APPEARANCE) null else SECTION_APPEARANCE },
                    onNavigateToCustomTheme = onNavigateToCustomTheme,
                    onIconPackChanged = onIconPackChanged,
                    onNavigateToEsdeSettings = onNavigateToEsdeSettings
                )
            }

            item(key = SECTION_ESDE) {
                ESDEDisplaySection(
                    isExpanded = expandedSection == SECTION_ESDE,
                    onToggle = { expandedSection = if (expandedSection == SECTION_ESDE) null else SECTION_ESDE },
                    onRunSetupWizard = onRunSetupWizard,
                    onNavigateToMarqueePressShortcut = onNavigateToMarqueePressShortcut
                )
            }

            item(key = SECTION_LAYOUT) {
                LayoutSection(
                    isExpanded = expandedSection == SECTION_LAYOUT,
                    onToggle = { expandedSection = if (expandedSection == SECTION_LAYOUT) null else SECTION_LAYOUT },
                    isThorDevice = isThorDevice,
                    allAppsUnfiltered = allAppsUnfiltered,
                    onNavigateToBackButtonShortcut = onNavigateToBackButtonShortcut,
                    onNavigateToDockSettings = onNavigateToDockSettings
                )
            }

            item(key = SECTION_SYSTEM) {
                SystemSection(
                    isExpanded = expandedSection == SECTION_SYSTEM,
                    onToggle = { expandedSection = if (expandedSection == SECTION_SYSTEM) null else SECTION_SYSTEM },
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
                    onToggle = { expandedSection = if (expandedSection == SECTION_SUPPORT) null else SECTION_SUPPORT },
                    onNavigateToFAQ = onNavigateToFAQ
                )
            }

            item(key = SECTION_EXTRAS) {
                ExtrasSection(
                    isExpanded = expandedSection == SECTION_EXTRAS,
                    onToggle = { expandedSection = if (expandedSection == SECTION_EXTRAS) null else SECTION_EXTRAS }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(6.dp)
                .padding(vertical = 24.dp, horizontal = 2.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val trackHeight = containerHeight - thumbHeightPx - 48.dp.toPx()
                        val newProgress = ((change.position.y - thumbHeightPx / 2) / trackHeight).coerceIn(0f, 1f)
                        val totalItems = listState.layoutInfo.totalItemsCount
                        val targetIndex = (newProgress * (totalItems - 1)).roundToInt()
                        scope.launch { listState.scrollToItem(targetIndex) }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(0.15f)
                    .offset { IntOffset(0, ((containerHeight - thumbHeightPx - 48.dp.toPx()) * scrollProgress).roundToInt()) }
                    .clip(RoundedCornerShape(2.dp))
                    .background(ThemePrimaryColor)
            )
        }
    }
}
