package jr.brian.jarnfe.canvas

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.focus.LocalJarnTabsFocus
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import jr.brian.jarnfe.R
import jr.brian.jarnfe.apps.shouldAskDisplay
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.rss.JarnRssRoute
import jr.brian.jarnfe.rss.JarnRssSheetSlot
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.JarnNotices
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
    var pendingGrab by remember { mutableStateOf<CanvasPendingGrab?>(null) }
    var addTarget by remember { mutableStateOf<CanvasAddTarget?>(null) }
    var widgetRevision by remember { mutableIntStateOf(0) }
    var artPreview by remember { mutableStateOf<CanvasArtPreview?>(null) }
    var arrival by remember { mutableStateOf<CanvasCarriedTile?>(null) }
    var entryFocus by remember { mutableStateOf<String?>(null) }
    val selected = JarnCanvas.selectedPage
    val undo = JarnCanvas.undo
    val undoWrites = remember(data) { CanvasDataUndoWrites(data) }
    val editor = remember { CanvasEditor(navigate = { JarnCanvas.selectedPage = it }) }
    val shownPage = snapshot.pages.getOrNull(selected)
        ?.let { it.withoutTiles(undo.hiddenOn(it.index)).withPendingFolderApps(undo.pendingFolderAdds) }
    editor.showPage(shownPage?.index)
    val carried = editor.grab?.takeIf { it.isAwayFromHome }?.let { canvasCarriedTile(it, snapshot.pages) } ?: arrival
    val page = shownPage?.withCarriedTile(carried)
    CanvasUndoExits(undo)
    LaunchedEffect(selected) { undo.settleForPageChange() }
    LaunchedEffect(arrival, snapshot) {
        val landing = arrival ?: return@LaunchedEffect
        val saved = snapshot.pages.getOrNull(landing.pageIndex)?.layout?.items.orEmpty().any { it.id == landing.item.id }
        if (!saved) delay(CANVAS_SETTLE_TIMEOUT_MS)
        arrival = null
    }
    LaunchedEffect(overlay != null) { if (overlay != null) undo.settle() }
    LaunchedEffect(page?.layout, overlay) {
        val settled = artPreview?.isSettled(page?.layout?.items.orEmpty()) == true
        if (overlay !is CanvasOverlay.ArtMedia && settled) artPreview = null
    }
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

    val pageMotion = rememberCanvasPageMotion(selected)
    Box(Modifier.fillMaxSize().then(pageMotion)) {
        key(editor.surfaceKey) {
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
                entryFocus = entryFocus,
                onEntryFocus = { entryFocus = it },
                onArrival = { arrival = it },
                onPendingGrabHandled = { pendingGrab = null },
                onOptions = { overlay = CanvasOverlay.Options },
                onAddAt = { cell ->
                    addTarget = page?.let { CanvasAddTarget(it.index, cell) }
                    overlay = CanvasOverlay.Add
                },
                handlers = handlers,
                overlays = { commands ->
                    if (overlay == CanvasOverlay.Options) {
                        CanvasOptionsSheet(
                            canEdit = page?.kind == JarnCanvasPageKind.Canvas,
                            alreadyEditing = commands.editing,
                            onAdd = {
                                addTarget = null
                                overlay = CanvasOverlay.Add
                            },
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
                            grabNewTile = { id ->
                                val cell = addTarget?.takeIf { it.pageIndex == page?.index }?.cell
                                addTarget = null
                                pendingGrab = CanvasPendingGrab(id, cell)
                            },
                            previewArt = { artPreview = it },
                            removeTile = { item ->
                                page?.let { undo.removeTile(undoWrites, it.index, item.id) }
                            },
                        ),
                        addingHere = addTarget != null,
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
    pendingGrab: CanvasPendingGrab?,
    entryFocus: String?,
    onEntryFocus: (String?) -> Unit,
    onArrival: (CanvasCarriedTile?) -> Unit,
    onPendingGrabHandled: () -> Unit,
    onOptions: () -> Unit,
    onAddAt: (GridCell) -> Unit,
    handlers: CanvasTileHandlers,
    overlays: @Composable (CanvasSurfaceCommands) -> Unit,
) {
    val rssSheets = remember { JarnRssSheetSlot() }
    val focus = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocused by remember { mutableStateOf(entryFocus) }
    var boardHasFocus by remember { mutableStateOf(false) }
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
            editor.beginMove(it.layout, item.id, minCol, minRow)
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
    val carrier = remember(editor) { CanvasPageCarrier(editor) { JarnCanvas.snapshot?.pages.orEmpty() } }
    val moveBetweenPages: (CanvasGrabDrop.BetweenPages) -> Unit = { drop ->
        moveCanvasTile(drop, snapshot.pages, editor, undo, undoWrites, onArrival, onEntryFocus) { lastFocused = it }
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
        hasCarryTarget = carrier::hasTarget,
        carry = { direction ->
            val result = carrier.carry(direction, enterAtEdge = false)
            result.showNotice()
            result == CanvasCarryResult.Moved
        },
        moveBetweenPages = moveBetweenPages,
        addAt = onAddAt,
        addToFolder = { drop ->
            lastFocused = drop.folderTileId
            undo.addToFolder(undoWrites, canvasPendingFolderAdd(drop, snapshot.pages))
        },
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
        if (editor.grab != null && undo.entry is CanvasPendingTileExit) undo.settle()
    }
    LaunchedEffect(page?.layout, editor.settled) {
        val held = editor.settled ?: return@LaunchedEffect
        if (!canvasPlacementsAgree(held, page?.layout?.activeArrangement.orEmpty())) delay(CANVAS_SETTLE_TIMEOUT_MS)
        editor.settled = null
    }
    LaunchedEffect(pendingGrab, page?.layout) {
        val pending = pendingGrab ?: return@LaunchedEffect
        val id = pending.itemId
        val item = page?.layout?.items?.firstOrNull { it.id == id } ?: return@LaunchedEffect
        if (id in page.layout.activeArrangement) {
            val (minCol, minRow) = minSpansFor(data, item, cellPx)
            lastFocused = id
            editor.beginAdded(page.layout, id, minCol, minRow, pending.cell)
            onPendingGrabHandled()
        }
    }

    val focusTarget = page?.takeIf { it.kind != JarnCanvasPageKind.Rss }
        ?.let { canvasFocusTarget(it, lastFocused, editor.editing) }
    val tabsFocus = LocalJarnTabsFocus.current
    LaunchedEffect(focusTarget, page?.index, sheetOpen, isActive, editor.editing) {
        if (focusTarget == null || sheetOpen || !isActive) return@LaunchedEffect
        withFrameNanos { }
        if (editor.arranging || !tabsFocus.hasFocus) runCatching { requesterFor(focusTarget).requestFocus() }
    }

    val moveArrangeFocus: (Int, Int) -> Unit = { deltaCol, deltaRow ->
        val next = canvasFocusStep(page.visiblePlacements(editor), lastFocused, deltaCol, deltaRow)
            ?: focusTarget.takeUnless { boardHasFocus }
        next?.let { id -> runCatching { requesterFor(id).requestFocus() } }
    }
    val focusedTile = page?.layout?.items?.firstOrNull { it.id == lastFocused }
    val restoreUndo: () -> Unit = {
        val restored = undo.undo(page?.layout?.activeArrangement.orEmpty()) { moved, undone ->
            if (undone) {
                onEntryFocus(moved.move.itemId)
                JarnCanvas.selectedPage = moved.move.fromPage
            } else {
                JarnNotices.show(R.string.jarnfe_canvas_notice_move_failed)
            }
        }
        restored?.let { editor.settled = it }
    }
    val pagePush = canvasPagePush(
        page = page.takeIf { boardHasFocus },
        pages = snapshot.pages,
        focusedId = lastFocused,
        onPage = { target, entry ->
            onEntryFocus(entry)
            JarnCanvas.selectedPage = target
        },
        onCarry = { direction -> carrier.carry(direction, enterAtEdge = true).showNotice() },
    )
    val pageSwipe = rememberCanvasPageSwipe(
        driver = touch.driver,
        editor = editor,
        enabled = !sheetOpen && interactingId == null,
        hasPrevious = page != null && page.index > 0,
        hasNext = page != null && page.index < pageCount - 1,
        onPage = { direction ->
            onEntryFocus(null)
            page?.let { JarnCanvas.selectedPage = it.index + direction }
            touch.driver.actions?.tick?.invoke()
        },
    )
    CanvasInputLayers(
        editor = editor,
        hasTiles = page?.layout?.items?.isNotEmpty() == true,
        confirmLabel = page?.let { focusedConfirmLabel(data, it, focusedTile) },
        ownsButtons = canvasOwnsButtons(boardHasFocus, focusTarget != null),
        sheetOpen = sheetOpen || interactingId != null || page?.kind == JarnCanvasPageKind.Rss,
        pagePush = pagePush,
        onContext = { if (focusedTile != null) handlers.openTileMenu(focusedTile) else onOptions() },
        onOptions = onOptions,
        onUndo = restoreUndo.takeIf { undo.entry != null && editor.grab == null },
        onDrop = {
            val current = page ?: return@CanvasInputLayers
            val finished = editor.grab ?: return@CanvasInputLayers
            editor.drop(moveBetweenPages = moveBetweenPages) { placements ->
                data.commitPlacements(current.index, current.layout.activeOrientation, placements)
                recordDrop(undo, undoWrites, finished, placements)
            }
        },
        onUndoAdd = {
            val homePage = editor.grab?.home?.pageIndex ?: return@CanvasInputLayers
            editor.undoAdd { id -> scope.launch { data.removeTile(homePage, id) } }
        },
        onArrangeMove = moveArrangeFocus,
    )

    Column(Modifier.fillMaxSize()) {
        CanvasPageHeader(
            page = page,
            pageCount = pageCount,
            homePage = snapshot.homePage,
            arrangeStep = editor.arrangeStep,
            moving = editor.editing && !editor.arranging,
            onOptions = onOptions,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onFocusChanged { boardHasFocus = it.hasFocus }
                .canvasPageSwipe(pageSwipe),
            contentAlignment = Alignment.Center,
        ) {
            when {
                page == null -> CanvasNotice(R.string.jarnfe_canvas_no_pages_title, R.string.jarnfe_canvas_no_pages_message)
                page.kind == JarnCanvasPageKind.Rss -> JarnRssRoute(state, sheetOpen, rssSheets, onOptions)
                page.kind != JarnCanvasPageKind.Canvas ->
                    CanvasNotice(R.string.jarnfe_canvas_other_page_title, R.string.jarnfe_canvas_other_page_message)
                page.layout.items.isEmpty() && !editor.editing -> CanvasEmptySpace(
                    page = page,
                    sheetOpen = sheetOpen,
                    onAdd = { onAddAt(canvasFirstOpenCell(page.layout)) },
                    onOptions = onOptions,
                )
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
    page?.let { CanvasPagePeek(pageSwipe, it.index) }
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
    val added = finished.itemId.takeIf { finished.isNewTile }
    undo.recordLayout(
        writes,
        CanvasLayoutChange(
            pageIndex = finished.pageIndex,
            orientation = finished.home.orientation,
            before = before,
            after = placements,
            kind = if (added != null) CanvasUndoKind.Added else canvasUndoKind(finished.itemId, before, placements),
            addedTileId = added,
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

private fun JarnCanvasPage?.visiblePlacements(editor: CanvasEditor): Map<String, GridRect> {
    val shown = this?.layout ?: return emptyMap()
    val ids = shown.items.mapTo(HashSet()) { it.id }
    return editor.placements(shown).filterKeys { it in ids }
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
