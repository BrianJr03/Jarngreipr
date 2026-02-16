package jr.brian.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.VideoPlayerActivity
import jr.brian.home.esde.viewmodel.ESDEViewModel
import jr.brian.home.model.Shortcut
import jr.brian.home.service.AppNotificationListenerService
import jr.brian.home.ui.components.UpdateAvailableDialog
import jr.brian.home.ui.components.WhatsNewDialog
import jr.brian.home.ui.components.dialog.NotificationAccessDialog
import jr.brian.home.ui.components.dialog.hasUserDeclinedNotificationAccess
import jr.brian.home.ui.components.dialog.openAppSettings
import jr.brian.home.ui.components.dialog.openNotificationAccessSettings
import jr.brian.home.ui.components.dialog.setNotificationAccessDeclined
import jr.brian.home.ui.navigation.appDockSettingsScreen
import jr.brian.home.ui.navigation.appSearchScreen
import jr.brian.home.ui.navigation.backButtonShortcutScreen
import jr.brian.home.ui.navigation.controlPadScreen
import jr.brian.home.ui.navigation.crashLogsScreen
import jr.brian.home.ui.navigation.customThemeScreen
import jr.brian.home.ui.navigation.esdeSettingsScreen
import jr.brian.home.ui.navigation.faqScreen
import jr.brian.home.ui.navigation.launcherScreen
import jr.brian.home.ui.navigation.marqueePressShortcutScreen
import jr.brian.home.ui.navigation.monitorScreen
import jr.brian.home.ui.navigation.recentAppsScreen
import jr.brian.home.ui.navigation.settingsScreen
import jr.brian.home.ui.navigation.volumeControlsScreen
import jr.brian.home.ui.navigation.widgetPickerScreen
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppUpdateManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.LocalWhatsNewManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.PatchNotesUtil
import jr.brian.home.util.Routes
import jr.brian.home.util.UpdateChecker
import jr.brian.home.util.UpdateInfo
import jr.brian.home.util.launchApp
import jr.brian.home.viewmodels.MainViewModel
import jr.brian.home.viewmodels.PowerViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import androidx.compose.ui.graphics.Color as GraphicsColor

@OptIn(ExperimentalMaterial3Api::class)
@androidx.media3.common.util.UnstableApi
@Composable
fun MainContent(
    hideLauncherUI: Boolean = false,
    triggerMarqueePressShortcut: Boolean = false,
    onMarqueePressShortcutHandled: () -> Unit = {},
    onAnyOverlayVisibleChanged: (Boolean) -> Unit = {},
    onCurrentPageChanged: (Int) -> Unit = {},
    onPagerScrollProgressChanged: (Float) -> Unit = {},
    onDockPositioned: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val mainViewModel: MainViewModel = hiltViewModel()
    val widgetViewModel: WidgetViewModel = hiltViewModel()
    val powerViewModel: PowerViewModel = hiltViewModel()
    val esdeViewModel: ESDEViewModel = hiltViewModel()
    val wallpaperManager = LocalWallpaperManager.current
    val whatsNewManager = LocalWhatsNewManager.current
    val appUpdateManager = LocalAppUpdateManager.current
    val esdePreferencesManager = LocalESDEPreferencesManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val esdePrefsState by esdePreferencesManager.state.collectAsStateWithLifecycle()
    val shouldShowWhatsNew by whatsNewManager.shouldShowWhatsNew.collectAsStateWithLifecycle()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    var currentPagerPage by remember { mutableStateOf(0) }
    var isAnyLauncherSheetVisible by remember { mutableStateOf(false) }

    val isNotOnLauncher = currentRoute != null && currentRoute != Routes.LAUNCHER

    LaunchedEffect(isNotOnLauncher) {
        esdeViewModel.setLauncherActive(!isNotOnLauncher)
    }

    LaunchedEffect(currentPagerPage) {
        onCurrentPageChanged(currentPagerPage)
    }

    val updateDialogState = rememberDialogState<UpdateInfo>()
    val whatsNewDialogState = rememberDialogState<Unit>()
    val notificationAccessDialogState = rememberDialogState<Unit>()
    var currentVersionName by remember { mutableStateOf("") }

    val isAnyDialogVisible = whatsNewDialogState.isVisible ||
            updateDialogState.isVisible ||
            notificationAccessDialogState.isVisible
    val isAnyOverlayVisible = isNotOnLauncher || isAnyDialogVisible || isAnyLauncherSheetVisible

    LaunchedEffect(isAnyOverlayVisible) {
        onAnyOverlayVisibleChanged(isAnyOverlayVisible)
        if (isAnyOverlayVisible) {
            VideoPlayerActivity.finishIfRunning()
        }
    }

    LaunchedEffect(Unit) {
        mainViewModel.loadAllApps(context)
        widgetViewModel.initializeWidgetHost(context)

        val versionName = context.packageManager.getPackageInfo(
            context.packageName,
            0
        ).versionName ?: "Unknown"

        currentVersionName = versionName
        whatsNewManager.checkAndShowWhatsNew(versionName)

        val update = UpdateChecker.checkForUpdate(versionName)
        if (update.isUpdateAvailable && appUpdateManager.shouldShowUpdateDialog(context, update.latestVersion)
        ) {
            updateDialogState.show(update)
        }

        if (!AppNotificationListenerService.isNotificationAccessGranted(context) &&
            !hasUserDeclinedNotificationAccess(context)
        ) {
            notificationAccessDialogState.show()
        }
    }

    LaunchedEffect(shouldShowWhatsNew) {
        if (shouldShowWhatsNew) {
            whatsNewDialogState.show()
        }
    }

    LaunchedEffect(triggerMarqueePressShortcut) {
        if (triggerMarqueePressShortcut) {
            when (esdePrefsState.marqueePressShortcut) {
                Shortcut.NONE -> navController.navigate(Routes.ESDE_SETTINGS)
                Shortcut.SETTINGS -> navController.navigate(Routes.SETTINGS)
                Shortcut.APP_SEARCH -> navController.navigate(Routes.APP_SEARCH)
                Shortcut.POWERED_OFF -> powerViewModel.togglePower()
                Shortcut.QUICK_DELETE -> {}
                Shortcut.CUSTOM_THEME -> navController.navigate(Routes.CUSTOM_THEME)
                Shortcut.MONITOR -> navController.navigate(Routes.MONITOR)
                Shortcut.CONTROL_PAD -> navController.navigate(Routes.CONTROL_PAD)
                Shortcut.VOLUME_CONTROLS -> navController.navigate(Routes.VOLUME_CONTROLS)
                Shortcut.RECENT_APPS -> navController.navigate(Routes.RECENT_APPS)
                Shortcut.APP -> {
                    esdePrefsState.marqueePressShortcutAppPackage?.let { packageName ->
                        launchApp(
                            context = context,
                            packageName = packageName,
                            displayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                                packageName
                            )
                        )
                    }
                }
            }
            onMarqueePressShortcutHandled()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                powerViewModel.powerOn()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_PACKAGE_ADDED,
                    Intent.ACTION_PACKAGE_REMOVED,
                    Intent.ACTION_PACKAGE_CHANGED -> {
                        context?.let { mainViewModel.loadAllApps(it) }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (wallpaperManager.isTransparent()) {
                GraphicsColor.Transparent
            } else {
                MaterialTheme.colorScheme.background
            }
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.LAUNCHER
            ) {
                launcherScreen(
                    navController = navController,
                    context = context,
                    mainViewModel = mainViewModel,
                    widgetViewModel = widgetViewModel,
                    powerViewModel = powerViewModel,
                    onPagerScrollProgressChanged = onPagerScrollProgressChanged,
                    onCurrentPageChanged = { page -> currentPagerPage = page },
                    onSheetVisibilityChanged = { visible -> isAnyLauncherSheetVisible = visible },
                    onDockPositioned = onDockPositioned,
                    hideLauncherUI = hideLauncherUI
                )

                appDockSettingsScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )

                appSearchScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )

                backButtonShortcutScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )

                controlPadScreen(navController = navController)

                crashLogsScreen(navController = navController)

                customThemeScreen(navController = navController)

                faqScreen(navController = navController)

                monitorScreen(navController = navController)

                recentAppsScreen(navController = navController)

                settingsScreen(
                    navController = navController,
                    context = context,
                    mainViewModel = mainViewModel
                )

                volumeControlsScreen(navController = navController)

                widgetPickerScreen(
                    navController = navController,
                    widgetViewModel = widgetViewModel
                )

                esdeSettingsScreen(
                    navController = navController
                )

                marqueePressShortcutScreen(
                    navController = navController,
                    mainViewModel = mainViewModel
                )
            }
        }

        if (whatsNewDialogState.isVisible) {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"

            var patchNotes by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(Unit) {
                patchNotes = PatchNotesUtil.fetchPatchNotesWithFallback(
                    context = context,
                    currentVersionName = versionName,
                )
            }

            patchNotes?.let { notes ->
                WhatsNewDialog(
                    versionName = versionName,
                    patchNotes = notes,
                    onDismiss = {
                        whatsNewManager.markWhatsNewAsSeen(versionName)
                        whatsNewDialogState.dismiss()
                    }
                )
            }
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