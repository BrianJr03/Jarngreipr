package jr.brian.home.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.ui.components.dialog.ThemeShareInfoDialog
import jr.brian.home.ui.components.settings.ScreenHeader
import jr.brian.home.ui.screens.themeshare.NearbyWallpaperSection
import jr.brian.home.ui.screens.themeshare.ReceivedThemesSection
import jr.brian.home.ui.screens.themeshare.ReceivedWallpapersSection
import jr.brian.home.ui.screens.themeshare.ThemeShareSettingsSection
import jr.brian.home.ui.screens.themeshare.ThemeSharingSection
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalThemeManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.viewmodels.ThemeShareViewModel
import jr.brian.ping.PingPermissions
import jr.brian.ping.PingPermissions.hasPingPermissions
import jr.brian.pingnearby.PingNearbyPermissions
import jr.brian.pingnearby.PingNearbyPermissions.hasNearbyPermissions

@Composable
fun ThemeShareScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThemeShareViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val themeManager = LocalThemeManager.current
    val receivedThemes by viewModel.receivedThemes.collectAsStateWithLifecycle()
    val receivedWallpapers by viewModel.receivedWallpapers.collectAsStateWithLifecycle()
    val isDiscoveringWallpaper = themeManager.isWallpaperNearbyRunning

    val infoDialogState = rememberDialogState<Unit>()

    var isPinging by remember { mutableStateOf(themeManager.isPingAutoStart) }
    var isSettingsExpanded by remember { mutableStateOf(false) }
    var isThemeSharingExpanded by remember { mutableStateOf(false) }
    var isNearbyWallpaperExpanded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        with(PingPermissions) {
            if (!context.hasPingPermissions()) {
                requestPingPermissions(permissionLauncher)
            }
            requestBatteryOptimizationExemption(context)
        }
        with(PingNearbyPermissions) {
            if (!context.hasNearbyPermissions()) {
                requestNearbyPermissions(permissionLauncher)
            }
        }
        val prefs = context.getSharedPreferences("gaming_launcher_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("theme_share_info_seen", false)) {
            infoDialogState.show()
        }
    }

    LaunchedEffect(themeManager.isPingAutoStart) {
        if (themeManager.isPingAutoStart && !isPinging) {
            if (context.hasPingPermissions()) {
                themeManager.shareCurrentTheme()
                isPinging = true
            }
        } else if (!themeManager.isPingAutoStart && isPinging) {
            themeManager.stopSharing()
            isPinging = false
        }
    }

    LaunchedEffect(themeManager.isWallpaperNearbyAutoStart) {
        if (themeManager.isWallpaperNearbyAutoStart && !isDiscoveringWallpaper) {
            if (context.hasNearbyPermissions()) {
                themeManager.startWallpaperNearby()
            }
        } else if (!themeManager.isWallpaperNearbyAutoStart && isDiscoveringWallpaper) {
            themeManager.stopWallpaperNearby()
        }
    }

    LaunchedEffect(themeManager) {
        themeManager.receivedWallpaperFile.collect { (_, uri) ->
            viewModel.saveReceivedWallpaperFile(uri.toString())
        }
    }

    if (infoDialogState.isVisible) {
        ThemeShareInfoDialog(
            onDismiss = {
                val prefs = context.getSharedPreferences("gaming_launcher_prefs", Context.MODE_PRIVATE)
                prefs.edit { putBoolean("theme_share_info_seen", true) }
                infoDialogState.dismiss()
            }
        )
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(containerColor = OledBackgroundColor) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 16.dp)
                .systemBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ScreenHeader(onBackClick = onNavigateBack) }

            item {
                ThemeShareSettingsSection(
                    isExpanded = isSettingsExpanded,
                    onToggle = { isSettingsExpanded = !isSettingsExpanded },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            item {
                ThemeSharingSection(
                    isPinging = isPinging,
                    isExpanded = isThemeSharingExpanded,
                    onToggle = { isThemeSharingExpanded = !isThemeSharingExpanded },
                    onPingClick = {
                        if (context.hasPingPermissions()) {
                            if (isPinging) themeManager.stopSharing()
                            else themeManager.shareCurrentTheme()
                            isPinging = !isPinging
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            item {
                NearbyWallpaperSection(
                    isExpanded = isNearbyWallpaperExpanded,
                    onToggle = { isNearbyWallpaperExpanded = !isNearbyWallpaperExpanded },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            item {
                ReceivedThemesSection(
                    receivedThemes = receivedThemes,
                    onDeleteTheme = { displayName, themeId ->
                        viewModel.deleteSharedTheme(displayName, themeId)
                    }
                )
            }

            item {
                ReceivedWallpapersSection(
                    receivedWallpapers = receivedWallpapers,
                    onDelete = { viewModel.deleteReceivedWallpaper(it) }
                )
            }
        }
    }
}
