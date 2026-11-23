package jr.brian.jarnfe.canvas

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
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
)

/** Turns gesture effects into moves on the one shared grab in [CanvasEditor]. */
@Stable
internal class CanvasTouchDriver(
    private val editor: CanvasEditor,
    private val thresholds: CanvasTouchThresholds = CanvasTouchThresholds(),
    private val density: Float = 1f,
    private val toWindow: (Offset) -> Offset = { it },
    private val clockMs: () -> Long = { System.nanoTime() / NANOS_PER_MS },
) {
    var actions: CanvasTouchActions? = null
    var geometry: CanvasBoardGeometry? = null

    /** The widget holding the finger, so a second finger elsewhere on the board can reach it. */
    var activeWidget: CanvasWidgetFinger? = null

    private val widgetFrames = mutableMapOf<String, CanvasWidgetFinger>()

    /** The tile held but not moved yet. It isn't a grab, so letting go can still open its menu. */
    var liftedId by mutableStateOf<String?>(null)
        private set

    /** The tile riding above the board, for its whole lift, drag and settle. */
    val visual = CanvasDragVisual()

    val dock = CanvasRemoveDock()

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
        anchor(press)
        moveTo(point)
    }

    private fun takeOver(gesture: CanvasTouchGesture, id: String, press: Offset) {
        if (!editor.takeOver(CanvasGrabDriver.Touch, id)) {
            gesture.abandon()
            return
        }
        actions?.haptic?.invoke()
        anchor(press)
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

    /** Every finger move lands here. It writes two floats and solves only on a new cell. */
    private fun moveTo(point: Offset) {
        val board = geometry ?: return
        if (editor.grab?.driver != CanvasGrabDriver.Touch) return
        follow(point)
        if (trackDock(point)) return
        val col = board.axisCell(point.x - grabOffset.x, cellCol, thresholds.cellHysteresis)
        val row = board.axisCell(point.y - grabOffset.y, cellRow, thresholds.cellHysteresis)
        if (col == cellCol && row == cellRow) return
        cellCol = col
        cellRow = row
        editor.drive(CanvasGrabDriver.Touch) { movedTo(col, row) }
    }

    /** True while the tile is over the remove bar, where its neighbors go back and stay put. */
    private fun trackDock(point: Offset): Boolean {
        if (!showsDock) return false
        val window = toWindow(point)
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
        editor.drop { placements ->
            active.commit(layout, placements)
            active.onDropped(finished, placements)
        }
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

/** Lets the board tell a widget about a finger that landed outside of it. */
internal fun interface CanvasWidgetFinger {
    fun onOtherFinger(palmLike: Boolean)
}
