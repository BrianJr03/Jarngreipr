package jr.brian.home.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.AppDisplayPreferenceManager
import jr.brian.home.data.AppPositionManager
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.esde.data.LocalESDEPreferencesManager
import jr.brian.home.esde.ui.components.SyncLogoPositionLock
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.components.apps.AppOptionsMenu
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.apps.FreePositionedAppsLayout
import jr.brian.home.ui.components.dialog.AppDrawerOptionsDialog
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.components.dialog.RenameAppDialog
import jr.brian.home.ui.components.settings.displayName
import jr.brian.home.ui.components.widget.AppGridLayout
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalFloatyModeManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberFocusRequesterMap
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.util.openAppInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsModalContent(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean = false,
    allApps: List<AppInfo> = emptyList(),
    pageIndex: Int,
    isHeaderVisible: Boolean,
    forceFloatyMode: Boolean = false,
    onAppOpened: () -> Unit
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appPositionManager = LocalAppPositionManager.current
    val folderManager = LocalFolderManager.current
    val esdePrefsManager = LocalESDEPreferencesManager.current
    val floatyModeManager = LocalFloatyModeManager.current
    
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    SyncLogoPositionLock(esdePrefsState, esdePrefsManager)
    val folders by folderManager.getFolders(pageIndex)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val dragLockedByPage by appPositionManager.isDragLockedByPage.collectAsStateWithLifecycle()
    val isDragLocked = dragLockedByPage[pageIndex] ?: true

    LaunchedEffect(pageIndex) {
        appPositionManager.setDragLock(pageIndex, true)
    }

    val hasExternalDisplay = rememberHasExternalDisplay()

    val appOptionsDialogState = rememberDialogState<AppInfo>()
    val customIconDialogState = rememberDialogState<AppInfo>()
    val renameDialogState = rememberDialogState<AppInfo>()
    val folderContentsDialogState = rememberDialogState<Folder>()
    val drawerOptionsDialogState = rememberDialogState<Unit>()
    val appDrawerOptionsDialogState = rememberDialogState<Unit>()
    val appVisibilityDialogState = rememberDialogState<Unit>()
    val createFolderDialogState = rememberDialogState<Unit>()

    val appFocusRequesters = rememberFocusRequesterMap()
    var savedAppIndex by remember { mutableIntStateOf(0) }

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val preventNestedScroll = remember { mutableStateOf(false) }
    val scheduledJob = remember { mutableStateOf<Job?>(null) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0) {
                    val firstIndex = gridState.firstVisibleItemIndex
                    if (firstIndex == 0 && gridState.firstVisibleItemScrollOffset == 0) {
                        return super.onPreScroll(available, source)
                    }

                    preventNestedScroll.value = true
                }
                return super.onPreScroll(available, source)
            }

            override suspend fun onPostFling(
                consumed: Velocity,
                available: Velocity
            ): Velocity {
                if (preventNestedScroll.value) {
                    return available.times(100f)
                }
                return super.onPostFling(consumed, available)
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {

                if (preventNestedScroll.value) {
                    scheduledJob.value?.cancel()
                    scheduledJob.value = scope.launch {
                        delay(300)
                        preventNestedScroll.value = false
                        scheduledJob.value = null
                    }
                    return available.times(100f)
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }
    val isModalFloatyEnabled =
        floatyModeManager.isFloatyModeActive && floatyModeManager.isAppsModalFloatyEffectEnabled

    appOptionsDialogState.item?.let { appInfo ->
        if (appOptionsDialogState.isVisible) {
            val currentIconSize = 64f

            val appVisibilityManager = LocalAppVisibilityManager.current
            val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
            val isAppHidden = remember(appInfo, pageIndex, hiddenAppsByPage) {
                appVisibilityManager.isAppHidden(pageIndex, appInfo.packageName)
            }

            AppOptionsMenu(
                appLabel = appInfo.displayName(),
                currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                    appInfo.packageName
                ),
                onDismiss = appOptionsDialogState::dismiss,
                onAppInfoClick = {
                    openAppInfo(context, appInfo.packageName)
                },
                onDisplayPreferenceChange = { preference ->
                    appDisplayPreferenceManager.setAppDisplayPreference(
                        appInfo.packageName,
                        preference
                    )
                },
                app = null,
                currentIconSize = currentIconSize,
                onIconSizeChange = {},
                onToggleVisibility = {
                    if (isAppHidden) {
                        appVisibilityManager.showApp(pageIndex, appInfo.packageName)
                    } else {
                        appVisibilityManager.hideApp(pageIndex, appInfo.packageName)
                    }
                },
                onCustomIconClick = {
                    customIconDialogState.show(appInfo)
                    appOptionsDialogState.dismiss()
                },
                onRenameClick = {
                    renameDialogState.show(appInfo)
                    appOptionsDialogState.dismiss()
                }
            )
        }
    }

    customIconDialogState.item?.let { appInfo ->
        if (customIconDialogState.isVisible) {
            CustomIconDialog(
                packageName = appInfo.packageName,
                appLabel = appInfo.displayName(),
                onDismiss = customIconDialogState::dismiss
            )
        }
    }

    renameDialogState.item?.let { appInfo ->
        if (renameDialogState.isVisible) {
            RenameAppDialog(
                packageName = appInfo.packageName,
                appLabel = appInfo.label,
                onDismiss = renameDialogState::dismiss
            )
        }
    }

    if (drawerOptionsDialogState.isVisible) {
        AppDrawerOptionsDialog(
            onDismiss = drawerOptionsDialogState::dismiss,
            onCreateFolderClick = {
                createFolderDialogState.show()
            },
        )
    }

    if (createFolderDialogState.isVisible) {
        CreateFolderDialog(
            apps = apps,
            onDismiss = createFolderDialogState::dismiss,
            pageIndex = pageIndex,
            allApps = allApps
        )
    }

    if (appDrawerOptionsDialogState.isVisible) {
        AppsTabOptionsDialog(
            onDismiss = appDrawerOptionsDialogState::dismiss,
            onShowAppVisibility = { appVisibilityDialogState.show() },
            isFreeModeEnabled = false,
            onResetPositions = {
                appPositionManager.clearAllPositions(pageIndex)
            },
            isDragLocked = isDragLocked,
            onToggleDragLock = { lockOnly ->
                appPositionManager.setDragLock(pageIndex, lockOnly ?: !isDragLocked)
            },
            isLogoPositionLocked = esdePrefsState.marqueePositionLocked,
            onToggleMarqueePositionLock = { esdePrefsManager.toggleLogoPositionLocked() }
        )
    }

    if (appVisibilityDialogState.isVisible) {
        AppVisibilityDialog(
            apps = appsUnfiltered,
            onDismiss = appVisibilityDialogState::dismiss,
            pageIndex = pageIndex
        )
    }

    Box(
        modifier =
            modifier
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            drawerOptionsDialogState.show()
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
                onAddClick = { appVisibilityDialogState.show() }
            )
        } else {
            AppsContentLayout(
                apps = apps,
                appsUnfiltered = appsUnfiltered,
                appPositionManager = appPositionManager,
                forceFloatyMode = forceFloatyMode || isModalFloatyEnabled,
                hasExternalDisplay = hasExternalDisplay,
                appDisplayPreferenceManager = appDisplayPreferenceManager,
                pageIndex = pageIndex,
                isDragLocked = isDragLocked,
                columns = gridSettingsManager.columnCount,
                appFocusRequesters = appFocusRequesters,
                onAppFocusChanged = { savedAppIndex = it },
                onAppOpened = onAppOpened,
                onAppLongClick = appOptionsDialogState::show,
                folders = folders,
                onFolderClick = folderContentsDialogState::show,
                isHeaderVisible = isHeaderVisible,
                gridState = gridState,
                nestedScrollConnection = nestedScrollConnection
            )
        }
    }

    folderContentsDialogState.item?.let { folder ->
        if (folderContentsDialogState.isVisible) {
            val folderApps =
                appsUnfiltered.filter { it.packageName in folder.appPackageNames }
            FolderContentsDialog(
                folderName = folder.name,
                apps = folderApps,
                folderId = folder.id,
                pageIndex = pageIndex,
                allApps = appsUnfiltered,
                onDismiss = folderContentsDialogState::dismiss
            )
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppsContentLayout(
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    appPositionManager: AppPositionManager,
    forceFloatyMode: Boolean,
    hasExternalDisplay: Boolean,
    appDisplayPreferenceManager: AppDisplayPreferenceManager,
    pageIndex: Int,
    isDragLocked: Boolean,
    columns: Int,
    appFocusRequesters: SnapshotStateMap<Int, FocusRequester>,
    onAppFocusChanged: (Int) -> Unit,
    onAppOpened: () -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    isHeaderVisible: Boolean,
    gridState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    if (forceFloatyMode) {
        FreePositionedAppsLayout(
            apps = apps,
            appPositionManager = appPositionManager,
            keyboardVisible = false,
            onAppClick = { app ->
                onAppOpened()
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
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            pageIndex = pageIndex,
            isDragLocked = isDragLocked,
            forceFloatyMode = true,
            allApps = appsUnfiltered
        )
    } else {
        ModalAppSelectionContent(
            apps = apps,
            columns = columns,
            appFocusRequesters = appFocusRequesters,
            onAppFocusChanged = onAppFocusChanged,
            onAppClick = { app ->
                onAppOpened()
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
                scope.launch {
                    gridState.scrollToItem(0)
                }
            },
            onAppLongClick = onAppLongClick,
            onAppDoubleClick = { app ->
                onAppOpened()
                launchAppOnOppositeDisplay(
                    context = context,
                    packageName = app.packageName,
                    currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                )
                scope.launch {
                    gridState.scrollToItem(0)
                }
            },
            allApps = appsUnfiltered,
            folders = folders,
            onFolderClick = onFolderClick,
            isHeaderVisible = isHeaderVisible,
            gridState = gridState,
            nestedScrollConnection = nestedScrollConnection
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
    isHeaderVisible: Boolean,
    gridState: LazyGridState,
    nestedScrollConnection: NestedScrollConnection
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
        gridState = gridState,
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = 20.dp,
            )
            .nestedScroll(nestedScrollConnection),
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
            top = 10.dp,
            bottom = 20.dp,
        ),
        horizontalSpacing = 10.dp,
        verticalSpacing = 24.dp
    )
}