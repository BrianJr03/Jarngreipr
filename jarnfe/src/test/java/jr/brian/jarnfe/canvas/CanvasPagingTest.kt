package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import jr.brian.home.canvas.model.satisfiesInvariant
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.JarnInputRouter
import jr.brian.jarnfe.input.JarnKeyInput
import jr.brian.jarnfe.input.JarnKeyPhase
import jr.brian.jarnfe.input.JarnScreen
import jr.brian.jarnfe.input.jarnBindings
import android.view.KeyEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CanvasPagingTest {
    private fun swipe(atStart: Boolean = true, atEnd: Boolean = true, back: Boolean = true, forward: Boolean = true) =
        CanvasPageSwipeGesture(1f, back, forward, atStart, atEnd)

    private fun layout(rects: Map<String, GridRect>, inactive: Map<String, GridRect> = rects) = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = 4,
        verticalColumns = 3,
        items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        horizontalArrangement = rects,
        verticalArrangement = inactive,
    )

    private fun moved(): JarnCanvasTileMoved {
        val source = layout(mapOf("a" to GridRect(1, 0, 1, 1)))
        val destination = layout(mapOf("c" to GridRect(1, 0, 1, 1)))
        val move = JarnCanvasTileMove("a", 0, 1, CanvasScrollOrientation.HORIZONTAL, mapOf("a" to GridRect(2, 0, 1, 1)))
        return JarnCanvasTileMoved(
            move = move,
            sourceBefore = source,
            sourceAfter = source.copy(items = emptyList(), horizontalArrangement = emptyMap(), verticalArrangement = emptyMap()),
            destinationBefore = destination,
            destinationAfter = jarnLayoutWithArrival(destination, source, source.items.first(), move),
        )
    }

    private class MoveWrites(val result: JarnCanvasTileMoved?, val undoes: Boolean = true) : CanvasUndoWrites {
        val restored = mutableListOf<JarnCanvasTileMoved>()

        override fun commit(pageIndex: Int, orientation: CanvasScrollOrientation, placements: Map<String, GridRect>) = Unit

        override suspend fun remove(pageIndex: Int, itemId: String) = Unit

        override suspend fun moveTile(move: JarnCanvasTileMove) = result

        override suspend fun undoTileMove(moved: JarnCanvasTileMoved): Boolean {
            restored += moved
            return undoes
        }
    }

    @Test
    fun `a swipe under the threshold springs back`() {
        val gesture = swipe()
        gesture.moved(71f, 0f)
        assertEquals(0, gesture.released(0f))
    }

    @Test
    fun `a swipe past the threshold changes page the matching way`() {
        val back = swipe()
        back.moved(72f, 0f)
        assertEquals(-1, back.released(0f))
        val forward = swipe()
        forward.moved(-72f, 0f)
        assertEquals(1, forward.released(0f))
    }

    @Test
    fun `a fling changes page under the distance, only in its own direction`() {
        val fling = swipe()
        fling.moved(20f, 0f)
        assertEquals(-1, fling.released(900f))
        val against = swipe()
        against.moved(20f, 0f)
        assertEquals(0, against.released(-900f))
    }

    @Test
    fun `a swipe that started away from the end never changes page`() {
        val gesture = swipe(atStart = false, atEnd = false)
        gesture.moved(200f, 0f)
        assertEquals(0f, gesture.overPx)
        assertEquals(0, gesture.released(2000f))
    }

    @Test
    fun `distance the scroll used does not count toward the page`() {
        val gesture = CanvasPageSwipeGesture(1f, true, true, startedAtStart = false, startedAtEnd = true, startScroll = 100f)
        gesture.moved(-100f, 0f, 200f)
        assertEquals(0f, gesture.overPx)
        gesture.moved(-180f, 0f, 200f)
        assertEquals(-80f, gesture.overPx)
    }

    @Test
    fun `a mostly vertical drag is not a page swipe`() {
        val gesture = swipe()
        gesture.moved(10f, 40f)
        gesture.moved(200f, 40f)
        assertFalse(gesture.isSideways)
        assertEquals(0, gesture.released(0f))
    }

    @Test
    fun `no page that way means no overscroll`() {
        val gesture = swipe(back = false)
        gesture.moved(200f, 0f)
        assertEquals(0, gesture.released(0f))
    }

    @Test
    fun `one dwell flips one page until the finger leaves the edge`() {
        val dwell = CanvasEdgeDwell(550L)
        assertEquals(0, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 0))
        assertEquals(0, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 549))
        assertEquals(1, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 550))
        assertEquals(0, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 5_000))
        dwell.step(0, atScrollEnd = false, canFlip = true, nowMs = 5_001)
        assertEquals(0, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 5_002))
        assertEquals(1, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 5_552))
    }

    @Test
    fun `the dwell only counts at the scroll end`() {
        val dwell = CanvasEdgeDwell(550L)
        dwell.step(1, atScrollEnd = false, canFlip = true, nowMs = 0)
        assertEquals(0, dwell.step(1, atScrollEnd = false, canFlip = true, nowMs = 600))
        assertEquals(0, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 601))
        assertEquals(1, dwell.step(1, atScrollEnd = true, canFlip = true, nowMs = 1_151))
    }

    @Test
    fun `the edge zone is signed and empty outside the side strips`() {
        assertTrue(canvasEdgeDepth(10f, 0f, 500f, 36f) < 0f)
        assertTrue(canvasEdgeDepth(490f, 0f, 500f, 36f) > 0f)
        assertEquals(0f, canvasEdgeDepth(250f, 0f, 500f, 36f))
    }

    @Test
    fun `an arriving tile is placed in both orientations`() {
        val source = layout(
            mapOf("a" to GridRect(1, 0, 2, 2)),
            inactive = mapOf("a" to GridRect(0, 0, 3, 1)),
        )
        val destination = layout(mapOf("c" to GridRect(1, 0, 1, 1)))
        val move = JarnCanvasTileMove(
            itemId = "a",
            fromPage = 0,
            toPage = 1,
            orientation = CanvasScrollOrientation.HORIZONTAL,
            placements = mapOf("a" to GridRect(2, 0, 2, 2), "c" to GridRect(1, 0, 1, 1)),
        )
        val arrived = jarnLayoutWithArrival(destination, source, source.items.first(), move)
        assertTrue(arrived.satisfiesInvariant())
        assertEquals(GridRect(2, 0, 2, 2), arrived.horizontalArrangement["a"])
        val inactive = arrived.verticalArrangement.getValue("a")
        assertEquals(3, inactive.colSpan)
        assertFalse(inactive.overlaps(arrived.verticalArrangement.getValue("c")))
    }

    @Test
    fun `a tile too wide for the other orientation is narrowed to fit`() {
        val source = layout(mapOf("a" to GridRect(1, 0, 1, 1)), inactive = mapOf("a" to GridRect(0, 0, 6, 1)))
        val destination = layout(emptyMap())
        val move = JarnCanvasTileMove(
            "a", 0, 1, CanvasScrollOrientation.HORIZONTAL, mapOf("a" to GridRect(1, 0, 1, 1)),
        )
        val arrived = jarnLayoutWithArrival(destination, source, source.items.first(), move)
        assertEquals(3, arrived.verticalArrangement.getValue("a").colSpan)
    }

    @Test
    fun `folders and duplicates are refused, everything else may move`() {
        val empty = CanvasLayout()
        assertFalse(jarnTileMayChangePage(CanvasItem.FolderItem("f", "id"), empty))
        val rom = CanvasItem.RomItem("rom-k", "k")
        assertTrue(jarnTileMayChangePage(rom, empty))
        assertFalse(jarnTileMayChangePage(rom, empty.copy(items = listOf(CanvasItem.RomItem("other", "k")))))
        val widget = CanvasItem.WidgetItem("widget-5", 5)
        assertTrue(jarnTileMayChangePage(widget, empty))
        assertFalse(jarnTileMayChangePage(widget, empty.copy(items = listOf(CanvasItem.WidgetItem("old", 5)))))
        listOf(
            CanvasItem.AppItem("a", "pkg"),
            CanvasItem.PhotoContainer("p", "file://x"),
            CanvasItem.EsdeArtItem("e"),
            CanvasItem.LastPlayedItem("l"),
            CanvasItem.RssLauncherItem("r"),
            CanvasItem.RssMusicItem("m"),
        ).forEach { assertTrue(jarnTileMayChangePage(it, empty)) }
    }

    @Test
    fun `a between-page move stays undoable across a page change`() = runTest {
        val record = moved()
        val writes = MoveWrites(record)
        val undo = CanvasUndoController(backgroundScope)
        undo.moveTile(writes, record.move) { }
        runCurrent()
        undo.settleForPageChange()
        assertTrue(undo.entry is CanvasTileMoveEntry)
        var restoredTo: Int? = null
        assertNull(undo.undo(emptyMap()) { done, ok -> if (ok) restoredTo = done.move.fromPage })
        runCurrent()
        assertEquals(listOf(record), writes.restored)
        assertEquals(0, restoredTo)
        assertNull(undo.entry)
    }

    @Test
    fun `a between-page undo ends with the window and restores nothing later`() = runTest {
        val record = moved()
        val writes = MoveWrites(record)
        val undo = CanvasUndoController(backgroundScope)
        undo.moveTile(writes, record.move) { }
        runCurrent()
        advanceTimeBy(CANVAS_UNDO_WINDOW_MS + 1)
        runCurrent()
        assertNull(undo.entry)
        assertTrue(writes.restored.isEmpty())
    }

    @Test
    fun `a failed move opens no undo and reports the failure`() = runTest {
        val writes = MoveWrites(null)
        val undo = CanvasUndoController(backgroundScope)
        var reported = false
        undo.moveTile(writes, moved().move) { reported = it == null }
        runCurrent()
        assertTrue(reported)
        assertNull(undo.entry)
    }

    @Test
    fun `the d-pad edge means nothing lies further that way`() {
        val left = GridRect(0, 0, 1, 1)
        val right = GridRect(3, 2, 1, 1)
        val rects = listOf(left, right)
        assertTrue(canvasDpadAtEdge(rects, left, -1))
        assertFalse(canvasDpadAtEdge(rects, left, 1))
        assertTrue(canvasDpadAtEdge(rects, right, 1))
        assertFalse(canvasDpadAtEdge(rects, right, -1))
    }

    @Test
    fun `a page change lands on the matching edge, nearest the row`() {
        val placements = mapOf(
            "top" to GridRect(0, 0, 1, 1),
            "low" to GridRect(0, 3, 1, 1),
            "far" to GridRect(5, 2, 1, 1),
        )
        assertEquals("low", canvasEntryTile(placements, 1, 2))
        assertEquals("top", canvasEntryTile(placements, 1, 0))
        assertEquals("far", canvasEntryTile(placements, -1, 0))
        assertNull(canvasEntryTile(emptyMap(), 1, 0))
    }

    @Test
    fun `page steps skip pages without a grid and stop at the ends`() {
        val pages = listOf(
            JarnCanvasPage(0, JarnCanvasPageKind.Canvas),
            JarnCanvasPage(1, JarnCanvasPageKind.Rss),
            JarnCanvasPage(2, JarnCanvasPageKind.Canvas),
        )
        assertEquals(2, canvasPageInDirection(pages, 0, 1)?.index)
        assertEquals(0, canvasPageInDirection(pages, 2, -1)?.index)
        assertNull(canvasPageInDirection(pages, 0, -1))
        assertNull(canvasPageInDirection(pages, 2, 1))
    }

    @Test
    fun `carrying a folder is refused and leaves the grab home`() {
        val folderLayout = CanvasLayout(
            items = listOf(CanvasItem.FolderItem("f", "id")),
            horizontalArrangement = mapOf("f" to GridRect(1, 0, 1, 1)),
            verticalArrangement = mapOf("f" to GridRect(1, 0, 1, 1)),
        )
        val pages = listOf(
            JarnCanvasPage(0, JarnCanvasPageKind.Canvas, folderLayout),
            JarnCanvasPage(1, JarnCanvasPageKind.Canvas),
        )
        val editor = CanvasEditor().apply {
            showPage(0)
            enter()
            begin(folderLayout, "f", 1, 1)
        }
        assertEquals(CanvasCarryResult.Folder, CanvasPageCarrier(editor) { pages }.carry(1, enterAtEdge = true))
        assertEquals(0, editor.grab?.pageIndex)
    }

    @Test
    fun `a carried tile is placed on arrival and stays one grab`() {
        val home = layout(mapOf("a" to GridRect(1, 0, 1, 1)))
        val other = layout(mapOf("c" to GridRect(3, 0, 1, 1)))
        val pages = listOf(
            JarnCanvasPage(0, JarnCanvasPageKind.Canvas, home),
            JarnCanvasPage(1, JarnCanvasPageKind.Canvas, other),
        )
        var selected = 0
        val editor = CanvasEditor(navigate = { selected = it }).apply {
            showPage(0)
            enter()
            begin(home, "a", 1, 1)
        }
        val surface = editor.surfaceKey
        assertEquals(CanvasCarryResult.Moved, CanvasPageCarrier(editor) { pages }.carry(1, enterAtEdge = true))
        editor.showPage(selected)
        assertEquals(1, selected)
        assertEquals(surface, editor.surfaceKey)
        assertNotNull(editor.grab?.preview?.placements?.get("a"))
        assertTrue(editor.grab?.dropResult() is CanvasGrabDrop.BetweenPages)
    }

    @Test
    fun `a carried tile draws on the page it is over`() {
        val other = JarnCanvasPage(1, JarnCanvasPageKind.Canvas, layout(mapOf("c" to GridRect(3, 0, 1, 1))))
        val tile = CanvasItem.AppItem("a", "pkg")
        val shown = other.withCarriedTile(CanvasCarriedTile(1, tile, GridRect(1, 0, 1, 1)))
        assertTrue(shown.layout.items.any { it.id == "a" })
        assertEquals(GridRect(1, 0, 1, 1), shown.layout.activeArrangement["a"])
        assertEquals(other, other.withCarriedTile(CanvasCarriedTile(0, tile, GridRect(1, 0, 1, 1))))
    }

    @Test
    fun `a fresh press at the edge carries, and a key repeat never does`() {
        val edgeLayout = layout(mapOf("a" to GridRect(0, 1, 1, 1)))
        val editor = CanvasEditor().apply {
            showPage(0)
            enter()
            begin(edgeLayout, "a", 1, 1)
        }
        val carries = mutableListOf<Int>()
        editor.pushGrabSideways(-1) { carries += it }
        assertEquals(listOf(-1), carries)

        val router = JarnInputRouter()
        var presses = 0
        var repeats = 0
        val layer = router.createLayer(JarnScreen.Stage)
        layer.update(jarnBindings { on(JarnAction.MoveLeft, onRepeat = { repeats++ }) { presses++ } })
        router.attach(layer)
        val code = KeyEvent.KEYCODE_DPAD_LEFT
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 0L))
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 50L, repeatCount = 1))
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, 60L))
        assertEquals(1, presses)
        assertEquals(1, repeats)
    }

    @Test
    fun `a press held from before the edge binding appears stays with focus`() {
        val router = JarnInputRouter()
        var pages = 0
        val layer = router.createLayer(JarnScreen.Stage)
        router.attach(layer)
        val code = KeyEvent.KEYCODE_DPAD_LEFT
        assertFalse(router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 0L)))
        layer.update(jarnBindings { on(JarnAction.MoveLeft, repeats = false) { pages++ } })
        assertFalse(router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 50L, repeatCount = 1)))
        router.dispatch(JarnKeyInput(code, JarnKeyPhase.Up, 60L))
        assertEquals(0, pages)
        assertTrue(router.dispatch(JarnKeyInput(code, JarnKeyPhase.Down, 100L)))
        assertEquals(1, pages)
    }
}
