package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.shouldAskDisplay
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.rss.JarnRssRoute
import jr.brian.jarnfe.rss.JarnRssSheetSlot
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.JarnNotices
import jr.brian.jarnfe.wallpaper.readableOverWallpaper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CANVAS_SETTLE_TIMEOUT_MS = 1_500L

@Composable
internal fun CanvasRoute(state: JarnSessionState) {
    val context = LocalContext.current
    val data = remember(context) { context.jarnCanvasDataOrNull() }
    if (data == null) {
        JarnEmptyState(
            title = stringResource(R.string.jarnfe_empty_canvas_title),
            message = stringResource(R.string.jarnfe_empty_canvas_message),
        )
        return
    }
    LaunchedEffect(data) { JarnCanvas.observe(data) }
    val snapshot = JarnCanvas.snapshot
    if (snapshot == null) {
        JarnText(
            text = stringResource(R.string.jarnfe_canvas_loading),
            style = JarnTheme.type.body,
            color = JarnTheme.colors.textMid,
        )
    } else {
        CanvasWorkspace(data, snapshot, state)
    }
}

@Composable
private fun CanvasWorkspace(data: JarnCanvasData, snapshot: JarnCanvasSnapshot, state: JarnSessionState) {
    var overlay by remember { mutableStateOf<CanvasOverlay?>(null) }
    var pendingGrab by remember { mutableStateOf<String?>(null) }
    var widgetRevision by remember { mutableIntStateOf(0) }
    var artPreview by remember { mutableStateOf<CanvasArtPreview?>(null) }
    val selected = JarnCanvas.selectedPage
    val undo = JarnCanvas.undo
    val undoWrites = remember(data) { CanvasDataUndoWrites(data) }
    val page = snapshot.pages.getOrNull(selected)?.let { it.withoutTiles(undo.hiddenOn(it.index)) }
    CanvasUndoExits(undo)
    LaunchedEffect(selected) { undo.settle() }
    LaunchedEffect(overlay != null) { if (overlay != null) undo.settle() }
    LaunchedEffect(page?.layout, overlay) {
        val settled = artPreview?.isSettled(page?.layout?.items.orEmpty()) == true
        if (overlay !is CanvasOverlay.ArtMedia && settled) artPreview = null
    }
    val editor = remember { CanvasEditor(navigate = { JarnCanvas.selectedPage = it }) }
    editor.showPage(page?.index)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val launchRom = rememberCanvasRomLauncher()
    val choosePhoto = rememberCanvasPhotoPicker(data)
    val launchApp: (JarnCanvasApp) -> Unit = { app ->
        if (data.appOptions(app.packageName).shouldAskDisplay()) {
            overlay = CanvasOverlay.AskDisplay(app.packageName)
        } else {
            activity?.let { data.launchApp(it, app.packageName) }
        }
    }
    val handlers = CanvasTileHandlers(
        onOpenList = { overlay = CanvasOverlay.Items(it) },
        openFolder = { overlay = CanvasOverlay.Folder(it) },
        launchApp = launchApp,
        launchRom = launchRom,
        choosePhoto = { item -> page?.let { choosePhoto(it.index, item) } },
        openMusic = { overlay = CanvasOverlay.Music },
        openRss = { overlay = CanvasOverlay.Rss },
        openTileMenu = { overlay = CanvasOverlay.TileMenu(it.id) },
    )

    Box(Modifier.fillMaxSize()) {
        key(page?.index) {
            CanvasPageSurface(
                editor = editor,
                scope = scope,
                data = data,
                page = page,
                snapshot = snapshot,
                state = state,
                pageCount = snapshot.pages.size,
                undo = undo,
                undoWrites = undoWrites,
                sheetOpen = overlay != null,
                widgetRevision = widgetRevision,
                artPreview = artPreview,
                pendingGrab = pendingGrab,
                onPendingGrabHandled = { pendingGrab = null },
                onOptions = { overlay = CanvasOverlay.Options },
                handlers = handlers,
                overlays = { commands ->
                    if (overlay == CanvasOverlay.Options) {
                        CanvasOptionsSheet(
                            canEdit = page?.kind == JarnCanvasPageKind.Canvas,
                            alreadyEditing = commands.editing,
                            onAdd = { overlay = CanvasOverlay.Add },
                            onEdit = {
                                overlay = null
                                commands.startEdit()
                            },
                            onPages = { overlay = CanvasOverlay.Pages },
                            onClose = { overlay = null },
                        )
                    }
                    CanvasTileOverlays(
                        overlay = overlay,
                        data = data,
                        page = page,
                        snapshot = snapshot,
                        commands = CanvasTileCommands(
                            useWidget = commands.useWidget,
                            moveTile = commands.moveTile,
                            choosePhoto = { item -> page?.let { choosePhoto(it.index, item) } },
                            playRandom = { item ->
                                snapshot.recent.take(item.resolvedCount).randomOrNull()?.let(launchRom)
                            },
                            refreshWidgets = { widgetRevision += 1 },
                            grabNewTile = { pendingGrab = it },
                            previewArt = { artPreview = it },
                            removeTile = { item ->
                                page?.let { undo.removeTile(undoWrites, it.index, item.id) }
                            },
                        ),
                        onOverlay = { overlay = it },
                    )
                },
            )
        }
        if (overlay == CanvasOverlay.Pages) {
            CanvasPagesSheet(
                data = data,
                snapshot = snapshot,
                selectedPage = selected,
                onGoTo = { index ->
                    JarnCanvas.selectedPage = index
                    overlay = null
                },
                onSelectionMoved = { JarnCanvas.selectedPage = it },
                onClose = { overlay = null },
            )
        }
        (overlay as? CanvasOverlay.Items)?.let { items ->
            CanvasListSheet(
                target = items.target,
                onLaunchRom = { rom ->
                    overlay = null
                    launchRom(rom)
                },
                onClose = { overlay = null },
            )
        }
    }
}

private class CanvasTileHandlers(
    val onOpenList: (CanvasListTarget) -> Unit,
    val openFolder: (String) -> Unit,
    val launchApp: (JarnCanvasApp) -> Unit,
    val launchRom: (JarnCanvasRom) -> Unit,
    val choosePhoto: (CanvasItem.PhotoContainer) -> Unit,
    val openMusic: () -> Unit,
    val openRss: () -> Unit,
    val openTileMenu: (CanvasItem) -> Unit,
)

private class CanvasSurfaceCommands(
    val editing: Boolean,
    val startEdit: () -> Unit,
    val useWidget: (CanvasItem.WidgetItem) -> Unit,
    val moveTile: (CanvasItem) -> Unit,
)

@Composable
private fun CanvasPageSurface(
    editor: CanvasEditor,
    scope: CoroutineScope,
    data: JarnCanvasData,
    page: JarnCanvasPage?,
    snapshot: JarnCanvasSnapshot,
    state: JarnSessionState,
    pageCount: Int,
    undo: CanvasUndoController,
    undoWrites: CanvasUndoWrites,
    sheetOpen: Boolean,
    widgetRevision: Int,
    artPreview: CanvasArtPreview?,
    pendingGrab: String?,
    onPendingGrabHandled: () -> Unit,
    onOptions: () -> Unit,
    handlers: CanvasTileHandlers,
    overlays: @Composable (CanvasSurfaceCommands) -> Unit,
) {
    val rssSheets = remember { JarnRssSheetSlot() }
    val focus = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocused by remember { mutableStateOf<String?>(null) }
    var interactingId by remember { mutableStateOf<String?>(null) }
    var cellPx by remember { mutableFloatStateOf(0f) }
    val router = LocalJarnInputRouter.current
    val screen = LocalJarnScreen.current
    val isActive = router.activeScreen == screen
    val requesterFor: (String) -> FocusRequester = { id -> focus.getOrPut(id) { FocusRequester() } }
    val hasWidgets = page?.kind == JarnCanvasPageKind.Canvas &&
        page.layout.items.any { it is CanvasItem.WidgetItem }
    val lease = rememberCanvasWidgetLease(data, hasWidgets)

    val grabTile: (CanvasItem) -> Unit = { item ->
        page?.let {
            val (minCol, minRow) = minSpansFor(data, item, cellPx)
            lastFocused = item.id
            editor.enter()
            editor.begin(it.layout, item.id, minCol, minRow)
        }
    }
    val useWidget: (CanvasItem.WidgetItem) -> Unit = { item ->
        if (data.widgetHealth(item.widgetId) == JarnWidgetHealth.Live) {
            interactingId = item.id
        } else {
            handlers.openTileMenu(item)
        }
    }
    val activate: (CanvasItem) -> Unit = { item ->
        page?.let { activateTile(item, it, snapshot, handlers, useWidget) }
    }
    val touch = rememberCanvasTouch(
        editor = editor,
        page = page,
        enabled = !sheetOpen && interactingId == null,
        minSpans = { minSpansFor(data, it, cellPx) },
        openMenu = handlers.openTileMenu,
        commit = { layout, placements ->
            page?.let { data.commitPlacements(it.index, layout.activeOrientation, placements) }
        },
        onGrabbed = { lastFocused = it },
        onDropped = { finished, placements -> recordDrop(undo, undoWrites, finished, placements) },
        removeTile = { undo.removeTile(undoWrites, it.home.pageIndex, it.itemId) },
    )
    val tapTile: (CanvasItem, Float) -> Unit = tap@{ item, tileCellPx ->
        if (touch.driver.claimEditTap(item.id)) return@tap
        when (canvasTapAction(editor.grab != null, editor.editing)) {
            CanvasTapAction.Ignore -> Unit
            CanvasTapAction.Grab -> page?.let {
                touch.driver.clearSelection()
                val (minCol, minRow) = minSpansFor(data, item, tileCellPx)
                editor.begin(it.layout, item.id, minCol, minRow)
            }
            CanvasTapAction.Open -> activate(item)
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            editor.cancelGrab()
            interactingId = null
            undo.settle()
        }
    }
    LaunchedEffect(editor.editing) {
        if (!editor.editing) touch.driver.clearSelection()
    }
    LaunchedEffect(editor.grab?.itemId) {
        if (editor.grab != null && undo.entry is CanvasPendingRemoval) undo.settle()
    }
    LaunchedEffect(page?.layout, editor.settled) {
        val held = editor.settled ?: return@LaunchedEffect
        if (!canvasPlacementsAgree(held, page?.layout?.activeArrangement.orEmpty())) delay(CANVAS_SETTLE_TIMEOUT_MS)
        editor.settled = null
    }
    LaunchedEffect(pendingGrab, page?.layout) {
        val id = pendingGrab ?: return@LaunchedEffect
        val item = page?.layout?.items?.firstOrNull { it.id == id } ?: return@LaunchedEffect
        if (id in page.layout.activeArrangement) {
            val (minCol, minRow) = minSpansFor(data, item, cellPx)
            lastFocused = id
            editor.beginAdded(page.layout, id, minCol, minRow)
            onPendingGrabHandled()
        }
    }

    val focusTarget = page?.takeIf { it.kind != JarnCanvasPageKind.Rss }
        ?.let { canvasFocusTarget(it, lastFocused, editor.editing) }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(focusTarget, sheetOpen, isActive, editor.editing) {
        if (focusTarget == null || sheetOpen || !isActive) return@LaunchedEffect
        withFrameNanos { }
        if (!tabsFocus.hasFocus) runCatching { requesterFor(focusTarget).requestFocus() }
    }

    val focusedTile = page?.layout?.items?.firstOrNull { it.id == lastFocused }
    val restoreUndo: () -> Unit = {
        undo.undo(page?.layout?.activeArrangement.orEmpty())?.let { editor.settled = it }
    }
    CanvasInputLayers(
        editor = editor,
        hasTiles = page?.layout?.items?.isNotEmpty() == true,
        confirmLabel = page?.let { focusedConfirmLabel(data, it, focusedTile) },
        sheetOpen = sheetOpen || interactingId != null || page?.kind == JarnCanvasPageKind.Rss,
        onContext = { if (focusedTile != null) handlers.openTileMenu(focusedTile) else onOptions() },
        onOptions = onOptions,
        onUndo = restoreUndo.takeIf { undo.entry != null && editor.grab == null },
        onDrop = {
            val current = page ?: return@CanvasInputLayers
            val finished = editor.grab ?: return@CanvasInputLayers
            editor.drop { placements ->
                data.commitPlacements(current.index, current.layout.activeOrientation, placements)
                recordDrop(undo, undoWrites, finished, placements)
            }
        },
        onUndoAdd = {
            val homePage = editor.grab?.home?.pageIndex ?: return@CanvasInputLayers
            editor.undoAdd { id -> scope.launch { data.removeTile(homePage, id) } }
        },
    )

    Column(Modifier.fillMaxSize()) {
        CanvasPageHeader(page, pageCount, snapshot.homePage, editor.editing)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                page == null -> CanvasNotice(R.string.jarnfe_canvas_no_pages_title, R.string.jarnfe_canvas_no_pages_message)
                page.kind == JarnCanvasPageKind.Rss -> JarnRssRoute(state, sheetOpen, rssSheets, onOptions)
                page.kind != JarnCanvasPageKind.Canvas ->
                    CanvasNotice(R.string.jarnfe_canvas_other_page_title, R.string.jarnfe_canvas_other_page_message)
                else -> {
                    val stageArt = rememberCanvasStageArt(state, page)
                    CanvasGrid(
                        page = page,
                        editor = editor,
                        driver = touch.driver,
                        boardModifier = Modifier.canvasBoardTouch(touch.driver, touch.space),
                        onBoardGeometry = { touch.driver.geometry = it },
                        focusRequesterFor = requesterFor,
                        onFocused = { lastFocused = it },
                        onCellSize = { cellPx = it },
                        onVisibleCells = editor::reportVisibleCells,
                        onTileClick = tapTile,
                        onMenuClick = onOptions,
                    ) { item ->
                        if (item is CanvasItem.WidgetItem) {
                            CanvasWidgetTile(
                                data = data,
                                widgetId = item.widgetId,
                                lease = lease,
                                revision = widgetRevision,
                                editing = editor.editing,
                                interacting = interactingId == item.id,
                                frozen = touch.driver.visual.tileId == item.id,
                                touch = CanvasWidgetTouchBinding(
                                    driver = touch.driver,
                                    space = touch.space,
                                    itemId = item.id,
                                    enabled = !sheetOpen && interactingId == null,
                                    onBlankTap = { tapTile(item, cellPx) },
                                ),
                                onEditTap = { tapTile(item, cellPx) },
                                onLeave = { interactingId = null },
                            )
                        } else {
                            CanvasTileContent(item.withArtPreview(artPreview), page, snapshot, stageArt)
                        }
                    }
                }
            }
        }
    }
    CanvasTouchChrome(touch.driver, undo.entry.takeIf { editor.grab == null }, restoreUndo)
    rssSheets.Host()
    overlays(
        CanvasSurfaceCommands(
            editing = editor.editing,
            startEdit = editor::enter,
            useWidget = useWidget,
            moveTile = grabTile,
        ),
    )
}

@Composable
private fun CanvasTouchChrome(driver: CanvasTouchDriver, undoEntry: CanvasUndoEntry?, onUndo: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        undoEntry?.let {
            CanvasUndoToast(
                entry = it,
                onUndo = onUndo,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(JarnTheme.space.md),
            )
        }
        CanvasRemoveDockBar(driver.dock, driver.showsDock, Modifier.align(Alignment.BottomCenter))
    }
}

private fun recordDrop(
    undo: CanvasUndoController,
    writes: CanvasUndoWrites,
    finished: CanvasGrab,
    placements: Map<String, GridRect>,
) {
    val before = finished.home.baseline.placements
    undo.recordLayout(
        writes,
        CanvasLayoutChange(
            pageIndex = finished.pageIndex,
            orientation = finished.home.orientation,
            before = before,
            after = placements,
            kind = canvasUndoKind(finished.itemId, before, placements),
        ),
    )
}

private fun JarnCanvasPage.withoutTiles(hidden: Set<String>): JarnCanvasPage =
    if (hidden.isEmpty()) {
        this
    } else {
        copy(
            layout = layout.copy(
                items = layout.items.filter { it.id !in hidden },
                verticalArrangement = layout.verticalArrangement - hidden,
                horizontalArrangement = layout.horizontalArrangement - hidden,
            ),
        )
    }

private fun minSpansFor(data: JarnCanvasData, item: CanvasItem, cellPx: Float): Pair<Int, Int> =
    if (item is CanvasItem.WidgetItem) data.widgetMinSpans(item.widgetId, cellPx) else 1 to 1

private fun canvasFocusTarget(page: JarnCanvasPage, last: String?, editing: Boolean): String? {
    val placements = page.layout.activeArrangement
    val ids = page.layout.items.map { it.id }.filter { it in placements }
    val hasMenu = !editing && reservedRectsForActive(page.layout).isNotEmpty()
    return when {
        last != null && last in ids -> last
        last == CANVAS_MENU_FOCUS_ID && hasMenu -> last
        ids.isNotEmpty() -> ids.minWith(compareBy({ placements.getValue(it).row }, { placements.getValue(it).col }))
        hasMenu -> CANVAS_MENU_FOCUS_ID
        else -> null
    }
}

private fun activateTile(
    item: CanvasItem,
    page: JarnCanvasPage,
    snapshot: JarnCanvasSnapshot,
    handlers: CanvasTileHandlers,
    useWidget: (CanvasItem.WidgetItem) -> Unit,
) {
    when (item) {
        is CanvasItem.AppItem -> page.apps[item.packageName]?.let(handlers.launchApp) ?: showMissing()
        is CanvasItem.FolderItem ->
            if (item.folderId in page.folders) handlers.openFolder(item.id) else showMissing()
        is CanvasItem.RomItem -> page.roms[item.romKey]?.let(handlers.launchRom) ?: showMissing()
        is CanvasItem.LastPlayedItem ->
            handlers.onOpenList(CanvasListTarget.LastPlayed(snapshot.recent.take(item.resolvedCount)))
        is CanvasItem.EsdeArtItem -> JarnSession.update { copy(deckTab = DeckTab.Now) }
        is CanvasItem.WidgetItem -> useWidget(item)
        is CanvasItem.PhotoContainer -> handlers.choosePhoto(item)
        is CanvasItem.RssLauncherItem -> handlers.openRss()
        is CanvasItem.RssMusicItem -> handlers.openMusic()
    }
}

private fun showMissing() = JarnNotices.show(R.string.jarnfe_canvas_notice_missing)

private fun focusedConfirmLabel(data: JarnCanvasData, page: JarnCanvasPage, tile: CanvasItem?): Int? {
    if (tile == null) return R.string.jarnfe_hint_open
    val health = (tile as? CanvasItem.WidgetItem)?.let { data.widgetHealth(it.widgetId) }
    return canvasConfirmHint(tile, canvasItemResolved(tile, page), health)
}

@Composable
private fun CanvasInputLayers(
    editor: CanvasEditor,
    hasTiles: Boolean,
    confirmLabel: Int?,
    sheetOpen: Boolean,
    onContext: () -> Unit,
    onOptions: () -> Unit,
    onUndo: (() -> Unit)?,
    onDrop: () -> Unit,
    onUndoAdd: () -> Unit,
) {
    val grab = editor.grab
    JarnInputLayer(
        enabled = !editor.editing && !sheetOpen,
        bindings = {
            if (hasTiles && confirmLabel != null) hint(JarnAction.Confirm, confirmLabel)
            on(JarnAction.Context, R.string.jarnfe_hint_context, onPress = onContext)
            on(JarnAction.Menu, R.string.jarnfe_canvas_options_title, onPress = onOptions)
            onUndo?.let { on(JarnAction.Search, R.string.jarnfe_canvas_undo, onPress = it) }
        },
    )
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = editor.editing && grab == null && !sheetOpen,
        bindings = {
            if (hasTiles) hint(JarnAction.Confirm, R.string.jarnfe_canvas_hint_grab)
            on(JarnAction.Menu, R.string.jarnfe_canvas_options_title, onPress = onOptions)
            on(JarnAction.Back, R.string.jarnfe_canvas_hint_done, onPress = editor::exit)
            onUndo?.let { on(JarnAction.Search, R.string.jarnfe_canvas_undo, onPress = it) }
        },
    )
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = grab != null,
        bindings = {
            if (grab?.driver == CanvasGrabDriver.Touch) {
                grabCancelBinding(editor, grab.isNewTile, onUndoAdd)
            } else {
                grabBindings(editor, grab?.axis ?: CanvasResizeAxis.Width, grab?.isNewTile == true, onDrop, onUndoAdd)
            }
        },
    )
}

private fun JarnBindingsBuilder.grabBindings(
    editor: CanvasEditor,
    axis: CanvasResizeAxis,
    isNewTile: Boolean,
    onDrop: () -> Unit,
    onUndoAdd: () -> Unit,
) {
    on(JarnAction.MoveUp, R.string.jarnfe_hint_move) { editor.step { moved(0, -1) } }
    on(JarnAction.MoveDown) { editor.step { moved(0, 1) } }
    on(JarnAction.MoveLeft) { editor.step { moved(-1, 0) } }
    on(JarnAction.MoveRight) { editor.step { moved(1, 0) } }
    on(JarnAction.TabPrevious, R.string.jarnfe_canvas_hint_shrink) { editor.step { resized(-1) } }
    on(JarnAction.TabNext, R.string.jarnfe_canvas_hint_grow) { editor.step { resized(1) } }
    val axisLabel = when (axis) {
        CanvasResizeAxis.Width -> R.string.jarnfe_canvas_hint_axis_width
        CanvasResizeAxis.Height -> R.string.jarnfe_canvas_hint_axis_height
    }
    on(JarnAction.Context, axisLabel) { editor.step { axisToggled() } }
    on(JarnAction.Confirm, R.string.jarnfe_canvas_hint_drop, onPress = onDrop)
    grabCancelBinding(editor, isNewTile, onUndoAdd)
}

private fun JarnBindingsBuilder.grabCancelBinding(
    editor: CanvasEditor,
    isNewTile: Boolean,
    onUndoAdd: () -> Unit,
) {
    if (isNewTile) {
        on(JarnAction.Back, R.string.jarnfe_canvas_hint_undo_add, onPress = onUndoAdd)
    } else {
        on(JarnAction.Back, R.string.jarnfe_canvas_hint_cancel, onPress = editor::cancelGrab)
    }
}

@Composable
private fun CanvasPageHeader(page: JarnCanvasPage?, pageCount: Int, homePage: Int, editing: Boolean) {
    if (page == null) return
    val colors = JarnTheme.colors
    val title = stringResource(R.string.jarnfe_canvas_page_of, page.index + 1, pageCount)
    val detail = when {
        editing -> stringResource(R.string.jarnfe_canvas_editing)
        page.index == homePage -> stringResource(R.string.jarnfe_canvas_home)
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = JarnTheme.space.deckMargin, vertical = JarnTheme.space.xs),
        horizontalArrangement = Arrangement.spacedBy(JarnTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        JarnText(title, JarnTheme.type.labelStrong, colors.textHigh, maxLines = 1)
        detail?.let {
            JarnText(it, JarnTheme.type.caption, colors.readableOverWallpaper(colors.accent), maxLines = 1)
        }
    }
}

@Composable
private fun CanvasNotice(titleRes: Int, messageRes: Int) {
    JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
}
