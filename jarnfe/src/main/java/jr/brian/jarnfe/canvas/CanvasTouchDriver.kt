package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import kotlin.math.abs
import kotlin.math.roundToInt

/** The board's cell layout in pixels, in the coordinates of the scrolled content. */
internal data class CanvasBoardGeometry(val cellPx: Float, val spacingPx: Float, val paddingPx: Float) {
    val pitch: Float get() = cellPx + spacingPx

    fun extentOf(span: Int): Float = cellPx * span + spacingPx * (span - 1)

    fun originOf(rect: GridRect): Offset =
        Offset(paddingPx + pitch * rect.col, paddingPx + pitch * rect.row)

    fun contains(rect: GridRect, point: Offset): Boolean {
        val origin = originOf(rect)
        val width = extentOf(rect.colSpan)
        val height = extentOf(rect.rowSpan)
        return point.x >= origin.x && point.x < origin.x + width &&
            point.y >= origin.y && point.y < origin.y + height
    }

    /** Changes the index only once the tile is past the cell's midpoint by [hysteresis]. */
    fun axisCell(tilePx: Float, current: Int, hysteresis: Float): Int {
        if (pitch <= 0f) return current
        val exact = (tilePx - paddingPx) / pitch
        return if (abs(exact - current) > 0.5f + hysteresis) exact.roundToInt() else current
    }
}

/** What the current page lets touch do. Replaced on every composition. */
internal class CanvasTouchActions(
    val enabled: Boolean,
    val layout: CanvasLayout?,
    val minSpans: (CanvasItem) -> Pair<Int, Int>,
    val openMenu: (CanvasItem) -> Unit,
    val commit: (CanvasLayout, Map<String, GridRect>) -> Unit,
    val onGrabbed: (String) -> Unit,
    val haptic: () -> Unit,
    val showBlocked: () -> Unit,
    val tick: () -> Unit = {},
    val onDropped: (CanvasGrab, Map<String, GridRect>) -> Unit = { _, _ -> },
    val removeTile: (CanvasGrab) -> Unit = {},
    val hasCarryTarget: (Int) -> Boolean = { false },
    val carry: (Int) -> Boolean = { false },
    val moveBetweenPages: ((CanvasGrabDrop.BetweenPages) -> Unit)? = null,
    val addAt: (GridCell) -> Unit = {},
    val addToFolder: (CanvasFolderDrop) -> Unit = {},
)

/** Turns gesture effects into moves on the one shared grab in [CanvasEditor]. */
@Stable
internal class CanvasTouchDriver(
    private val editor: CanvasEditor,
    private val thresholds: CanvasTouchThresholds = CanvasTouchThresholds(),
    private val density: Float = 1f,
    private val toWindow: (Offset) -> Offset = { it },
    private val fromWindow: (Offset) -> Offset = { it },
    private val clockMs: () -> Long = { System.nanoTime() / NANOS_PER_MS },
) {
    var actions: CanvasTouchActions? = null
    var geometry: CanvasBoardGeometry? = null

    /** The widget holding the finger, so a second finger elsewhere on the board can reach it. */
    var activeWidget: CanvasWidgetFinger? = null

    /** True once a widget's own list took this finger, so it can't also swipe the page. */
    var widgetClaimedFinger = false

    var scrollPort: CanvasScrollPort? = null
    var viewport: LayoutCoordinates? = null

    /** True while a held tile is in an edge zone, the only time a frame loop needs to run. */
    var inEdgeZone by mutableStateOf(false)
        private set

    private val edges = CanvasEdgeThresholds()
    private val edgeDwell = CanvasEdgeDwell(edges.dwellMs)
    private var fingerWindow = Offset.Unspecified
    private var arrivalDirection = 0

    private val widgetFrames = mutableMapOf<String, CanvasWidgetFinger>()

    /** The tile held but not moved yet. It isn't a grab, so letting go can still open its menu. */
    var liftedId by mutableStateOf<String?>(null)
        private set

    /** The tile riding above the board, for its whole lift, drag and settle. */
    val visual = CanvasDragVisual()

    val dock = CanvasRemoveDock()

    val folderHover = CanvasFolderHover(thresholds.folderDwellMs)

    /** True from a hold on an empty cell until that finger lifts, so it can't also swipe the page. */
    var holdsEmptyCell = false
        private set

    private var reflowPaused = false

    /** The tile showing resize handles in Edit layout. */
    var selectedId by mutableStateOf<String?>(null)
        private set

    val handleTileId: String?
        get() {
            val grab = editor.grab
            return selectedId?.takeIf {
                editor.editing && (grab == null || (grab.itemId == it && grab.resizeCorner != null))
            }
        }

    val showsDock: Boolean
        get() = editor.grab?.let { it.driver == CanvasGrabDriver.Touch && it.resizeCorner == null } == true

    /** The tile a finger holds right now. A controller that took over isn't a finger. */
    val heldId: String?
        get() = editor.grab?.takeIf { it.driver == CanvasGrabDriver.Touch }?.itemId ?: liftedId

    private var grabOffset = Offset.Zero
    private var cellCol = 0
    private var cellRow = 0
    private var pressedCorner: CanvasResizeCorner? = null
    private var resizeFrom: GridRect? = null
    private var resizeTarget: GridRect? = null
    private var tappedId: String? = null
    private var tappedAtMs = 0L

    fun tileAt(point: Offset): String? {
        val board = geometry ?: return null
        val active = actions?.takeIf { it.enabled } ?: return null
        val layout = active.layout ?: return null
        return editor.placements(layout).entries.firstOrNull { board.contains(it.value, point) }?.key
    }

    /** A free cell a hold can add to. Never one while a grab is running. */
    fun emptyCellAt(point: Offset): GridCell? {
        val board = geometry ?: return null
        val layout = actions?.takeIf { it.enabled }?.layout ?: return null
        if (editor.grab != null) return null
        return canvasEmptyCell(board, layout, editor.placements(layout), point)
    }

    fun handleEmptyHold(gesture: CanvasTouchGesture, effect: CanvasTouchEffect, cell: GridCell) {
        when (effect) {
            CanvasTouchEffect.Lift ->
                if (actions?.enabled == true && editor.grab == null) {
                    holdsEmptyCell = true
                    actions?.haptic?.invoke()
                } else {
                    gesture.abandon()
                }
            CanvasTouchEffect.OpenMenu -> {
                val active = actions?.takeIf { holdsEmptyCell && it.enabled && editor.grab == null }
                holdsEmptyCell = false
                active?.addAt?.invoke(cell)
            }
            CanvasTouchEffect.BeginDrag -> gesture.abandon()
            else -> Unit
        }
    }

    fun endEmptyHold() {
        holdsEmptyCell = false
    }

    /** The handle under [point], remembered for the resize this press is about to start. */
    fun pressCorner(point: Offset): CanvasResizeCorner? {
        pressedCorner = null
        val board = geometry ?: return null
        val layout = actions?.takeIf { it.enabled }?.layout ?: return null
        if (editor.grab != null) return null
        val id = handleTileId ?: return null
        val rect = editor.placements(layout)[id] ?: return null
        pressedCorner = canvasHandleAt(board, rect, point, density)
        return pressedCorner
    }

    fun clearSelection() {
        selectedId = null
    }

    /** True once for the click that follows a finger tap, which selects instead of grabbing. */
    fun claimEditTap(id: String): Boolean {
        val claimed = tappedId == id && clockMs() - tappedAtMs <= EDIT_TAP_CLAIM_MS
        tappedId = null
        return claimed
    }

    /** Only a widget with a live view runs its own hold. A broken one lifts like any other tile. */
    fun attachWidget(id: String, frame: CanvasWidgetFinger) {
        widgetFrames[id] = frame
    }

    fun detachWidget(id: String, frame: CanvasWidgetFinger) {
        if (widgetFrames[id] === frame) widgetFrames.remove(id)
    }

    fun startFor(id: String): CanvasTouchStart {
        val grab = editor.grab
        return when {
            grab != null && grab.itemId == id -> CanvasTouchStart.TakeOver
            grab != null -> {
                actions?.showBlocked?.invoke()
                CanvasTouchStart.Blocked
            }
            editor.editing -> CanvasTouchStart.Edit
            id in widgetFrames -> CanvasTouchStart.BrowseWidget
            else -> CanvasTouchStart.Browse
        }
    }

    /** A widget only runs its own hold from browse. Edit layout goes through the board. */
    fun widgetCanLift(): Boolean =
        actions?.enabled == true && editor.grab == null && !editor.editing

    fun handle(gesture: CanvasTouchGesture, effect: CanvasTouchEffect, id: String, press: Offset, point: Offset) {
        if (gesture.phase == CanvasTouchPhase.Lifted && liftedId == id) follow(point)
        when (effect) {
            CanvasTouchEffect.None -> Unit
            CanvasTouchEffect.Tap -> selectOnTap(id)
            CanvasTouchEffect.Lift -> lift(gesture, id, press, point)
            CanvasTouchEffect.OpenMenu -> {
                liftedId = null
                itemFor(id)?.let { actions?.openMenu?.invoke(it) }
            }
            CanvasTouchEffect.BeginDrag -> beginDrag(gesture, id, press, point)
            CanvasTouchEffect.BeginResize -> beginResize(gesture, id)
            CanvasTouchEffect.TakeOver -> takeOver(gesture, id, press)
            CanvasTouchEffect.Move -> moveTo(point)
            CanvasTouchEffect.Resize -> resizeTo(press, point)
            CanvasTouchEffect.Drop -> drop()
            CanvasTouchEffect.Unlift -> liftedId = null
            CanvasTouchEffect.CancelDrag -> {
                dock.reset()
                clearFolderHover()
                if (editor.grab?.driver == CanvasGrabDriver.Touch) editor.cancelGrab()
            }
        }
    }

    private fun selectOnTap(id: String) {
        if (!editor.editing || editor.grab != null) return
        selectedId = id
        tappedId = id
        tappedAtMs = clockMs()
    }

    private fun beginResize(gesture: CanvasTouchGesture, id: String) {
        val active = actions
        val item = itemFor(id)
        val layout = active?.layout
        val corner = pressedCorner
        if (active == null || item == null || layout == null || corner == null) {
            gesture.abandon()
            return
        }
        val (minCol, minRow) = active.minSpans(item)
        if (!editor.beginTouch(layout, id, minCol, minRow, corner)) {
            gesture.abandon()
            return
        }
        active.onGrabbed(id)
        resizeFrom = editor.grab?.rect
        resizeTarget = resizeFrom
        active.tick()
    }

    private fun resizeTo(press: Offset, point: Offset) {
        val board = geometry?.takeIf { it.pitch > 0f } ?: return
        val grab = editor.grab?.takeIf { it.driver == CanvasGrabDriver.Touch } ?: return
        val corner = grab.resizeCorner ?: return
        val from = resizeFrom ?: return
        val target = canvasResizeTarget(
            from = from,
            corner = corner,
            deltaCol = ((point.x - press.x) / board.pitch).roundToInt(),
            deltaRow = ((point.y - press.y) / board.pitch).roundToInt(),
            minColSpan = grab.minColSpan,
            minRowSpan = grab.minRowSpan,
            crossAxisCount = grab.baseline.config.crossAxisCount,
            direction = grab.baseline.config.pushDirection,
        )
        if (target == resizeTarget) return
        resizeTarget = target
        val shown = grab.rect
        editor.drive(CanvasGrabDriver.Touch) { resizedTo(target) }
        if (editor.grab?.rect != shown) actions?.tick?.invoke()
    }

    private fun lift(gesture: CanvasTouchGesture, id: String, press: Offset, point: Offset) {
        if (editor.grab != null) {
            gesture.abandon()
            return
        }
        liftedId = id
        actions?.layout?.let(editor::placements)?.get(id)?.let { floatFrom(id, it, press) }
        follow(point)
        actions?.haptic?.invoke()
    }

    private fun beginDrag(gesture: CanvasTouchGesture, id: String, press: Offset, point: Offset) {
        liftedId = null
        val active = actions
        val item = itemFor(id)
        val layout = active?.layout
        if (active == null || item == null || layout == null) {
            gesture.abandon()
            return
        }
        val (minCol, minRow) = active.minSpans(item)
        if (!editor.beginTouch(layout, id, minCol, minRow)) {
            gesture.abandon()
            return
        }
        active.onGrabbed(id)
        dock.reset()
        clearFolderHover()
        edgeDwell.reset()
        anchor(press)
        moveTo(point)
    }

    private fun takeOver(gesture: CanvasTouchGesture, id: String, press: Offset) {
        if (!editor.takeOver(CanvasGrabDriver.Touch, id)) {
            gesture.abandon()
            return
        }
        actions?.haptic?.invoke()
        clearFolderHover()
        anchor(press)
        trackFinger(press)
    }

    private fun anchor(press: Offset) {
        val grab = editor.grab ?: return
        val rect = grab.rect
        floatFrom(grab.itemId, rect, press)
        cellCol = rect.col
        cellRow = rect.row
    }

    private fun floatFrom(id: String, rect: GridRect, press: Offset) {
        val origin = geometry?.originOf(rect) ?: Offset.Zero
        grabOffset = press - origin
        visual.hold(id, origin.x, origin.y)
    }

    private fun follow(point: Offset) {
        visual.follow(point.x - grabOffset.x, point.y - grabOffset.y)
    }

    private fun moveTo(point: Offset) {
        trackFinger(point)
        place(point)
    }

    private fun trackFinger(point: Offset) {
        fingerWindow = toWindow(point)
        val inZone = fingerInEdgeZone()
        if (!inZone) edgeDwell.reset()
        inEdgeZone = inZone
    }

    private fun fingerInEdgeZone(): Boolean {
        val port = scrollPort ?: return false
        val bounds = viewport?.takeIf { it.isAttached }?.boundsInWindow() ?: return false
        val finger = fingerWindow.takeIf { it.isSpecified } ?: return false
        val edgePx = edges.edgeDp * density
        return canvasEdgeDepth(finger.x, bounds.left, bounds.right, edgePx, edges.minDepth) != 0f ||
            (!port.horizontal && canvasEdgeDepth(finger.y, bounds.top, bounds.bottom, edgePx, edges.minDepth) != 0f)
    }

    /**
     * One frame of a held drag: scrolls at an edge, then flips after a dwell at the scroll end. The
     * finger is still while this runs, so its board point is rebuilt from the window plus the scroll.
     */
    fun edgeFrame(nowMs: Long, dtMs: Long) {
        val grab = editor.grab?.takeIf { it.driver == CanvasGrabDriver.Touch && it.resizeCorner == null } ?: return
        val active = actions ?: return
        val port = scrollPort ?: return
        val bounds = viewport?.takeIf { it.isAttached }?.boundsInWindow() ?: return
        val finger = fingerWindow.takeIf { it.isSpecified } ?: return
        if (dock.armed) {
            edgeDwell.reset()
            return
        }
        val edgePx = edges.edgeDp * density
        val side = canvasEdgeDepth(finger.x, bounds.left, bounds.right, edgePx, edges.minDepth)
        val along = if (port.horizontal) side else canvasEdgeDepth(finger.y, bounds.top, bounds.bottom, edgePx, edges.minDepth)
        val scrolled = if (along == 0f) 0f else port.scrollBy(along * edges.scrollDpPerMs * density * dtMs)
        val board = fromWindow(finger)
        if (board.isSpecified) {
            place(board + if (port.horizontal) Offset(scrolled, 0f) else Offset(0f, scrolled))
        }
        val direction = if (side > 0f) 1 else if (side < 0f) -1 else 0
        val atEnd = !port.horizontal || scrolled == 0f
        val canFlip = folderHover.targetId == null && active.hasCarryTarget(direction)
        val flip = edgeDwell.step(direction, atEnd, canFlip, nowMs)
        if (flip != 0 && editor.grab?.itemId == grab.itemId && active.carry(flip)) {
            arrivalDirection = flip
            clearFolderHover()
            editor.grab?.rect?.let {
                cellCol = it.col
                cellRow = it.row
            }
            active.tick()
        }
    }

    /** The side a flip came in from, read once so the new page opens at the matching end. */
    fun takeArrivalDirection(): Int = arrivalDirection.also { arrivalDirection = 0 }

    /** Every finger move lands here. It writes two floats and solves only on a new cell. */
    private fun place(point: Offset) {
        val board = geometry ?: return
        if (editor.grab?.driver != CanvasGrabDriver.Touch) return
        follow(point)
        if (trackDock(point)) {
            clearFolderHover()
            return
        }
        if (trackFolder(board, point)) return
        val hysteresis = if (reflowPaused) RESUME_HYSTERESIS else thresholds.cellHysteresis
        reflowPaused = false
        val col = board.axisCell(point.x - grabOffset.x, cellCol, hysteresis)
        val row = board.axisCell(point.y - grabOffset.y, cellRow, hysteresis)
        if (col == cellCol && row == cellRow) return
        cellCol = col
        cellRow = row
        editor.drive(CanvasGrabDriver.Touch) { movedTo(col, row) }
    }

    /** True while the tile is over the remove bar, where its neighbors go back and stay put. */
    private fun trackDock(point: Offset): Boolean {
        if (!showsDock) return false
        val window = fingerWindow
        if (!window.isSpecified) return dock.armed
        if (dock.update(window)) {
            actions?.haptic?.invoke()
            editor.drive(CanvasGrabDriver.Touch) { previewCleared() }
            editor.grab?.rect?.let {
                cellCol = it.col
                cellRow = it.row
            }
        }
        return dock.armed
    }

    /** True while the finger aims at a folder, which holds every tile at its frozen spot. */
    private fun trackFolder(board: CanvasBoardGeometry, point: Offset): Boolean {
        val active = actions ?: return false
        val grab = editor.grab?.takeIf { it.driver == CanvasGrabDriver.Touch } ?: return false
        val targets = active.layout?.let { canvasFolderTargets(it, grab) }.orEmpty()
        val folderId = canvasFolderUnder(board, targets, point, thresholds.folderInset)
        val entering = folderId != null && folderHover.targetId == null
        if (folderHover.update(folderId, clockMs())) active.haptic()
        if (entering) {
            reflowPaused = true
            editor.drive(CanvasGrabDriver.Touch) { previewCleared() }
            editor.grab?.rect?.let {
                cellCol = it.col
                cellRow = it.row
            }
        }
        return folderId != null
    }

    fun folderDwellLeftMs(): Long = folderHover.remainingMs(clockMs())

    fun tickFolderDwell() {
        val target = folderHover.targetId ?: return
        if (editor.grab?.driver != CanvasGrabDriver.Touch) {
            clearFolderHover()
            return
        }
        if (folderHover.update(target, clockMs())) actions?.haptic?.invoke()
    }

    /** What the board should show for [tileId], only while a finger holds the grab. */
    fun folderDropState(tileId: String): CanvasFolderDropState? {
        val grab = editor.grab?.takeIf { it.driver == CanvasGrabDriver.Touch } ?: return null
        val aimedAt = tileId == folderHover.targetId
        return when {
            aimedAt && folderHover.armed -> CanvasFolderDropState.Armed
            aimedAt -> CanvasFolderDropState.Hover
            tileId == grab.itemId && folderHover.armed -> CanvasFolderDropState.Armed
            else -> null
        }
    }

    private fun clearFolderHover() {
        folderHover.reset()
        reflowPaused = false
    }

    /** A controller that took the grab mid-drag keeps it, so lifting the finger can't drop it. */
    private fun drop() {
        val finished = editor.grab?.takeIf { it.driver == CanvasGrabDriver.Touch } ?: return
        val active = actions
        val layout = active?.layout
        if (active == null || layout == null) {
            dock.reset()
            editor.cancelGrab()
            return
        }
        if (dock.armed && showsDock) {
            removeHeld(active)
            return
        }
        dock.reset()
        if (dropIntoFolder(active, layout, finished)) return
        clearFolderHover()
        editor.drop(moveBetweenPages = active.moveBetweenPages) { placements ->
            active.commit(layout, placements)
            active.onDropped(finished, placements)
        }
    }

    /** Only an armed folder takes the tile. Letting go before that puts it back where it was. */
    private fun dropIntoFolder(active: CanvasTouchActions, layout: CanvasLayout, held: CanvasGrab): Boolean {
        val target = folderHover.targetId ?: return false
        folderHover.update(target, clockMs())
        if (!folderHover.armed) return false
        val app = layout.items.firstOrNull { it.id == held.itemId } as? CanvasItem.AppItem ?: return false
        val folder = layout.items.firstOrNull { it.id == target } as? CanvasItem.FolderItem ?: return false
        clearFolderHover()
        val taken = editor.takeForFolder() ?: return false
        visual.finish()
        if (selectedId == taken.itemId) selectedId = null
        active.addToFolder(CanvasFolderDrop(taken, app.packageName, folder.id, folder.folderId))
        return true
    }

    /** Finishing the visual here skips the settle, so the tile leaves from under the finger. */
    private fun removeHeld(active: CanvasTouchActions) {
        dock.reset()
        val taken = editor.takeForRemoval() ?: return
        visual.finish()
        if (selectedId == taken.itemId) selectedId = null
        active.removeTile(taken)
    }

    private fun itemFor(id: String): CanvasItem? = actions?.layout?.items?.firstOrNull { it.id == id }
}

private const val NANOS_PER_MS = 1_000_000L
private const val EDIT_TAP_CLAIM_MS = 400L
private const val RESUME_HYSTERESIS = -0.5f

/** Lets the board tell a widget about a finger that landed outside of it. */
internal fun interface CanvasWidgetFinger {
    fun onOtherFinger(palmLike: Boolean)
}
