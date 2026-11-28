package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeRect
import jr.brian.jarnfe.input.JarnStickVector
import jr.brian.jarnfe.input.JarnSticks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val EPS = 1e-4f
private const val FRAME = 16_666_667L

private fun left(x: Float, y: Float = 0f) = JarnSticks(left = JarnStickVector(x, y))
private fun right(x: Float, y: Float = 0f) = JarnSticks(right = JarnStickVector(x, y))

class ThemeEditorSticksTest {
    private val title = textLayer("title", ThemeRect(0.3f, 0.3f, 0.2f, 0.1f))
    private val held = editorWith(title).copy(selected = ThemeEditorTarget.Layer("title")).pickedUp()

    private fun ThemeEditorState.rect() = rectOf(selected)!!.edges

    @After
    fun tearDown() = JarnThemeEditor.leave()

    @Test
    fun `full deflection moves half the screen a second`() {
        val moved = held.copy(snap = false).stickStep(left(1f), 0.1f, ThemeEditorStickTravel())
        assertEquals(0.35f, moved.rect().left, EPS)
    }

    @Test
    fun `small pushes add up across frames instead of rounding away`() {
        val travel = ThemeEditorStickTravel()
        var state = held
        repeat(20) { state = state.stickStep(left(0.1f), 0.016f, travel) }
        assertTrue("moved ${state.rect().left}", state.rect().left > 0.3f)
        assertEquals(0f, (state.rect().left * 100) % 1f, EPS)
    }

    @Test
    fun `speed follows deflection`() {
        val slow = held.copy(snap = false).stickStep(left(0.5f), 0.1f, ThemeEditorStickTravel())
        val fast = held.copy(snap = false).stickStep(left(1f), 0.1f, ThemeEditorStickTravel())
        assertEquals(2 * (slow.rect().left - 0.3f), fast.rect().left - 0.3f, 0.003f)
    }

    @Test
    fun `the right stick resizes, up is taller, and keeps the resize axis`() {
        val wider = held.stickStep(right(1f), 0.1f, ThemeEditorStickTravel())
        assertEquals(0.23f, wider.rect().width, EPS)
        val taller = held.stickStep(right(0f, -1f), 0.1f, ThemeEditorStickTravel())
        assertEquals(0.13f, taller.rect().height, EPS)
        assertEquals(held.axis, taller.axis)
    }

    @Test
    fun `nothing moves without a held piece`() {
        val browsing = held.dropped()
        assertEquals(browsing, browsing.stickStep(left(1f), 0.1f, ThemeEditorStickTravel()))
    }

    @Test
    fun `each push is one undo step, and putting back removes them all`() {
        JarnThemeEditor.open("mine", held.spec)
        JarnThemeEditor.update { copy(selected = ThemeEditorTarget.Layer("title")) }
        JarnThemeEditor.pickUp()
        val driver = ThemeEditorStickDriver()
        fun push(sticks: JarnSticks, frames: Int) {
            driver.onSticks(sticks)
            repeat(frames) { driver.onFrame(FRAME * (it + 1L) + pushes * 1_000_000_000L) }
            driver.onSticks(JarnSticks())
            driver.onFrame(FRAME * (frames + 2L) + pushes * 1_000_000_000L)
            pushes += 1
        }
        val start = JarnThemeEditor.state!!.spec
        push(left(1f), 6)
        val afterFirst = JarnThemeEditor.state!!.spec
        assertNotEquals(start, afterFirst)
        push(left(0f, 1f), 6)
        assertTrue(JarnThemeEditor.canUndo)

        JarnThemeEditor.cancel()
        assertEquals(start, JarnThemeEditor.state!!.spec)
        assertFalse(JarnThemeEditor.canUndo)
    }

    @Test
    fun `dropping after pushes undoes one push at a time`() {
        JarnThemeEditor.open("mine", held.spec)
        JarnThemeEditor.update { copy(selected = ThemeEditorTarget.Layer("title")) }
        JarnThemeEditor.pickUp()
        val driver = ThemeEditorStickDriver()
        val start = JarnThemeEditor.state!!.spec
        driver.onSticks(left(1f))
        repeat(6) { driver.onFrame(FRAME * (it + 1L)) }
        driver.onSticks(JarnSticks())
        driver.onFrame(FRAME * 8)
        val afterPush = JarnThemeEditor.state!!.spec
        JarnThemeEditor.nudge(0, 1, fresh = true)
        JarnThemeEditor.drop()

        JarnThemeEditor.undo()
        assertEquals(afterPush, JarnThemeEditor.state!!.spec)
        JarnThemeEditor.undo()
        assertEquals(start, JarnThemeEditor.state!!.spec)
    }

    private var pushes = 0L
}
