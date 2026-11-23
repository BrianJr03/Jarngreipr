package jr.brian.jarnfe.canvas

import androidx.compose.ui.geometry.Offset
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CanvasMoveRefusalTest {

    private fun layout(rects: Map<String, GridRect>, menuVisible: Boolean = false) = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = 4,
        items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        verticalArrangement = rects,
        horizontalArrangement = rects,
        menuButtonVisible = menuVisible,
    )

    private val lone = layout(mapOf("a" to GridRect(1, 0, 1, 1)))

    @Test
    fun `a free move is not refused`() {
        assertNull(beginCanvasGrab(lone, "a", visiblePushCells = 6)!!.moved(1, 1).refusal)
    }

    @Test
    fun `stepping off the board says why`() {
        val grab = beginCanvasGrab(lone, "a")!!.moved(0, -1)
        assertEquals(CanvasMoveRefusal.BoardEdge, grab.refusal)
        assertEquals(0, grab.rect.row)
    }

    @Test
    fun `stepping back onto the board clears the reason`() {
        val grab = beginCanvasGrab(lone, "a")!!.moved(0, -1).moved(0, 1)
        assertNull(grab.refusal)
    }

    @Test
    fun `the menu cell refuses a tile`() {
        val page = layout(mapOf("a" to GridRect(1, 0, 1, 1)), menuVisible = true)
        assertEquals(CanvasMoveRefusal.MenuCell, beginCanvasGrab(page, "a")!!.moved(-1, 0).refusal)
    }

    @Test
    fun `a move from code never shows a reason`() {
        assertNull(beginCanvasGrab(lone, "a")!!.movedTo(1, -4).refusal)
    }

    @Test
    fun `clearing the preview clears the reason`() {
        assertNull(beginCanvasGrab(lone, "a")!!.moved(0, -1).previewCleared().refusal)
    }

    @Test
    fun `shrinking at the minimum says smallest`() {
        assertEquals(CanvasMoveRefusal.SmallestSize, beginCanvasGrab(lone, "a")!!.resized(-1).refusal)
    }

    @Test
    fun `growing past the rows says largest`() {
        var grab = beginCanvasGrab(lone, "a")!!.axisToggled()
        repeat(3) { grab = grab.resized(1) }
        assertNull(grab.refusal)
        assertEquals(CanvasMoveRefusal.LargestSize, grab.resized(1).refusal)
    }

    @Test
    fun `a corner drag past its limit is refused on either side`() {
        val from = GridRect(2, 1, 2, 2)
        val clamped = from.copy(colSpan = 1)
        assertEquals(
            CanvasMoveRefusal.SmallestSize,
            canvasResizeRefusal(from, CanvasResizeCorner.SouthEast, -3, 0, clamped),
        )
        assertEquals(
            CanvasMoveRefusal.LargestSize,
            canvasResizeRefusal(from, CanvasResizeCorner.NorthWest, 0, -4, from.copy(row = 0, rowSpan = 3)),
        )
        assertNull(canvasResizeRefusal(from, CanvasResizeCorner.SouthEast, 1, 0, from.copy(colSpan = 3)))
    }

    @Test
    fun `the chip centers on the tile's lower edge`() {
        val origin = canvasChipOrigin(Offset(100f, 100f), 100f, 100f, 40f, 20f, 500f, 500f, 5f)
        assertEquals(Offset(130f, 175f), origin)
    }

    @Test
    fun `the chip stays on the board`() {
        val origin = canvasChipOrigin(Offset(-30f, 480f), 40f, 100f, 80f, 20f, 500f, 500f, 5f)
        assertEquals(Offset(0f, 480f), origin)
    }
}
