package jr.brian.jarnfe.canvas

import androidx.compose.ui.geometry.Offset
import jr.brian.home.canvas.grid.reservedRectsForActive
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasAddAndFolderTest {
    private val rects = mapOf(
        "a" to GridRect(1, 0, 1, 1),
        "f" to GridRect(3, 0, 1, 1),
        "p" to GridRect(1, 2, 1, 1),
    )
    private val layout = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = 4,
        items = listOf(
            CanvasItem.AppItem("a", "pkg.a"),
            CanvasItem.FolderItem("f", "folder"),
            CanvasItem.PhotoContainer("p"),
        ),
        verticalArrangement = rects,
        horizontalArrangement = rects,
    )
    private val board = CanvasBoardGeometry(cellPx = 90f, spacingPx = 10f, paddingPx = 20f)

    private fun center(rect: GridRect) = board.originOf(rect) + Offset(45f, 45f)

    private inner class Harness(val heldId: String = "a") {
        var now = 0L
        val editor = CanvasEditor().apply { showPage(0) }
        val driver = CanvasTouchDriver(editor, clockMs = { now })
        val commits = mutableListOf<Map<String, GridRect>>()
        val added = mutableListOf<GridCell>()
        val drops = mutableListOf<CanvasFolderDrop>()
        var haptics = 0
        val gesture = CanvasTouchGesture(1f)
        val press = center(rects.getValue(heldId))

        init {
            driver.geometry = board
            driver.actions = CanvasTouchActions(
                enabled = true,
                layout = layout,
                minSpans = { 1 to 1 },
                openMenu = {},
                commit = { _, placements -> commits += placements },
                onGrabbed = {},
                haptic = { haptics += 1 },
                showBlocked = {},
                addAt = { added += it },
                addToFolder = { drops += it },
            )
        }

        fun send(effect: CanvasTouchEffect, point: Offset = press) =
            driver.handle(gesture, effect, heldId, press, point)

        fun lift() {
            send(gesture.down(driver.startFor(heldId), press.x, press.y, 0L))
            send(gesture.timeReached(360L))
        }

        fun dragTo(point: Offset) = send(gesture.moved(point.x, point.y), point)

        fun release(point: Offset) = send(gesture.up(), point)

        fun holdEmpty(cell: GridCell) {
            gesture.down(CanvasTouchStart.Browse, 0f, 0f, 0L)
            driver.handleEmptyHold(gesture, gesture.timeReached(360L), cell)
        }
    }

    private class FolderWrites(val result: Boolean = true, val throws: Boolean = false) : CanvasUndoWrites {
        val writes = mutableListOf<CanvasPendingFolderAdd>()

        override fun commit(
            pageIndex: Int,
            orientation: CanvasScrollOrientation,
            placements: Map<String, GridRect>,
        ) = Unit

        override suspend fun remove(pageIndex: Int, itemId: String) = Unit

        override suspend fun addAppToFolder(add: CanvasPendingFolderAdd): Boolean {
            check(!throws)
            writes += add
            return result
        }
    }

    private val pending = CanvasPendingFolderAdd(0, "a", "pkg.a", 0, "folder", JarnCanvasApp("pkg.a", "A"))

    @Test
    fun `an empty cell is found, but not a tile, a gap, the menu, or past the grid`() {
        val placements = layout.activeArrangement
        val free = GridRect(2, 1, 1, 1)
        assertEquals(GridCell(2, 1), canvasEmptyCell(board, layout, placements, center(free)))
        assertNull(canvasEmptyCell(board, layout, placements, center(rects.getValue("a"))))
        val gap = board.originOf(free) + Offset(95f, 10f)
        assertNull(canvasEmptyCell(board, layout, placements, gap))
        assertNull(canvasEmptyCell(board, layout, placements, center(GridRect(2, 4, 1, 1))))
        reservedRectsForActive(layout).forEach {
            assertNull(canvasEmptyCell(board, layout, placements, center(it)))
        }
    }

    @Test
    fun `holding an empty cell buzzes, and letting go opens Add for that cell`() {
        val h = Harness()
        val cell = GridCell(2, 1)
        h.holdEmpty(cell)
        assertEquals(1, h.haptics)
        assertTrue(h.driver.holdsEmptyCell)
        assertTrue(h.added.isEmpty())
        h.driver.handleEmptyHold(h.gesture, h.gesture.up(), cell)
        assertEquals(listOf(cell), h.added)
        assertFalse(h.driver.holdsEmptyCell)
    }

    @Test
    fun `moving past slop before the hold leaves the finger to the scroll`() {
        val h = Harness()
        val cell = GridCell(2, 1)
        h.gesture.down(CanvasTouchStart.Browse, 0f, 0f, 0L)
        h.driver.handleEmptyHold(h.gesture, h.gesture.moved(9f, 0f), cell)
        assertEquals(CanvasTouchPhase.PassedThrough, h.gesture.phase)
        assertFalse(h.gesture.ownsPointer)
        h.driver.handleEmptyHold(h.gesture, h.gesture.timeReached(360L), cell)
        h.driver.handleEmptyHold(h.gesture, h.gesture.up(), cell)
        assertEquals(0, h.haptics)
        assertTrue(h.added.isEmpty())
    }

    @Test
    fun `moving away after the hold, or a second finger, opens nothing`() {
        val moved = Harness()
        val cell = GridCell(2, 1)
        moved.holdEmpty(cell)
        moved.driver.handleEmptyHold(moved.gesture, moved.gesture.moved(11f, 0f), cell)
        moved.driver.handleEmptyHold(moved.gesture, moved.gesture.up(), cell)
        assertTrue(moved.added.isEmpty())
        val second = Harness()
        second.holdEmpty(cell)
        second.driver.handleEmptyHold(second.gesture, second.gesture.otherFinger(palmLike = false), cell)
        second.driver.handleEmptyHold(second.gesture, second.gesture.up(), cell)
        assertTrue(second.added.isEmpty())
    }

    @Test
    fun `a new tile lands on the held cell through the solver and can still be undone`() {
        val editor = CanvasEditor().apply { showPage(0) }
        editor.beginAdded(layout, "a", 1, 1, GridCell(3, 0))
        val grab = editor.grab
        assertEquals(GridRect(3, 0, 1, 1), grab?.rect)
        assertTrue(grab?.isNewTile == true)
        assertTrue(grab?.preview?.placements?.getValue("f") != GridRect(3, 0, 1, 1))
        var removed: String? = null
        editor.undoAdd { removed = it }
        assertEquals("a", removed)
    }

    @Test
    fun `only the folder's center counts as aiming at it`() {
        val rect = rects.getValue("f")
        val origin = board.originOf(rect)
        val targets = mapOf("f" to rect)
        assertEquals("f", canvasFolderUnder(board, targets, center(rect), 0.15f))
        assertEquals("f", canvasFolderUnder(board, targets, origin + Offset(13.5f, 13.5f), 0.15f))
        assertNull(canvasFolderUnder(board, targets, origin + Offset(13f, 45f), 0.15f))
        assertNull(canvasFolderUnder(board, targets, origin + Offset(45f, 77f), 0.15f))
    }

    @Test
    fun `only an app tile can aim at a folder`() {
        val app = beginCanvasGrab(layout, "a")!!
        val photo = beginCanvasGrab(layout, "p")!!
        val folder = beginCanvasGrab(layout, "f")!!
        assertEquals(setOf("f"), canvasFolderTargets(layout, app).keys)
        assertTrue(canvasFolderTargets(layout, photo).isEmpty())
        assertTrue(canvasFolderTargets(layout, folder).isEmpty())
        assertTrue(canvasFolderTargets(layout, app.copy(resizeCorner = CanvasResizeCorner.entries.first())).isEmpty())
    }

    @Test
    fun `hovering pauses reflow, arms once at the dwell, and leaving resumes it`() {
        val h = Harness().apply { lift() }
        val liftHaptics = h.haptics
        h.now = 1_000L
        h.dragTo(center(rects.getValue("f")))
        assertEquals(rects, h.editor.grab?.preview?.placements)
        assertEquals("f", h.driver.folderHover.targetId)
        h.now = 1_419L
        h.driver.tickFolderDwell()
        assertFalse(h.driver.folderHover.armed)
        h.now = 1_420L
        h.driver.tickFolderDwell()
        h.driver.tickFolderDwell()
        assertTrue(h.driver.folderHover.armed)
        assertEquals(liftHaptics + 1, h.haptics)
        h.dragTo(center(GridRect(2, 0, 1, 1)))
        assertNull(h.driver.folderHover.targetId)
        assertFalse(h.driver.folderHover.armed)
        assertEquals(2, h.editor.grab?.rect?.col)
    }

    @Test
    fun `dropping on an armed folder hands it over and saves no layout`() {
        val h = Harness().apply { lift() }
        val folder = center(rects.getValue("f"))
        h.dragTo(folder)
        h.now = 420L
        h.driver.tickFolderDwell()
        h.release(folder)
        val drop = h.drops.single()
        assertEquals("pkg.a", drop.packageName)
        assertEquals("folder", drop.folderId)
        assertNull(h.editor.grab)
        assertFalse(h.editor.editing)
        assertTrue(h.commits.isEmpty())
    }

    @Test
    fun `a still finger that lifts after the dwell still counts`() {
        val h = Harness().apply { lift() }
        val folder = center(rects.getValue("f"))
        h.dragTo(folder)
        h.now = 500L
        h.release(folder)
        assertEquals(1, h.drops.size)
    }

    @Test
    fun `dropping before the dwell puts the tile back and saves nothing`() {
        val h = Harness().apply { lift() }
        val folder = center(rects.getValue("f"))
        h.dragTo(folder)
        h.now = 419L
        h.release(folder)
        assertTrue(h.drops.isEmpty())
        assertTrue(h.commits.isEmpty())
        assertNull(h.editor.grab)
        assertEquals(rects, h.editor.placements(layout))
    }

    @Test
    fun `a photo tile over a folder reflows as usual and never drops in`() {
        val h = Harness("p").apply { lift() }
        val folder = center(rects.getValue("f"))
        h.dragTo(folder)
        h.now = 1_000L
        h.driver.tickFolderDwell()
        assertNull(h.driver.folderHover.targetId)
        h.release(folder)
        assertTrue(h.drops.isEmpty())
        assertEquals(1, h.commits.size)
    }

    @Test
    fun `nothing is written during the window and undo brings the tile back`() = runTest {
        val writes = FolderWrites()
        val undo = CanvasUndoController(backgroundScope)
        undo.addToFolder(writes, pending)
        assertEquals(setOf("a"), undo.hiddenOn(0))
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS - 1)
        runCurrent()
        assertTrue(writes.writes.isEmpty())
        assertNull(undo.undo(rects))
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS * 2)
        runCurrent()
        assertTrue(writes.writes.isEmpty())
        assertTrue(undo.hiddenOn(0).isEmpty())
        assertTrue(undo.pendingFolderAdds.isEmpty())
    }

    @Test
    fun `the write runs once when the window ends`() = runTest {
        val writes = FolderWrites()
        val undo = CanvasUndoController(backgroundScope)
        undo.addToFolder(writes, pending)
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS + 1)
        runCurrent()
        undo.settle()
        runCurrent()
        assertEquals(listOf(pending), writes.writes)
        assertEquals(setOf("a"), undo.hiddenOn(0))
        advanceTimeBy(CANVAS_REMOVAL_HIDE_MS + 1)
        runCurrent()
        assertTrue(undo.hiddenOn(0).isEmpty())
    }

    @Test
    fun `a failed or crashed write shows the tile again at once`() = runTest {
        listOf(FolderWrites(result = false), FolderWrites(throws = true)).forEach { writes ->
            val undo = CanvasUndoController(backgroundScope)
            undo.addToFolder(writes, pending)
            undo.settle()
            runCurrent()
            assertTrue(undo.hiddenOn(0).isEmpty())
            assertTrue(undo.pendingFolderAdds.isEmpty())
        }
    }

    @Test
    fun `the folder shows a pending app once, and only on its own page`() {
        val app = JarnCanvasApp("pkg.a", "A")
        val page = JarnCanvasPage(
            index = 0,
            kind = JarnCanvasPageKind.Canvas,
            folders = mapOf("folder" to JarnCanvasFolder("folder", "Games", listOf(JarnCanvasApp("b", "B")))),
        )
        val shown = page.withPendingFolderApps(listOf(pending))
        assertEquals(listOf("b", "pkg.a"), shown.folders.getValue("folder").apps.map { it.packageName })
        val already = page.copy(folders = mapOf("folder" to JarnCanvasFolder("folder", "Games", listOf(app))))
        assertEquals(1, already.withPendingFolderApps(listOf(pending)).folders.getValue("folder").apps.size)
        assertEquals(page, page.copy(index = 1).withPendingFolderApps(listOf(pending)).copy(index = 0))
    }
}
