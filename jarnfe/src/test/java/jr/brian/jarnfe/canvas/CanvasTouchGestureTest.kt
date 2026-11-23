package jr.brian.jarnfe.canvas

import jr.brian.jarnfe.canvas.CanvasTouchEffect.BeginDrag
import jr.brian.jarnfe.canvas.CanvasTouchEffect.CancelDrag
import jr.brian.jarnfe.canvas.CanvasTouchEffect.Drop
import jr.brian.jarnfe.canvas.CanvasTouchEffect.Lift
import jr.brian.jarnfe.canvas.CanvasTouchEffect.Move
import jr.brian.jarnfe.canvas.CanvasTouchEffect.None
import jr.brian.jarnfe.canvas.CanvasTouchEffect.OpenMenu
import jr.brian.jarnfe.canvas.CanvasTouchEffect.Tap
import jr.brian.jarnfe.canvas.CanvasTouchEffect.TakeOver
import jr.brian.jarnfe.canvas.CanvasTouchEffect.Unlift
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTouchGestureTest {
    private fun gesture(start: CanvasTouchStart = CanvasTouchStart.Browse) =
        CanvasTouchGesture(density = 1f).apply { down(start, 100f, 100f, 0L) }

    private fun lifted(start: CanvasTouchStart = CanvasTouchStart.Browse) = gesture(start).apply {
        timeReached(if (start == CanvasTouchStart.BrowseWidget) 500L else 360L)
    }

    @Test
    fun `a tap opens as soon as the finger lifts`() {
        assertEquals(Tap, gesture().up())
    }

    @Test
    fun `the hold waits the full time`() {
        val g = gesture()
        assertEquals(None, g.timeReached(359L))
        assertEquals(Lift, g.timeReached(360L))
        assertEquals(CanvasTouchPhase.Lifted, g.phase)
    }

    @Test
    fun `releasing a held tile opens its menu`() {
        assertEquals(OpenMenu, lifted().up())
    }

    @Test
    fun `small movement inside the slop keeps the hold alive`() {
        val g = gesture()
        assertEquals(None, g.moved(107f, 100f))
        assertEquals(Lift, g.timeReached(360L))
    }

    @Test
    fun `a quick drag scrolls and never lifts or moves the tile`() {
        val g = gesture()
        assertEquals(None, g.moved(120f, 100f))
        assertEquals(CanvasTouchPhase.PassedThrough, g.phase)
        assertFalse(g.ownsPointer)
        assertNull(g.liftAtMs)
        assertEquals(None, g.timeReached(1000L))
        assertEquals(None, g.moved(200f, 100f))
        assertEquals(None, g.up())
    }

    @Test
    fun `a held tile starts moving only past the drag distance`() {
        val g = lifted()
        assertEquals(None, g.moved(109f, 100f))
        assertEquals(BeginDrag, g.moved(111f, 100f))
        assertEquals(Move, g.moved(130f, 100f))
        assertEquals(Drop, g.up())
    }

    @Test
    fun `edit layout drags right away with no hold`() {
        val g = gesture(CanvasTouchStart.Edit)
        assertNull(g.liftAtMs)
        assertEquals(BeginDrag, g.moved(109f, 100f))
        assertEquals(Drop, g.up())
    }

    @Test
    fun `a tap in edit layout stays a tap`() {
        assertEquals(Tap, gesture(CanvasTouchStart.Edit).up())
    }

    @Test
    fun `a second finger during a press stops the hold and the tap`() {
        val g = gesture()
        assertEquals(None, g.otherFinger(palmLike = false))
        assertTrue(g.ownsPointer)
        assertEquals(None, g.timeReached(360L))
        assertEquals(None, g.up())
    }

    @Test
    fun `a second finger on a held tile restores it without a menu`() {
        val g = lifted()
        assertEquals(Unlift, g.otherFinger(palmLike = false))
        assertEquals(None, g.up())
    }

    @Test
    fun `a second finger during a drag cancels and never drops`() {
        val g = lifted()
        g.moved(130f, 100f)
        assertEquals(CancelDrag, g.otherFinger(palmLike = false))
        assertEquals(None, g.moved(150f, 100f))
        assertEquals(None, g.up())
    }

    @Test
    fun `a palm contact changes nothing`() {
        val g = lifted()
        g.moved(130f, 100f)
        assertEquals(None, g.otherFinger(palmLike = true))
        assertEquals(Move, g.moved(150f, 100f))
        assertEquals(Drop, g.up())
    }

    @Test
    fun `palm detection covers large contacts and the side and bottom edges`() {
        assertTrue(canvasTouchIsPalm(32f, 200f, 200f, 800f, 600f))
        assertTrue(canvasTouchIsPalm(10f, 15f, 200f, 800f, 600f))
        assertTrue(canvasTouchIsPalm(10f, 785f, 200f, 800f, 600f))
        assertTrue(canvasTouchIsPalm(10f, 200f, 585f, 800f, 600f))
        assertFalse(canvasTouchIsPalm(10f, 200f, 5f, 800f, 600f))
        assertFalse(canvasTouchIsPalm(31f, 200f, 200f, 800f, 600f))
    }

    @Test
    fun `touching the moving tile takes the grab and release drops it`() {
        val g = CanvasTouchGesture(1f)
        assertEquals(TakeOver, g.down(CanvasTouchStart.TakeOver, 0f, 0f, 0L))
        assertEquals(Move, g.moved(1f, 0f))
        assertEquals(Drop, g.up())
    }

    @Test
    fun `touching another tile during a grab does nothing`() {
        val g = CanvasTouchGesture(1f)
        assertEquals(None, g.down(CanvasTouchStart.Blocked, 0f, 0f, 0L))
        assertTrue(g.ownsPointer)
        assertEquals(None, g.moved(50f, 0f))
        assertEquals(None, g.up())
    }

    @Test
    fun `a system cancel restores a drag and opens nothing`() {
        val dragging = lifted().apply { moved(130f, 100f) }
        assertEquals(CancelDrag, dragging.cancelled())
        assertEquals(Unlift, lifted().cancelled())
        assertEquals(None, gesture().cancelled())
    }

    @Test
    fun `a widget waits longer before it lifts`() {
        val g = gesture(CanvasTouchStart.BrowseWidget)
        assertEquals(None, g.timeReached(499L))
        assertEquals(Lift, g.timeReached(500L))
    }

    @Test
    fun `a widget hold aborts on movement`() {
        val g = gesture(CanvasTouchStart.BrowseWidget)
        g.moved(120f, 100f)
        assertEquals(None, g.timeReached(500L))
        assertEquals(CanvasTouchPhase.PassedThrough, g.phase)
    }

    @Test
    fun `a widget hold aborts when a child claims the touch`() {
        val g = gesture(CanvasTouchStart.BrowseWidget)
        g.claimedElsewhere()
        assertEquals(None, g.timeReached(500L))
        assertEquals(None, g.up())
    }

    @Test
    fun `a widget hold aborts on a second finger and on up`() {
        val second = gesture(CanvasTouchStart.BrowseWidget)
        second.otherFinger(palmLike = false)
        assertEquals(None, second.timeReached(500L))
        val released = gesture(CanvasTouchStart.BrowseWidget)
        assertEquals(Tap, released.up())
        assertEquals(None, released.timeReached(500L))
    }

    @Test
    fun `a claim after the lift can't take the tile back`() {
        val g = lifted(CanvasTouchStart.BrowseWidget)
        g.claimedElsewhere()
        assertEquals(CanvasTouchPhase.Lifted, g.phase)
    }

    @Test
    fun `a widget claims a swipe only along an axis it can scroll`() {
        val verticalList = CanvasScrollAxes(horizontal = false, vertical = true)
        assertEquals(true, canvasWidgetClaim(1f, 4f, verticalList, minPx = 3f))
        assertEquals(false, canvasWidgetClaim(4f, 1f, verticalList, minPx = 3f))
        val carousel = CanvasScrollAxes(horizontal = true, vertical = false)
        assertEquals(true, canvasWidgetClaim(4f, 1f, carousel, minPx = 3f))
        assertEquals(false, canvasWidgetClaim(1f, 4f, carousel, minPx = 3f))
    }

    @Test
    fun `a widget waits for real movement and never claims without a scrollable child`() {
        val verticalList = CanvasScrollAxes(horizontal = false, vertical = true)
        assertNull(canvasWidgetClaim(1f, 1f, verticalList, minPx = 3f))
        assertNull(canvasWidgetClaim(0f, 40f, CanvasScrollAxes(false, false), minPx = 3f))
    }

    @Test
    fun `a tap during a grab never restarts it`() {
        assertEquals(CanvasTapAction.Ignore, canvasTapAction(hasGrab = true, editing = true))
        assertEquals(CanvasTapAction.Grab, canvasTapAction(hasGrab = false, editing = true))
        assertEquals(CanvasTapAction.Open, canvasTapAction(hasGrab = false, editing = false))
    }
}
