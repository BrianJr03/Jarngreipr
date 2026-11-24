package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.CanvasScrollOrientation.HORIZONTAL
import jr.brian.home.canvas.model.CanvasScrollOrientation.VERTICAL
import jr.brian.home.canvas.model.GridCell
import jr.brian.home.canvas.model.GridRect
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JarnCanvasSetupTest {

    private class FakeWrites : CanvasUndoWrites {
        val commits = mutableListOf<Map<String, GridRect>>()

        override fun commit(
            pageIndex: Int,
            orientation: CanvasScrollOrientation,
            placements: Map<String, GridRect>,
        ) {
            commits += placements
        }

        override suspend fun remove(pageIndex: Int, itemId: String) = Unit
    }

    private fun layout(
        orientation: CanvasScrollOrientation = HORIZONTAL,
        horizontal: Map<String, GridRect> = emptyMap(),
        vertical: Map<String, GridRect> = horizontal,
        menu: Boolean = false,
        columns: Int = 4,
        rows: Int = 6,
    ) = CanvasLayout(
        activeOrientation = orientation,
        verticalColumns = columns,
        horizontalRows = rows,
        items = horizontal.keys.map { CanvasItem.AppItem(id = it, packageName = "pkg.$it") },
        verticalArrangement = vertical,
        horizontalArrangement = horizontal,
        menuButtonVisible = menu,
    )

    private fun JarnCanvasSetupResult.applied() = (this as JarnCanvasSetupResult.Applied).layout

    private fun assertNoOverlaps(placements: Map<String, GridRect>) {
        val rects = placements.values.toList()
        rects.forEachIndexed { index, rect ->
            rects.drop(index + 1).forEach { assertFalse("$rect overlaps $it", rect.overlaps(it)) }
        }
    }

    @Test
    fun `reducing rows past a tile is refused and changes nothing`() {
        val start = layout(horizontal = mapOf("a" to GridRect(0, 4, 1, 2)))
        val result = canvasLayoutAfterSetup(start, JarnCanvasSetupChange.CrossAxis(HORIZONTAL, 5))
        assertEquals(JarnCanvasSetupResult.Refused(JarnCanvasSetupRefusal.TilesPastEdge), result)
    }

    @Test
    fun `reducing rows that all tiles fit in keeps every placement`() {
        val start = layout(horizontal = mapOf("a" to GridRect(0, 3, 1, 2)))
        val next = canvasLayoutAfterSetup(start, JarnCanvasSetupChange.CrossAxis(HORIZONTAL, 5)).applied()
        assertEquals(5, next.horizontalRows)
        assertEquals(start.horizontalArrangement, next.horizontalArrangement)
        assertEquals(start.verticalArrangement, next.verticalArrangement)
    }

    @Test
    fun `changing columns refuses when the menu cell would land on a tile`() {
        val start = layout(
            orientation = VERTICAL,
            horizontal = mapOf("a" to GridRect(2, 0, 1, 1)),
            menu = true,
        )
        val result = canvasLayoutAfterSetup(start, JarnCanvasSetupChange.CrossAxis(VERTICAL, 3))
        assertEquals(JarnCanvasSetupResult.Refused(JarnCanvasSetupRefusal.MenuCellTaken), result)
    }

    @Test
    fun `tidy keeps every tile and closes gaps without overlap`() {
        val start = layout(
            horizontal = mapOf(
                "a" to GridRect(3, 0, 2, 2),
                "b" to GridRect(7, 0, 1, 1),
                "c" to GridRect(9, 3, 1, 2),
            ),
            menu = true,
        )
        val tidied = canvasTidied(start)!!
        assertEquals(start.horizontalArrangement.keys, tidied.keys)
        assertNoOverlaps(tidied)
        assertFalse(tidied.values.any { it.overlaps(GridRect(0, 0, 1, 1)) })
        start.horizontalArrangement.forEach { (id, rect) ->
            assertEquals(rect.colSpan, tidied.getValue(id).colSpan)
            assertEquals(rect.rowSpan, tidied.getValue(id).rowSpan)
        }
    }

    @Test
    fun `tidy reports a page with no gaps`() {
        assertNull(canvasTidied(layout(horizontal = mapOf("a" to GridRect(0, 0, 1, 1)))))
    }

    @Test
    fun `tidy saves once and the normal undo restores the old layout`() = runTest {
        val before = mapOf("a" to GridRect(4, 0, 1, 1))
        val page = JarnCanvasPage(0, JarnCanvasPageKind.Canvas, layout(horizontal = before))
        val writes = FakeWrites()
        val undo = CanvasUndoController(backgroundScope)
        assertTrue(tidyCanvasPage(undo, writes, page))
        val after = writes.commits.single()
        assertEquals(CanvasUndoKind.Tidied, undo.entry?.kind)
        assertEquals(before, undo.undo(after))
        assertEquals(listOf(after, before), writes.commits)
    }

    @Test
    fun `tidy on a tidy page writes nothing and opens no undo`() = runTest {
        val page = JarnCanvasPage(
            0,
            JarnCanvasPageKind.Canvas,
            layout(horizontal = mapOf("a" to GridRect(0, 0, 1, 1))),
        )
        val writes = FakeWrites()
        val undo = CanvasUndoController(backgroundScope)
        assertFalse(tidyCanvasPage(undo, writes, page))
        assertTrue(writes.commits.isEmpty())
        assertNull(undo.entry)
    }

    @Test
    fun `first open cell avoids the menu tile`() {
        assertEquals(GridCell(0, 1), canvasFirstOpenCell(layout(menu = true)))
        assertEquals(GridCell(0, 0), canvasFirstOpenCell(layout(orientation = VERTICAL, menu = true)))
        assertEquals(GridCell(0, 0), canvasFirstOpenCell(layout()))
    }
}
