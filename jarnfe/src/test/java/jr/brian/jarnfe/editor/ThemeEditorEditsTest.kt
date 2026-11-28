package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val EPS = 1e-4f

class ThemeEditorEditsTest {
    private val title = textLayer("title", ThemeRect(0.3f, 0.3f, 0.2f, 0.1f))
    private val editor = editorWith(title).copy(selected = ThemeEditorTarget.Layer("title"))

    private fun ThemeEditorState.left() = rectOf(selected)!!.edges.left

    @Test
    fun `a nudge moves one grid step`() {
        assertEquals(0.31f, editor.nudged(1, 0).left(), EPS)
    }

    @Test
    fun `a line within range pulls the piece onto it`() {
        val near = editor.withSelectedRect(ThemeRect(0.285f, 0.3f, 0.2f, 0.1f))
        assertEquals("its right edge lands on the screen center",0.3f, near.nudged(1, 0).left(), EPS)
    }

    @Test
    fun `the next nudge leaves the line`() {
        val onCenter = editor.withSelectedRect(ThemeRect(0.4f, 0.3f, 0.2f, 0.1f))
        assertNotEquals(0.4f, onCenter.nudged(1, 0).left(), EPS)
    }

    @Test
    fun `snapping off moves in finer steps off the grid`() {
        assertEquals(0.3025f, editor.copy(snap = false).nudged(1, 0).left(), EPS)
    }

    @Test
    fun `pieces stay on screen`() {
        assertEquals(0f, editor.copy(snap = false).nudged(-1, 0, steps = 1000).left(), EPS)
        assertEquals("a long move stops at the first guide", SAFE_MARGIN, editor.nudged(-1, 0, steps = 100).left(), EPS)
    }

    @Test
    fun `resizing follows the axis and keeps a minimum`() {
        val wider = editor.resized(1)
        assertEquals(0.21f, wider.rectOf(wider.selected)!!.width, EPS)
        val taller = editor.axisToggled().resized(1)
        assertEquals(0.11f, taller.rectOf(taller.selected)!!.height, EPS)
        val tiny = editor.resized(-1, steps = 100)
        assertEquals(MIN_PIECE_SIZE, tiny.rectOf(tiny.selected)!!.width, EPS)
    }

    @Test
    fun `holding speeds up`() {
        assertEquals(1, holdSteps(0))
        assertEquals(2, holdSteps(6))
        assertEquals(4, holdSteps(20))
    }

    @Test
    fun `putting back restores the draft`() {
        val held = editor.pickedUp().nudged(1, 0)
        val back = held.cancelled()
        assertEquals(editor.spec, back.spec)
        assertNull(back.held)
    }

    @Test
    fun `the d-pad jumps to the nearest piece that way`() {
        val right = textLayer("right", ThemeRect(0.7f, 0.3f, 0.1f, 0.1f))
        val below = textLayer("below", ThemeRect(0.3f, 0.6f, 0.1f, 0.1f))
        val start = editorWith(title, right, below).copy(selected = ThemeEditorTarget.Layer("title"))
        assertEquals(ThemeEditorTarget.Layer("right"), start.jumped(1, 0).selected)
        assertEquals(ThemeEditorTarget.Layer("below"), start.jumped(0, 1).selected)
    }

    @Test
    fun `the navigator moves like any piece`() {
        val moved = editorWith().resized(-1)
        assertNotEquals(editorWith().rectOf(ThemeEditorTarget.Navigator), moved.rectOf(ThemeEditorTarget.Navigator))
    }

    @Test
    fun `undo takes a whole hold back, and redo returns it`() {
        JarnThemeEditor.open("mine", editor.spec)
        JarnThemeEditor.update { copy(selected = ThemeEditorTarget.Layer("title")) }
        JarnThemeEditor.pickUp()
        JarnThemeEditor.nudge(1, 0, fresh = true)
        JarnThemeEditor.nudge(1, 0, fresh = false)
        JarnThemeEditor.drop()
        val moved = JarnThemeEditor.state!!.spec
        JarnThemeEditor.undo()
        assertEquals(editor.spec.views.systems, JarnThemeEditor.state!!.spec.views.systems)
        JarnThemeEditor.redo()
        assertEquals(moved, JarnThemeEditor.state!!.spec)
        JarnThemeEditor.leave()
    }
}
