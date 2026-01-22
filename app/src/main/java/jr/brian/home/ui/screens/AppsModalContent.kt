package jr.brian.home.ui.screens

import android.content.Context
import android.hardware.display.DisplayManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.apps.AppGridItem
import jr.brian.home.ui.components.apps.AppOptionsMenu
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.util.launchApp
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsModalContent(
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean = false,
    onShowBottomSheet: () -> Unit = {},
    allApps: List<AppInfo> = emptyList(),
    pageIndex: Int,
    isHeaderVisible: Boolean
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appPositionManager = LocalAppPositionManager.current
    val folderManager = LocalFolderManager.current

    val folders by folderManager.getFolders(pageIndex)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val dragLockedByPage by appPositionManager.isDragLockedByPage.collectAsStateWithLifecycle()
    val isDragLocked = dragLockedByPage[pageIndex] ?: true

    LaunchedEffect(pageIndex) {
        appPositionManager.setDragLock(pageIndex, true)
    }

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.displays.size > 1
    }

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var showAppOptionsMenu by remember { mutableStateOf(false) }
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }
    var showCustomIconDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showFolderContentsDialog by remember { mutableStateOf(false) }
    var selectedFolder by remember { mutableStateOf<Folder?>(null) }

    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var savedAppIndex by remember { mutableIntStateOf(0) }

    if (showAppOptionsMenu && selectedApp != null) {
        val currentIconSize = 64f

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
            app = null,
            currentIconSize = currentIconSize,
            onIconSizeChange = {},
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

    if (showDrawerOptionsDialog) {
        DrawerOptionsDialog(
            onDismiss = { showDrawerOptionsDialog = false },
            onPowerClick = {},
            onTabsClick = {},
            onMenuClick = {
                showAppDrawerOptionsDialog = true
            },
            onSettingsClick = {},
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
            isFreeModeEnabled = false,
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
            EmptyAppsState(
                onAddClick = { showAppVisibilityDialog = true }
            )
        } else {
            ModalAppSelectionContent(
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
                    val currentPreference =
                        appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    val oppositePreference =
                        if (currentPreference == DisplayPreference.PRIMARY_DISPLAY) {
                            DisplayPreference.CURRENT_DISPLAY
                        } else {
                            DisplayPreference.PRIMARY_DISPLAY
                        }
                    launchApp(context, app.packageName, oppositePreference)
                },
                allApps = appsUnfiltered,
                folders = folders,
                onFolderClick = { folder ->
                    selectedFolder = folder
                    showFolderContentsDialog = true
                },
                isHeaderVisible = isHeaderVisible
            )
        }
    }

    if (showFolderContentsDialog && selectedFolder != null) {
        val folderApps =
            appsUnfiltered.filter { it.packageName in selectedFolder!!.appPackageNames }
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
private fun ModalAppSelectionContent(
    apps: List<AppInfo>,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onAppFocusChanged: (Int) -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit = {},
    onAppDoubleClick: (AppInfo) -> Unit = {},
    allApps: List<AppInfo> = emptyList(),
    folders: List<Folder> = emptyList(),
    onFolderClick: (Folder) -> Unit = {},
    isHeaderVisible: Boolean
) {
    val gridSettingsManager = LocalGridSettingsManager.current
    val rows = gridSettingsManager.rowCount
    val unlimitedMode = gridSettingsManager.unlimitedMode
    val maxAppsPerPage = if (unlimitedMode) Int.MAX_VALUE else columns * rows

    val filteredApps = remember(apps, maxAppsPerPage) {
        apps.sortedBy { it.label.uppercase() }
    }

    AppGridLayout(
        apps = filteredApps,
        columns = columns,
        maxAppsPerPage = maxAppsPerPage,
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp,
            ),
        appFocusRequesters = appFocusRequesters,
        onFocusChanged = onAppFocusChanged,
        onNavigateLeft = {},
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
        onAppDoubleClick = onAppDoubleClick,
        folders = folders,
        allApps = allApps,
        onFolderClick = onFolderClick,
        isHeaderVisible = isHeaderVisible,
        contentPadding = PaddingValues(
            top = 30.dp,
            bottom = 20.dp,
        ),
        horizontalSpacing = 10.dp,
        verticalSpacing = 24.dp
    )
}