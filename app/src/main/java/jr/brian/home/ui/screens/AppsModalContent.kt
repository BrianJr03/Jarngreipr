package jr.brian.home.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import jr.brian.home.ui.components.apps.FloatyObstacleSpec
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
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppsModalContent(
    modifier: Modifier = Modifier,
    apps: List<AppInfo>,
    appsUnfiltered: List<AppInfo>,
    isLoading: Boolean = false,
    showHideAppButton: Boolean = true,
    allApps: List<AppInfo> = emptyList(),
    pageIndex: Int,
    isHeaderVisible: Boolean,
    forceFloatyMode: Boolean = false,
    onGameInProgressChanged: (Boolean) -> Unit = {},
    onCloseRequested: () -> Unit = {},
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
                    return available
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
                    return available
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }
    val isModalFloatyEnabled =
        floatyModeManager.isFloatyModeActive && floatyModeManager.isAppsModalFloatyEffectEnabled
    val modalFloatyCount = floatyModeManager.appDrawerFloatyAppCount
    val isBubblePopEnabled = isModalFloatyEnabled && floatyModeManager.isAppDrawerBubblePopEnabled
    val limitedApps = if (isModalFloatyEnabled && modalFloatyCount > 0) {
        apps.take(modalFloatyCount)
    } else {
        apps
    }
    var poppedPackages by remember(pageIndex) { mutableStateOf<Set<String>>(emptySet()) }
    val maxGameLevel = 3
    var currentGameLevel by remember(pageIndex, isBubblePopEnabled) { mutableIntStateOf(1) }
    var countdownSeconds by remember(pageIndex, isBubblePopEnabled) { mutableIntStateOf(0) }
    var isCountdownRunning by remember(pageIndex, isBubblePopEnabled) { mutableStateOf(false) }
    var showWinner by remember(pageIndex, isBubblePopEnabled) { mutableStateOf(false) }
    var showTimeUp by remember(pageIndex, isBubblePopEnabled) { mutableStateOf(false) }
    var isGameInProgress by remember(pageIndex, isBubblePopEnabled) { mutableStateOf(false) }
    LaunchedEffect(isCountdownRunning, currentGameLevel, limitedApps.size, isBubblePopEnabled) {
        if (!isBubblePopEnabled || !isCountdownRunning || limitedApps.isEmpty()) return@LaunchedEffect
        if (countdownSeconds <= 0) {
            countdownSeconds =
                (limitedApps.size * levelTimeMultiplier(currentGameLevel)).roundToInt()
                    .coerceAtLeast(1)
        }
        while (isCountdownRunning && countdownSeconds > 0) {
            delay(1000)
            countdownSeconds -= 1
        }
        if (isCountdownRunning) {
            isCountdownRunning = false
            showTimeUp = true
            isGameInProgress = false
            poppedPackages = emptySet()
        }
    }
    LaunchedEffect(limitedApps, isBubblePopEnabled, pageIndex) {
        if (!isBubblePopEnabled) {
            poppedPackages = emptySet()
            currentGameLevel = 1
            countdownSeconds = 0
            isCountdownRunning = false
            showWinner = false
            showTimeUp = false
            isGameInProgress = false
        } else {
            poppedPackages = poppedPackages.filter { packageName ->
                limitedApps.any { it.packageName == packageName }
            }.toSet()
            if (limitedApps.isEmpty()) {
                isCountdownRunning = false
                countdownSeconds = 0
            }
        }
    }
    val displayedApps = if (isBubblePopEnabled) {
        limitedApps.filter { it.packageName !in poppedPackages }
    } else {
        limitedApps
    }
    val gameTopBarObstacleSpec = FloatyObstacleSpec(
        width = 0.dp,
        height = 56.dp,
        topPadding = 8.dp,
        endPadding = 0.dp,
        fullWidth = true,
        horizontalPadding = 8.dp
    )

    appOptionsDialogState.item?.let { appInfo ->
        if (appOptionsDialogState.isVisible) {
            val currentIconSize = 64f

            val appVisibilityManager = LocalAppVisibilityManager.current
            val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
            val isAppHidden = remember(appInfo, pageIndex, hiddenAppsByPage) {
                appVisibilityManager.isAppHidden(pageIndex, appInfo.packageName)
            }

            val onToggleVisibility = {
                if (isAppHidden) {
                    appVisibilityManager.showApp(pageIndex, appInfo.packageName)
                } else {
                    appVisibilityManager.hideApp(pageIndex, appInfo.packageName)
                }
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
                onToggleVisibility = if (showHideAppButton) onToggleVisibility else null,
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
        } else if (apps.isEmpty() && folders.isEmpty()) {
            EmptyAppsState(
                onAddClick = { appVisibilityDialogState.show() }
            )
        } else {
            AppsContentLayout(
                apps = displayedApps,
                appsUnfiltered = appsUnfiltered,
                appPositionManager = appPositionManager,
                forceFloatyMode = forceFloatyMode || isModalFloatyEnabled,
                hasExternalDisplay = hasExternalDisplay,
                appDisplayPreferenceManager = appDisplayPreferenceManager,
                pageIndex = pageIndex,
                isDragLocked = isDragLocked,
                columns = gridSettingsManager.columnCount,
                appFocusRequesters = appFocusRequesters,
                onAppFocusChanged = {},
                onAppOpened = onAppOpened,
                onAppLongClick = appOptionsDialogState::show,
                bubblePopEnabled = isBubblePopEnabled,
                floatyObstacleSpec = if (isCountdownRunning) gameTopBarObstacleSpec else null,
                onBubblePop = { appInfo ->
                    if (!isBubblePopEnabled || appInfo.packageName in poppedPackages) return@AppsContentLayout
                    if (showWinner || showTimeUp) return@AppsContentLayout
                    if (!isCountdownRunning) {
                        countdownSeconds =
                            (limitedApps.size * levelTimeMultiplier(currentGameLevel)).roundToInt()
                                .coerceAtLeast(1)
                        isCountdownRunning = true
                        showTimeUp = false
                        isGameInProgress = true
                    }
                    val updated = poppedPackages + appInfo.packageName
                    poppedPackages = updated
                    if (limitedApps.isNotEmpty() && updated.size >= limitedApps.size) {
                        isCountdownRunning = false
                        countdownSeconds = 0
                        if (currentGameLevel >= maxGameLevel) {
                            showWinner = true
                            isGameInProgress = false
                        } else {
                            currentGameLevel += 1
                            scope.launch {
                                delay(220)
                                poppedPackages = emptySet()
                            }
                        }
                    }
                },
                folders = folders,
                onFolderClick = folderContentsDialogState::show,
                isHeaderVisible = isHeaderVisible,
                gridState = gridState,
                nestedScrollConnection = nestedScrollConnection
            )
        }
        FloatyGameOverlay(
            enabled = isBubblePopEnabled,
            showWinner = showWinner,
            showTimeUp = showTimeUp,
            onRestart = {
                currentGameLevel = 1
                countdownSeconds = 0
                isCountdownRunning = false
                showWinner = false
                showTimeUp = false
                isGameInProgress = false
                poppedPackages = emptySet()
            }
        )
        AnimatedVisibility(isBubblePopEnabled && isCountdownRunning) {
            GameTopBar(
                level = currentGameLevel,
                secondsRemaining = countdownSeconds,
                onClose = onCloseRequested
            )
        }
    }
    LaunchedEffect(isBubblePopEnabled, isGameInProgress, showWinner, showTimeUp) {
        onGameInProgressChanged(isBubblePopEnabled && isGameInProgress && !showWinner && !showTimeUp)
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
                backgroundColorArgb = folder.backgroundColorArgb,
                backgroundImagePath = folder.backgroundImagePath,
                onDismiss = folderContentsDialogState::dismiss
            )
        }
    }
}

@Composable
private fun GameTopBar(
    level: Int,
    secondsRemaining: Int,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "L$level: ${secondsRemaining}s",
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close App Drawer",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun FloatyGameOverlay(
    enabled: Boolean,
    showWinner: Boolean,
    showTimeUp: Boolean,
    onRestart: () -> Unit
) {
    if (!enabled) return
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (showWinner || showTimeUp) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (showWinner) "Winner!" else "Time's up!",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onRestart) {
                    Text("Restart")
                }
            }
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
    bubblePopEnabled: Boolean,
    floatyObstacleSpec: FloatyObstacleSpec?,
    onBubblePop: (AppInfo) -> Unit,
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
            allApps = appsUnfiltered,
            bubblePopEnabled = bubblePopEnabled,
            floatyObstacleSpec = floatyObstacleSpec,
            onBubblePop = onBubblePop
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

private fun levelTimeMultiplier(level: Int): Float = when (level) {
    1 -> 1f
    2 -> 0.7f
    else -> 0.5f
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
            bottom = 16.dp,
        ),
        equalizeMargins = true,
        verticalSpacing = 24.dp
    )
}