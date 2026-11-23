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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import jr.brian.core.ui.components.JarnEmptyState
import jr.brian.core.ui.components.JarnText
import jr.brian.core.ui.theme.JarnTheme
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnBindingsBuilder
import jr.brian.jarnfe.input.JarnInputLayer
import jr.brian.jarnfe.input.JarnLayerMode
import jr.brian.jarnfe.input.LocalJarnInputRouter
import jr.brian.jarnfe.input.LocalJarnScreen
import jr.brian.jarnfe.library.JarnLibrary
import jr.brian.jarnfe.library.LibrarySelection
import jr.brian.jarnfe.session.DeckTab
import jr.brian.jarnfe.session.JarnSession
import jr.brian.jarnfe.session.JarnSessionState
import jr.brian.jarnfe.shell.JarnNotices

private enum class CanvasSheet { None, Options, Pages }

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
    var sheet by remember { mutableStateOf(CanvasSheet.None) }
    var listTarget by remember { mutableStateOf<CanvasListTarget?>(null) }
    val selected = JarnCanvas.selectedPage
    val page = snapshot.pages.getOrNull(selected)
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val launchRom = rememberCanvasRomLauncher()
    val launchApp: (JarnCanvasApp) -> Unit = { app ->
        activity?.let { data.launchApp(it, app.packageName) }
    }
    val sheetOpen = sheet != CanvasSheet.None || listTarget != null

    Box(Modifier.fillMaxSize()) {
        key(page?.index) {
            CanvasPageSurface(
                data = data,
                page = page,
                snapshot = snapshot,
                state = state,
                pageCount = snapshot.pages.size,
                sheetOpen = sheetOpen,
                onOptions = { sheet = CanvasSheet.Options },
                onOpenList = { listTarget = it },
                launchApp = launchApp,
                launchRom = launchRom,
                optionsSheet = { startEdit ->
                    if (sheet == CanvasSheet.Options) {
                        CanvasOptionsSheet(
                            canEdit = page?.kind == JarnCanvasPageKind.Canvas,
                            onEdit = {
                                sheet = CanvasSheet.None
                                startEdit()
                            },
                            onPages = { sheet = CanvasSheet.Pages },
                            onClose = { sheet = CanvasSheet.None },
                        )
                    }
                },
            )
        }
        if (sheet == CanvasSheet.Pages) {
            CanvasPagesSheet(
                data = data,
                snapshot = snapshot,
                selectedPage = selected,
                onGoTo = { index ->
                    JarnCanvas.selectedPage = index
                    sheet = CanvasSheet.None
                },
                onSelectionMoved = { JarnCanvas.selectedPage = it },
                onClose = { sheet = CanvasSheet.None },
            )
        }
        listTarget?.let { target ->
            CanvasListSheet(
                target = target,
                onLaunchApp = { app ->
                    listTarget = null
                    launchApp(app)
                },
                onLaunchRom = { rom ->
                    listTarget = null
                    launchRom(rom)
                },
                onClose = { listTarget = null },
            )
        }
    }
}

@Composable
private fun CanvasPageSurface(
    data: JarnCanvasData,
    page: JarnCanvasPage?,
    snapshot: JarnCanvasSnapshot,
    state: JarnSessionState,
    pageCount: Int,
    sheetOpen: Boolean,
    onOptions: () -> Unit,
    onOpenList: (CanvasListTarget) -> Unit,
    launchApp: (JarnCanvasApp) -> Unit,
    launchRom: (JarnCanvasRom) -> Unit,
    optionsSheet: @Composable (startEdit: () -> Unit) -> Unit,
) {
    val editor = remember { CanvasEditor() }
    val focus = remember { mutableMapOf<String, FocusRequester>() }
    var lastFocused by remember { mutableStateOf<String?>(null) }
    val router = LocalJarnInputRouter.current
    val screen = LocalJarnScreen.current
    val isActive = router.activeScreen == screen
    val requesterFor: (String) -> FocusRequester = { id -> focus.getOrPut(id) { FocusRequester() } }

    LaunchedEffect(isActive) { if (!isActive) editor.cancelGrab() }
    LaunchedEffect(page?.layout) { editor.settled = null }

    val focusTarget = page?.let { canvasFocusTarget(it, lastFocused, editor.editing) }
    LaunchedEffect(focusTarget, sheetOpen, isActive, editor.editing) {
        if (focusTarget == null || sheetOpen || !isActive) return@LaunchedEffect
        withFrameNanos { }
        runCatching { requesterFor(focusTarget).requestFocus() }
    }

    CanvasInputLayers(
        editor = editor,
        hasTiles = page?.layout?.items?.isNotEmpty() == true,
        sheetOpen = sheetOpen,
        onOptions = onOptions,
        onDrop = {
            val current = page ?: return@CanvasInputLayers
            editor.drop { placements ->
                data.commitPlacements(current.index, current.layout.activeOrientation, placements)
            }
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
                page.kind != JarnCanvasPageKind.Canvas ->
                    CanvasNotice(R.string.jarnfe_canvas_other_page_title, R.string.jarnfe_canvas_other_page_message)
                else -> {
                    val stageArt = canvasStageArt(state)
                    CanvasGrid(
                        page = page,
                        placements = editor.placements(page.layout),
                        editing = editor.editing,
                        grabbedId = editor.grab?.itemId,
                        focusRequesterFor = requesterFor,
                        onFocused = { lastFocused = it },
                        onTileClick = { item, cellPx ->
                            if (editor.editing) {
                                val (minCol, minRow) = minSpansFor(data, item, cellPx)
                                editor.begin(page.layout, item.id, minCol, minRow)
                            } else {
                                activateTile(item, page, snapshot, onOpenList, launchApp, launchRom)
                            }
                        },
                        onMenuClick = onOptions,
                    ) { item -> CanvasTileContent(item, page, snapshot, stageArt) }
                }
            }
        }
    }
    optionsSheet { editor.enter() }
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

private fun canvasStageArt(state: JarnSessionState): CanvasStageArt? =
    when (val selection = JarnLibrary.state.selection(state.stageRoute, state.librarySection)) {
        is LibrarySelection.Game -> CanvasStageArt(selection.game.title, selection.game.artPath)
        is LibrarySelection.System -> CanvasStageArt(selection.system.title, selection.system.artPath)
        LibrarySelection.None -> null
    }

private fun activateTile(
    item: CanvasItem,
    page: JarnCanvasPage,
    snapshot: JarnCanvasSnapshot,
    onOpenList: (CanvasListTarget) -> Unit,
    launchApp: (JarnCanvasApp) -> Unit,
    launchRom: (JarnCanvasRom) -> Unit,
) {
    when (item) {
        is CanvasItem.AppItem -> page.apps[item.packageName]?.let(launchApp) ?: showMissing()
        is CanvasItem.FolderItem ->
            page.folders[item.folderId]?.let { onOpenList(CanvasListTarget.Folder(it)) } ?: showMissing()
        is CanvasItem.RomItem -> page.roms[item.romKey]?.let(launchRom) ?: showMissing()
        is CanvasItem.LastPlayedItem ->
            onOpenList(CanvasListTarget.LastPlayed(snapshot.recent.take(item.resolvedCount)))
        is CanvasItem.EsdeArtItem -> JarnSession.update { copy(deckTab = DeckTab.Now) }
        is CanvasItem.WidgetItem -> JarnNotices.show(R.string.jarnfe_canvas_notice_widget)
        is CanvasItem.PhotoContainer -> JarnNotices.show(R.string.jarnfe_canvas_notice_photo)
        is CanvasItem.RssLauncherItem,
        is CanvasItem.RssMusicItem -> JarnNotices.show(R.string.jarnfe_canvas_notice_rss)
    }
}

private fun showMissing() = JarnNotices.show(R.string.jarnfe_canvas_notice_missing)

@Composable
private fun CanvasInputLayers(
    editor: CanvasEditor,
    hasTiles: Boolean,
    sheetOpen: Boolean,
    onOptions: () -> Unit,
    onDrop: () -> Unit,
) {
    val grab = editor.grab
    JarnInputLayer(
        enabled = !editor.editing && !sheetOpen,
        bindings = {
            if (hasTiles) hint(JarnAction.Confirm, R.string.jarnfe_hint_open)
            on(JarnAction.Context, R.string.jarnfe_hint_context, onPress = onOptions)
        },
    )
    JarnInputLayer(
        mode = JarnLayerMode.Modal,
        enabled = editor.editing && grab == null,
        bindings = {
            if (hasTiles) hint(JarnAction.Confirm, R.string.jarnfe_canvas_hint_grab)
            on(JarnAction.Back, R.string.jarnfe_canvas_hint_done, onPress = editor::exit)
        },
    )
    JarnInputLayer(
        mode = JarnLayerMode.Capture,
        enabled = grab != null,
        bindings = { grabBindings(editor, grab?.axis ?: CanvasResizeAxis.Width, onDrop) },
    )
}

private fun JarnBindingsBuilder.grabBindings(
    editor: CanvasEditor,
    axis: CanvasResizeAxis,
    onDrop: () -> Unit,
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
    on(JarnAction.Back, R.string.jarnfe_canvas_hint_cancel, onPress = editor::cancelGrab)
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
        detail?.let { JarnText(it, JarnTheme.type.caption, colors.accent, maxLines = 1) }
    }
}

@Composable
private fun CanvasNotice(titleRes: Int, messageRes: Int) {
    JarnEmptyState(title = stringResource(titleRes), message = stringResource(messageRes))
}
