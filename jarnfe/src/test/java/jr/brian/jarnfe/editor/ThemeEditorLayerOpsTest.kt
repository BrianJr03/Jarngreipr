package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemePrimary
import jr.brian.core.model.theme.ThemeTextRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeEditorLayerOpsTest {
    @Test
    fun `a new piece lands on top and is selected`() {
        val editor = editorWith(textLayer("a", z = 4)).withNewLayer(ThemeEditorNewLayer.Picture)
        val added = editor.viewSpec!!.layers.last()
        assertTrue(added is ThemeLayer.Picture)
        assertEquals(5, added.z)
        assertEquals(ThemeEditorTarget.Layer(added.id), editor.selected)
    }

    @Test
    fun `ids stay unique`() {
        val twice = editorWith().withNewLayer(ThemeEditorNewLayer.Text).withNewLayer(ThemeEditorNewLayer.Text)
        val ids = twice.viewSpec!!.layers.map { it.id }
        assertEquals(ids.distinct(), ids)
    }

    @Test
    fun `duplicate copies, offsets and selects the copy`() {
        val editor = editorWith(textLayer("a")).withDuplicate("a")
        val (original, copy) = editor.viewSpec!!.layers
        assertEquals(original.rect.x + 0.02f, copy.rect.x, 1e-4f)
        assertEquals(ThemeEditorTarget.Layer(copy.id), editor.selected)
    }

    @Test
    fun `delete removes the piece and selects the list`() {
        val editor = editorWith(textLayer("a")).copy(selected = ThemeEditorTarget.Layer("a")).withoutLayer("a")
        assertNull(editor.layer("a"))
        assertEquals(ThemeEditorTarget.Navigator, editor.selected)
    }

    @Test
    fun `order swaps with the neighbor, and the lowest can go under the list`() {
        val editor = editorWith(textLayer("low", z = 1), textLayer("high", z = 2))
        val raised = editor.withLayerOrder("low", 1)
        assertEquals(listOf("high", "low"), raised.viewSpec!!.layers.sortedBy { it.z }.map { it.id })
        assertEquals(0, editor.withLayerOrder("low", -1).layer("low")!!.z)
    }

    @Test
    fun `the list changes type and keeps its box`() {
        val editor = editorWith()
        val box = editor.viewSpec!!.primary.box
        val next = editor.withPrimary { it.nextType() }.viewSpec!!.primary
        assertTrue(next is ThemePrimary.List)
        assertEquals(box, next.box)
    }

    @Test
    fun `any role has an editor size`() {
        assertEquals(ThemeEditorTextSize.M, ThemeTextRole.LabelStrong.editorSize)
        assertEquals(ThemeEditorTextSize.XL, ThemeTextRole.Display.editorSize)
    }
}
