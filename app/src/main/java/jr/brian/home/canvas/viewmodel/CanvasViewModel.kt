package jr.brian.home.canvas.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jr.brian.home.canvas.data.CanvasLayoutManager
import jr.brian.home.canvas.data.CanvasTabType
import jr.brian.home.canvas.grid.GridSolver
import jr.brian.home.canvas.grid.LayoutSnapshot
import jr.brian.home.canvas.grid.toSnapshot
import jr.brian.home.canvas.grid.withSnapshot
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.CanvasUiState
import jr.brian.home.canvas.model.EsdeArtType
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.ResolvedCanvasItem
import jr.brian.home.data.FolderManager
import jr.brian.home.data.PinnedRomManager
import jr.brian.home.model.app.AppInfo
import jr.brian.home.model.app.Folder
import jr.brian.home.model.rom.PinnedRomInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CanvasViewModel @Inject constructor(
    private val canvasLayoutManager: CanvasLayoutManager,
    private val folderManager: FolderManager,
    private val pinnedRomManager: PinnedRomManager
) : ViewModel() {

    /**
     * Add a display-only ES-DE art tile to the current canvas page. The tile
     * binds reactively to
     * [jr.brian.home.esde.util.LocalEsdeWallpaperState] at render time — no
     * system / game binding is stored on the item itself. Placement is
     * auto-computed into both arrangements via the standard
     * [canvasLayoutManager.addItem] path.
     */
    fun addEsdeArtItem(artType: EsdeArtType) {
        val pageIndex = boundPage() ?: return
        canvasLayoutManager.addItem(
            pageIndex = pageIndex,
            item = CanvasItem.EsdeArtItem(
                id = "esde-${artType.name.lowercase()}-${UUID.randomUUID()}",
                artType = artType
            )
        )
    }

    private val _pageIndex = MutableStateFlow(UNBOUND_PAGE)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())

    private val foldersFlow = _pageIndex.flatMapLatest { idx ->
        if (idx < 0) flowOf(emptyList())
        else folderManager.getFolders(idx, CanvasTabType.VALUE)
    }

    private val romsFlow = _pageIndex.flatMapLatest { idx ->
        if (idx < 0) flowOf(emptyList())
        else pinnedRomManager.getPinnedRoms(idx, CanvasTabType.VALUE)
    }

    /** Live canvas-owned folders for the currently bound page. */
    val canvasFolders: StateFlow<List<Folder>> = foldersFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Live canvas-owned ROMs for the currently bound page. */
    val canvasRoms: StateFlow<List<PinnedRomInfo>> = romsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Every ROM the user has pinned anywhere in the app, deduplicated by key.
     * Used by the canvas picker so existing ROMs can be re-pinned to the canvas
     * without going back through ES-DE search.
     */
    val allPinnedRoms: StateFlow<List<PinnedRomInfo>> = pinnedRomManager.allPinnedRoms
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Re-pin [rom] to the current canvas page (under [CanvasTabType.VALUE]) and add
     * a matching [CanvasItem.RomItem] to the layout if it isn't already there.
     */
    fun pinRomToCanvas(rom: PinnedRomInfo) {
        val pageIndex = boundPage() ?: return
        pinnedRomManager.addPinnedRom(pageIndex, rom, CanvasTabType.VALUE)
        val layout = canvasLayoutManager.getLayout(pageIndex)
        val alreadyOnCanvas = layout.items
            .filterIsInstance<CanvasItem.RomItem>()
            .any { it.romKey == rom.key }
        if (!alreadyOnCanvas) {
            canvasLayoutManager.addItem(
                pageIndex,
                CanvasItem.RomItem(
                    id = "rom-${rom.key}",
                    romKey = rom.key
                )
            )
        }
    }

    val uiState: StateFlow<CanvasUiState> = combine(
        _pageIndex,
        canvasLayoutManager.layoutsByPage,
        _apps,
        foldersFlow,
        romsFlow
    ) { pageIndex, layouts, apps, folders, roms ->
        val layout = if (pageIndex >= 0) layouts[pageIndex] ?: CanvasLayout() else CanvasLayout()
        CanvasUiState(
            pageIndex = pageIndex,
            layout = layout,
            resolvedItems = resolveAll(layout.items, apps, folders, roms)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CanvasUiState())

    fun setPageIndex(pageIndex: Int) {
        _pageIndex.value = pageIndex
    }

    fun setApps(apps: List<AppInfo>) {
        _apps.value = apps
    }

    fun addItem(item: CanvasItem) {
        boundPage()?.let { canvasLayoutManager.addItem(it, item) }
    }

    fun addItem(item: CanvasItem, colSpan: Int, rowSpan: Int) {
        boundPage()?.let {
            canvasLayoutManager.addItem(it, item, colSpan = colSpan, rowSpan = rowSpan)
        }
    }

    fun moveItem(id: String, col: Int, row: Int) {
        boundPage()?.let { canvasLayoutManager.moveItem(it, id, col, row) }
    }

    fun resizeItem(id: String, colSpan: Int, rowSpan: Int) {
        boundPage()?.let { canvasLayoutManager.resizeItem(it, id, colSpan, rowSpan) }
    }

    fun removeItem(id: String) {
        boundPage()?.let { canvasLayoutManager.removeItem(it, id) }
    }

    /** Move an item in the layout list, used by drag-to-reposition. */
    fun reorderItems(fromIndex: Int, toIndex: Int) {
        boundPage()?.let { canvasLayoutManager.reorderItems(it, fromIndex, toIndex) }
    }

    /**
     * Replace the current page's item placements with [snapshot]. Used by the
     * solver-driven drag/resize gestures to commit a multi-item layout update
     * atomically (one push to persistence per gesture, not per neighbor).
     * Preserves the layout's orientation, grid dimensions, and edit-mode flag.
     */
    fun commitLayoutSnapshot(snapshot: LayoutSnapshot) {
        val pageIndex = boundPage() ?: return
        val current = canvasLayoutManager.getLayout(pageIndex)
        canvasLayoutManager.replaceLayout(pageIndex, current.withSnapshot(snapshot))
    }

    /**
     * Resize [id] to ([colSpan], [rowSpan]) via [GridSolver.solveResize], pushing
     * any overlapped neighbors along the push axis (cascading). Honors the given
     * minimum spans (use widget-derived mins for [CanvasItem.WidgetItem]; 1×1 for
     * other variants). Persists the resulting placement atomically. No-op if the
     * item isn't on the current page.
     */
    fun resizeItemWithSolver(
        id: String,
        colSpan: Int,
        rowSpan: Int,
        minColSpan: Int = 1,
        minRowSpan: Int = 1
    ) {
        val pageIndex = boundPage() ?: return
        val current = canvasLayoutManager.getLayout(pageIndex)
        val baseline = current.toSnapshot()
        val existing = baseline.placements[id] ?: return
        val newRect = GridRect(existing.col, existing.row, colSpan, rowSpan)
        val result = GridSolver.solveResize(baseline, id, newRect, minColSpan, minRowSpan)
        canvasLayoutManager.replaceLayout(pageIndex, current.withSnapshot(result.snapshot))
    }

    /**
     * Pull items in the active orientation toward the grid origin, closing
     * gaps left by previous moves/deletes/shrinks. The inactive orientation is
     * untouched. This is the **only** path that closes gaps — solvers preserve
     * them — so users explicitly opt in from the canvas edit menu.
     */
    fun compactLayout() {
        boundPage()?.let { canvasLayoutManager.compactLayout(it) }
    }

    fun setOrientation(orientation: CanvasScrollOrientation) {
        boundPage()?.let { canvasLayoutManager.setOrientation(it, orientation) }
    }

    fun setGrid(columns: Int, rows: Int) {
        boundPage()?.let { canvasLayoutManager.setGrid(it, columns, rows) }
    }

    fun setEditMode(enabled: Boolean) {
        boundPage()?.let { canvasLayoutManager.setEditMode(it, enabled) }
    }

    init {
        // Reactive folder sync: any canvas-owned folder that appears in FolderManager
        // (created by CreateFolderDialog) auto-gets a CanvasItem.FolderItem on the
        // current canvas page. Removing the FolderItem also deletes the folder
        // entity (see `removeItem`), so this loop won't immediately re-add it.
        viewModelScope.launch {
            canvasFolders.collect { folders ->
                val pageIndex = boundPage() ?: return@collect
                val layout = canvasLayoutManager.getLayout(pageIndex)
                val referenced = layout.items
                    .filterIsInstance<CanvasItem.FolderItem>()
                    .map { it.folderId }
                    .toSet()
                folders.filter { it.id !in referenced }
                    .forEach { folder ->
                        canvasLayoutManager.addItem(
                            pageIndex,
                            CanvasItem.FolderItem(
                                id = "folder-${folder.id}",
                                folderId = folder.id
                            )
                        )
                    }
            }
        }
    }

    /**
     * Remove an item from the canvas. For folders, this also deletes the
     * underlying folder entity so the reactive sync doesn't immediately re-add
     * a FolderItem for it.
     */
    fun removeItemAndCleanup(item: CanvasItem) {
        val pageIndex = boundPage() ?: return
        if (item is CanvasItem.FolderItem) {
            viewModelScope.launch {
                folderManager.deleteFolder(pageIndex, item.folderId, CanvasTabType.VALUE)
            }
        }
        canvasLayoutManager.removeItem(pageIndex, item.id)
    }

    private fun boundPage(): Int? = _pageIndex.value.takeIf { it >= 0 }

    private fun resolveAll(
        items: List<CanvasItem>,
        apps: List<AppInfo>,
        folders: List<Folder>,
        roms: List<PinnedRomInfo>
    ): List<ResolvedCanvasItem> {
        val appsByPkg = apps.associateBy { it.packageName }
        val foldersById = folders.associateBy { it.id }
        val romsByKey = roms.associateBy { it.key }
        return items.map { item ->
            when (item) {
                is CanvasItem.AppItem ->
                    ResolvedCanvasItem.App(item, appsByPkg[item.packageName])
                is CanvasItem.FolderItem ->
                    ResolvedCanvasItem.Folder(item, foldersById[item.folderId])
                is CanvasItem.RomItem ->
                    ResolvedCanvasItem.Rom(item, romsByKey[item.romKey])
                is CanvasItem.WidgetItem ->
                    ResolvedCanvasItem.Widget(item)
                is CanvasItem.RssLauncherItem ->
                    ResolvedCanvasItem.RssLauncher(item)
                is CanvasItem.EsdeArtItem ->
                    ResolvedCanvasItem.EsdeArt(item)
            }
        }
    }

    companion object {
        private const val UNBOUND_PAGE = -1
    }
}
