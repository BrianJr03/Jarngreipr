package jr.brian.home.ui.screens

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.AppInfo
import jr.brian.home.ui.components.OnScreenKeyboard
import jr.brian.home.ui.components.apps.AppGridItem
import jr.brian.home.ui.components.apps.AppOptionsMenu
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.apps.FreePositionedAppsLayout
import jr.brian.home.ui.components.dialog.AppDrawerOptionsDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.components.wallpaper.WallpaperDisplay
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.viewmodels.PowerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean = false,
    onSettingsClick: () -> Unit = {},
    powerViewModel: PowerViewModel? = hiltViewModel(),
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    keyboardVisible: Boolean = true,
    onShowBottomSheet: () -> Unit = {},
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appPositionManager = LocalAppPositionManager.current
    var searchQuery by remember { mutableStateOf("") }

    val isPoweredOff by powerViewModel?.isPoweredOff?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val isFreeModeEnabled by appPositionManager.isFreeModeEnabled.collectAsStateWithLifecycle()

    BackHandler(enabled = isPoweredOff) {}

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        displayManager.displays.size > 1
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppOptionsMenu by remember { mutableStateOf(false) }
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }

    val keyboardFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var savedKeyboardIndex by remember { mutableIntStateOf(0) }
    var savedAppIndex by remember { mutableIntStateOf(0) }

    if (showAppOptionsMenu && selectedApp != null) {
        val currentIconSize = if (isFreeModeEnabled) {
            appPositionManager.getPosition(selectedApp!!.packageName)?.iconSize ?: 64f
        } else {
            64f
        }

        AppOptionsMenu(
            appLabel = selectedApp!!.label,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                selectedApp!!.packageName
            ),
            onDismiss = { showAppOptionsMenu = false },
            onAppInfoClick = {
                openAppInfo(context, selectedApp!!.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    selectedApp!!.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            app = if (isFreeModeEnabled) selectedApp else null,
            currentIconSize = currentIconSize,
            onIconSizeChange = { newSize ->
                if (isFreeModeEnabled) {
                    val currentPos = appPositionManager.getPosition(selectedApp!!.packageName)
                    appPositionManager.savePosition(
                        jr.brian.home.model.AppPosition(
                            packageName = selectedApp!!.packageName,
                            x = currentPos?.x ?: 0f,
                            y = currentPos?.y ?: 0f,
                            iconSize = newSize
                        )
                    )
                }
            }
        )
    }

    if (showDrawerOptionsDialog) {
        DrawerOptionsDialog(
            onDismiss = { showDrawerOptionsDialog = false }
        )
    }

    if (showAppDrawerOptionsDialog) {
        AppDrawerOptionsDialog(
            onDismiss = { showAppDrawerOptionsDialog = false },
            onShowAppVisibility = { showAppVisibilityDialog = true },
            isFreeModeEnabled = isFreeModeEnabled,
            onToggleFreeMode = {
                appPositionManager.setFreeMode(!isFreeModeEnabled)
            },
            onResetPositions = {
                appPositionManager.clearAllPositions()
            }
        )
    }

    if (showAppVisibilityDialog) {
        AppVisibilityDialog(
            apps = appsUnfiltered,
            onDismiss = { showAppVisibilityDialog = false }
        )
    }

    val wallpaperManager = LocalWallpaperManager.current

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            showDrawerOptionsDialog = true
                        }
                    )
                },
    ) {
        WallpaperDisplay(
            wallpaperUri = wallpaperManager.getWallpaperUri(),
            wallpaperType = wallpaperManager.getWallpaperType(),
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppSelectionContent(
                apps = apps,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                columns = gridSettingsManager.columnCount,
                keyboardFocusRequesters = keyboardFocusRequesters,
                appFocusRequesters = appFocusRequesters,
                savedKeyboardIndex = savedKeyboardIndex,
                savedAppIndex = savedAppIndex,
                onKeyboardFocusChanged = { savedKeyboardIndex = it },
                onAppFocusChanged = { savedAppIndex = it },
                onAppClick = { app ->
                    val displayPreference = if (hasExternalDisplay) {
                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    } else {
                        DisplayPreference.CURRENT_DISPLAY
                    }
                    launchApp(
                        context = context,
                        packageName = app.packageName,
                        displayPreference = displayPreference
                    )
                },
                onAppLongClick = { app ->
                    selectedApp = app
                    showAppOptionsMenu = true
                },
                keyboardVisible = keyboardVisible,
                onSettingsClick = onSettingsClick,
                powerViewModel = powerViewModel,
                totalPages = totalPages,
                pagerState = pagerState,
                onMenuClick = { showAppDrawerOptionsDialog = true },
                onShowBottomSheet = onShowBottomSheet,
                isFreeModeEnabled = isFreeModeEnabled,
                appPositionManager = appPositionManager
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppSelectionContent(
    apps: List<AppInfo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    keyboardFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    savedKeyboardIndex: Int,
    savedAppIndex: Int,
    onKeyboardFocusChanged: (Int) -> Unit,
    onAppFocusChanged: (Int) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    keyboardVisible: Boolean = true,
    onSettingsClick: () -> Unit = {},
    powerViewModel: PowerViewModel? = null,
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    onMenuClick: () -> Unit = {},
    onShowBottomSheet: () -> Unit = {},
    isFreeModeEnabled: Boolean = false,
    appPositionManager: jr.brian.home.data.AppPositionManager? = null
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    val rows = gridSettingsManager.rowCount
    val unlimitedMode = gridSettingsManager.unlimitedMode
    val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows

    var keyboardCoordinates by remember {
        mutableStateOf<androidx.compose.ui.layout.LayoutCoordinates?>(
            null
        )
    }

    val filteredApps =
        remember(apps, searchQuery) {
            if (searchQuery.isBlank()) {
                apps.sortedBy { it.label.uppercase() }
            } else {
                apps
                    .filter { it.label.contains(searchQuery, ignoreCase = true) }
                    .sortedBy { it.label.uppercase() }
            }
        }

    Column(modifier = modifier.fillMaxSize()) {
        if (pagerState != null) {
            val settingsIconFocusRequester = remember { FocusRequester() }
            val menuIconFocusRequester = remember { FocusRequester() }
            val powerSettingsManager = LocalPowerSettingsManager.current
            val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()

            ScreenHeaderRow(
                totalPages = totalPages,
                pagerState = pagerState,
                leadingIcon = if (keyboardVisible) null else Icons.Default.Settings,
                leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                onLeadingIconClick = onSettingsClick,
                leadingIconFocusRequester = settingsIconFocusRequester,
                trailingIcon = if (keyboardVisible) null else Icons.Default.Menu,
                trailingIconContentDescription = null,
                onTrailingIconClick = onMenuClick,
                trailingIconFocusRequester = menuIconFocusRequester,
                onNavigateToGrid = {
                    appFocusRequesters[0]?.requestFocus()
                },
                onNavigateFromGrid = {
                    menuIconFocusRequester.requestFocus()
                },
                powerViewModel = powerViewModel,
                showPowerButton = isPowerButtonVisible,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                keyboardCoordinates = if (keyboardVisible) keyboardCoordinates else null,
                keyboardContent = null,
                onFolderClick = onShowBottomSheet
            )
        }

        if (isFreeModeEnabled && !keyboardVisible && appPositionManager != null) {
            FreePositionedAppsLayout(
                apps = filteredApps,
                appPositionManager = appPositionManager,
                keyboardVisible = false,
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        } else {
            Row(modifier = Modifier
                .weight(1f)
                .fillMaxSize()) {
                if (keyboardVisible) {
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .onGloballyPositioned { coordinates ->
                                keyboardCoordinates = coordinates
                            }
                    ) {
                        OnScreenKeyboard(
                            searchQuery = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            keyboardFocusRequesters = keyboardFocusRequesters,
                            onFocusChanged = onKeyboardFocusChanged,
                            onNavigateRight = {
                                appFocusRequesters[savedAppIndex]?.requestFocus()
                            },
                        )
                    }
                }

                AppGrid(
                    apps = filteredApps,
                    columns = columns,
                    maxAppsPerPage = maxAppsPerPage,
                    modifier = Modifier.weight(if (keyboardVisible) 0.5f else 1f),
                    appFocusRequesters = appFocusRequesters,
                    onFocusChanged = onAppFocusChanged,
                    onNavigateLeft = {
                        if (keyboardVisible) {
                            keyboardFocusRequesters[savedKeyboardIndex]?.requestFocus()
                        }
                    },
                    onAppClick = onAppClick,
                    onAppLongClick = onAppLongClick,
                    keyboardVisible = keyboardVisible
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(
    columns: Int,
    maxAppsPerPage: Int,
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    keyboardVisible: Boolean = true
) {
    val gridState = rememberLazyGridState()

    val displayedApps = remember(apps, maxAppsPerPage) {
        apps.take(maxAppsPerPage)
    }

    LaunchedEffect(Unit) {
        appFocusRequesters[0]?.requestFocus()
    }

    val coroutineScope = rememberCoroutineScope()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (keyboardVisible) 16.dp else 8.dp,
            vertical = if (keyboardVisible) 0.dp else 8.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(if (keyboardVisible) 16.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(if (keyboardVisible) 16.dp else 24.dp),
    ) {
        items(displayedApps.size) { index ->
            val app = displayedApps[index]
            val itemFocusRequester =
                remember(index) {
                    FocusRequester().also { appFocusRequesters[index] = it }
                }

            AppGridItem(
                app = app,
                keyboardVisible = keyboardVisible,
                focusRequester = itemFocusRequester,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) },
                onFocusChanged = { onFocusChanged(index) },
                onNavigateUp = {
                    val prevIndex = index - columns
                    if (prevIndex >= 0) {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(prevIndex)
                        }
                        appFocusRequesters[prevIndex]?.requestFocus()
                    }
                },
                onNavigateDown = {
                    val nextIndex = index + columns
                    if (nextIndex < displayedApps.size) {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(nextIndex)
                        }
                        appFocusRequesters[nextIndex]?.requestFocus()
                    }
                },
                onNavigateLeft = {
                    if (index % columns == 0) {
                        onNavigateLeft()
                    } else {
                        val prevIndex = index - 1
                        if (prevIndex >= 0) {
                            appFocusRequesters[prevIndex]?.requestFocus()
                        }
                    }
                },
                onNavigateRight = {
                    val nextIndex = index + 1
                    if (nextIndex < displayedApps.size && nextIndex / columns == index / columns) {
                        appFocusRequesters[nextIndex]?.requestFocus()
                    }
                },
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

private fun launchApp(
    context: Context,
    packageName: String,
    displayPreference: DisplayPreference = DisplayPreference.CURRENT_DISPLAY
) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            when (displayPreference) {
                DisplayPreference.PRIMARY_DISPLAY -> {
                    val options = ActivityOptions.makeBasic()
                    options.launchDisplayId = 0
                    context.startActivity(intent, options.toBundle())
                }

                DisplayPreference.CURRENT_DISPLAY -> {
                    context.startActivity(intent)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun openAppInfo(
    context: Context,
    packageName: String
) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = "package:$packageName".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}