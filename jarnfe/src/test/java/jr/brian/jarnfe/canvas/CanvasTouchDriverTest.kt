package jr.brian.jarnfe.canvas

import androidx.compose.ui.geometry.Offset
import jr.brian.home.canvas.model.CanvasItem
import jr.brian.home.canvas.model.CanvasLayout
import jr.brian.home.canvas.model.CanvasScrollOrientation
import jr.brian.home.canvas.model.GridRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTouchDriverTest {
    private val rects = mapOf("a" to GridRect(1, 0, 1, 1), "b" to GridRect(3, 0, 1, 1))
    private val layout = CanvasLayout(
        activeOrientation = CanvasScrollOrientation.HORIZONTAL,
        horizontalRows = 4,
        items = rects.keys.map { CanvasItem.AppItem(it, "pkg.$it") },
        verticalArrangement = rects,
        horizontalArrangement = rects,
    )
    private val board = CanvasBoardGeometry(cellPx = 90f, spacingPx = 10f, paddingPx = 20f)

    private inner class Harness {
        val editor = CanvasEditor().apply { showPage(0) }
        val driver = CanvasTouchDriver(editor)
        val menus = mutableListOf<String>()
        val commits = mutableListOf<Map<String, GridRect>>()
        var haptics = 0
        var blocked = 0

        init {
            driver.geometry = board
            driver.actions = CanvasTouchActions(
                enabled = true,
                layout = layout,
                placements = rects,
                minSpans = { 1 to 1 },
                openMenu = { menus += it.id },
                commit = { _, placements -> commits += placements },
                onGrabbed = {},
                haptic = { haptics += 1 },
                showBlocked = { blocked += 1 },
            )
        }

        val press = Offset(board.originOf(rects.getValue("a")).x + 40f, 60f)
        val gesture = CanvasTouchGesture(1f)

        fun send(effect: CanvasTouchEffect, point: Offset = press) =
            driver.handle(gesture, effect, "a", press, point)

        fun down() = send(gesture.down(driver.startFor("a"), press.x, press.y, 0L))

        fun hold() {
            down()
            send(gesture.timeReached(360L))
        }

        fun moveBy(cells: Float): Offset {
            val point = press + Offset(cells * 100f, 0f)
            send(gesture.moved(point.x, point.y), point)
            return point
        }
    }

    @Test
    fun `a hold lifts with a haptic and no grab, and release opens the menu`() {
        val h = Harness().apply { hold() }
        assertEquals("a", h.driver.liftedId)
        assertEquals(1, h.haptics)
        assertNull(h.editor.grab)
        assertFalse(h.editor.editing)
        h.send(h.gesture.up())
        assertNull(h.driver.liftedId)
        assertEquals(listOf("a"), h.menus)
    }

    @Test
    fun `hold and drag reflows live and the drop commits and returns to browse`() {
        val h = Harness().apply { hold() }
        h.moveBy(1f)
        assertEquals(CanvasGrabDriver.Touch, h.editor.grab?.driver)
        assertEquals(2, h.editor.grab?.rect?.col)
        h.send(h.gesture.up())
        assertNull(h.editor.grab)
        assertFalse(h.editor.editing)
        assertEquals(2, h.commits.single().getValue("a").col)
        assertTrue(h.menus.isEmpty())
    }

    @Test
    fun `the cell holds until the tile is clearly past its midpoint`() {
        val h = Harness().apply { hold() }
        h.moveBy(0.7f)
        assertEquals(1, h.editor.grab?.rect?.col)
        h.moveBy(0.8f)
        assertEquals(2, h.editor.grab?.rect?.col)
        h.moveBy(1.3f)
        assertEquals(2, h.editor.grab?.rect?.col)
    }

    @Test
    fun `a second finger restores the layout and saves nothing`() {
        val h = Harness().apply { hold() }
        h.moveBy(1f)
        h.send(h.gesture.otherFinger(palmLike = false))
        assertNull(h.editor.grab)
        assertFalse(h.editor.editing)
        assertEquals(rects, h.editor.placements(layout))
        h.send(h.gesture.up())
        assertTrue(h.commits.isEmpty())
    }

    @Test
    fun `touching the moving tile takes it from the controller`() {
        val h = Harness()
        h.editor.enter()
        h.editor.begin(layout, "a", 1, 1)
        h.editor.step { moved(1, 0) }
        h.down()
        assertEquals(CanvasGrabDriver.Touch, h.editor.grab?.driver)
        assertEquals(2, h.editor.grab?.rect?.col)
        h.send(h.gesture.up())
        assertEquals(2, h.commits.single().getValue("a").col)
        assertTrue(h.editor.editing)
    }

    @Test
    fun `touching another tile during a controller grab leaves the move alone`() {
        val h = Harness()
        h.editor.enter()
        h.editor.begin(layout, "b", 1, 1)
        h.editor.step { moved(1, 0) }
        val before = h.editor.grab
        h.down()
        assertEquals(1, h.blocked)
        assertEquals(before, h.editor.grab)
        h.send(h.gesture.up())
        assertEquals(before, h.editor.grab)
        assertTrue(h.commits.isEmpty())
    }

    @Test
    fun `a controller that takes over mid-drag isn't dropped by the finger`() {
        val h = Harness().apply { hold() }
        h.moveBy(1f)
        h.editor.takeOver(CanvasGrabDriver.Controller, "a")
        h.send(h.gesture.up())
        assertEquals(CanvasGrabDriver.Controller, h.editor.grab?.driver)
        assertTrue(h.commits.isEmpty())
    }

    @Test
    fun `a tile can't lift once a controller grab has started`() {
        val h = Harness().apply { down() }
        h.editor.enter()
        h.editor.begin(layout, "b", 1, 1)
        h.send(h.gesture.timeReached(360L))
        assertNull(h.driver.liftedId)
        assertEquals(CanvasTouchPhase.Blocked, h.gesture.phase)
        assertEquals("b", h.editor.grab?.itemId)
    }

    @Test
    fun `hit testing skips the gaps between tiles`() {
        val h = Harness()
        val origin = board.originOf(rects.getValue("a"))
        assertEquals("a", h.driver.tileAt(origin + Offset(10f, 10f)))
        assertNull(h.driver.tileAt(origin + Offset(95f, 10f)))
    }
}
