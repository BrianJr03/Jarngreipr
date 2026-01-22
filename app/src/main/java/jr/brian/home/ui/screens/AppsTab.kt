package jr.brian.home.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.ui.components.apps.AppGridItem
import jr.brian.home.ui.components.apps.AppOptionsMenu
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.apps.FreePositionedAppsLayout
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.util.launchApp
import jr.brian.home.util.openAppInfo
import jr.brian.home.viewmodels.PowerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsTab(
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean = false,
    pageIndex: Int = 0,
    totalPages: Int = 1,
    powerViewModel: PowerViewModel = hiltViewModel(),
    pagerState: PagerState? = null,
    onSettingsClick: () -> Unit = {},
    onShowBottomSheet: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
    allApps: List<AppInfo> = emptyList(),
    onNavigateToSearch: () -> Unit = {}
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appPositionManager = LocalAppPositionManager.current
    val folderManager = LocalFolderManager.current

    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val folders by folderManager.getFolders(pageIndex).collectAsStateWithLifecycle(initialValue = emptyList())

    val freeModeByPage by appPositionManager.isFreeModeByPage.collectAsStateWithLifecycle()
    val isFreeModeEnabled = freeModeByPage[pageIndex] ?: false

    val dragLockedByPage by appPositionManager.isDragLockedByPage.collectAsStateWithLifecycle()
    val isDragLocked = dragLockedByPage[pageIndex] ?: true

    LaunchedEffect(pageIndex) {
        appPositionManager.setDragLock(pageIndex, true)
    }

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
    var showHomeTabDialog by remember { mutableStateOf(false) }
    var showCustomIconDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showFolderContentsDialog by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }

    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var savedAppIndex by remember { mutableIntStateOf(0) }

    if (showAppOptionsMenu && selectedApp != null) {
        val currentIconSize = if (isFreeModeEnabled) {
            appPositionManager.getPosition(pageIndex, selectedApp!!.packageName)?.iconSize ?: 64f
        } else {
            64f
        }

        val appVisibilityManager = LocalAppVisibilityManager.current
        val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
        val isAppHidden = remember(selectedApp, pageIndex, hiddenAppsByPage) {
            appVisibilityManager.isAppHidden(pageIndex, selectedApp!!.packageName)
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
                    val currentPos = appPositionManager.getPosition(
                        pageIndex,
                        selectedApp!!.packageName
                    )
                    appPositionManager.savePosition(
                        pageIndex,
                        AppPosition(
                            packageName = selectedApp!!.packageName,
                            x = currentPos?.x ?: 0f,
                            y = currentPos?.y ?: 0f,
                            iconSize = newSize
                        )
                    )
                }
            },
            onToggleVisibility = {
                if (isAppHidden) {
                    appVisibilityManager.showApp(pageIndex, selectedApp!!.packageName)
                } else {
                    appVisibilityManager.hideApp(pageIndex, selectedApp!!.packageName)
                }
            },
            onCustomIconClick = {
                showAppOptionsMenu = false
                showCustomIconDialog = true
            }
        )
    }

    if (showCustomIconDialog && selectedApp != null) {
        CustomIconDialog(
            packageName = selectedApp!!.packageName,
            appLabel = selectedApp!!.label,
            onDismiss = { showCustomIconDialog = false }
        )
    }

    if (showHomeTabDialog) {
        val homeTabManager = LocalHomeTabManager.current
        val currentHomeTabIndex by homeTabManager.homeTabIndex.collectAsStateWithLifecycle()
        val pageCountManager = LocalPageCountManager.current
        val pageTypeManager = LocalPageTypeManager.current
        val pageTypes by pageTypeManager.pageTypes.collectAsStateWithLifecycle()

        HomeTabSelectionDialog(
            currentTabIndex = currentHomeTabIndex,
            totalPages = totalPages,
            onTabSelected = { index ->
                homeTabManager.setHomeTabIndex(index)
            },
            onDismiss = { showHomeTabDialog = false },
            onDeletePage = { pageIndex ->
                onDeletePage(pageIndex)
            },
            onAddPage = { pageType ->
                pageTypeManager.addPage(pageType)
                pageCountManager.addPage()
            },
            pageTypes = pageTypes,
            onNavigateToSearch = onNavigateToSearch
        )
    }

    if (showDrawerOptionsDialog) {
        DrawerOptionsDialog(
            onDismiss = { showDrawerOptionsDialog = false },
            onPowerClick = {
                powerViewModel.togglePower()
            },
            onTabsClick = {
                showHomeTabDialog = true
            },
            onMenuClick = {
                showAppDrawerOptionsDialog = true
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = {
                showCreateFolderDialog = true
            }
        )
    }

    if (showCreateFolderDialog) {
        CreateFolderDialog(
            apps = apps,
            onDismiss = { showCreateFolderDialog = false },
            pageIndex = pageIndex,
            allApps = allApps
        )
    }

    if (showAppDrawerOptionsDialog) {
        AppsTabOptionsDialog(
            onDismiss = { showAppDrawerOptionsDialog = false },
            onShowAppVisibility = { showAppVisibilityDialog = true },
            isFreeModeEnabled = isFreeModeEnabled,
            onToggleFreeMode = {
                appPositionManager.setFreeMode(pageIndex, !isFreeModeEnabled)
            },
            onResetPositions = {
                appPositionManager.clearAllPositions(pageIndex)
            },
            isDragLocked = isDragLocked,
            onToggleDragLock = { lockOnly ->
                appPositionManager.setDragLock(pageIndex, lockOnly ?: !isDragLocked)
            }
        )
    }

    if (showAppVisibilityDialog) {
        AppVisibilityDialog(
            apps = appsUnfiltered,
            onDismiss = { showAppVisibilityDialog = false },
            pageIndex = pageIndex
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            powerViewModel.togglePower()
                        },
                        onLongPress = {
                            showDrawerOptionsDialog = true
                        }
                    )
                },
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (apps.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (pagerState != null) {
                    val settingsIconFocusRequester = remember { FocusRequester() }
                    val menuIconFocusRequester = remember { FocusRequester() }
                    val powerSettingsManager = LocalPowerSettingsManager.current
                    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
                    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()

                    AnimatedVisibility(
                        visible = isHeaderVisible,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it })
                    ) {
                        ScreenHeaderRow(
                            totalPages = totalPages,
                            pagerState = pagerState,
                            leadingIcon = Icons.Default.Settings,
                            leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                            onLeadingIconClick = onSettingsClick,
                            leadingIconFocusRequester = settingsIconFocusRequester,
                            trailingIcon = Icons.Default.Menu,
                            trailingIconContentDescription = null,
                            onTrailingIconClick = { showAppDrawerOptionsDialog = true },
                            trailingIconFocusRequester = menuIconFocusRequester,
                            onNavigateToGrid = {},
                            onNavigateFromGrid = {
                                menuIconFocusRequester.requestFocus()
                            },
                            powerViewModel = powerViewModel,
                            showPowerButton = isPowerButtonVisible,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                            onFolderClick = onShowBottomSheet,
                            onDeletePage = onDeletePage,
                            pageIndicatorBorderColor = pageIndicatorBorderColor,
                            onNavigateToSearch = onNavigateToSearch
                        )
                    }
                }

                EmptyAppsState(
                    onAddClick = { showAppVisibilityDialog = true }
                )
            }
        } else {
            AppSelectionContent(
                apps = apps,
                columns = gridSettingsManager.columnCount,
                appFocusRequesters = appFocusRequesters,
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
                onAppDoubleClick = { app ->
                    // Launch on opposite display from current preference
                    val currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    val oppositePreference = if (currentPreference == DisplayPreference.PRIMARY_DISPLAY) {
                        DisplayPreference.CURRENT_DISPLAY
                    } else {
                        DisplayPreference.PRIMARY_DISPLAY
                    }
                    launchApp(context, app.packageName, oppositePreference)
                },
                onSettingsClick = onSettingsClick,
                powerViewModel = powerViewModel,
                totalPages = totalPages,
                pagerState = pagerState,
                onMenuClick = { showAppDrawerOptionsDialog = true },
                onShowBottomSheet = onShowBottomSheet,
                isFreeModeEnabled = isFreeModeEnabled,
                appPositionManager = appPositionManager,
                onDeletePage = onDeletePage,
                isDragLocked = isDragLocked,
                pageIndex = pageIndex,
                pageIndicatorBorderColor = pageIndicatorBorderColor,
                allApps = appsUnfiltered,
                onNavigateToSearch = onNavigateToSearch,
                folders = folders,
                onFolderClick = { folder ->
                    selectedFolder = folder
                    showFolderContentsDialog = true
                }
            )
        }
    }
    
    if (showFolderContentsDialog && selectedFolder != null) {
        val folderApps = appsUnfiltered.filter { it.packageName in selectedFolder!!.appPackageNames }
        FolderContentsDialog(
            folderName = selectedFolder!!.name,
            apps = folderApps,
            folderId = selectedFolder!!.id,
            pageIndex = pageIndex,
            allApps = appsUnfiltered,
            onDismiss = {
                showFolderContentsDialog = false
                selectedFolder = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppSelectionContent(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onAppFocusChanged: (Int) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppDoubleClick: (AppInfo) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    powerViewModel: PowerViewModel? = null,
    totalPages: Int = 1,
    pagerState: PagerState? = null,
    onMenuClick: () -> Unit = {},
    onShowBottomSheet: () -> Unit = {},
    isFreeModeEnabled: Boolean = false,
    appPositionManager: jr.brian.home.data.AppPositionManager? = null,
    onDeletePage: (Int) -> Unit = {},
    isDragLocked: Boolean = false,
    pageIndex: Int = 0,
    pageIndicatorBorderColor: Color = ThemePrimaryColor,
    allApps: List<AppInfo> = emptyList(),
    onNavigateToSearch: () -> Unit = {},
    folders: List<Folder> = emptyList(),
    onFolderClick: (Folder) -> Unit = {}
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    val rows = gridSettingsManager.rowCount
    val unlimitedMode = gridSettingsManager.unlimitedMode
    val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows

    val filteredApps = remember(apps, maxAppsPerPage) {
        apps.sortedBy { it.label.uppercase() }
    }

    val powerSettingsManager = LocalPowerSettingsManager.current
    val isPowerButtonVisible by powerSettingsManager.powerButtonVisible.collectAsStateWithLifecycle()
    val isHeaderVisible by powerSettingsManager.headerVisible.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        if (pagerState != null) {
            val settingsIconFocusRequester = remember { FocusRequester() }
            val menuIconFocusRequester = remember { FocusRequester() }

            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it })
            ) {
                ScreenHeaderRow(
                    totalPages = totalPages,
                    pagerState = pagerState,
                    leadingIcon = Icons.Default.Settings,
                    leadingIconContentDescription = stringResource(R.string.keyboard_label_settings),
                    onLeadingIconClick = onSettingsClick,
                    leadingIconFocusRequester = settingsIconFocusRequester,
                    trailingIcon = Icons.Default.Menu,
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
                    onFolderClick = onShowBottomSheet,
                    onDeletePage = onDeletePage,
                    pageIndicatorBorderColor = pageIndicatorBorderColor,
                    onNavigateToSearch = onNavigateToSearch
                )
            }
        }

        if (isFreeModeEnabled && appPositionManager != null) {
            FreePositionedAppsLayout(
                apps = filteredApps,
                appPositionManager = appPositionManager,
                keyboardVisible = false,
                onAppClick = onAppClick,
                isDragLocked = isDragLocked,
                pageIndex = pageIndex,
                allApps = allApps,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            )
        } else {
            AppGridLayout(
                apps = filteredApps,
                columns = columns,
                maxAppsPerPage = maxAppsPerPage,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                appFocusRequesters = appFocusRequesters,
                onFocusChanged = onAppFocusChanged,
                onNavigateLeft = {},
                onAppClick = onAppClick,
                onAppLongClick = onAppLongClick,
                onAppDoubleClick = onAppDoubleClick,
                folders = folders,
                allApps = allApps,
                onFolderClick = onFolderClick,
                isHeaderVisible = isHeaderVisible
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGridLayout(
    columns: Int,
    maxAppsPerPage: Int,
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onFocusChanged: (Int) -> Unit = {},
    onNavigateLeft: () -> Unit = {},
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppDoubleClick: (AppInfo) -> Unit = {},
    folders: List<Folder> = emptyList(),
    allApps: List<AppInfo> = emptyList(),
    onFolderClick: (Folder) -> Unit = {},
    isHeaderVisible: Boolean,
    horizontalSpacing: Dp = 32.dp,
    verticalSpacing: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = 8.dp,
        vertical = if (isHeaderVisible) 8.dp else 20.dp,
    ),
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
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        // Render apps first
        items(displayedApps.size) { index ->
            val app = displayedApps[index]
            val itemFocusRequester =
                remember(index) {
                    FocusRequester().also { appFocusRequesters[index] = it }
                }

            AppGridItem(
                app = app,
                focusRequester = itemFocusRequester,
                onClick = { onAppClick(app) },
                onLongClick = { onAppLongClick(app) },
                onDoubleClick = { onAppDoubleClick(app) },
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
        
        // Render folders after apps
        items(folders.size) { index ->
            val folder = folders[index]
            val folderApps = allApps.filter { it.packageName in folder.appPackageNames }
            FolderGridItem(
                folder = folder,
                apps = folderApps,
                onClick = { onFolderClick(folder) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}