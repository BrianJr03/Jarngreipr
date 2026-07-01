package jr.brian.home.canvas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import jr.brian.home.R
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.EsdeContentScale
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.esde.model.GameImageType
import jr.brian.home.canvas.viewmodel.CanvasViewModel
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.esde.ui.RomSearchResultsActivity
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.model.rom.toPinnedRomInfo
import jr.brian.home.service.AppNotificationListenerService
import jr.brian.home.ui.components.NotificationShade
import jr.brian.home.ui.components.dialog.AppSelectionDialog
import jr.brian.home.ui.components.dialog.ConfirmationDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.CustomIconDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.components.dialog.PinnedRomOptionsDialog
import jr.brian.home.ui.components.dialog.RenameAppDialog
import jr.brian.home.ui.components.settings.displayName
import jr.brian.home.ui.util.rememberHasExternalDisplay
import jr.brian.home.util.openAppInfo
import jr.brian.home.ui.screens.AllNotificationsScreen
import jr.brian.home.ui.screens.RssTab
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.ui.theme.managers.LocalGridSettingsManager
import jr.brian.home.ui.theme.managers.LocalNotificationManager
import jr.brian.home.ui.util.topEdgePullDown
import jr.brian.home.util.launchApp
import jr.brian.home.util.launchAppOnOppositeDisplay
import jr.brian.home.viewmodels.NowPlayingViewModel
import jr.brian.home.viewmodels.WidgetViewModel
import kotlinx.coroutines.launch

/**
 * The Unified Canvas tab — a single continuous grid that hosts apps, folders,
 * ROMs, widgets, and the optional RSS launcher item on one configurable surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedCanvasTab(
    pageIndex: Int,
    pagerState: PagerState,
    totalPages: Int,
    modifier: Modifier = Modifier,
    apps: List<AppInfo> = emptyList(),
    onNavigateToRssSettings: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onDeletePage: (Int) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    dismissShadeSignal: Int = 0,
    viewModel: CanvasViewModel = hiltViewModel(key = "canvas-page-$pageIndex"),
    romSearchViewModel: RomSearchViewModel = hiltViewModel(),
    widgetViewModel: WidgetViewModel = hiltViewModel(),
    nowPlayingViewModel: NowPlayingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val gridSettingsManager = LocalGridSettingsManager.current
    val notificationCountManager = LocalNotificationManager.current
    val esdePrefsManager = jr.brian.home.esde.data.LocalESDEPreferencesManager.current
    val esdePrefsState by esdePrefsManager.state.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPinnedRoms by viewModel.allPinnedRoms.collectAsStateWithLifecycle()
    val appWidgetHost = widgetViewModel.getAppWidgetHost()

    LaunchedEffect(pageIndex) { viewModel.setPageIndex(pageIndex) }
    LaunchedEffect(apps) { viewModel.setApps(apps) }

    val pendingRomForPin by romSearchViewModel.pendingRomForPin
        .collectAsStateWithLifecycle()
    LaunchedEffect(pendingRomForPin) {
        val pending = pendingRomForPin ?: return@LaunchedEffect
        if (pending.first == pageIndex) {
            romSearchViewModel.clearPendingRomForPin()
            viewModel.pinRomToCanvas(pending.second.toPinnedRomInfo())
        }
    }

    var addDialogVisible by remember { mutableStateOf(false) }
    var addDialogStartInEdit by remember { mutableStateOf(false) }
    var pickAppVisible by remember { mutableStateOf(false) }
    var createFolderVisible by remember { mutableStateOf(false) }
    var pickRomVisible by remember { mutableStateOf(false) }
    var folderToOpen by remember { mutableStateOf<ResolvedCanvasItem.Folder?>(null) }
    var pendingRemoval by remember { mutableStateOf<ResolvedCanvasItem?>(null) }
    var rssSheetVisible by remember { mutableStateOf(false) }
    val rssSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var pickWidgetVisible by remember { mutableStateOf(false) }
    var pickEsdeArtVisible by remember { mutableStateOf(false) }
    var esdeArtRetypeTarget by remember { mutableStateOf<ResolvedCanvasItem.EsdeArt?>(null) }
    var resizeRequest by remember { mutableStateOf<CanvasResizeRequest?>(null) }
    var appOptionsTarget by remember { mutableStateOf<ResolvedCanvasItem.App?>(null) }
    var romOptionsTarget by remember { mutableStateOf<ResolvedCanvasItem.Rom?>(null) }
    var customIconTarget by remember { mutableStateOf<AppInfo?>(null) }
    var renameTarget by remember { mutableStateOf<AppInfo?>(null) }
    val hasExternalDisplay = rememberHasExternalDisplay()

    // Hoisted so the floating add icon can fade based on grid scroll.
    val canvasScrollState = rememberScrollState()

    var showNotificationShade by remember { mutableStateOf(false) }
    var showAllNotifications by remember { mutableStateOf(false) }

    LaunchedEffect(dismissShadeSignal) { showNotificationShade = false }

    val nowPlaying by nowPlayingViewModel.nowPlaying.collectAsStateWithLifecycle()
    val nowPlayingVolume by nowPlayingViewModel.volume.collectAsStateWithLifecycle()
    val nowPlayingPosition by nowPlayingViewModel.currentPosition.collectAsStateWithLifecycle()
    val nowPlayingDuration by nowPlayingViewModel.duration.collectAsStateWithLifecycle()
    val notifications by notificationCountManager.activeNotifications.collectAsStateWithLifecycle()

    val isHorizontalOrientation =
        uiState.layout.activeOrientation == CanvasScrollOrientation.HORIZONTAL

    Box(
        modifier = modifier
            .fillMaxSize()
            .topEdgePullDown(
                enabled = gridSettingsManager.notificationShadeEnabled && !showNotificationShade,
                isAtTop = { isHorizontalOrientation || canvasScrollState.value == 0 },
                onTrigger = { showNotificationShade = true }
            )
    ) {
        CanvasGrid(
            state = uiState,
            scrollState = canvasScrollState,
            onTap = {
                handleTap(
                    resolved = it,
                    onLaunchApp = { app ->
                        launchApp(
                            context = context,
                            packageName = app.packageName,
                            displayPreference = appDisplayPreferenceManager
                                .getAppDisplayPreference(app.packageName)
                        )
                    },
                    onOpenRss = { rssSheetVisible = true },
                    onOpenFolder = { folder -> folderToOpen = folder },
                    onLaunchRom = { rom ->
                        launchPinnedRom(
                            context = context,
                            rom = rom,
                            romSearchViewModel = romSearchViewModel,
                            displayPreference = appDisplayPreferenceManager
                                .getAppDisplayPreference(rom.key)
                        )
                    },
                    onChangeEsdeArtType = { esdeArt -> esdeArtRetypeTarget = esdeArt }
                )
            },
            onLongPress = { resolved ->
                when (resolved) {
                    is ResolvedCanvasItem.App if resolved.info != null ->
                        appOptionsTarget = resolved

                    is ResolvedCanvasItem.Rom if resolved.info != null ->
                        romOptionsTarget = resolved

                    else -> pendingRemoval = resolved
                }
            },
            onDoubleTap = { resolved ->
                if (resolved is ResolvedCanvasItem.App) {
                    resolved.info?.let { app ->
                        launchAppOnOppositeDisplay(
                            context = context,
                            packageName = app.packageName,
                            currentPreference = appDisplayPreferenceManager
                                .getAppDisplayPreference(app.packageName)
                        )
                    }
                }
            },
            onReorder = { from, to -> viewModel.reorderItems(from, to) },
            onResizeWidget = { widget ->
                // Legacy widget-overlay tap path. Min spans default to 1×1; the
                // corner resize handle path provides precise widget mins.
                resizeRequest = CanvasResizeRequest(widget, 1, 1)
            },
            onCommitLayout = { snapshot -> viewModel.commitLayoutSnapshot(snapshot) },
            onRequestResizeDialog = { item, minC, minR ->
                resizeRequest = CanvasResizeRequest(item, minC, minR)
            },
            onAddClick = { addDialogVisible = true },
            onAddLongClick = { viewModel.setEditMode(!uiState.layout.editMode) },
            modifier = Modifier.fillMaxSize(),
            appWidgetHost = appWidgetHost
        )

        NotificationShade(
            visible = showNotificationShade,
            nowPlaying = nowPlaying,
            currentPosition = nowPlayingPosition,
            duration = nowPlayingDuration,
            volume = nowPlayingVolume,
            onPlayPause = { nowPlayingViewModel.togglePlayPause() },
            onPrevious = { nowPlayingViewModel.skipToPrevious() },
            onNext = { nowPlayingViewModel.skipToNext() },
            onVolumeChange = { nowPlayingViewModel.setVolume(it) },
            onSeek = { nowPlayingViewModel.seekTo(it) },
            onDismiss = { showNotificationShade = false },
            onSettingsClick = { showNotificationShade = false; onSettingsClick() },
            notifications = notifications,
            onDismissNotification = { key -> AppNotificationListenerService.cancel(key) },
            onClearAllNotifications = { AppNotificationListenerService.cancelAll() },
            onSeeAllNotifications = {
                showNotificationShade = false
                showAllNotifications = true
            },
            initialTabPage = notificationCountManager.shadeTabPage,
            onTabPageChange = { notificationCountManager.saveShadeTabPage(it) },
            backgroundColorArgb = gridSettingsManager.shadeBackgroundColorArgb,
            onBackgroundColorChange = { gridSettingsManager.setShadeBackgroundColorArgb(it) },
            cornerRadiusDp = gridSettingsManager.shadeCornerRadiusDp,
            onCornerRadiusChange = { gridSettingsManager.setShadeCornerRadiusDp(it) },
            backgroundAlpha = gridSettingsManager.shadeBackgroundAlpha,
            onBackgroundAlphaChange = { gridSettingsManager.setShadeBackgroundAlpha(it) },
            accentColorArgb = gridSettingsManager.shadeAccentColorArgb,
            onAccentColorChange = { gridSettingsManager.setShadeAccentColorArgb(it) }
        )

        AnimatedVisibility(
            visible = showAllNotifications,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200))
        ) {
            AllNotificationsScreen(
                notifications = notifications,
                onDismissNotification = { key -> AppNotificationListenerService.cancel(key) },
                onClearAll = { AppNotificationListenerService.cancelAll() },
                onDismiss = { showAllNotifications = false }
            )
        }
    }

    if (rssSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { rssSheetVisible = false },
            sheetState = rssSheetState,
            containerColor = OledBackgroundColor
        ) {
            RssTab(
                onSettingsClick = {
                    scope.launch { runCatching { rssSheetState.hide() } }
                    rssSheetVisible = false
                    onNavigateToRssSettings()
                },
                dismissShadeSignal = dismissShadeSignal
            )
        }
    }

    if (addDialogVisible) {
        CanvasMainDialog(
            layout = uiState.layout,
            pagerState = pagerState,
            totalPages = totalPages,
            startInEdit = addDialogStartInEdit,
            onChoice = { choice ->
                handleAddChoice(
                    choice = choice,
                    context = context,
                    onPickApp = { pickAppVisible = true },
                    onAddRssLauncher = { viewModel.addItem(newRssLauncher()) },
                    onAddRssMusic = { viewModel.addItem(newRssMusic()) },
                    onAddFolder = { createFolderVisible = true },
                    onAddRom = { pickRomVisible = true },
                    onAddWidget = { pickWidgetVisible = true },
                    onAddEsdeDisplay = { pickEsdeArtVisible = true }
                )
            },
            onOrientationChanged = viewModel::setOrientation,
            onGridChanged = viewModel::setGrid,
            onEditModeChanged = viewModel::setEditMode,
            onTidy = { viewModel.compactLayout() },
            onSettingsClick = onSettingsClick,
            onDeletePage = onDeletePage,
            onNavigateToSearch = onNavigateToSearch,
            onDismiss = {
                addDialogVisible = false
                addDialogStartInEdit = false
            }
        )
    }

    if (pickRomVisible) {
        CanvasRomPickerDialog(
            roms = allPinnedRoms,
            onRomSelected = { rom -> viewModel.pinRomToCanvas(rom) },
            onBrowseRoms = {
                romSearchViewModel.enterSelectMode(pageIndex)
                onNavigateToRomSearch()
            },
            onDismiss = { pickRomVisible = false }
        )
    }

    if (pickEsdeArtVisible) {
        val defaultType = GameImageType.Fanart
        CanvasEsdeArtChooserDialog(
            initialType = defaultType,
            initialContentScale = defaultEsdeContentScaleFor(defaultType),
            titleRes = R.string.canvas_esde_picker_title,
            onConfirm = { imageType, scale -> viewModel.addEsdeArtItem(imageType, scale) },
            onDismiss = { pickEsdeArtVisible = false }
        )
    }

    esdeArtRetypeTarget?.let { target ->
        CanvasEsdeArtChooserDialog(
            initialType = target.raw.resolvedImageType,
            initialContentScale = target.raw.resolvedContentScale,
            titleRes = R.string.canvas_esde_chooser_title,
            onConfirm = { imageType, scale ->
                viewModel.updateEsdeArtItem(target.raw.id, imageType, scale)
            },
            onDismiss = { esdeArtRetypeTarget = null }
        )
    }

    if (pickWidgetVisible) {
        CanvasWidgetPickerDialog(
            appWidgetHost = appWidgetHost,
            onWidgetPicked = { widgetId, providerInfo ->
                val (cols, rows) = providerInfo.spanForCanvas()
                viewModel.addItem(
                    item = CanvasItem.WidgetItem(
                        id = "widget-$widgetId",
                        widgetId = widgetId
                    ),
                    colSpan = cols,
                    rowSpan = rows
                )
            },
            onDismiss = { pickWidgetVisible = false }
        )
    }

    resizeRequest?.let { req ->
        val currentRect = uiState.layout.activeArrangement[req.item.id]
        CanvasResizeDialog(
            item = req.item,
            currentColSpan = currentRect?.colSpan ?: req.minColSpan,
            currentRowSpan = currentRect?.rowSpan ?: req.minRowSpan,
            minColSpan = req.minColSpan,
            minRowSpan = req.minRowSpan,
            onResize = { cols, rows ->
                viewModel.resizeItemWithSolver(
                    id = req.item.id,
                    colSpan = cols,
                    rowSpan = rows,
                    minColSpan = req.minColSpan,
                    minRowSpan = req.minRowSpan
                )
            },
            onDismiss = { resizeRequest = null }
        )
    }

    if (createFolderVisible) {
        CreateFolderDialog(
            apps = apps,
            allApps = apps,
            pageIndex = pageIndex,
            tabType = CanvasTabType.VALUE,
            onDismiss = { createFolderVisible = false }
        )
    }

    folderToOpen?.let { resolved ->
        val folder = resolved.folder ?: run {
            folderToOpen = null
            return@let
        }
        val folderApps = remember(folder.appPackageNames, apps) {
            val byPkg = apps.associateBy { it.packageName }
            folder.appPackageNames.mapNotNull { byPkg[it] }
        }
        FolderContentsDialog(
            folderName = folder.name,
            apps = folderApps,
            folderId = folder.id,
            pageIndex = pageIndex,
            allApps = apps,
            tabType = CanvasTabType.VALUE,
            backgroundColorArgb = folder.backgroundColorArgb,
            backgroundImagePath = folder.backgroundImagePath,
            onDismiss = { folderToOpen = null }
        )
    }

    if (pickAppVisible) {
        AppSelectionDialog(
            apps = apps,
            onAppSelected = {},
            onDismiss = { pickAppVisible = false },
            multiSelect = true,
            onMultiSelectConfirm = { picked ->
                picked.forEach { viewModel.addItem(newAppItem(it)) }
                pickAppVisible = false
            }
        )
    }

    pendingRemoval?.let { resolved ->
        ConfirmationDialog(
            title = stringResource(R.string.canvas_remove_item_title),
            message = stringResource(R.string.canvas_remove_item_message),
            confirmText = stringResource(R.string.canvas_remove_confirm),
            cancelText = stringResource(R.string.canvas_remove_cancel),
            onConfirm = {
                if (resolved is ResolvedCanvasItem.Widget) {
                    appWidgetHost?.deleteAppWidgetId(resolved.raw.widgetId)
                }
                viewModel.removeItemAndCleanup(resolved.raw)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null }
        )
    }

    val onEditCanvas: () -> Unit = {
        addDialogStartInEdit = true
        addDialogVisible = true
    }

    romOptionsTarget?.let { target ->
        val rom = target.info ?: run {
            romOptionsTarget = null
            return@let
        }
        val spinEligible = rom.systemName.lowercase() in
            jr.brian.home.esde.util.ESDEMediaConstants.DISC_PLATFORMS
        PinnedRomOptionsDialog(
            rom = rom,
            onDismiss = { romOptionsTarget = null },
            onMediaTypeSelected = { mediaType ->
                viewModel.updateRomMediaType(rom, mediaType)
            },
            onRemove = {
                viewModel.removeCanvasRom(target.raw)
            },
            currentContentScale = target.raw.resolvedContentScale,
            onContentScaleChange = { scale ->
                viewModel.updateRomContentScale(target.raw.id, scale)
            },
            onEditCanvas = {
                romOptionsTarget = null
                onEditCanvas()
            },
            continuousSpinEligible = spinEligible,
            continuousSpinEnabled = rom.key in esdePrefsState.canvasContinuousSpinRoms,
            onContinuousSpinChange = if (spinEligible) { enabled ->
                esdePrefsManager.setCanvasContinuousSpin(rom.key, enabled)
            } else null
        )
    }

    appOptionsTarget?.let { target ->
        val app = target.info ?: run {
            appOptionsTarget = null
            return@let
        }
        CanvasAppOptionsDialog(
            app = app,
            currentDisplayPreference = appDisplayPreferenceManager
                .getAppDisplayPreference(app.packageName),
            hasExternalDisplay = hasExternalDisplay,
            onDismiss = { appOptionsTarget = null },
            onAppInfoClick = { openAppInfo(context, app.packageName) },
            onDisplayPreferenceChange = { preference ->
                appDisplayPreferenceManager
                    .setAppDisplayPreference(app.packageName, preference)
            },
            onRemoveFromCanvas = {
                viewModel.removeItemAndCleanup(target.raw)
                appOptionsTarget = null
            },
            onCustomIconClick = {
                customIconTarget = app
                appOptionsTarget = null
            },
            onRenameClick = {
                renameTarget = app
                appOptionsTarget = null
            },
            onEditCanvas = {
                appOptionsTarget = null
                onEditCanvas()
            }
        )
    }

    customIconTarget?.let { app ->
        CustomIconDialog(
            packageName = app.packageName,
            appLabel = app.displayName(),
            onDismiss = { customIconTarget = null }
        )
    }

    renameTarget?.let { app ->
        RenameAppDialog(
            packageName = app.packageName,
            appLabel = app.label,
            onDismiss = { renameTarget = null }
        )
    }
}

private fun handleTap(
    resolved: ResolvedCanvasItem,
    onLaunchApp: (AppInfo) -> Unit,
    onOpenRss: () -> Unit,
    onOpenFolder: (ResolvedCanvasItem.Folder) -> Unit,
    onLaunchRom: (PinnedRomInfo) -> Unit,
    onChangeEsdeArtType: (ResolvedCanvasItem.EsdeArt) -> Unit
) {
    when (resolved) {
        is ResolvedCanvasItem.App -> resolved.info?.let(onLaunchApp)
        is ResolvedCanvasItem.RssLauncher -> onOpenRss()
        is ResolvedCanvasItem.Folder -> if (resolved.folder != null) onOpenFolder(resolved)
        is ResolvedCanvasItem.Rom -> resolved.info?.let(onLaunchRom)
        is ResolvedCanvasItem.Widget -> {
            // Widget interaction lives in Phase J.
        }
        is ResolvedCanvasItem.RssMusic -> {
            // Music tile owns its own tap handling (artwork → NowPlayingDialog,
            // transport buttons drive playback). The canvas-level onTap is a no-op.
        }
        is ResolvedCanvasItem.EsdeArt -> onChangeEsdeArtType(resolved)
    }
}

private fun handleAddChoice(
    choice: CanvasAddChoice,
    context: android.content.Context,
    onPickApp: () -> Unit,
    onAddRssLauncher: () -> Unit,
    onAddRssMusic: () -> Unit,
    onAddFolder: () -> Unit,
    onAddRom: () -> Unit,
    onAddWidget: () -> Unit,
    onAddEsdeDisplay: () -> Unit
) {
    when (choice) {
        CanvasAddChoice.APP -> onPickApp()
        CanvasAddChoice.RSS_LAUNCHER -> onAddRssLauncher()
        CanvasAddChoice.RSS_MUSIC -> onAddRssMusic()
        CanvasAddChoice.FOLDER -> onAddFolder()
        CanvasAddChoice.ROM -> onAddRom()
        CanvasAddChoice.WIDGET -> onAddWidget()
        CanvasAddChoice.ESDE_DISPLAY -> onAddEsdeDisplay()
    }
}

/**
 * Map a widget's preferred size in `minWidth/minHeight` (dp) to canvas grid cells.
 * Mirrors the heuristic in [jr.brian.home.data.WidgetProviderRepository] but kept
 * inline here to avoid threading the repo into the canvas path. Falls back to 2×2.
 */
private fun android.appwidget.AppWidgetProviderInfo.spanForCanvas(): Pair<Int, Int> {
    val cellPx = 80f
    val cols = ((minWidth / cellPx).toInt()).coerceIn(1, 6)
    val rows = ((minHeight / cellPx).toInt()).coerceIn(1, 6)
    return cols to rows
}

private fun launchPinnedRom(
    context: android.content.Context,
    rom: PinnedRomInfo,
    romSearchViewModel: RomSearchViewModel,
    displayPreference: DisplayPreference
) {
    romSearchViewModel.requestRomLaunch(rom)
    val intent = android.content.Intent(context, RomSearchResultsActivity::class.java).apply {
        addFlags(
            android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        )
    }
    launchApp(context, rom.key, displayPreference, intent)
}

private fun newAppItem(app: AppInfo): CanvasItem.AppItem =
    CanvasItem.AppItem(
        id = "app-${app.packageName}-${UUID.randomUUID()}",
        packageName = app.packageName
    )

private fun newRssLauncher(): CanvasItem.RssLauncherItem =
    CanvasItem.RssLauncherItem(
        id = "rss-${UUID.randomUUID()}"
    )

private fun newRssMusic(): CanvasItem.RssMusicItem =
    CanvasItem.RssMusicItem(
        id = "rss-music-${UUID.randomUUID()}"
    )

/**
 * Default Fit/Crop choice presented when no tile-specific value exists yet.
 * Mirrors the legacy hardcoded EsdeArtTile rule so a freshly-added tile renders
 * the same way the pre-toggle code would have rendered it.
 */
private fun defaultEsdeContentScaleFor(type: GameImageType): EsdeContentScale =
    when (type) {
        GameImageType.Marquee -> EsdeContentScale.FIT
        else -> EsdeContentScale.CROP
    }

/**
 * Bundles a target item with its resolved minimum spans so the resize dialog
 * can clamp the steppers. Widget mins come from
 * [android.appwidget.AppWidgetProviderInfo] via the corner-handle path;
 * non-widgets and the legacy widget-overlay path default to 1×1.
 */
private data class CanvasResizeRequest(
    val item: CanvasItem,
    val minColSpan: Int,
    val minRowSpan: Int
)
