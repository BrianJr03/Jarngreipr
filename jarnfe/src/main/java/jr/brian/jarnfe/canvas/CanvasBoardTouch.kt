package jr.brian.jarnfe.canvas

import android.view.MotionEvent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import jr.brian.home.canvas.model.GridCell

/** Holds the board's position so window and screen touches can be read in board coordinates. */
internal class CanvasBoardSpace(private val density: Float) {
    var coordinates: LayoutCoordinates? = null

    fun toWindow(boardPoint: Offset): Offset {
        val board = coordinates?.takeIf { it.isAttached } ?: return Offset.Unspecified
        return board.localToWindow(boardPoint)
    }

    fun fromWindow(window: Offset): Offset {
        val board = coordinates?.takeIf { it.isAttached } ?: return Offset.Unspecified
        return board.windowToLocal(window)
    }

    /** [Offset.Unspecified] until the board is attached, so a move never boxes a nullable. */
    fun fromScreen(x: Float, y: Float): Offset {
        val board = coordinates ?: return Offset.Unspecified
        return if (board.isAttached) board.screenToLocal(Offset(x, y)) else Offset.Unspecified
    }

    fun isPalm(contactPx: Float, boardPoint: Offset): Boolean {
        val board = coordinates?.takeIf { it.isAttached } ?: return false
        val window = board.localToWindow(boardPoint)
        val root = board.findRootCoordinates().size
        return canvasTouchIsPalm(
            contactDp = contactPx / density,
            xDp = window.x / density,
            yDp = window.y / density,
            windowWidthDp = root.width / density,
            windowHeightDp = root.height / density,
        )
    }
}

/**
 * Watches the whole board on the Initial pass. The board doesn't move with a dragged tile, so its
 * coordinates stay stable, and nothing is consumed until a hold or drag actually owns the finger.
 */
internal fun Modifier.canvasBoardTouch(driver: CanvasTouchDriver, space: CanvasBoardSpace): Modifier = this
    .onPlaced { space.coordinates = it }
    .pointerInput(driver, space) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val corner = driver.pressCorner(down.position)
            val id = if (corner != null) driver.selectedId else driver.tileAt(down.position)
            if (id == null) {
                driver.clearSelection()
                val cell = driver.emptyCellAt(down.position)
                if (cell != null) holdEmptyCell(driver, space, cell, down.id, down.position, down.uptimeMillis)
                return@awaitEachGesture
            }
            val start = if (corner != null) CanvasTouchStart.Resize else driver.startFor(id)
            if (start == CanvasTouchStart.BrowseWidget) {
                watchWidgetFingers(driver, space, down.id)
            } else {
                val gesture = CanvasTouchGesture(density)
                val press = down.position
                driver.handle(gesture, gesture.down(start, press.x, press.y, down.uptimeMillis), id, press, press)
                if (gesture.ownsPointer) down.consume()
                trackFinger(space, gesture, down.id, press, down.uptimeMillis) { effect, point ->
                    driver.handle(gesture, effect, id, press, point)
                }
            }
        }
    }

private suspend fun AwaitPointerEventScope.watchWidgetFingers(
    driver: CanvasTouchDriver,
    space: CanvasBoardSpace,
    primary: PointerId,
) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        forEachNewFinger(event, primary) { point ->
            driver.activeWidget?.onOtherFinger(space.isPalm(event.contactPx(), point))
        }
        if (!event.changes.fastAny { it.pressed }) return
    }
}

/** Uses the browse gesture: its hold is this hold, and its release opens Add instead of a menu. */
private suspend fun AwaitPointerEventScope.holdEmptyCell(
    driver: CanvasTouchDriver,
    space: CanvasBoardSpace,
    cell: GridCell,
    primary: PointerId,
    press: Offset,
    downTime: Long,
) {
    val gesture = CanvasTouchGesture(density)
    gesture.down(CanvasTouchStart.Browse, press.x, press.y, downTime)
    try {
        trackFinger(space, gesture, primary, press, downTime) { effect, _ ->
            driver.handleEmptyHold(gesture, effect, cell)
        }
    } finally {
        driver.endEmptyHold()
    }
}

private suspend fun AwaitPointerEventScope.trackFinger(
    space: CanvasBoardSpace,
    gesture: CanvasTouchGesture,
    primary: PointerId,
    press: Offset,
    downTime: Long,
    handle: (CanvasTouchEffect, Offset) -> Unit,
) {
    var now = downTime
    var last = press
    while (gesture.phase != CanvasTouchPhase.Idle) {
        val liftAt = gesture.liftAtMs
        val event = if (liftAt == null) {
            awaitPointerEvent(PointerEventPass.Initial)
        } else {
            withTimeoutOrNull((liftAt - now).coerceAtLeast(1L)) { awaitPointerEvent(PointerEventPass.Initial) }
        }
        if (event == null) {
            now = liftAt ?: now
            handle(gesture.timeReached(now), last)
            continue
        }
        val change = event.changes.fastFirstOrNull { it.id == primary }
        if (change == null) {
            handle(gesture.cancelled(), last)
            return
        }
        now = change.uptimeMillis
        last = change.position
        handle(gesture.timeReached(now), last)
        forEachNewFinger(event, primary) { point ->
            handle(gesture.otherFinger(space.isPalm(event.contactPx(), point)), last)
        }
        if (!change.pressed) {
            val owned = gesture.ownsPointer
            val effect = if (event.motionEvent == null) gesture.cancelled() else gesture.up()
            handle(effect, last)
            if (owned) event.changes.fastForEach { it.consume() }
            return
        }
        handle(gesture.moved(last.x, last.y), last)
        if (gesture.ownsPointer) {
            event.changes.fastForEach { it.consume() }
        } else {
            val settled = awaitPointerEvent(PointerEventPass.Final)
            if (settled.changes.fastAny { it.id == primary && it.pressed && it.isConsumed }) gesture.claimedElsewhere()
        }
    }
}

private inline fun forEachNewFinger(event: PointerEvent, primary: PointerId, action: (Offset) -> Unit) {
    event.changes.fastForEach { if (it.id != primary && it.changedToDownIgnoreConsumed()) action(it.position) }
}

private fun PointerEvent.contactPx(): Float {
    val motion: MotionEvent = motionEvent ?: return 0f
    return motion.getTouchMajor(motion.actionIndex)
}
