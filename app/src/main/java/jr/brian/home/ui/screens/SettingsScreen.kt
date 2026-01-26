package jr.brian.home.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import jr.brian.home.R
import jr.brian.home.model.app.AppInfo
import jr.brian.home.ui.components.InfoBox
import jr.brian.home.ui.components.UpdateAvailableDialog
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateInfo
import kotlinx.coroutines.launch
import jr.brian.home.ui.components.settings.BackButtonShortcutItem
import jr.brian.home.ui.components.settings.GridColumnSelectorItem
import jr.brian.home.ui.components.settings.IconPackSelectorItem
import jr.brian.home.ui.components.settings.OledModeToggleItem
import jr.brian.home.ui.components.settings.SettingItem
import jr.brian.home.ui.components.settings.SettingsSectionHeader
import jr.brian.home.ui.components.settings.ThemeSelectorItem
import jr.brian.home.ui.components.settings.ThorSettingsItem
import jr.brian.home.ui.components.settings.VisibilitySettingsItem
import jr.brian.home.ui.components.settings.WallpaperSelectorItem
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemeAccentColor
import jr.brian.home.util.DeviceModel
import jr.brian.home.util.OverlayInfoUtil
import jr.brian.home.util.SettingsScreenUtil.DEFAULT_VERSION_NAME
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_GRID
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_THOR
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_THEME
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_ICON_PACK
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_BACK_BUTTON
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_OLED
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_VISIBILITY
import jr.brian.home.util.SettingsScreenUtil.EXPANDED_WALLPAPER
import kotlinx.coroutines.delay

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
    
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    
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
                VersionInfo()
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
                                    updateInfo = update
                                    showUpdateDialog = true
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
            
            if (showUpdateDialog && updateInfo != null) {
                UpdateAvailableDialog(
                    updateInfo = updateInfo!!,
                    currentVersion = currentVersionName,
                    onDismiss = {
                        showUpdateDialog = false
                    },
                    onRemindLater = {
                        showUpdateDialog = false
                    },
                    onSkipVersion = {
                        appUpdateManager.skipVersion(context, updateInfo!!.latestVersion)
                        showUpdateDialog = false
                    },
                    onDownloadComplete = {
                        appUpdateManager.markVersionDownloaded(context, updateInfo!!.latestVersion)
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
    isCheckingForUpdates: Boolean = false,
    onCheckForUpdates: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val firstItemFocusRequester = remember { FocusRequester() }
    var expandedItem by remember { mutableStateOf<String?>(null) }

    val isThorDevice = remember {
        android.os.Build.MODEL == DeviceModel.THOR
    }

    // Helper to check if an item should be visible
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

    LaunchedEffect(Unit) {
        delay(10)
        firstItemFocusRequester.requestFocus()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 4.dp),
        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 0.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ===== APPEARANCE SECTION =====
        item(key = "header_appearance") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_THEME) || isVisible(EXPANDED_ICON_PACK) || isVisible(EXPANDED_WALLPAPER),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsSectionHeader(
                    title = stringResource(id = R.string.settings_section_appearance)
                )
            }
        }

        // Color Theme
        item(key = "theme") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_THEME),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ThemeSelectorItem(
                    focusRequester = firstItemFocusRequester,
                    isExpanded = expandedItem == EXPANDED_THEME,
                    onExpandChanged = {
                        expandedItem = if (it) EXPANDED_THEME else null
                    },
                    onNavigateToCustomTheme = {
                        expandedItem = null
                        onNavigateToCustomTheme()
                    }
                )
            }
        }

        // Icon Pack
        item(key = "icon_pack") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_ICON_PACK),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                IconPackSelectorItem(
                    isExpanded = expandedItem == EXPANDED_ICON_PACK,
                    onExpandChanged = {
                        expandedItem = if (it) EXPANDED_ICON_PACK else null
                    },
                    onIconPackChanged = onIconPackChanged
                )
            }
        }

        // OLED Black Mode
        item(key = "oled") {
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OledModeToggleItem(
                    isExpanded = expandedItem == EXPANDED_OLED
                )
            }
        }

        // Wallpaper
        item(key = "wallpaper") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_WALLPAPER),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                WallpaperSelectorItem(
                    isExpanded = expandedItem == EXPANDED_WALLPAPER,
                    onExpandChanged = {
                        expandedItem = if (it) EXPANDED_WALLPAPER else null
                    }
                )
            }
        }

        // ===== LAYOUT SECTION =====
        item(key = "header_layout") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_BACK_BUTTON) || isVisible(EXPANDED_GRID) || isVisible(EXPANDED_THOR) || isVisible(EXPANDED_VISIBILITY),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsSectionHeader(
                    title = stringResource(id = R.string.settings_section_layout)
                )
            }
        }

        // Back Button Shortcut
        item(key = "back_button") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_BACK_BUTTON),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BackButtonShortcutItem(
                    isExpanded = expandedItem == EXPANDED_BACK_BUTTON,
                    onExpandChanged = {
                        expandedItem = if (it) EXPANDED_BACK_BUTTON else null
                    },
                    onConfigureClick = {
                        expandedItem = null
                        onNavigateToBackButtonShortcut()
                    }
                )
            }
        }

        // Grid Layout
        item(key = "grid") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_GRID),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GridColumnSelectorItem(
                    isExpanded = expandedItem == EXPANDED_GRID,
                    onExpandChanged = {
                        expandedItem = if (it) EXPANDED_GRID else null
                    },
                    totalAppsCount = allAppsUnfiltered.size
                )
            }
        }

        // Thor Settings (device-specific)
        if (isThorDevice) {
            item(key = "thor") {
                AnimatedVisibility(
                    visible = isVisible(EXPANDED_THOR),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    ThorSettingsItem(
                        isExpanded = expandedItem == EXPANDED_THOR,
                        onExpandChanged = {
                            expandedItem = if (it) EXPANDED_THOR else null
                        }
                    )
                }
            }
        }

        // Visibility Options
        item(key = "visibility") {
            AnimatedVisibility(
                visible = isVisible(EXPANDED_VISIBILITY),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                VisibilitySettingsItem(
                    isExpanded = expandedItem == EXPANDED_VISIBILITY,
                    onExpandChanged = {
                        expandedItem = if (it) EXPANDED_VISIBILITY else null
                    }
                )
            }
        }

        // ===== SYSTEM SECTION =====
        item(key = "header_system") {
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsSectionHeader(
                    title = stringResource(id = R.string.settings_section_system)
                )
            }
        }

        // Check for Updates
        item(key = "updates") {
            AnimatedVisibility(
                visible = isVisible(),
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

        // Crash Logs
        item(key = "crash_logs") {
            AnimatedVisibility(
                visible = isVisible(),
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

        // GamePad
        item(key = "control_pad") {
            AnimatedVisibility(
                visible = isVisible(),
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

        // System Monitor
        item(key = "monitor") {
            AnimatedVisibility(
                visible = isVisible(),
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

        // ===== SUPPORT SECTION =====
        item(key = "header_support") {
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsSectionHeader(
                    title = stringResource(id = R.string.settings_section_support)
                )
            }
        }

        // Buy Me A Coffee
        item(key = "coffee") {
            val url = stringResource(R.string.settings_buy_me_coffee_url)
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
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
        }

        // FAQ
        item(key = "faq") {
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingItem(
                    title = stringResource(id = R.string.settings_faq_title),
                    description = stringResource(id = R.string.settings_faq_description),
                    icon = Icons.AutoMirrored.Filled.Help,
                    onClick = onNavigateToFAQ,
                )
            }
        }

        // ===== EXTRAS SECTION =====
        item(key = "header_extras") {
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SettingsSectionHeader(
                    title = stringResource(id = R.string.settings_section_extras)
                )
            }
        }

        item(key = "thor_fact") {
            val randomMessage = remember { OverlayInfoUtil.getRandomFact() }
            AnimatedVisibility(
                visible = isVisible(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InfoBox(
                    label = stringResource(R.string.welcome_overlay_thor_fact_label),
                    content = stringResource(randomMessage),
                    isPrimary = true,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionInfo() {
    val context = LocalContext.current
    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
            ?: DEFAULT_VERSION_NAME
    } catch (_: Exception) {
        DEFAULT_VERSION_NAME
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, end = 32.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = stringResource(R.string.settings_version_label, versionName),
            color = ThemeAccentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
