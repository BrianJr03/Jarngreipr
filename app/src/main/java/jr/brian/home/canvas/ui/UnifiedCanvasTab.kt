package jr.brian.home.canvas.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.UUID
import jr.brian.home.R
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.canvas.viewmodel.CanvasViewModel
import jr.brian.home.data.AppDisplayPreferenceManager.DisplayPreference
import jr.brian.home.esde.ui.RomSearchResultsActivity
import jr.brian.home.esde.viewmodels.RomSearchViewModel
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.rom.PinnedRomInfo
import jr.brian.home.ui.animations.animatedFocusedScale
import jr.brian.home.ui.colors.borderBrush
import jr.brian.home.ui.colors.cardGradient
import jr.brian.home.ui.components.dialog.AppSelectionDialog
import jr.brian.home.ui.components.dialog.ConfirmationDialog
import jr.brian.home.ui.components.dialog.CreateFolderDialog
import jr.brian.home.ui.components.dialog.FolderContentsDialog
import jr.brian.home.ui.screens.RssTab
import jr.brian.home.ui.theme.OledBackgroundColor
import jr.brian.home.ui.theme.ThemePrimaryColor
import jr.brian.home.ui.theme.managers.LocalAppDisplayPreferenceManager
import jr.brian.home.util.launchApp
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
    modifier: Modifier = Modifier,
    apps: List<AppInfo> = emptyList(),
    onNavigateToRssSettings: () -> Unit = {},
    onNavigateToRomSearch: () -> Unit = {},
    dismissShadeSignal: Int = 0,
    viewModel: CanvasViewModel = hiltViewModel(key = "canvas-page-$pageIndex"),
    romSearchViewModel: RomSearchViewModel = hiltViewModel(),
    widgetViewModel: WidgetViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appDisplayPreferenceManager = LocalAppDisplayPreferenceManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPinnedRoms by viewModel.allPinnedRoms.collectAsStateWithLifecycle()
    val appWidgetHost = widgetViewModel.getAppWidgetHost()

    LaunchedEffect(pageIndex) { viewModel.setPageIndex(pageIndex) }
    LaunchedEffect(apps) { viewModel.setApps(apps) }

    var addDialogVisible by remember { mutableStateOf(false) }
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

    // Hoisted so the floating add icon can fade based on grid scroll.
    val canvasScrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        CanvasGrid(
            state = uiState,
            scrollState = canvasScrollState,
            onTap = {
                handleTap(
                    context = context,
                    resolved = it,
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
            onLongPress = { pendingRemoval = it },
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
            modifier = Modifier.fillMaxSize(),
            appWidgetHost = appWidgetHost
        )
        FloatingAddIcon(
            scrollState = canvasScrollState,
            onClick = { addDialogVisible = true },
            onLongClick = { viewModel.setEditMode(!uiState.layout.editMode) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )
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
            onChoice = { choice ->
                handleAddChoice(
                    choice = choice,
                    context = context,
                    onPickApp = { pickAppVisible = true },
                    onAddRssLauncher = { viewModel.addItem(newRssLauncher()) },
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
            onDismiss = { addDialogVisible = false }
        )
    }

    if (pickRomVisible) {
        CanvasRomPickerDialog(
            roms = allPinnedRoms,
            onRomSelected = { rom -> viewModel.pinRomToCanvas(rom) },
            onBrowseRoms = onNavigateToRomSearch,
            onDismiss = { pickRomVisible = false }
        )
    }

    if (pickEsdeArtVisible) {
        CanvasEsdeArtChooserDialog(
            initialType = jr.brian.home.esde.model.GameImageType.Fanart,
            titleRes = R.string.canvas_esde_picker_title,
            onConfirm = { imageType -> viewModel.addEsdeArtItem(imageType) },
            onDismiss = { pickEsdeArtVisible = false }
        )
    }

    esdeArtRetypeTarget?.let { target ->
        CanvasEsdeArtChooserDialog(
            initialType = target.raw.resolvedImageType,
            titleRes = R.string.canvas_esde_chooser_title,
            onConfirm = { imageType ->
                viewModel.updateEsdeArtItemImageType(target.raw.id, imageType)
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
}

/**
 * The canvas's single top-bar entry point: a floating add icon pinned at
 * top-right. Fades while the grid is scrolling so it doesn't obscure
 * content during gestures; snaps to full opacity when focused (D-pad /
 * gamepad) or pressed so it's never unreachable. Long-press toggles edit
 * mode as a power-user shortcut — same control surface is reachable via the
 * dialog's Edit Canvas option.
 */
@Composable
private fun FloatingAddIcon(
    scrollState: ScrollState,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val targetAlpha = when {
        isFocused -> 1f
        scrollState.isScrollInProgress -> 0.15f
        else -> 1f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 200),
        label = "canvas-add-icon-alpha"
    )
    TopBarIconButton(
        icon = Icons.Default.Add,
        contentDescription = stringResource(R.string.canvas_add_item),
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.alpha(alpha),
        onFocusedChanged = { isFocused = it }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TopBarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onFocusedChanged: (Boolean) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(44.dp)
            .scale(animatedFocusedScale(isFocused))
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusedChanged(it.isFocused)
            }
            .background(
                brush = cardGradient(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = if (isFocused) 3.dp else 2.dp,
                brush = borderBrush(isFocused = isFocused),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = ThemePrimaryColor
        )
    }
}

private fun handleTap(
    context: android.content.Context,
    resolved: ResolvedCanvasItem,
    onOpenRss: () -> Unit,
    onOpenFolder: (ResolvedCanvasItem.Folder) -> Unit,
    onLaunchRom: (PinnedRomInfo) -> Unit,
    onChangeEsdeArtType: (ResolvedCanvasItem.EsdeArt) -> Unit
) {
    when (resolved) {
        is ResolvedCanvasItem.App -> resolved.info?.let { launchApp(context, it.packageName) }
        is ResolvedCanvasItem.RssLauncher -> onOpenRss()
        is ResolvedCanvasItem.Folder -> if (resolved.folder != null) onOpenFolder(resolved)
        is ResolvedCanvasItem.Rom -> resolved.info?.let(onLaunchRom)
        is ResolvedCanvasItem.Widget -> {
            // Widget interaction lives in Phase J.
        }
        is ResolvedCanvasItem.EsdeArt -> onChangeEsdeArtType(resolved)
    }
}

private fun handleAddChoice(
    choice: CanvasAddChoice,
    context: android.content.Context,
    onPickApp: () -> Unit,
    onAddRssLauncher: () -> Unit,
    onAddFolder: () -> Unit,
    onAddRom: () -> Unit,
    onAddWidget: () -> Unit,
    onAddEsdeDisplay: () -> Unit
) {
    when (choice) {
        CanvasAddChoice.APP -> onPickApp()
        CanvasAddChoice.RSS_LAUNCHER -> onAddRssLauncher()
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
