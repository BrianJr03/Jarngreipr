package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasSharedGrabTest {

    private fun layout(rects: Map<String, GridRect>, folders: Set<String> = emptySet(), rows: Int = 4) = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = rows,
        items = rects.keys.map {
            if (it in folders) CanvasItem.FolderItem(it, "folder.$it") else CanvasItem.AppItem(it, "pkg.$it")
        },
        verticalArrangement = rects,
        horizontalArrangement = rects,
    )

    private val homeLayout = layout(mapOf("a" to GridRect(1, 0, 1, 1), "b" to GridRect(2, 0, 1, 1)))
    private val otherLayout = layout(mapOf("c" to GridRect(1, 0, 1, 1)))
    private val homePage = JarnCanvasPage(0, JarnCanvasPageKind.Canvas, homeLayout)
    private val otherPage = JarnCanvasPage(1, JarnCanvasPageKind.Canvas, otherLayout)

    private class Harness {
        var selected = 0
        val editor = CanvasEditor(navigate = { selected = it })

        init {
            editor.showPage(0)
        }

        fun sync() = editor.showPage(selected)
    }

    private fun grabbed(driver: CanvasGrabDriver = CanvasGrabDriver.Controller) = Harness().apply {
        if (driver == CanvasGrabDriver.Touch) {
            editor.beginTouch(homeLayout, "a", 1, 1)
        } else {
            editor.enter()
            editor.begin(homeLayout, "a", 1, 1)
        }
    }

    @Test
    fun `a touch grab can be taken by the controller and handed back`() {
        val h = grabbed(CanvasGrabDriver.Touch)
        h.editor.drive(CanvasGrabDriver.Touch) { movedTo(2, 0) }
        val before = h.editor.grab!!
        assertTrue(h.editor.takeOver(CanvasGrabDriver.Controller, "a"))
        assertEquals(before.preview, h.editor.grab!!.preview)
        h.editor.step { moved(0, 1) }
        assertEquals(GridRect(2, 1, 1, 1), h.editor.grab!!.rect)
        assertTrue(h.editor.takeOver(CanvasGrabDriver.Touch, "a"))
        assertEquals(CanvasGrabDriver.Touch, h.editor.grab!!.driver)
    }

    @Test
    fun `touching a different tile does not take over`() {
        val h = grabbed()
        assertFalse(h.editor.takeOver(CanvasGrabDriver.Touch, "b"))
        assertEquals(CanvasGrabDriver.Controller, h.editor.grab!!.driver)
    }

    @Test
    fun `only the input holding the grab can move it`() {
        val h = grabbed(CanvasGrabDriver.Touch)
        val before = h.editor.grab
        h.editor.step { moved(1, 0) }
        assertSame(before, h.editor.grab)
    }

    @Test
    fun `a second touch grab is refused while one is held`() {
        val h = grabbed()
        assertFalse(h.editor.beginTouch(homeLayout, "b", 1, 1))
        assertEquals("a", h.editor.grab!!.itemId)
    }

    @Test
    fun `a touch grab from browse returns to browse on cancel and drop`() {
        val cancelled = grabbed(CanvasGrabDriver.Touch)
        assertTrue(cancelled.editor.editing)
        cancelled.editor.cancelGrab()
        assertFalse(cancelled.editor.editing)

        val dropped = grabbed(CanvasGrabDriver.Touch)
        dropped.editor.drive(CanvasGrabDriver.Touch) { movedTo(1, 1) }
        dropped.editor.drop { }
        assertFalse(dropped.editor.editing)
    }

    @Test
    fun `a touch grab from edit mode stays in edit mode`() {
        val h = Harness()
        h.editor.enter()
        h.editor.beginTouch(homeLayout, "a", 1, 1)
        h.editor.cancelGrab()
        assertTrue(h.editor.editing)
    }

    @Test
    fun `a page change keeps the grab and its home`() {
        val h = grabbed()
        assertTrue(h.editor.changePage(otherPage))
        assertEquals(1, h.selected)
        h.sync()
        val grab = h.editor.grab!!
        assertEquals("a", grab.itemId)
        assertEquals(1, grab.pageIndex)
        assertEquals(0, grab.home.pageIndex)
        assertTrue(h.editor.editing)
        assertEquals(otherLayout.activeArrangement, grab.preview.placements)
    }

    @Test
    fun `a page change the grab did not ask for ends the session`() {
        val h = grabbed()
        h.selected = 1
        h.sync()
        assertNull(h.editor.grab)
        assertFalse(h.editor.editing)
    }

    @Test
    fun `changing page without a grab leaves edit mode like before`() {
        val h = Harness()
        h.editor.enter()
        h.selected = 1
        h.sync()
        assertFalse(h.editor.editing)
    }

    @Test
    fun `cancel from the home page restores the starting layout`() {
        val h = grabbed()
        h.editor.step { moved(1, 0) }
        h.editor.cancelGrab()
        assertNull(h.editor.grab)
        assertEquals(homeLayout.activeArrangement, h.editor.placements(homeLayout))
        assertEquals(0, h.selected)
    }

    @Test
    fun `cancel from another page goes home with nothing written`() {
        val h = grabbed()
        val commits = mutableListOf<Map<String, GridRect>>()
        h.editor.changePage(otherPage)
        h.sync()
        h.editor.step { movedTo(1, 0) }
        h.editor.cancelGrab()
        assertEquals(0, h.selected)
        h.sync()
        h.editor.drop { commits += it }
        assertTrue(commits.isEmpty())
        assertTrue(h.editor.editing)
        assertEquals(homeLayout.activeArrangement, h.editor.placements(homeLayout))
    }

    @Test
    fun `cancel after a resize restores the starting layout`() {
        val h = grabbed(CanvasGrabDriver.Touch)
        h.editor.drive(CanvasGrabDriver.Touch) { resizedTo(GridRect(1, 0, 3, 2)) }
        h.editor.cancelGrab()
        assertEquals(homeLayout.activeArrangement, h.editor.placements(homeLayout))
    }

    @Test
    fun `undo add from another page removes the tile and goes home`() {
        val h = Harness()
        val removed = mutableListOf<String>()
        h.editor.beginAdded(homeLayout, "a", 1, 1)
        h.editor.changePage(otherPage)
        h.sync()
        h.editor.undoAdd { removed += it }
        assertEquals(listOf("a"), removed)
        assertEquals(0, h.selected)
        h.sync()
        assertFalse(h.editor.editing)
    }

    @Test
    fun `a folder is refused a page change`() {
        val folders = layout(mapOf("f" to GridRect(1, 0, 1, 1)), folders = setOf("f"))
        val grab = beginCanvasGrab(folders, "f")!!
        assertTrue(grab.isFolder)
        assertNull(grab.changedPage(otherPage))
    }

    @Test
    fun `a refused page change leaves the editor where it was`() {
        val folders = layout(mapOf("f" to GridRect(1, 0, 1, 1)), folders = setOf("f"))
        val h = Harness()
        h.editor.enter()
        h.editor.begin(folders, "f", 1, 1)
        val before = h.editor.grab
        assertFalse(h.editor.changePage(otherPage))
        assertSame(before, h.editor.grab)
        assertEquals(0, h.selected)
    }

    @Test
    fun `a page that is not a canvas or already holds the tile is refused`() {
        val grab = beginCanvasGrab(homeLayout, "a")!!
        assertNull(grab.changedPage(JarnCanvasPage(1, JarnCanvasPageKind.Rss)))
        assertNull(grab.changedPage(JarnCanvasPage(1, JarnCanvasPageKind.Canvas, homeLayout)))
    }

    @Test
    fun `the home baseline is never changed by moves or page changes`() {
        val start = beginCanvasGrab(homeLayout, "a")!!
        val frozen = start.home
        val travelled = start.moved(1, 0).resized(1)
            .changedPage(otherPage)!!.movedTo(1, 0).resizedTo(GridRect(1, 0, 2, 2))
            .changedPage(homePage)!!
        assertEquals(frozen, travelled.home)
        assertEquals(homeLayout.activeArrangement, travelled.home.baseline.placements)
        assertEquals(homeLayout.activeArrangement, travelled.preview.placements)
        assertEquals(frozen.rect, travelled.rect)
    }

    @Test
    fun `solving on another page pushes from that page's baseline`() {
        val away = beginCanvasGrab(homeLayout, "a")!!.changedPage(otherPage)!!.movedTo(1, 0)
        assertEquals(GridRect(1, 0, 1, 1), away.preview.placements["a"])
        assertEquals(GridRect(2, 0, 1, 1), away.preview.placements["c"])
        assertEquals(otherLayout.activeArrangement, away.baseline.placements)
    }

    @Test
    fun `a tile is fitted to a shorter page`() {
        val tall = layout(mapOf("a" to GridRect(0, 0, 1, 4)))
        val short = JarnCanvasPage(1, JarnCanvasPageKind.Canvas, layout(mapOf("c" to GridRect(3, 0, 1, 1)), rows = 2))
        val away = beginCanvasGrab(tall, "a")!!.changedPage(short)!!
        assertEquals(2, away.cursor.rowSpan)
    }

    @Test
    fun `a between-page drop writes the destination first`() {
        val away = beginCanvasGrab(homeLayout, "a")!!.changedPage(otherPage)!!.movedTo(2, 1)
        val drop = away.dropResult() as CanvasGrabDrop.BetweenPages
        assertEquals(listOf(1, 0), drop.writes.map { it.pageIndex })
        assertEquals(GridRect(2, 1, 1, 1), drop.destination.placements["a"])
        assertEquals(setOf("b"), drop.source.placements.keys)
    }

    @Test
    fun `dropping on another page before placing it changes nothing`() {
        val away = beginCanvasGrab(homeLayout, "a")!!.changedPage(otherPage)!!
        assertEquals(CanvasGrabDrop.Unchanged, away.dropResult())
    }

    @Test
    fun `a between-page drop with no write path cancels and goes home`() {
        val h = grabbed()
        val commits = mutableListOf<Map<String, GridRect>>()
        h.editor.changePage(otherPage)
        h.sync()
        h.editor.step { movedTo(2, 1) }
        h.editor.drop { commits += it }
        assertTrue(commits.isEmpty())
        assertNull(h.editor.grab)
        assertEquals(0, h.selected)
    }

    @Test
    fun `a between-page drop reaches the write path once`() {
        val h = grabbed()
        val moves = mutableListOf<CanvasGrabDrop.BetweenPages>()
        h.editor.changePage(otherPage)
        h.sync()
        h.editor.step { movedTo(2, 1) }
        h.editor.drop(moveBetweenPages = { moves += it }) { }
        assertEquals(1, moves.size)
        assertEquals(1, h.selected)
        assertNull(h.editor.grab)
    }

    @Test
    fun `going home and dropping in place writes nothing`() {
        val h = grabbed()
        val commits = mutableListOf<Map<String, GridRect>>()
        h.editor.changePage(otherPage)
        h.sync()
        h.editor.changePage(homePage)
        h.sync()
        h.editor.drop { commits += it }
        assertTrue(commits.isEmpty())
        assertEquals(0, h.selected)
    }

    @Test
    fun `touch and controller drops write the same result`() {
        val pad = grabbed()
        val touch = grabbed(CanvasGrabDriver.Touch)
        pad.editor.step { movedTo(2, 0) }
        touch.editor.drive(CanvasGrabDriver.Touch) { movedTo(2, 0) }
        var padWrite: Map<String, GridRect>? = null
        var touchWrite: Map<String, GridRect>? = null
        pad.editor.drop { padWrite = it }
        touch.editor.drop { touchWrite = it }
        assertEquals(padWrite, touchWrite)
    }
}
