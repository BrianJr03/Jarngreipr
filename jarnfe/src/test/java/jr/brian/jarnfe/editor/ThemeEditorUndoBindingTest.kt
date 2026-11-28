package jr.brian.jarnfe.editor

import jr.brian.jarnfe.R
import jr.brian.jarnfe.input.JarnAction
import jr.brian.jarnfe.input.jarnBindings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeEditorUndoBindingTest {
    @Test
    fun `y stays bound with nothing to undo, so Search never opens`() {
        val bindings = jarnBindings { undoBinding(canUndo = false) }
        assertNotNull(bindings.entries[JarnAction.Search]?.onPress)
        assertNull(bindings.labels[JarnAction.Search])
    }

    @Test
    fun `y shows Undo when there is something to undo`() {
        val bindings = jarnBindings { undoBinding(canUndo = true) }
        assertEquals(R.string.jarnfe_canvas_undo, bindings.labels[JarnAction.Search])
    }
}
