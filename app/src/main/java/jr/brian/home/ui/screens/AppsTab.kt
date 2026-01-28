package jr.brian.home.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jr.brian.home.R
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
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
import jr.brian.home.ui.components.dialog.DrawerOptionsDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.components.dialog.HomeTabSelectionDialog
import jr.brian.home.ui.components.header.ScreenHeaderRow
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalAppPositionManager
import jr.brian.home.ui.theme.managers.LocalAppVisibilityManager
import jr.brian.home.ui.theme.managers.LocalFolderManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalHomeTabManager
import jr.brian.home.ui.theme.managers.LocalPageCountManager
import jr.brian.home.ui.theme.managers.LocalPageTypeManager
import jr.brian.home.ui.theme.managers.LocalPowerSettingsManager
import jr.brian.home.ui.util.rememberDialogState
import jr.brian.home.util.launchApp
import jr.brian.home.util.openAppInfo
import jr.brian.home.viewmodels.PowerViewModel

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
    onNavigateToRecentApps: () -> Unit = {}
) {
    val context = LocalContext.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val appPositionManager = LocalAppPositionManager.current
    val folderManager = LocalFolderManager.current

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

    val hasExternalDisplay = remember {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
        displayManager.displays.size > 1
    }

    val appOptionsDialogState = rememberDialogState<AppInfo>()
    val customIconDialogState = rememberDialogState<AppInfo>()
    val folderContentsDialogState = rememberDialogState<Folder>()
    var showDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppDrawerOptionsDialog by remember { mutableStateOf(false) }
    var showAppVisibilityDialog by remember { mutableStateOf(false) }
    var showHomeTabDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }

    val appFocusRequesters = remember { mutableStateMapOf<Int, FocusRequester>() }
    var savedAppIndex by remember { mutableIntStateOf(0) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val (pressScale, offsetY) = onPressScaleAndOffset(isPressed && !showDrawerOptionsDialog)

    if (appOptionsDialogState.isVisible && appOptionsDialogState.item != null) {
        val currentIconSize = if (isFreeModeEnabled) {
            appPositionManager.getPosition(pageIndex, appOptionsDialogState.item!!.packageName)?.iconSize ?: 64f
        } else {
            64f
        }

        val appVisibilityManager = LocalAppVisibilityManager.current
        val hiddenAppsByPage by appVisibilityManager.hiddenAppsByPage.collectAsStateWithLifecycle()
        val isAppHidden = remember(appOptionsDialogState.item, pageIndex, hiddenAppsByPage) {
            appVisibilityManager.isAppHidden(pageIndex, appOptionsDialogState.item!!.packageName)
        }

        AppOptionsMenu(
            appLabel = appOptionsDialogState.item!!.label,
            currentDisplayPreference = appDisplayPreferenceManager.getAppDisplayPreference(
                appOptionsDialogState.item!!.packageName
            ),
            onDismiss = appOptionsDialogState::dismiss,
            onAppInfoClick = {
                openAppInfo(context, appOptionsDialogState.item!!.packageName)
            },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager.setAppDisplayPreference(
                    appOptionsDialogState.item!!.packageName,
                    preference
                )
            },
            hasExternalDisplay = hasExternalDisplay,
            app = if (isFreeModeEnabled) appOptionsDialogState.item else null,
            currentIconSize = currentIconSize,
            onIconSizeChange = { newSize ->
                if (isFreeModeEnabled) {
                    val currentPos = appPositionManager.getPosition(
                        pageIndex,
                        appOptionsDialogState.item!!.packageName
                    )
                    appPositionManager.savePosition(
                        pageIndex,
                        AppPosition(
                            packageName = appOptionsDialogState.item!!.packageName,
                            x = currentPos?.x ?: 0f,
                            y = currentPos?.y ?: 0f,
                            iconSize = newSize
                        )
                    )
                }
            },
            onToggleVisibility = {
                if (isAppHidden) {
                    appVisibilityManager.showApp(pageIndex, appOptionsDialogState.item!!.packageName)
                } else {
                    appVisibilityManager.hideApp(pageIndex, appOptionsDialogState.item!!.packageName)
                }
            },
            onCustomIconClick = {
                customIconDialogState.show(appOptionsDialogState.item)
                appOptionsDialogState.dismiss()
            }
        )
    }

    if (customIconDialogState.isVisible && customIconDialogState.item != null) {
        CustomIconDialog(
            packageName = customIconDialogState.item!!.packageName,
            appLabel = customIconDialogState.item!!.label,
            onDismiss = customIconDialogState::dismiss
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
            },
            onRecentAppsClick = onNavigateToRecentApps
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
                .offset(y = offsetY)
                .scale(pressScale)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {},
                    onDoubleClick = {
                        powerViewModel.togglePower()
                    },
                    onLongClick = {
                        showDrawerOptionsDialog = true
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
                onFolderClick = folderContentsDialogState::show
            )
        }
    }

    if (folderContentsDialogState.isVisible && folderContentsDialogState.item != null) {
        val folderApps =
            appsUnfiltered.filter { it.packageName in folderContentsDialogState.item!!.appPackageNames }
        FolderContentsDialog(
            folderName = folderContentsDialogState.item!!.name,
            apps = folderApps,
            folderId = folderContentsDialogState.item!!.id,
            pageIndex = pageIndex,
            allApps = appsUnfiltered,
            onDismiss = folderContentsDialogState::dismiss
        )
    }
}
