package jr.brian.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.esde.preferences.LocalESDEPreferencesManager
import jr.brian.home.esde.setup.SetupStep
import jr.brian.home.esde.ui.ESDESetupScreen
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.AppPosition
import jr.brian.home.model.app.Folder
import jr.brian.home.ui.animations.onPressScaleAndOffset
import jr.brian.home.ui.components.apps.AppOptionsMenu
import jr.brian.home.ui.components.apps.AppVisibilityDialog
import jr.brian.home.ui.components.apps.AppsTabContent
import jr.brian.home.ui.components.dialog.AppsTabOptionsDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.DockAppSelectionDialog
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.dock.AppDock
import jr.brian.home.ui.extensions.pagerFriendlyClickable
import jr.brian.home.ui.extensions.pagerFriendlyClickableSimple
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalDockManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalTabAnimationManager
import jr.brian.home.ui.theme.managers.LocalWallpaperManager
import jr.brian.home.ui.theme.managers.WallpaperType
import jr.brian.home.ui.util.rememberBottomFlingTrigger
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.ui.util.rememberFocusRequesterMap
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.util.openAppInfo
import jr.brian.home.viewmodels.PowerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

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
    onNavigateToSearch: () -> Unit = {},
    onNavigateToDockSettings: () -> Unit = {},
    onDockPositioned: (Float) -> Unit = {},
    onShowAppDrawer: () -> Unit = {},
    onScrollStateChanged: (isScrolling: Boolean, hasScrollableContent: Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appPositionManager = LocalAppPositionManager.current
    val folderManager = LocalFolderManager.current
    val dockManager = LocalDockManager.current
    val tabAnimationManager = LocalTabAnimationManager.current
    val esdePrefsManager = LocalESDEPreferencesManager.current
    
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    val isPoweredOff by powerViewModel.isPoweredOff.collectAsStateWithLifecycle()
    val folders by folderManager.getFolders(pageIndex)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val freeModeByPage by appPositionManager.isFreeModeByPage.collectAsStateWithLifecycle()
    val isFreeModeEnabled = freeModeByPage[pageIndex] ?: false

    val dragLockedByPage by appPositionManager.isDragLockedByPage.collectAsStateWithLifecycle()
    val isDragLocked = dragLockedByPage[pageIndex] ?: true

    LaunchedEffect(pageIndex) {
        appPositionManager.setDragLock(pageIndex, true)
    }

    BackHandler(enabled = isPoweredOff) {}

    val hasExternalDisplay = rememberHasExternalDisplay()

    val appOptionsDialogState = rememberDialogState<AppInfo>()
    val customIconDialogState = rememberDialogState<AppInfo>()
    val folderContentsDialogState = rememberDialogState<Folder>()
    val drawerOptionsDialogState = rememberDialogState<Unit>()
    val appDrawerOptionsDialogState = rememberDialogState<Unit>()
    val appVisibilityDialogState = rememberDialogState<Unit>()
    val homeTabDialogState = rememberDialogState<Unit>()
    val createFolderDialogState = rememberDialogState<Unit>()
    val dockAppSelectionDialogState = rememberDialogState<Int>()
    val esdeSetupDialogState = rememberDialogState<SetupStep>()
    val wallpaperManager = LocalWallpaperManager.current

    val appFocusRequesters = rememberFocusRequesterMap()
    var savedAppIndex by remember { mutableIntStateOf(0) }

    val isTabAnimationEnabled = tabAnimationManager.isTabAnimationEnabled
    val interactionSource = remember { MutableInteractionSource() }
    val isPressedState = remember { mutableStateOf(false) }
    val (pressScale, offsetY) = onPressScaleAndOffset(
        isTabAnimationEnabled && isPressedState.value && !drawerOptionsDialogState.isVisible
    )

    val gridState = rememberLazyGridState()
    var isScrolling by remember { mutableStateOf(false) }

    val hasScrollableContent by remember {
        derivedStateOf {
            gridState.canScrollForward || gridState.canScrollBackward
        }
    }

    val bottomFlingTrigger = rememberBottomFlingTrigger(
        gridState = gridState,
        onFlingAtBottom = onShowAppDrawer
    )

    LaunchedEffect(isScrolling, hasScrollableContent) {
        onScrollStateChanged(isScrolling, hasScrollableContent)
    }

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (scrolling) {
                    isScrolling = true
                } else {
                    delay(300)
                    isScrolling = false
                }
            }
    }

    appOptionsDialogState.item?.let { appInfo ->
        if (appOptionsDialogState.isVisible) {
            val currentIconSize = if (isFreeModeEnabled) {
                appPositionManager.getPosition(pageIndex, appInfo.packageName)?.iconSize ?: 64f
            } else {
                64f
            }

            val appVisibilityManager = LocalAppVisibilityManager.current
            val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
            val isAppHidden = remember(appInfo, pageIndex, hiddenAppsByPage) {
                appVisibilityManager.isAppHidden(pageIndex, appInfo.packageName)
            }

            val isInDock = dockManager.isAppInDock(appInfo.packageName)

            AppOptionsMenu(
                appLabel = appInfo.label,
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
                app = if (isFreeModeEnabled) appInfo else null,
                currentIconSize = currentIconSize,
                isInDock = isInDock,
                onRemoveFromDock = {
                    dockManager.removeAppFromDock(appInfo.packageName)
                },
                onIconSizeChange = { newSize ->
                    if (isFreeModeEnabled) {
                        val currentPos = appPositionManager.getPosition(
                            pageIndex,
                            appInfo.packageName
                        )
                        appPositionManager.savePosition(
                            pageIndex,
                            AppPosition(
                                packageName = appInfo.packageName,
                                x = currentPos?.x ?: 0f,
                                y = currentPos?.y ?: 0f,
                                iconSize = newSize
                            )
                        )
                    }
                },
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
                }
            )
        }
    }

    customIconDialogState.item?.let { appInfo ->
        if (customIconDialogState.isVisible) {
            CustomIconDialog(
                packageName = appInfo.packageName,
                appLabel = appInfo.label,
                onDismiss = customIconDialogState::dismiss
            )
        }
    }

    if (homeTabDialogState.isVisible) {
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
            onDismiss = homeTabDialogState::dismiss,
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

    if (drawerOptionsDialogState.isVisible) {
        DrawerOptionsDialog(
            onDismiss = drawerOptionsDialogState::dismiss,
            onPowerClick = {
                powerViewModel.togglePower()
            },
            onTabsClick = {
                homeTabDialogState.show()
            },
            onMenuClick = {
                appDrawerOptionsDialogState.show()
            },
            onSettingsClick = onSettingsClick,
            onQuickDeleteClick = onShowBottomSheet,
            onCreateFolderClick = {
                createFolderDialogState.show()
            },
            onDockSettingsClick = onNavigateToDockSettings,
            onESDESetupClick = {
                esdeSetupDialogState.show(SetupStep.Welcome)
            }
        )
    }

    ESDESetupScreen(
        dialogState = esdeSetupDialogState,
        onDismiss = { },
        onSetupComplete = {
            wallpaperManager.setESDE()
        }
    )

    if (createFolderDialogState.isVisible) {
        CreateFolderDialog(
            apps = apps,
            onDismiss = createFolderDialogState::dismiss,
            pageIndex = pageIndex,
            allApps = allApps
        )
    }

    if (appDrawerOptionsDialogState.isVisible) {
        val isEsdeMode = wallpaperManager.getWallpaperType() == WallpaperType.ESDE
        
        AppsTabOptionsDialog(
            onDismiss = appDrawerOptionsDialogState::dismiss,
            onShowAppVisibility = { appVisibilityDialogState.show() },
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
            },
            isMarqueePositionLocked = esdePrefsState.marqueePositionLocked,
            onToggleMarqueePositionLock = if (isEsdeMode) {
                { esdePrefsManager.toggleMarqueePositionLocked() }
            } else null
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
            Modifier
                .fillMaxSize()
                .nestedScroll(bottomFlingTrigger)
                .windowInsetsPadding(WindowInsets.statusBars)
                .offset(y = offsetY)
                .scale(pressScale)
                .then(
                    if (isTabAnimationEnabled) {
                        Modifier.pagerFriendlyClickable(
                            isFreeModeEnabled, isDragLocked,
                            interactionSource = interactionSource,
                            isPressedState = isPressedState,
                            onDoubleTap = { powerViewModel.togglePower() },
                            onLongPress = {
                                if (isFreeModeEnabled && !isDragLocked) {
                                    appDrawerOptionsDialogState.show()
                                } else {
                                    drawerOptionsDialogState.show()
                                }
                            }
                        )
                    } else {
                        Modifier.pagerFriendlyClickableSimple(
                            onDoubleTap = { powerViewModel.togglePower() },
                            onLongPress = {
                                if (isFreeModeEnabled && !isDragLocked) {
                                    appDrawerOptionsDialogState.show()
                                } else {
                                    drawerOptionsDialogState.show()
                                }
                            }
                        )
                    }
                ),
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppsTabContent(
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
                    appOptionsDialogState.show(app)
                },
                onAppDoubleClick = { app ->
                    launchAppOnOppositeDisplay(
                        context = context,
                        packageName = app.packageName,
                        currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    )
                },
                onSettingsClick = onSettingsClick,
                powerViewModel = powerViewModel,
                totalPages = totalPages,
                pagerState = pagerState,
                onMenuClick = { appDrawerOptionsDialogState.show() },
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
                onFolderClick = folderContentsDialogState::show,
                gridState = gridState
            )
        }

        val isDockVisible by dockManager.isDockVisible.collectAsStateWithLifecycle()
        val isDockVisibleOnPage = dockManager.isDockVisibleOnPage(pageIndex)

        AnimatedVisibility(
            visible = isDockVisible && isDockVisibleOnPage && !isScrolling,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            AppDock(
                apps = appsUnfiltered,
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
                onAppDoubleClick = { app ->
                    launchAppOnOppositeDisplay(
                        context = context,
                        packageName = app.packageName,
                        currentPreference = appDisplayPreferenceManager.getAppDisplayPreference(app.packageName)
                    )
                },
                onAppLongClick = { app ->
                    appOptionsDialogState.show(app)
                },
                onEmptySlotClick = { position ->
                    dockAppSelectionDialogState.show(position)
                },
                onEmptySlotLongClick = { position ->
                    dockManager.removeEmptySlot(position)
                },
                onDockPositioned = onDockPositioned
            )
        }

        }

    dockAppSelectionDialogState.item?.let { position ->
        if (dockAppSelectionDialogState.isVisible) {
            val availableApps = appsUnfiltered.filter { app ->
                !dockManager.isAppInDock(app.packageName)
            }
            DockAppSelectionDialog(
                apps = availableApps,
                onAppSelected = { app ->
                    dockManager.addAppToDock(app.packageName, position)
                    dockAppSelectionDialogState.dismiss()
                },
                onDismiss = dockAppSelectionDialogState::dismiss
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
