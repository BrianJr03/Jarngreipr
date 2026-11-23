package jr.brian.jarnfe.canvas

import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasArrangeTest {
    private val rects = mapOf("a" to GridRect(1, 0, 1, 1), "b" to GridRect(2, 0, 1, 1))
    private val layout = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = 4,
        items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        verticalArrangement = rects,
        horizontalArrangement = rects,
    )

    @Test
    fun `canvas buttons follow grid focus`() {
        assertTrue(canvasOwnsButtons(gridHasFocus = true, hasFocusTarget = true))
        assertFalse(canvasOwnsButtons(gridHasFocus = false, hasFocusTarget = true))
    }

    @Test
    fun `a page with nothing to focus keeps its options`() {
        assertTrue(canvasOwnsButtons(gridHasFocus = false, hasFocusTarget = false))
    }

    @Test
    fun `a menu move returns to browsing after the drop`() {
        val editor = CanvasEditor()
        editor.beginMove(layout, "a", 1, 1)
        assertTrue(editor.editing)
        assertFalse(editor.arranging)
        editor.step { moved(0, 1) }
        editor.drop { }
        assertNull(editor.grab)
        assertFalse(editor.editing)
    }

    @Test
    fun `cancelling a menu move returns to browsing`() {
        val editor = CanvasEditor()
        editor.beginMove(layout, "a", 1, 1)
        editor.cancelGrab()
        assertFalse(editor.editing)
    }

    @Test
    fun `a refused menu move does not strand edit mode`() {
        val editor = CanvasEditor()
        editor.settled = rects
        editor.beginMove(layout, "a", 1, 1)
        assertNull(editor.grab)
        assertFalse(editor.editing)
    }

    @Test
    fun `a menu move inside Arrange stays in Arrange`() {
        val editor = CanvasEditor()
        editor.enter()
        editor.beginMove(layout, "a", 1, 1)
        assertTrue(editor.arranging)
        editor.step { moved(0, 1) }
        editor.drop { }
        assertTrue(editor.arranging)
    }

    @Test
    fun `B cancels a held tile first and then leaves Arrange`() {
        val editor = CanvasEditor()
        editor.enter()
        editor.begin(layout, "a", 1, 1)
        editor.cancelGrab()
        assertNull(editor.grab)
        assertTrue(editor.arranging)
        editor.exit()
        assertFalse(editor.editing)
    }

    @Test
    fun `a tile added while browsing returns to browsing once placed`() {
        val editor = CanvasEditor()
        editor.beginAdded(layout, "a", 1, 1)
        assertFalse(editor.arranging)
        editor.drop { }
        assertFalse(editor.editing)
    }

    @Test
    fun `a new tile dropped where it landed still commits so it can be undone`() {
        val editor = CanvasEditor()
        val commits = mutableListOf<Map<String, GridRect>>()
        editor.beginAdded(layout, "a", 1, 1)
        editor.drop { commits += it }
        assertEquals(listOf(rects), commits)
    }

    @Test
    fun `an existing tile dropped in place commits nothing`() {
        val editor = CanvasEditor()
        val commits = mutableListOf<Map<String, GridRect>>()
        editor.beginMove(layout, "a", 1, 1)
        editor.drop { commits += it }
        assertTrue(commits.isEmpty())
    }

    @Test
    fun `the banner step follows who holds the tile`() {
        val editor = CanvasEditor()
        assertNull(editor.arrangeStep)
        editor.enter()
        assertEquals(CanvasArrangeStep.Choose, editor.arrangeStep)
        editor.begin(layout, "a", 1, 1)
        assertEquals(CanvasArrangeStep.HoldingWithController, editor.arrangeStep)
        editor.takeOver(CanvasGrabDriver.Touch, "a")
        assertEquals(CanvasArrangeStep.HoldingWithTouch, editor.arrangeStep)
    }

    @Test
    fun `a touch drag from browse shows no Arrange banner`() {
        val editor = CanvasEditor()
        editor.beginTouch(layout, "a", 1, 1)
        assertNull(editor.arrangeStep)
    }

    @Test
    fun `focus stops at the grid edge`() {
        assertNull(canvasFocusStep(rects, "b", 1, 0))
        assertNull(canvasFocusStep(rects, "a", 0, -1))
    }

    @Test
    fun `focus prefers a tile in the same row over a closer diagonal`() {
        val placements = mapOf(
            "from" to GridRect(0, 0, 1, 1),
            "diagonal" to GridRect(1, 1, 1, 1),
            "sameRow" to GridRect(3, 0, 1, 1),
        )
        assertEquals("sameRow", canvasFocusStep(placements, "from", 1, 0))
    }

    @Test
    fun `focus reaches a diagonal tile when the lane is empty`() {
        val placements = mapOf("from" to GridRect(0, 0, 1, 1), "diagonal" to GridRect(2, 2, 1, 1))
        assertEquals("diagonal", canvasFocusStep(placements, "from", 1, 0))
    }

    @Test
    fun `focus from an unknown tile goes nowhere`() {
        assertNull(canvasFocusStep(rects, "gone", 1, 0))
        assertNull(canvasFocusStep(rects, null, 1, 0))
    }
}
