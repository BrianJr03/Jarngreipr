package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasGrabTest {

    private fun layout(
        rects: Map<String, GridRect>,
        menuVisible: Boolean = false,
        rows: Int = 4,
    ) = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = rows,
        items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        verticalArrangement = rects,
        horizontalArrangement = rects,
        menuButtonVisible = menuVisible,
    )

    private val twoTiles = layout(mapOf("a" to GridRect(1, 0, 1, 1), "b" to GridRect(2, 0, 1, 1)))

    @Test
    fun `moving onto a neighbor pushes it and backing off restores it`() {
        val grab = beginCanvasGrab(twoTiles, "a")!!
        val pushed = grab.moved(1, 0)
        assertEquals(GridRect(2, 0, 1, 1), pushed.preview.placements["a"])
        assertEquals(GridRect(3, 0, 1, 1), pushed.preview.placements["b"])

        val back = pushed.moved(-1, 0)
        assertEquals(GridRect(2, 0, 1, 1), back.preview.placements["b"])
        assertFalse(back.hasChanges)
    }

    @Test
    fun `a move stays inside the rows`() {
        val grab = beginCanvasGrab(twoTiles, "a")!!
        assertEquals(0, grab.moved(0, -1).rect.row)
        assertEquals(3, grab.moved(0, 9).rect.row)
    }

    @Test
    fun `a held direction stops just past the furthest neighbor`() {
        var grab = beginCanvasGrab(twoTiles, "a")!!
        repeat(50) { grab = grab.moved(1, 0) }
        assertTrue(grab.cursor.col <= 3)
    }

    @Test
    fun `a grab never lands on the reserved menu cell`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 1, 1)), menuVisible = true)
        val grab = beginCanvasGrab(page, "a")!!.moved(-1, 0)
        assertNotEquals(GridRect(0, 0, 1, 1), grab.rect)
    }

    @Test
    fun `a lone tile can travel to the last visible cell`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 1, 1)))
        var grab = beginCanvasGrab(page, "a", visiblePushCells = 6)!!
        repeat(50) { grab = grab.moved(1, 0) }
        assertEquals(5, grab.rect.col)
    }

    @Test
    fun `a lone tile can still move when the viewport is unknown`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 1, 1)))
        assertEquals(2, beginCanvasGrab(page, "a")!!.moved(1, 0).rect.col)
    }

    @Test
    fun `a wide tile stops with its far edge on the last visible cell`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 2, 1)))
        var grab = beginCanvasGrab(page, "a", visiblePushCells = 6)!!
        repeat(50) { grab = grab.moved(1, 0) }
        assertEquals(4, grab.rect.col)
    }

    @Test
    fun `travel reaches one past a neighbor beyond the visible cells`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 1, 1), "b" to GridRect(9, 1, 1, 1)))
        var grab = beginCanvasGrab(page, "a", visiblePushCells = 4)!!
        repeat(50) { grab = grab.moved(1, 0) }
        assertEquals(10, grab.rect.col)
    }

    @Test
    fun `visible cells count only whole cells inside the padding`() {
        assertEquals(3, canvasVisibleCells(viewportPx = 330f, cellPx = 90f, spacingPx = 10f, paddingPx = 20f))
        assertEquals(2, canvasVisibleCells(viewportPx = 329f, cellPx = 90f, spacingPx = 10f, paddingPx = 20f))
        assertEquals(0, canvasVisibleCells(viewportPx = 0f, cellPx = 90f, spacingPx = 10f, paddingPx = 20f))
        assertEquals(0, canvasVisibleCells(viewportPx = 340f, cellPx = 0f, spacingPx = 10f, paddingPx = 20f))
    }

    @Test
    fun `the cursor stays on the tile when the reserved cell refuses it`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 1, 1)), menuVisible = true)
        val refused = beginCanvasGrab(page, "a", visiblePushCells = 6)!!.moved(-1, 0)
        assertEquals(GridRect(1, 0, 1, 1), refused.rect)
        assertEquals(refused.rect, refused.cursor)
        assertEquals(GridRect(2, 0, 1, 1), refused.moved(1, 0).rect)
    }

    @Test
    fun `backing off a resize that hit the reserved cell restores the tile`() {
        val page = CanvasLayout(
            activeOrientation = CanvasScrollOrientation.VERTICAL,
            verticalColumns = 4,
            items = listOf(CanvasItem.AppItem("a", "pkg.a")),
            verticalArrangement = mapOf("a" to GridRect(2, 0, 1, 1)),
            horizontalArrangement = mapOf("a" to GridRect(2, 0, 1, 1)),
            menuButtonVisible = true,
        )
        val grown = beginCanvasGrab(page, "a")!!.resized(1)
        assertEquals(1, grown.rect.row)
        assertEquals(GridRect(2, 0, 1, 1), grown.resized(-1).rect)
    }

    @Test
    fun `undo add removes the new tile and returns to browsing`() {
        val editor = CanvasEditor()
        val removed = mutableListOf<String>()
        editor.beginAdded(twoTiles, "a", 1, 1)
        assertTrue(editor.grab!!.isNewTile)
        editor.undoAdd { removed += it }
        assertEquals(listOf("a"), removed)
        assertNull(editor.grab)
        assertFalse(editor.editing)
    }

    @Test
    fun `undo add started in edit mode stays in edit mode`() {
        val editor = CanvasEditor()
        editor.enter()
        editor.beginAdded(twoTiles, "a", 1, 1)
        editor.undoAdd { }
        assertTrue(editor.editing)
    }

    @Test
    fun `undo add never removes a tile that already existed`() {
        val editor = CanvasEditor()
        val removed = mutableListOf<String>()
        editor.enter()
        editor.begin(twoTiles, "a", 1, 1)
        assertFalse(editor.grab!!.isNewTile)
        editor.undoAdd { removed += it }
        assertTrue(removed.isEmpty())
        assertEquals("a", editor.grab?.itemId)
    }

    @Test
    fun `a dropped new tile is kept and is no longer undoable`() {
        val editor = CanvasEditor()
        val removed = mutableListOf<String>()
        editor.beginAdded(twoTiles, "a", 1, 1)
        editor.drop { }
        editor.undoAdd { removed += it }
        editor.begin(twoTiles, "a", 1, 1)
        assertFalse(editor.grab!!.isNewTile)
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `resize follows the selected axis`() {
        val grab = beginCanvasGrab(twoTiles, "a")!!
        assertEquals(2, grab.resized(1).rect.colSpan)
        val height = grab.axisToggled().resized(1)
        assertEquals(1, height.rect.colSpan)
        assertEquals(2, height.rect.rowSpan)
    }

    @Test
    fun `resize respects minimum spans and the row count`() {
        val grab = beginCanvasGrab(twoTiles, "a", minColSpan = 1, minRowSpan = 1)!!
        assertEquals(1, grab.resized(-1).rect.colSpan)
        var tall = grab.axisToggled()
        repeat(10) { tall = tall.resized(1) }
        assertEquals(4, tall.rect.rowSpan)
    }

    @Test
    fun `move after resize keeps the new size`() {
        val grab = beginCanvasGrab(twoTiles, "a")!!.resized(1).moved(0, 1)
        assertEquals(GridRect(1, 1, 2, 1), grab.rect)
    }

    @Test
    fun `edit mode drop commits once and cancel commits nothing`() {
        val editor = CanvasEditor()
        val commits = mutableListOf<Map<String, GridRect>>()
        editor.enter()
        editor.begin(twoTiles, "a", 1, 1)
        editor.step { moved(0, 1) }
        editor.cancelGrab()
        editor.drop { commits += it }
        assertTrue(commits.isEmpty())

        editor.begin(twoTiles, "a", 1, 1)
        editor.step { moved(0, 1) }
        editor.drop { commits += it }
        assertEquals(1, commits.size)
        assertEquals(GridRect(1, 1, 1, 1), commits.single()["a"])
        assertNull(editor.grab)
    }

    @Test
    fun `a grab needs edit mode`() {
        val editor = CanvasEditor()
        editor.begin(twoTiles, "a", 1, 1)
        assertNull(editor.grab)
    }

    @Test
    fun `page order helpers keep every page`() {
        assertEquals(listOf(1, 2, 0, 3), jarnPageOrderAfterMove(4, 0, 2))
        assertEquals(listOf(0, 1, 2), jarnPageOrderAfterMove(3, 5, 1))
        assertEquals(1, jarnPageAfterDelete(2, 0, 4))
        assertEquals(1, jarnPageAfterDelete(1, 3, 4))
        assertEquals(2, jarnPageAfterDelete(3, 3, 4))
    }

    @Test
    fun `selection starts on Home and stays on a real page`() {
        val pages = List(3) { JarnCanvasPage(it, JarnCanvasPageKind.Canvas) }
        assertEquals(2, resolveSelectedPage(-1, JarnCanvasSnapshot(pages, homePage = 2)))
        assertEquals(2, resolveSelectedPage(5, JarnCanvasSnapshot(pages)))
        assertEquals(-1, resolveSelectedPage(1, JarnCanvasSnapshot()))
    }

    @Test
    fun `moving a page step by step keeps the moved page`() {
        val move = CanvasPageMove(0, listOf(0, 1, 2)).stepped(1).stepped(1).stepped(1)
        assertEquals(listOf(1, 2, 0), move.order)
        assertEquals(2, move.position)
    }
}
