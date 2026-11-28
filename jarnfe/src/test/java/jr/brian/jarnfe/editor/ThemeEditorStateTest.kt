package jr.brian.jarnfe.editor

import jr.brian.core.model.theme.ThemeLayer
import jr.brian.core.model.theme.ThemeRect
import jr.brian.core.model.theme.ThemeText
import jr.brian.core.model.theme.ThemeView
import jr.brian.core.model.theme.ThemeViews
import jr.brian.jarnfe.library.LibraryBuiltInThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

internal fun textLayer(id: String, rect: ThemeRect = ThemeRect(0.1f, 0.1f, 0.2f, 0.1f), z: Int = 1) =
    ThemeLayer.Text(id = id, rect = rect, z = z, content = ThemeText.Static(id))

internal fun editorWith(vararg layers: ThemeLayer): ThemeEditorState {
    val grid = LibraryBuiltInThemes.Grid.spec
    val systems = grid.views.systems!!.copy(layers = layers.toList())
    return ThemeEditorState("mine", grid.copy(views = grid.views.copy(systems = systems)))
}

class ThemeEditorStateTest {
    @Test
    fun `targets list the navigator then layers bottom to top`() {
        val editor = editorWith(textLayer("top", z = 3), textLayer("low", z = 1))
        assertEquals(
            listOf(ThemeEditorTarget.Navigator, ThemeEditorTarget.Layer("low"), ThemeEditorTarget.Layer("top")),
            editor.targets,
        )
    }

    @Test
    fun `selection and tabs wrap around`() {
        val editor = editorWith(textLayer("a"))
        assertEquals(ThemeEditorTarget.Layer("a"), editor.withSelectionStep(-1).selected)
        assertEquals(ThemeEditorTarget.Navigator, editor.withSelectionStep(2).selected)
        assertEquals(ThemeEditorTab.entries.last(), editor.withTab(-1).tab)
    }

    @Test
    fun `switching screens goes back to the navigator`() {
        val editor = editorWith(textLayer("a")).copy(selected = ThemeEditorTarget.Layer("a"))
        assertEquals(ThemeEditorTarget.Navigator, editor.withView(ThemeView.Games).selected)
    }

    @Test
    fun `a theme missing a view gets one to edit`() {
        val grid = LibraryBuiltInThemes.Grid.spec
        val half = grid.copy(views = ThemeViews(systems = grid.views.systems))
        assertNotNull(half.withBothViews(grid).views.games)
    }
}
