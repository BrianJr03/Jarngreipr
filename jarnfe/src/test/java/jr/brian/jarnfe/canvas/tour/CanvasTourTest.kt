package jr.brian.jarnfe.canvas.tour

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasTourTest {
    private class FakeMemory(override var seen: Boolean = false) : CanvasTourMemory {
        var marks = 0

        override fun markSeen() {
            seen = true
            marks++
        }
    }

    private fun CanvasTourState.pressAll(vararg inputs: CanvasTourInput) =
        inputs.fold(this) { state, input -> state.press(input) }

    @Test
    fun `every step can move forward until the tour ends`() {
        var run: CanvasTourState? = CanvasTourState()
        val seen = mutableListOf<CanvasTourStep>()
        while (run != null) {
            seen += run.step
            run = run.next()
        }
        assertEquals(CanvasTourStep.entries, seen)
    }

    @Test
    fun `do it for me finishes every practice step`() {
        CanvasTourStep.entries.filter { it.isPractice }.forEach { step ->
            assertTrue("$step", CanvasTourState(step).doneForMe().practiced)
        }
    }

    @Test
    fun `do it for me also works from a half done step`() {
        val halfDone = CanvasTourState(CanvasTourStep.OneShotMove).press(CanvasTourInput.Confirm)
        assertTrue(halfDone.doneForMe().practiced)
    }

    @Test
    fun `the wrong button teaches nothing`() {
        val launch = CanvasTourState(CanvasTourStep.Launch)
        assertEquals(launch, launch.press(CanvasTourInput.Context))
        val options = CanvasTourState(CanvasTourStep.TileOptions)
        assertEquals(options, options.press(CanvasTourInput.Confirm))
    }

    @Test
    fun `a one shot move needs a real move and returns to browsing`() {
        val held = CanvasTourState(CanvasTourStep.OneShotMove).press(CanvasTourInput.Confirm)
        assertEquals(CanvasTourMode.Holding, held.mode)
        assertFalse(held.press(CanvasTourInput.Confirm).practiced)
        val dropped = held.pressAll(CanvasTourInput.Right, CanvasTourInput.Confirm)
        assertTrue(dropped.practiced)
        assertEquals(CanvasTourMode.Browse, dropped.mode)
    }

    @Test
    fun `arrange opens from options and stays on`() {
        val arranged = CanvasTourState(CanvasTourStep.Arrange).pressAll(CanvasTourInput.Menu, CanvasTourInput.Confirm)
        assertTrue(arranged.practiced)
        assertEquals(CanvasTourMode.Arranging, arranged.mode)
    }

    @Test
    fun `shoulders resize only a held tile`() {
        val arranging = CanvasTourState(CanvasTourStep.Resize)
        assertEquals(arranging, arranging.press(CanvasTourInput.Grow))
        val grown = arranging.pressAll(CanvasTourInput.Confirm, CanvasTourInput.Grow)
        assertTrue(grown.practiced)
        assertEquals(2, grown.game.rect.colSpan)
    }

    @Test
    fun `shrinking a one cell tile is not a resize`() {
        val held = CanvasTourState(CanvasTourStep.Resize).press(CanvasTourInput.Confirm)
        assertFalse(held.press(CanvasTourInput.Shrink).practiced)
    }

    @Test
    fun `a fresh press past the edge changes page but a held one does not`() {
        val atEdge = CanvasTourState(CanvasTourStep.PageEdge)
        assertEquals(atEdge, atEdge.press(CanvasTourInput.Right, repeat = true))
        val carried = atEdge.press(CanvasTourInput.Right)
        assertTrue(carried.practiced)
        assertEquals(1, carried.game.page)
    }

    @Test
    fun `undo works only after a drop and restores the tile`() {
        val held = CanvasTourState(CanvasTourStep.Undo)
        assertEquals(held, held.press(CanvasTourInput.Undo))
        val undone = held.pressAll(CanvasTourInput.Confirm, CanvasTourInput.Undo)
        assertTrue(undone.practiced)
        assertEquals(0, undone.game.page)
    }

    @Test
    fun `tiles never leave the board or overlap`() {
        val held = CanvasTourState(CanvasTourStep.OneShotMove).press(CanvasTourInput.Confirm)
        assertEquals(held.game, held.press(CanvasTourInput.Up).game)
        val atBottom = held.press(CanvasTourInput.Down)
        assertEquals(atBottom.game, atBottom.press(CanvasTourInput.Down).game)
    }

    @Test
    fun `back undoes a half done step before leaving it`() {
        val start = CanvasTourState(CanvasTourStep.OneShotMove)
        assertEquals(start, start.press(CanvasTourInput.Confirm).back())
        assertEquals(CanvasTourStep.TileOptions, start.back()?.step)
        assertNull(CanvasTourState().back())
    }

    @Test
    fun `the tour offers itself exactly once`() {
        val memory = FakeMemory()
        val tour = CanvasTourController(memory)
        tour.offerOnce()
        assertNotNull(tour.state)
        assertTrue(memory.seen)
        tour.finish()
        tour.offerOnce()
        assertNull(tour.state)
        assertNull(CanvasTourController(memory).also { it.offerOnce() }.state)
    }

    @Test
    fun `a replay still works after the offer was used`() {
        val tour = CanvasTourController(FakeMemory(seen = true))
        tour.start()
        assertEquals(CanvasTourStep.Intro, tour.state?.step)
    }

    @Test
    fun `the practice pause only advances the step it belongs to`() {
        val tour = CanvasTourController(FakeMemory())
        tour.start()
        tour.primary()
        tour.primary()
        assertEquals(CanvasTourStep.Launch, tour.state?.step)
        tour.advanceFrom(CanvasTourStep.Intro)
        assertEquals(CanvasTourStep.Launch, tour.state?.step)
        tour.advanceFrom(CanvasTourStep.Launch)
        assertEquals(CanvasTourStep.TileOptions, tour.state?.step)
    }
}
