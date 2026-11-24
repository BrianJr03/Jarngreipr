package jr.brian.jarnfe.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import jr.brian.home.canvas.grid.PushDirection
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasResizeTest {
    private val board = CanvasBoardGeometry(cellPx = 47f, spacingPx = 8f, paddingPx = 20f)
    private val tile = GridRect(2, 2, 1, 1)
    private val origin = board.originOf(tile)

    private fun target(
        corner: CanvasResizeCorner,
        deltaCol: Int,
        deltaRow: Int,
        from: GridRect = GridRect(2, 2, 2, 2),
        minCol: Int = 1,
        minRow: Int = 1,
        direction: PushDirection = PushDirection.DOWN,
    ) = canvasResizeTarget(from, corner, deltaCol, deltaRow, minCol, minRow, 6, direction)

    @Test
    fun `each corner is reachable from outside the tile`() {
        val size = 47f
        assertEquals(CanvasResizeCorner.NorthWest, canvasHandleAt(board, tile, origin + Offset(-20f, -20f), 1f))
        assertEquals(CanvasResizeCorner.NorthEast, canvasHandleAt(board, tile, origin + Offset(size + 20f, -20f), 1f))
        assertEquals(CanvasResizeCorner.SouthWest, canvasHandleAt(board, tile, origin + Offset(-20f, size + 20f), 1f))
        assertEquals(
            CanvasResizeCorner.SouthEast,
            canvasHandleAt(board, tile, origin + Offset(size + 20f, size + 20f), 1f),
        )
    }

    @Test
    fun `a small tile keeps its center free for dragging`() {
        assertNull(canvasHandleAt(board, tile, origin + Offset(23.5f, 23.5f), 1f))
    }

    @Test
    fun `a point past the hit area misses`() {
        assertNull(canvasHandleAt(board, tile, origin + Offset(-40f, -40f), 1f))
    }

    @Test
    fun `dragging a corner keeps the opposite corner fixed`() {
        assertEquals(GridRect(1, 1, 3, 3), target(CanvasResizeCorner.NorthWest, -1, -1))
        assertEquals(GridRect(2, 2, 3, 3), target(CanvasResizeCorner.SouthEast, 1, 1))
        assertEquals(GridRect(2, 1, 3, 3), target(CanvasResizeCorner.NorthEast, 1, -1))
        assertEquals(GridRect(1, 2, 3, 3), target(CanvasResizeCorner.SouthWest, -1, 1))
    }

    @Test
    fun `minimum span holds from every corner`() {
        assertEquals(GridRect(3, 3, 1, 1), target(CanvasResizeCorner.NorthWest, 5, 5))
        assertEquals(GridRect(2, 2, 1, 1), target(CanvasResizeCorner.SouthEast, -5, -5))
        assertEquals(GridRect(2, 2, 2, 2), target(CanvasResizeCorner.SouthEast, -5, -5, minCol = 2, minRow = 2))
    }

    @Test
    fun `the cross axis limit holds in both directions`() {
        assertEquals(4, target(CanvasResizeCorner.SouthEast, 9, 0).colSpan)
        assertEquals(0, target(CanvasResizeCorner.NorthWest, -9, 0).col)
        assertEquals(4, target(CanvasResizeCorner.SouthEast, 0, 9, direction = PushDirection.RIGHT).rowSpan)
    }

    @Test
    fun `the push axis stops at the largest span`() {
        val grown = target(CanvasResizeCorner.SouthEast, 0, 40)
        assertEquals(MAX_PUSH_AXIS_SPAN, grown.rowSpan)
    }

    @Test
    fun `gesture owns a resize and a second finger restores`() {
        val gesture = CanvasTouchGesture(1f)
        assertEquals(CanvasTouchEffect.BeginResize, gesture.down(CanvasTouchStart.Resize, 0f, 0f, 0L))
        assertTrue(gesture.ownsPointer)
        assertEquals(CanvasTouchEffect.Resize, gesture.moved(30f, 0f))
        assertEquals(CanvasTouchEffect.CancelDrag, gesture.otherFinger(palmLike = false))
        assertEquals(CanvasTouchEffect.None, gesture.moved(60f, 0f))
    }

    @Test
    fun `lifting after a resize drops it`() {
        val gesture = CanvasTouchGesture(1f)
        gesture.down(CanvasTouchStart.Resize, 0f, 0f, 0L)
        assertEquals(CanvasTouchEffect.Drop, gesture.up())
        assertFalse(gesture.ownsPointer)
    }

    private val driverRects = mapOf("a" to GridRect(0, 0, 1, 1), "b" to GridRect(1, 0, 1, 1))
    private val driverLayout = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.VERTICAL,
        verticalColumns = 4,
        items = driverRects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        verticalArrangement = driverRects,
        horizontalArrangement = driverRects,
    )

    private inner class DriverHarness {
        val editor = CanvasEditor().apply { showPage(0) }
        val driver = CanvasTouchDriver(editor)
        val commits = mutableListOf<Map<String, GridRect>>()
        val removed = mutableListOf<String>()
        var ticks = 0

        init {
            driver.geometry = board
            driver.dock.bounds = Rect(0f, 500f, 1000f, 556f)
            driver.actions = CanvasTouchActions(
                enabled = true,
                layout = driverLayout,
                minSpans = { 1 to 1 },
                openMenu = {},
                commit = { _, placements -> commits += placements },
                onGrabbed = {},
                haptic = {},
                showBlocked = {},
                tick = { ticks += 1 },
                removeTile = { removed += it.itemId },
            )
        }

        fun selectA(): Offset {
            editor.enter()
            val center = board.originOf(driverRects.getValue("a")) + Offset(23f, 23f)
            val tap = CanvasTouchGesture(1f)
            driver.handle(tap, tap.down(CanvasTouchStart.Edit, center.x, center.y, 0L), "a", center, center)
            driver.handle(tap, tap.up(), "a", center, center)
            return center
        }
    }

    @Test
    fun `a finger tap selects and claims the click that follows`() {
        val harness = DriverHarness()
        harness.selectA()
        assertEquals("a", harness.driver.selectedId)
        assertTrue(harness.driver.claimEditTap("a"))
        assertFalse(harness.driver.claimEditTap("a"))
    }

    @Test
    fun `dropping on the bar removes without saving`() {
        val harness = DriverHarness()
        val center = harness.selectA()
        val gesture = CanvasTouchGesture(1f)
        val send = { effect: CanvasTouchEffect, point: Offset ->
            harness.driver.handle(gesture, effect, "a", center, point)
        }
        send(gesture.down(CanvasTouchStart.Edit, center.x, center.y, 0L), center)
        val over = Offset(center.x, 520f)
        send(gesture.moved(over.x, over.y), over)
        send(gesture.moved(over.x, over.y + 1f), over)
        assertTrue(harness.driver.dock.armed)
        assertEquals(driverRects, harness.editor.grab?.preview?.placements)
        send(gesture.up(), over)
        assertEquals(listOf("a"), harness.removed)
        assertTrue(harness.commits.isEmpty())
        assertNull(harness.driver.visual.tileId)
    }

    @Test
    fun `a second finger during resize restores the layout`() {
        val harness = DriverHarness()
        harness.selectA()
        val origin = board.originOf(driverRects.getValue("a"))
        val corner = origin + Offset(47f, 47f)
        assertEquals(CanvasResizeCorner.SouthEast, harness.driver.pressCorner(corner))
        val gesture = CanvasTouchGesture(1f)
        harness.driver.handle(gesture, gesture.down(CanvasTouchStart.Resize, corner.x, corner.y, 0L), "a", corner, corner)
        val far = corner + Offset(board.pitch, 0f)
        harness.driver.handle(gesture, gesture.moved(far.x, far.y), "a", corner, far)
        assertEquals(2, harness.editor.grab?.rect?.colSpan)
        assertTrue(harness.ticks >= 2)
        harness.driver.handle(gesture, gesture.otherFinger(palmLike = false), "a", corner, far)
        assertNull(harness.editor.grab)
        assertTrue(harness.commits.isEmpty())
        assertEquals(driverRects, harness.editor.placements(driverLayout))
        assertEquals("a", harness.driver.selectedId)
    }

    @Test
    fun `dock removal ends the grab without saving and keeps the tile`() {
        val rects = mapOf("a" to GridRect(0, 0, 1, 1), "b" to GridRect(1, 0, 1, 1))
        val layout = CanvasLayout(
            activeOrientation = CanvasScrollOrientation.HORIZONTAL,
            horizontalRows = 4,
            items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
            verticalArrangement = rects,
            horizontalArrangement = rects,
        )
        val editor = CanvasEditor().apply { showPage(0) }
        assertTrue(editor.beginTouch(layout, "a", 1, 1))
        val taken = editor.takeForRemoval()
        assertEquals("a", taken?.itemId)
        assertNull(editor.grab)
        assertNull(editor.settled)
        assertFalse(editor.editing)
    }
}
