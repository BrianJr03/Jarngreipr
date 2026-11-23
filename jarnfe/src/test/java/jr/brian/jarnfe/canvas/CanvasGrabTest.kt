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
