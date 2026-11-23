package jr.brian.jarnfe.canvas

import jr.brian.frontend.library.art.LibraryGame
import jr.brian.home.canvas.grid.reservedRectsFor
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasBulkAddTest {
    private fun apps(count: Int, prefix: String = "new") =
        List(count) { CanvasItem.AppItem("$prefix-$it", "pkg.$prefix.$it") }

    private fun CanvasLayout.assertNoOverlaps(orientation: CanvasScrollOrientation) {
        val rects = arrangementFor(orientation).values.toList() + reservedRectsFor(this, orientation)
        rects.forEachIndexed { index, rect ->
            rects.drop(index + 1).forEach { assertFalse("$rect overlaps $it", rect.overlaps(it)) }
        }
    }

    private fun game(path: String?, relative: String = "game.zip") = LibraryGame(
        id = 1,
        key = "1:nes:$relative",
        systemId = "nes",
        title = "Game",
        relativePath = relative,
        absolutePath = path,
        isFavorite = false,
        playCount = 0,
        lastPlayed = null,
    )

    @Test
    fun `added tiles land in free cells and never move a placed tile`() {
        val existing = CanvasItem.AppItem("old", "pkg.old")
        val rect = GridRect(1, 0, 1, 1)
        val layout = CanvasLayout(
            items = listOf(existing),
            verticalArrangement = mapOf("old" to rect),
            horizontalArrangement = mapOf("old" to rect),
        )
        val plan = jarnPlanCanvasAdds(layout, apps(10))
        assertEquals(10, plan.placedIds.size)
        assertEquals(0, plan.noRoom)
        assertEquals(rect, plan.layout.verticalArrangement["old"])
        assertEquals(rect, plan.layout.horizontalArrangement["old"])
        plan.layout.assertNoOverlaps(CanvasScrollOrientation.VERTICAL)
        plan.layout.assertNoOverlaps(CanvasScrollOrientation.HORIZONTAL)
    }

    @Test
    fun `a full page keeps every tile and reports what did not fit`() {
        val layout = CanvasLayout()
        val room = jarnCanvasFreeSlots(layout)
        val plan = jarnPlanCanvasAdds(layout, apps(room + 3))
        assertEquals(room, plan.placedIds.size)
        assertEquals(3, plan.noRoom)
        assertEquals(0, jarnCanvasFreeSlots(plan.layout))
        plan.layout.assertNoOverlaps(CanvasScrollOrientation.VERTICAL)
        plan.layout.assertNoOverlaps(CanvasScrollOrientation.HORIZONTAL)
        assertTrue(plan.layout.items.all { it.id in plan.layout.verticalArrangement })
        assertTrue(plan.layout.items.all { it.id in plan.layout.horizontalArrangement })
    }

    @Test
    fun `free slots use the tighter orientation and skip the reserved cell`() {
        val layout = CanvasLayout(verticalColumns = 2, horizontalRows = 6)
        assertEquals(2 * JARN_CANVAS_ADD_LANES - 1, jarnCanvasFreeSlots(layout))
    }

    @Test
    fun `selection stops at capacity but can still remove`() {
        val full = CanvasPickSelection<String>(capacity = 2).toggled("a", "a").toggled("b", "b")
        assertTrue(full.isFull)
        assertEquals(full, full.toggled("c", "c"))
        assertEquals(setOf("b"), full.toggled("a", "a").picked.keys)
    }

    @Test
    fun `select all keeps earlier picks and respects capacity`() {
        val selection = CanvasPickSelection<String>(capacity = 3).toggled("z", "z")
        val next = selection.withAll(listOf("a", "b", "c")) { it }
        assertEquals(listOf("z", "a", "b"), next.picked.keys.toList())
        assertEquals(0, next.cleared().count)
    }

    @Test
    fun `notices say clearly what happened`() {
        assertEquals(CanvasAddNotice.PageFull, canvasAddNotice(JarnCanvasAddResult(noRoom = 2)))
        assertEquals(
            CanvasAddNotice.SomeDidNotFit(2),
            canvasAddNotice(JarnCanvasAddResult(addedIds = listOf("a"), noRoom = 2)),
        )
        assertEquals(CanvasAddNotice.AlreadyHere, canvasAddNotice(JarnCanvasAddResult(alreadyPlaced = 1)))
        assertEquals(CanvasAddNotice.Added(2), canvasAddNotice(JarnCanvasAddResult(addedIds = listOf("a", "b"))))
        assertNull(canvasAddNotice(JarnCanvasAddResult(addedIds = listOf("a"))))
    }

    @Test
    fun `a library game gets the key Classic pins it under`() {
        assertEquals("nes//roms/nes/game.zip", game("/roms/nes/game.zip").toCanvasRomPick().rom.key)
        assertEquals("nes/game.zip", game(null).toCanvasRomPick().rom.key)
    }

    @Test
    fun `browse rows report when they were capped`() {
        assertEquals(listOf(1, 2) to true, canvasBrowseRows(listOf(1, 2, 3), limit = 2))
        assertEquals(listOf(1, 2) to false, canvasBrowseRows(listOf(1, 2), limit = 2))
    }

    @Test
    fun `search keeps order when blank and ranks matches otherwise`() {
        val titles = listOf("Zelda", "Mario Kart", "Super Mario")
        assertEquals(titles, canvasPickerMatches(titles, " ") { it })
        assertEquals(listOf("Mario Kart", "Super Mario"), canvasPickerMatches(titles, "mario") { it })
    }
}
